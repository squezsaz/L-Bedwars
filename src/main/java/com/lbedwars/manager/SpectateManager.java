package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.util.MessageUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class SpectateManager implements Listener {
   private final LBedWars plugin;
   private final Map<UUID, Location> previousLocations;
   private final Map<UUID, GameMode> previousGameModes;

   public SpectateManager(LBedWars plugin) {
      this.plugin = plugin;
      this.previousLocations = new HashMap();
      this.previousGameModes = new HashMap();
   }

   public void enableSpectate(Player player, Arena arena) {
      UUID uuid = player.getUniqueId();
      this.previousLocations.put(uuid, player.getLocation());
      this.previousGameModes.put(uuid, player.getGameMode());
      if (!arena.getSpectators().contains(uuid)) {
         arena.getSpectators().add(uuid);
      }

      this.plugin.getArenaManager().registerSpectator(uuid, arena);
      player.setGameMode(GameMode.SPECTATOR);
      if (arena.getSpectatorSpawn() != null) {
         player.teleport(arena.getSpectatorSpawn());
      }

      for(UUID pUuid : arena.getPlayers()) {
         Player p = Bukkit.getPlayer(pUuid);
         if (p != null) {
            p.hidePlayer(this.plugin, player);
         }
      }

      MessageUtil.sendMessage(player, "spectate.enabled");
   }

   public void disableSpectate(Player player) {
      UUID uuid = player.getUniqueId();
      Arena arena = this.plugin.getArenaManager().getArenaBySpectator(uuid);
      if (arena != null) {
         arena.getSpectators().remove(uuid);
         this.plugin.getArenaManager().unregisterSpectator(uuid);

         for(UUID pUuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(pUuid);
            if (p != null) {
               p.showPlayer(this.plugin, player);
            }
         }
      }

      Location prevLoc = (Location)this.previousLocations.remove(uuid);
      GameMode prevGm = (GameMode)this.previousGameModes.remove(uuid);
      if (prevGm != null) {
         player.setGameMode(prevGm);
      } else {
         player.setGameMode(GameMode.SURVIVAL);
      }

      if (prevLoc != null) {
         player.teleport(prevLoc);
      } else {
         Location mainLobby = this.plugin.getMainLobby();
         player.teleport(mainLobby != null ? mainLobby : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
      }

      MessageUtil.sendMessage(player, "spectate.disabled");
   }

   public boolean isSpectating(UUID uuid) {
      return this.previousLocations.containsKey(uuid) || this.plugin.getArenaManager().getArenaBySpectator(uuid) != null;
   }

   public void openSpectatorGUI(Player player, Arena arena) {
      String title = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.gui-title"));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, title);
      int slot = 0;

      for(Team team : arena.getTeams()) {
         for(UUID uuid : team.getAlivePlayers()) {
            if (slot >= 54) {
               break;
            }

            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
               ItemStack head = new ItemStack(Material.PLAYER_HEAD);
               SkullMeta meta = (SkullMeta)head.getItemMeta();
               String var10001 = String.valueOf(team.getColor());
               meta.setDisplayName(var10001 + target.getName());
               meta.setOwningPlayer(target);
               meta.setLore(List.of(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.gui-click-teleport"))));
               head.setItemMeta(meta);
               inv.setItem(slot++, head);
            }
         }
      }

      if (slot == 0) {
         ItemStack placeholder = new ItemStack(Material.BARRIER);
         ItemMeta pMeta = placeholder.getItemMeta();
         pMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.gui-no-players")));
         placeholder.setItemMeta(pMeta);
         inv.setItem(22, placeholder);
      }

      player.openInventory(inv);
   }

   @EventHandler
   public void onSpectatorGUIClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
               String title = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.gui-title"));
               if (event.getView().getTitle().equals(title)) {
                  event.setCancelled(true);
                  ItemStack head = event.getCurrentItem();
                  SkullMeta meta = (SkullMeta)head.getItemMeta();
                  Player target = Bukkit.getPlayer(meta.getOwningPlayer() != null ? meta.getOwningPlayer().getUniqueId() : null);
                  if (target != null && target.isOnline()) {
                     player.teleport(target.getLocation());
                     MessageUtil.sendMessage(player, "spectate.controls");
                  }

                  player.closeInventory();
               }

            }
         }
      }
   }
}
