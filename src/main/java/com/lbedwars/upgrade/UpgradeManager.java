package com.lbedwars.upgrade;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.ItemBuilder;
import com.lbedwars.util.MessageUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class UpgradeManager {
   private final LBedWars plugin;
   private final Map<String, Map<String, TeamUpgrade>> modeUpgrades;
   private final Map<String, Map<String, TeamUpgrade>> modeTraps;

   public UpgradeManager(LBedWars plugin) {
      this.plugin = plugin;
      this.modeUpgrades = new HashMap();
      this.modeTraps = new HashMap();
   }

   private Map<String, TeamUpgrade> loadUpgrades(String mode) {
      Map<String, TeamUpgrade> map = new LinkedHashMap();
      ConfigurationSection section = this.plugin.getConfigManager().getUpgradesConfig(mode).getConfigurationSection("upgrades");
      if (section == null) {
         return map;
      } else {
         for(String key : section.getKeys(false)) {
            Material icon = Material.getMaterial(section.getString(key + ".icon", "STONE"));
            int slot = section.getInt(key + ".slot", 0);
            int maxLevel = section.getInt(key + ".max-level", 1);
            List<Integer> costs = new ArrayList();
            List<Map<?, ?>> costsList = section.getMapList(key + ".costs");
            Material currency = Material.DIAMOND;

            for(int ci = 0; ci < costsList.size(); ++ci) {
               Map<?, ?> costMap = (Map)costsList.get(ci);
               costs.add(((Number)costMap.get("amount")).intValue());
               if (ci == 0) {
                  String currStr = (String)costMap.get("currency");
                  if (currStr != null) {
                     Material c = Material.getMaterial(currStr);
                     if (c != null) {
                        currency = c;
                     }
                  }
               }
            }

            List<String> effects = section.getStringList(key + ".effects");
            TeamUpgrade upgrade = new TeamUpgrade(key, (String)null, icon, slot, maxLevel, costs, currency, effects);
            map.put(key, upgrade);
         }

         return map;
      }
   }

   private Map<String, TeamUpgrade> loadTraps(String mode) {
      Map<String, TeamUpgrade> map = new LinkedHashMap();
      ConfigurationSection trapsSection = this.plugin.getConfigManager().getUpgradesConfig(mode).getConfigurationSection("traps");
      if (trapsSection != null) {
         for(String key : trapsSection.getKeys(false)) {
            if (!key.equals("slot") && !key.equals("menu-slot")) {
               Material tIcon = Material.getMaterial(trapsSection.getString(key + ".icon", "TRIPWIRE_HOOK"));
               int tCost = trapsSection.getInt(key + ".cost.amount", 2);
               int tSlot = trapsSection.getInt(key + ".slot", 19);
               List<String> tEffects = trapsSection.getStringList(key + ".effects");
               TeamUpgrade trap = new TeamUpgrade(key, (String)null, tIcon, tSlot, 1, List.of(tCost), Material.DIAMOND, tEffects);
               map.put(key, trap);
            }
         }
      }

      return map;
   }

   public Map<String, TeamUpgrade> getUpgrades(String mode) {
      if (mode == null) {
         mode = "solo";
      }

      return (Map)this.modeUpgrades.computeIfAbsent(mode, this::loadUpgrades);
   }

   public Map<String, TeamUpgrade> getUpgrades(Arena arena) {
      return this.getUpgrades(arena != null ? arena.getModeName() : "solo");
   }

   public Map<String, TeamUpgrade> getTraps(String mode) {
      if (mode == null) {
         mode = "solo";
      }

      return (Map)this.modeTraps.computeIfAbsent(mode, this::loadTraps);
   }

   public Map<String, TeamUpgrade> getTraps(Arena arena) {
      return this.getTraps(arena != null ? arena.getModeName() : "solo");
   }

   public void openUpgradeMenu(Player player, Arena arena) {
      Team team = arena.getTeamByPlayer(player.getUniqueId());
      if (team != null) {
         String mode = arena.getModeName();
         Map<String, TeamUpgrade> upgrades = this.getUpgrades(mode);
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, MessageUtil.get("shop.upgrade-title"));

         for(TeamUpgrade upgrade : upgrades.values()) {
            int currentLevel = team.getUpgradeLevel(upgrade.getId());
            boolean maxed = currentLevel >= upgrade.getMaxLevel();
            String upName = this.getUpgradeName(upgrade);
            ItemStack icon;
            if (maxed) {
               icon = (new ItemBuilder(upgrade.getIcon())).name(MessageUtil.get("upgrades.item-name-maxed", "name", upName)).lore(MessageUtil.get("upgrades.max-level")).build();
            } else {
               int cost = upgrade.getCost(currentLevel);
               icon = (new ItemBuilder(upgrade.getIcon())).name(MessageUtil.get("upgrades.item-name", "name", upName, "level", String.valueOf(currentLevel), "max", String.valueOf(upgrade.getMaxLevel()))).lore(MessageUtil.get("upgrades.price", "price", String.valueOf(cost), "currency", this.getMaterialName(upgrade.getCurrency())), "", MessageUtil.get("upgrades.click-upgrade")).build();
            }

            inv.setItem(upgrade.getSlot(), icon);
         }

         int trapMenuSlot = this.plugin.getConfigManager().getUpgradesConfig(mode).getInt("traps.menu-slot", 25);
         ItemStack trapIcon = (new ItemBuilder(Material.TRIPWIRE_HOOK)).name(MessageUtil.get("upgrades.trap-menu-name")).lore(MessageUtil.get("upgrades.trap-menu-lore")).build();
         inv.setItem(trapMenuSlot, trapIcon);
         player.openInventory(inv);
      }
   }

   public void openTrapMenu(Player player, Arena arena) {
      Team team = arena.getTeamByPlayer(player.getUniqueId());
      if (team != null) {
         String mode = arena.getModeName();
         Map<String, TeamUpgrade> traps = this.getTraps(mode);
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, MessageUtil.get("shop.trap-title"));

         for(Map.Entry<String, TeamUpgrade> entry : traps.entrySet()) {
            TeamUpgrade trap = (TeamUpgrade)entry.getValue();
            boolean hasTrap = team.hasUpgrade("trap_" + (String)entry.getKey());
            String trapName = this.getUpgradeName(trap);
            ItemStack icon;
            if (hasTrap) {
               icon = (new ItemBuilder(trap.getIcon())).name(MessageUtil.get("upgrades.trap-item-name-active", "name", trapName)).lore(MessageUtil.get("upgrades.trap-active")).build();
            } else {
               icon = (new ItemBuilder(trap.getIcon())).name(trapName).lore(MessageUtil.get("upgrades.price", "price", String.valueOf(trap.getCost(0)), "currency", this.getMaterialName(trap.getCurrency())), "", MessageUtil.get("upgrades.click-buy")).build();
            }

            inv.setItem(trap.getSlot(), icon);
         }

         ItemStack back = (new ItemBuilder(Material.ARROW)).name(MessageUtil.get("shop.back")).build();
         inv.setItem(49, back);
         player.openInventory(inv);
      }
   }

   public boolean purchaseUpgrade(Player player, Team team, String upgradeId) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      String mode = arena != null ? arena.getModeName() : "solo";
      Map<String, TeamUpgrade> upgrades = this.getUpgrades(mode);
      TeamUpgrade upgrade = (TeamUpgrade)upgrades.get(upgradeId);
      if (upgrade == null) {
         return false;
      } else {
         int currentLevel = team.getUpgradeLevel(upgradeId);
         if (currentLevel >= upgrade.getMaxLevel()) {
            MessageUtil.sendMessage(player, "upgrades.maxed");
            return false;
         } else {
            int cost = upgrade.getCost(currentLevel);
            if (!this.hasDiamonds(player, cost)) {
               MessageUtil.sendMessage(player, "shop.cannot-afford", "price", String.valueOf(cost), "currency", this.getMaterialName(upgrade.getCurrency()));
               return false;
            } else {
               this.removeDiamonds(player, cost);
               team.setUpgradeLevel(upgradeId, currentLevel + 1);
               this.applyUpgradeEffects(team, upgrade, currentLevel + 1);
               String upName = this.getUpgradeName(upgrade);
               MessageUtil.sendMessage(player, "upgrades.purchased", "upgrade", upName, "level", String.valueOf(currentLevel + 1));

               for(UUID uuid : team.getPlayers()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && !p.equals(player)) {
                     MessageUtil.sendMessage(p, "upgrades.team-purchased", "player", player.getName(), "upgrade", upName, "level", String.valueOf(currentLevel + 1));
                  }
               }

               this.plugin.getGameManager().playConfigSound(player, "sounds.upgrade-buy");
               return true;
            }
         }
      }
   }

   public boolean purchaseTrap(Player player, Team team, String trapId) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      String mode = arena != null ? arena.getModeName() : "solo";
      TeamUpgrade trap = (TeamUpgrade)this.getTraps(mode).get(trapId);
      if (trap == null) {
         return false;
      } else {
         String key = "trap_" + trapId;
         if (team.hasUpgrade(key)) {
            MessageUtil.sendMessage(player, "upgrades.maxed");
            return false;
         } else {
            int cost = trap.getCost(0);
            if (!this.hasDiamonds(player, cost)) {
               MessageUtil.sendMessage(player, "shop.cannot-afford", "price", String.valueOf(cost), "currency", this.getMaterialName(trap.getCurrency()));
               return false;
            } else {
               this.removeDiamonds(player, cost);
               team.setUpgradeLevel(key, 1);
               String trapName = this.getUpgradeName(trap);
               MessageUtil.sendMessage(player, "upgrades.purchased", "upgrade", trapName, "level", "1");

               for(UUID uuid : team.getPlayers()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && !p.equals(player)) {
                     MessageUtil.sendMessage(p, "upgrades.team-purchased", "player", player.getName(), "upgrade", trapName, "level", "1");
                  }
               }

               this.plugin.getGameManager().playConfigSound(player, "sounds.upgrade-buy");
               return true;
            }
         }
      }
   }

   private String getUpgradeName(TeamUpgrade upgrade) {
      String key = "upgrades." + upgrade.getId();
      String name = LBedWars.getInstance().getLanguageManager().getMessage(key);
      if (name != null) {
         return MessageUtil.colorize(name);
      } else {
         return upgrade.getName() != null ? upgrade.getName() : upgrade.getId();
      }
   }

   private void applyUpgradeEffects(Team team, TeamUpgrade upgrade, int level) {
      Arena arena = this.findArenaByTeam(team);
      List<String> effects = upgrade.getEffects();
      if (effects != null && !effects.isEmpty()) {
         if ("heal-pool".equals(upgrade.getId())) {
            this.startHealPool(team, arena);
         } else {
            String effect = level <= effects.size() ? (String)effects.get(level - 1) : (String)effects.get(effects.size() - 1);
            this.applyTeamEffect(team, arena, effect);
         }
      }
   }

   private Arena findArenaByTeam(Team team) {
      for(Arena a : this.plugin.getArenaManager().getAllArenas()) {
         if (a.getTeams().contains(team)) {
            return a;
         }
      }

      return null;
   }

   private void applyTeamEffect(Team team, Arena arena, String effect) {
      String[] parts = effect.split(":");
      if (parts.length >= 1) {
         String type = parts[0].toUpperCase();
         switch (type) {
            case "FORGE":
               if (parts.length >= 2 && arena != null) {
                  try {
                     int level = Integer.parseInt(parts[1]);
                     this.plugin.getGeneratorManager().upgradeAllForgeGenerators(arena, level, team);
                  } catch (NumberFormatException var18) {
                  }
               }
               break;
            case "SHARPNESS":
               if (parts.length >= 2) {
                  try {
                     int level = Integer.parseInt(parts[1]);
                     this.applyEnchantToTeamInventory(team, this.getEnchantment("SHARPNESS"), level);
                  } catch (NumberFormatException var17) {
                  }
               }
               break;
            case "PROTECTION":
               if (parts.length >= 2) {
                  try {
                     int level = Integer.parseInt(parts[1]);
                     this.applyEnchantToTeamInventory(team, this.getEnchantment("PROTECTION"), level);
                  } catch (NumberFormatException var16) {
                  }
               }
               break;
            case "EFFICIENCY":
               if (parts.length >= 2) {
                  try {
                     int level = Integer.parseInt(parts[1]);
                     this.applyEfficiencyToTeamPickaxes(team, level);
                  } catch (NumberFormatException var15) {
                  }
               }
            case "DRAGON_DAMAGE":
               break;
            default:
               PotionEffectType potionType = PotionEffectType.getByName(type);
               if (potionType != null) {
                  int amplifier = parts.length >= 2 ? this.tryParseInt(parts[1], 0) : 0;
                  int duration = parts.length >= 3 ? this.tryParseInt(parts[2], 200) : 200;
                  PotionEffect potion = new PotionEffect(potionType, duration * 20, amplifier, true, false);

                  for(UUID uuid : team.getAlivePlayers()) {
                     Player player = Bukkit.getPlayer(uuid);
                     if (player != null) {
                        player.addPotionEffect(potion);
                     }
                  }
               }
         }

      }
   }

   private void startHealPool(Team team, Arena arena) {
      this.stopHealPool(team);
      Location bedLoc = team.getBedLocation();
      if (bedLoc != null && team.isBedAlive()) {
         int taskId = FoliaUtil.runTaskTimer(() -> {
            if (team.isBedAlive()) {
               Location loc = team.getBedLocation();
               if (loc != null && loc.getWorld() != null) {
                  for(UUID uuid : team.getAlivePlayers()) {
                     Player player = Bukkit.getPlayer(uuid);
                     if (player != null && player.isOnline() && player.getWorld().equals(loc.getWorld()) && !(player.getLocation().distanceSquared(loc) > (double)25.0F)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, false));
                     }
                  }

               }
            }
         }, 0L, 40L);
         team.setHealPoolTaskId(taskId);
         if (bedLoc.getWorld() != null) {
            Location particleLoc = bedLoc.clone().add((double)0.5F, (double)1.0F, (double)0.5F);
            int[] counter = new int[]{0};
            int[] particleTaskId = new int[1];
            particleTaskId[0] = FoliaUtil.runTaskTimer(() -> {
               if (++counter[0] <= 20 && particleLoc.getWorld() != null) {
                  particleLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 10, (double)2.0F, (double)1.0F, (double)2.0F, (double)0.0F);
               } else {
                  FoliaUtil.cancelTask(particleTaskId[0]);
               }
            }, 0L, 10L);
         }

         for(UUID uuid : team.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               MessageUtil.sendMessage(player, "upgrades.heal-pool-activated");
            }
         }

      }
   }

   private void stopHealPool(Team team) {
      int taskId = team.getHealPoolTaskId();
      if (taskId != -1) {
         FoliaUtil.cancelTask(taskId);
         team.setHealPoolTaskId(-1);
      }

   }

   public void stopAllHealPools(Arena arena) {
      for(Team team : arena.getTeams()) {
         this.stopHealPool(team);
      }

   }

   private Enchantment getEnchantment(String name) {
      try {
         Field field = Enchantment.class.getField(name);
         return (Enchantment)field.get((Object)null);
      } catch (Exception var5) {
         try {
            return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
         } catch (Exception var4) {
            return null;
         }
      }
   }

   private void applyEnchantToTeamInventory(Team team, Enchantment enchant, int level) {
      if (enchant != null) {
         for(UUID uuid : team.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               for(int i = 0; i < 36; ++i) {
                  ItemStack item = player.getInventory().getContents()[i];
                  if (item != null && !item.getType().isAir() && this.canEnchant(item, enchant)) {
                     item.addUnsafeEnchantment(enchant, level);
                  }
               }

               ItemStack[] armor = player.getInventory().getArmorContents();

               for(int i = 0; i <= 2; ++i) {
                  if (armor[i] != null && !armor[i].getType().isAir() && this.canEnchant(armor[i], enchant)) {
                     armor[i].addUnsafeEnchantment(enchant, level);
                  }
               }
            }
         }

      }
   }

   private void applyEfficiencyToTeamPickaxes(Team team, int level) {
      Enchantment efficiency = this.getEnchantment("DIG_SPEED");
      if (efficiency != null) {
         for(UUID uuid : team.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               for(ItemStack item : player.getInventory().getContents()) {
                  if (item != null && !item.getType().isAir() && item.getType().name().endsWith("_PICKAXE")) {
                     int existing = item.getEnchantmentLevel(efficiency);
                     if (level > existing) {
                        item.addUnsafeEnchantment(efficiency, level);
                     }
                  }
               }
            }
         }

      }
   }

   private boolean canEnchant(ItemStack item, Enchantment enchant) {
      try {
         return enchant.canEnchantItem(item);
      } catch (Exception var4) {
         return true;
      }
   }

   private int tryParseInt(String s, int defaultValue) {
      try {
         return Integer.parseInt(s);
      } catch (NumberFormatException var4) {
         return defaultValue;
      }
   }

   public boolean checkAndTriggerTrap(Player enemy, Arena arena) {
      for(Team targetTeam : arena.getTeams()) {
         if (!targetTeam.getPlayers().contains(enemy.getUniqueId()) && targetTeam.isBedAlive()) {
            Location bed = targetTeam.getBedLocation();
            if (bed != null && bed.getWorld() != null && bed.getWorld().equals(enemy.getWorld()) && !(enemy.getLocation().distanceSquared(bed) > (double)25.0F)) {
               for(Map.Entry<String, TeamUpgrade> entry : this.getTraps(arena.getModeName()).entrySet()) {
                  String trapKey = "trap_" + (String)entry.getKey();
                  if (targetTeam.hasUpgrade(trapKey)) {
                     TeamUpgrade trap = (TeamUpgrade)entry.getValue();
                     this.applyTrapEffects(enemy, trap);
                     targetTeam.setUpgradeLevel(trapKey, 0);
                     String msg = MessageUtil.get("upgrades.trap-triggered", "player", enemy.getName(), "team", targetTeam.getColoredName());
                     this.plugin.getGameManager().playConfigSound(enemy, "sounds.trap-triggered");
                     MessageUtil.send(enemy, msg);

                     for(UUID uuid : targetTeam.getPlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                           this.plugin.getGameManager().playConfigSound(p, "sounds.trap-triggered");
                           MessageUtil.send(p, msg);
                        }
                     }

                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private void applyTrapEffects(Player player, TeamUpgrade trap) {
      List<String> effects = trap.getEffects();
      if (effects != null) {
         for(String effectStr : effects) {
            String[] parts = effectStr.split(":");
            if (parts.length >= 1) {
               PotionEffectType potionType = PotionEffectType.getByName(parts[0].toUpperCase());
               if (potionType != null) {
                  int amplifier = parts.length >= 2 ? this.tryParseInt(parts[1], 0) : 0;
                  int duration = parts.length >= 3 ? this.tryParseInt(parts[2], 200) : 200;
                  player.addPotionEffect(new PotionEffect(potionType, duration * 20, amplifier, true, false));
               }
            }
         }

      }
   }

   private boolean hasDiamonds(Player player, int amount) {
      int count = 0;

      for(ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.getType() == Material.DIAMOND) {
            count += item.getAmount();
         }
      }

      return count >= amount;
   }

   private void removeDiamonds(Player player, int amount) {
      for(ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.getType() == Material.DIAMOND) {
            int remove = Math.min(amount, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            amount -= remove;
            if (amount <= 0) {
               break;
            }
         }
      }

   }

   public double getDragonDamageMultiplier(String mode, int level) {
      if (mode == null) {
         mode = "solo";
      }

      if (level <= 0) {
         return (double)1.0F;
      } else {
         TeamUpgrade dragonBuff = (TeamUpgrade)this.getUpgrades(mode).get("dragon-buff");
         if (dragonBuff == null) {
            return (double)1.0F;
         } else {
            List<String> effects = dragonBuff.getEffects();
            if (effects != null && !effects.isEmpty()) {
               String effect = level <= effects.size() ? (String)effects.get(level - 1) : (String)effects.get(effects.size() - 1);
               String[] parts = effect.split(":");
               if (parts.length >= 2) {
                  try {
                     return Double.parseDouble(parts[1]);
                  } catch (NumberFormatException var8) {
                  }
               }

               return (double)1.0F;
            } else {
               return (double)1.0F;
            }
         }
      }
   }

   private String getMaterialName(Material material) {
      String key = "material." + material.name();
      String translated = LBedWars.getInstance().getLanguageManager().getMessage(key);
      if (translated != null) {
         return translated;
      } else {
         char var10000 = material.name().charAt(0);
         return var10000 + material.name().substring(1).toLowerCase().replace("_", " ");
      }
   }

   public Map<String, TeamUpgrade> getUpgrades() {
      return this.getUpgrades("solo");
   }

   public Map<String, TeamUpgrade> getTraps() {
      return this.getTraps("solo");
   }
}
