package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.MessageUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class CompassTracker {
   private final LBedWars plugin;
   private final Map<UUID, Set<UUID>> ownedPlayers;
   private final Map<UUID, UUID> activeTarget;
   private int taskId;


   public CompassTracker(LBedWars plugin) {
      this.plugin = plugin;
      this.ownedPlayers = new HashMap<>();
      this.activeTarget = new HashMap<>();
      this.taskId = -1;
   }

   public boolean isEnabled() {
      return this.plugin.getConfig().getBoolean("compass-tracker.enabled", true);
   }

   public boolean isCompassItem(ItemStack item) {
      if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
      String name = MessageUtil.get("compass-tracker.name");
      if (name == null || name.isEmpty()) return false;
      String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
      String expected = ChatColor.stripColor(name);
      return stripped.equals(expected);
   }

   public boolean isGuiTitle(String title) {
      String expected = MessageUtil.get("compass-tracker.gui-title");
      if (expected == null) return false;
      String expectedStripped = ChatColor.stripColor(MessageUtil.colorize(expected));
      String stripped = ChatColor.stripColor(title);
      return stripped.equals(expectedStripped);
   }

   public boolean canTrack(Player player) {
      if (!isEnabled()) return false;
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena == null || arena.getState() != ArenaState.PLAYING) return false;
      long aliveTeams = arena.getTeams().stream().filter(t -> !t.isEliminated()).count();
      int minTeams = this.plugin.getConfig().getInt("compass-tracker.min-teams", 2);
      return aliveTeams <= minTeams && aliveTeams > 0;
   }

   public boolean isOwned(UUID tracker, UUID target) {
      Set<UUID> set = this.ownedPlayers.get(tracker);
      return set != null && set.contains(target);
   }

   public UUID getActiveTarget(UUID tracker) {
      return this.activeTarget.get(tracker);
   }

   public void handleGuiClick(Player player, int slot) {
      if (!canTrack(player)) {
         player.closeInventory();
         MessageUtil.sendMessage(player, "compass-tracker.not-available", "min-teams", String.valueOf(this.plugin.getConfig().getInt("compass-tracker.min-teams", 2)));
         return;
      }
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena == null) return;
      Team playerTeam = arena.getTeamByPlayer(player.getUniqueId());
      if (playerTeam == null) return;

      List<Player> enemies = getAliveEnemies(player, arena, playerTeam);
      if (slot < 0 || slot >= enemies.size()) return;

      Player target = enemies.get(slot);
      UUID trackerUUID = player.getUniqueId();
      UUID targetUUID = target.getUniqueId();
      UUID currentActive = this.activeTarget.get(trackerUUID);

      if (targetUUID.equals(currentActive)) {
         this.activeTarget.remove(trackerUUID);
         MessageUtil.sendMessage(player, "compass-tracker.deselected", "player", target.getName());
         if (this.activeTarget.isEmpty() && this.ownedPlayers.isEmpty()) stopTask();
      } else if (isOwned(trackerUUID, targetUUID)) {
         this.activeTarget.put(trackerUUID, targetUUID);
         MessageUtil.sendMessage(player, "compass-tracker.selected", "player", target.getName());
         startTask();
      } else {
         Material currency = getCurrency();
         int cost = getCost();
         if (!hasEnough(player, currency, cost)) {
            MessageUtil.sendMessage(player, "compass-tracker.cannot-afford", "cost", String.valueOf(cost), "currency", getCurrencyName(currency));
            openTrackerGUI(player);
            return;
         }
         removeItems(player, currency, cost);
         this.ownedPlayers.computeIfAbsent(trackerUUID, k -> new HashSet<>()).add(targetUUID);
         this.activeTarget.put(trackerUUID, targetUUID);
         MessageUtil.sendMessage(player, "compass-tracker.tracking-started", "player", target.getName());
         startTask();
      }
      openTrackerGUI(player);
   }

   public void openTrackerGUI(Player player) {
      if (!isEnabled() || !canTrack(player)) {
         MessageUtil.sendMessage(player, "compass-tracker.not-available", "min-teams", String.valueOf(this.plugin.getConfig().getInt("compass-tracker.min-teams", 2)));
         return;
      }
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena == null) return;
      Team playerTeam = arena.getTeamByPlayer(player.getUniqueId());
      if (playerTeam == null) return;

      List<Player> enemies = getAliveEnemies(player, arena, playerTeam);
      if (enemies.isEmpty()) {
         MessageUtil.sendMessage(player, "compass-tracker.no-enemies");
         return;
      }

      int size = Math.max(9, ((enemies.size() / 9) + 1) * 9);
      String titleRaw = MessageUtil.get("compass-tracker.gui-title");
      String title = MessageUtil.colorize(titleRaw != null ? titleRaw : "&8Tracker");
      Inventory inv = Bukkit.createInventory(null, size, title);

      UUID trackerUUID = player.getUniqueId();
      Set<UUID> owned = this.ownedPlayers.getOrDefault(trackerUUID, new HashSet<>());
      UUID active = this.activeTarget.get(trackerUUID);

      for (int i = 0; i < enemies.size(); i++) {
         Player enemy = enemies.get(i);
         ItemStack item = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta) item.getItemMeta();
         meta.setOwningPlayer(enemy);
         meta.setDisplayName(MessageUtil.colorize("&c" + enemy.getName()));

         List<String> lore = new ArrayList<>();
         Team enemyTeam = arena.getTeamByPlayer(enemy.getUniqueId());
         if (enemyTeam != null) {
            lore.add(MessageUtil.colorize(enemyTeam.getColor() + enemyTeam.getName()));
         }
         int dist = (int) player.getLocation().distance(enemy.getLocation());
         lore.add(MessageUtil.colorize("&7" + MessageUtil.get("compass-tracker.gui-distance", "distance", String.valueOf(dist))));

         UUID enemyUUID = enemy.getUniqueId();
         if (enemyUUID.equals(active)) {
            lore.add(MessageUtil.colorize("&a\u25c9 " + MessageUtil.get("compass-tracker.gui-active")));
            lore.add(MessageUtil.colorize("&c" + MessageUtil.get("compass-tracker.gui-deselect")));
         } else if (owned.contains(enemyUUID)) {
            lore.add(MessageUtil.colorize("&e\u25cb " + MessageUtil.get("compass-tracker.gui-owned")));
            lore.add(MessageUtil.colorize("&e" + MessageUtil.get("compass-tracker.gui-select")));
         } else {
            lore.add(MessageUtil.colorize("&7" + MessageUtil.get("compass-tracker.gui-cost", "cost", String.valueOf(getCost()), "currency", getCurrencyName(getCurrency()))));
            lore.add(MessageUtil.colorize("&e" + MessageUtil.get("compass-tracker.gui-click")));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
         inv.setItem(i, item);
      }

      player.openInventory(inv);
   }

   private List<Player> getAliveEnemies(Player player, Arena arena, Team playerTeam) {
      List<Player> enemies = new ArrayList<>();
      for (UUID uuid : arena.getPlayers()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p == null) continue;
         Team t = arena.getTeamByPlayer(uuid);
         if (t != null && !t.equals(playerTeam) && t.isAlive(uuid)) {
            enemies.add(p);
         }
      }
      return enemies;
   }

   public void removeTracker(UUID trackerUUID) {
      this.ownedPlayers.remove(trackerUUID);
      this.activeTarget.remove(trackerUUID);
   }

   public void removeTarget(UUID targetUUID) {
      for (Set<UUID> set : this.ownedPlayers.values()) set.remove(targetUUID);
      this.activeTarget.values().remove(targetUUID);
   }

   public void cleanup(UUID uuid) {
      removeTracker(uuid);
      removeTarget(uuid);
   }

   public void clear() {
      this.ownedPlayers.clear();
      this.activeTarget.clear();
      stopTask();
   }

   public void giveCompass(Player player) {
      ItemStack compass = new ItemStack(Material.COMPASS);
      ItemMeta meta = compass.getItemMeta();
      String name = MessageUtil.get("compass-tracker.name");
      meta.setDisplayName(MessageUtil.colorize(name != null ? name : "&a&lTracker Compass"));
      meta.setUnbreakable(true);
      compass.setItemMeta(meta);
      player.getInventory().setItem(8, compass);
   }

   private void startTask() {
      if (this.taskId != -1) return;
      int interval = this.plugin.getConfig().getInt("compass-tracker.update-interval", 10);
      this.taskId = FoliaUtil.runTaskTimer(() -> {
         boolean hasAny = false;
         for (Map.Entry<UUID, UUID> entry : new HashMap<>(this.activeTarget).entrySet()) {
            UUID trackerUUID = entry.getKey();
            UUID targetUUID = entry.getValue();
            Player tracker = Bukkit.getPlayer(trackerUUID);
            if (tracker == null || !tracker.isOnline()) {
               this.activeTarget.remove(trackerUUID);
               continue;
            }
            Arena arena = this.plugin.getArenaManager().getArenaByPlayer(trackerUUID);
            if (arena == null || arena.getState() != ArenaState.PLAYING || !canTrack(tracker)) {
               this.activeTarget.remove(trackerUUID);
               continue;
            }
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline() || !arena.hasPlayer(targetUUID)) {
               removeTarget(targetUUID);
               continue;
            }
            Team targetTeam = arena.getTeamByPlayer(targetUUID);
            if (targetTeam == null || targetTeam.isEliminated()) {
               removeTarget(targetUUID);
               continue;
            }
            String arrow = getDirectionArrow(tracker.getLocation(), target.getLocation());
            String msg = arrow + " " + target.getName() + " (" + (int)tracker.getLocation().distance(target.getLocation()) + "m)";
            tracker.sendActionBar(MessageUtil.colorize(msg));
            hasAny = true;
         }
         if (!hasAny) stopTask();
      }, 0L, interval);
   }

   private void stopTask() {
      if (this.taskId != -1) {
         FoliaUtil.cancelTask(this.taskId);
         this.taskId = -1;
      }
   }

   private String getDirectionArrow(Location from, Location to) {
      double dx = to.getX() - from.getX();
      double dz = to.getZ() - from.getZ();
      double angle = Math.atan2(dz, dx);
      double yaw = Math.toRadians(from.getYaw());
      double diff = angle - yaw;
      while (diff < -Math.PI) diff += 2 * Math.PI;
      while (diff > Math.PI) diff -= 2 * Math.PI;
      if (diff > -Math.PI / 8 && diff <= Math.PI / 8) return "\u2191";
      if (diff > Math.PI / 8 && diff <= 3 * Math.PI / 8) return "\u2197";
      if (diff > 3 * Math.PI / 8 && diff <= 5 * Math.PI / 8) return "\u2192";
      if (diff > 5 * Math.PI / 8 && diff <= 7 * Math.PI / 8) return "\u2198";
      if (diff > 7 * Math.PI / 8 || diff <= -7 * Math.PI / 8) return "\u2193";
      if (diff > -7 * Math.PI / 8 && diff <= -5 * Math.PI / 8) return "\u2199";
      if (diff > -5 * Math.PI / 8 && diff <= -3 * Math.PI / 8) return "\u2190";
      return "\u2196";
   }

   private int getCost() {
      return this.plugin.getConfig().getInt("compass-tracker.cost", 2);
   }

   private Material getCurrency() {
      String curr = this.plugin.getConfig().getString("compass-tracker.currency", "EMERALD");
      try {
         return Material.valueOf(curr);
      } catch (IllegalArgumentException e) {
         return Material.EMERALD;
      }
   }

   private String getCurrencyName(Material mat) {
      String lang = MessageUtil.get("material." + mat.name());
      if (lang != null && !lang.isEmpty()) return lang;
      String name = mat.name().toLowerCase().replace("_", " ");
      return name.substring(0, 1).toUpperCase() + name.substring(1);
   }

   private boolean hasEnough(Player player, Material material, int amount) {
      int count = 0;
      for (ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.getType() == material) count += item.getAmount();
      }
      return count >= amount;
   }

   private void removeItems(Player player, Material material, int amount) {
      int remaining = amount;
      for (ItemStack item : player.getInventory().getContents()) {
         if (remaining <= 0) break;
         if (item != null && item.getType() == material) {
            int toRemove = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - toRemove);
            remaining -= toRemove;
          }
       }
   }
}
