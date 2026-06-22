package com.lbedwars.generator;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Generator {
   private final GeneratorType type;
   private final Location location;
   private final Arena arena;
   private int tier;
   private int taskId;
   private final List<ArmorStand> hologramLines;
   private int currentDelay;
   private boolean running;
   private boolean hologramEnabled;
   private long lastSpawnTime;
   private int hologramTaskId;
   private String ownerTeam;

   public Generator(GeneratorType type, Location location, Arena arena) {
      this.type = type;
      this.location = location.clone();
      this.arena = arena;
      this.tier = 1;
      this.taskId = -1;
      this.running = false;
      this.hologramEnabled = true;
      this.ownerTeam = null;
      this.currentDelay = this.getBaseDelay();
      this.lastSpawnTime = System.currentTimeMillis();
      this.hologramTaskId = -1;
      this.hologramLines = new ArrayList();
   }

   public void start() {
      if (!this.running) {
         this.running = true;
         this.lastSpawnTime = System.currentTimeMillis();
         if (this.hologramEnabled) {
            this.spawnHologram();
            this.hologramTaskId = FoliaUtil.runTaskTimer(this::updateHologram, 20L, 20L);
         }

         this.scheduleSpawn();
      }
   }

   public void stop() {
      this.running = false;
      if (this.hologramTaskId != -1) {
         FoliaUtil.cancelTask(this.hologramTaskId);
         this.hologramTaskId = -1;
      }

      for(ArmorStand stand : this.hologramLines) {
         stand.remove();
      }

      this.hologramLines.clear();
   }

   private void scheduleSpawn() {
      if (this.running) {
         FoliaUtil.runTaskLater(() -> {
            if (this.running) {
               if (this.trySpawnItem()) {
                  this.lastSpawnTime = System.currentTimeMillis();
                  this.scheduleSpawn();
               } else {
                  this.scheduleMaxStackPoll();
               }

            }
         }, (long)this.currentDelay * 20L);
      }
   }

   private void scheduleMaxStackPoll() {
      if (this.running) {
         FoliaUtil.runTaskLater(() -> {
            if (this.running) {
               if (this.trySpawnItem()) {
                  this.lastSpawnTime = System.currentTimeMillis();
                  this.scheduleSpawn();
               } else {
                  this.scheduleMaxStackPoll();
               }

            }
         }, 20L);
      }
   }

   private boolean trySpawnItem() {
      if (this.running && this.location.getWorld() != null) {
         ItemStack item = this.getDropItem();
         int maxStack = this.getMaxStack();
         if (maxStack > 0) {
            int count = 0;

            for(Entity e : this.location.getWorld().getNearbyEntities(this.location, (double)2.0F, (double)2.0F, (double)2.0F)) {
               if (e instanceof Item) {
                  Item ent = (Item)e;
                  if (ent.getItemStack().getType() == item.getType()) {
                     count += ent.getItemStack().getAmount();
                     if (count >= maxStack) {
                        return false;
                     }
                  }
               }
            }
         }

         World world = this.location.getWorld();
         Item drop = world.dropItem(this.location, item);
         drop.setVelocity(new Vector(0, 0, 0));
         drop.setPickupDelay(0);
         return true;
      } else {
         return false;
      }
   }

   private ItemStack getDropItem() {
      int amount = this.getSpawnAmount();
      ItemStack var10000;
      switch (this.type) {
         case IRON -> var10000 = new ItemStack(Material.IRON_INGOT, amount);
         case GOLD -> var10000 = new ItemStack(Material.GOLD_INGOT, amount);
         case DIAMOND -> var10000 = (new ItemBuilder(Material.DIAMOND, amount)).name("&bElmas").build();
         case EMERALD -> var10000 = (new ItemBuilder(Material.EMERALD, amount)).name("&aZümrüt").build();
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public int getMaxStack() {
      FileConfiguration config = LBedWars.getInstance().getConfig();
      int var10000;
      switch (this.type) {
         case IRON -> var10000 = config.getInt("generators.max-stack.iron", 64);
         case GOLD -> var10000 = config.getInt("generators.max-stack.gold", 32);
         case DIAMOND -> var10000 = config.getInt("generators.max-stack.diamond", 8);
         case EMERALD -> var10000 = config.getInt("generators.max-stack.emerald", 4);
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public int getBaseDelay() {
      String mode = this.arena.getModeName();
      FileConfiguration config = LBedWars.getInstance().getConfig();
      int var10000;
      switch (this.type) {
         case IRON:
            if (this.tier >= 1 && this.tier <= 4) {
               String forgePath = "forge.iron." + mode + ".level-" + this.tier;
               if (config.contains(forgePath)) {
                  var10000 = config.isConfigurationSection(forgePath) ? config.getInt(forgePath + ".delay") : config.getInt(forgePath);
                  break;
               }
            }

            int base = config.getInt("generator-delays.iron." + mode, 24);
            if (this.tier == 1) {
               var10000 = base;
            } else {
               int reduction = config.getInt("generators.team-generator.upgrade-iron-reduction", 4);
               var10000 = this.tier == 2 ? base - reduction : (this.tier == 3 ? base - reduction * 2 : (this.tier == 4 ? base - reduction * 3 : 8));
            }
            break;
         case GOLD:
            if (this.tier >= 1 && this.tier <= 4) {
               String forgePath = "forge.gold." + mode + ".level-" + this.tier;
               if (config.contains(forgePath)) {
                  var10000 = config.isConfigurationSection(forgePath) ? config.getInt(forgePath + ".delay") : config.getInt(forgePath);
                  break;
               }
            }

            int goldBase = config.getInt("generator-delays.gold." + mode, 8);
            if (this.tier == 1) {
               var10000 = goldBase;
            } else {
               int reduction = config.getInt("generators.team-generator.upgrade-gold-reduction", 1);
               var10000 = this.tier == 2 ? goldBase - reduction : (this.tier == 3 ? goldBase - reduction * 2 : (this.tier == 4 ? goldBase - reduction * 3 : Math.max(goldBase - reduction * 4, 2)));
            }
            break;
         case DIAMOND:
            if (this.tier >= 1) {
               String forgePath = "forge.diamond." + mode + ".level-" + this.tier;
               if (config.contains(forgePath)) {
                  var10000 = config.isConfigurationSection(forgePath) ? config.getInt(forgePath + ".delay") : config.getInt(forgePath);
                  break;
               }
            }

            int diamondBase = config.getInt("generator-delays.diamond." + mode, 30);
            var10000 = this.tier == 1 ? diamondBase : Math.max(diamondBase - this.tier * 5, 5);
            break;
         case EMERALD:
            if (this.tier >= 1) {
               String forgePath = "forge.emerald." + mode + ".level-" + this.tier;
               if (config.contains(forgePath)) {
                  var10000 = config.isConfigurationSection(forgePath) ? config.getInt(forgePath + ".delay") : config.getInt(forgePath);
                  break;
               }
            }

            int emeraldBase = config.getInt("generator-delays.emerald." + mode, 60);
            var10000 = this.tier == 1 ? emeraldBase : Math.max(emeraldBase - this.tier * 5, 5);
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public int getSpawnAmount() {
      String mode = this.arena.getModeName();
      FileConfiguration config = LBedWars.getInstance().getConfig();
      switch (this.type) {
         case IRON:
         case GOLD:
         case DIAMOND:
         case EMERALD:
            String forgePath = "forge." + this.type.name().toLowerCase() + "." + mode + ".level-" + this.tier;
            return config.contains(forgePath) && config.isConfigurationSection(forgePath) ? config.getInt(forgePath + ".amount", 1) : 1;
         default:
            throw new IncompatibleClassChangeError();
      }
   }

   public void autoUpgrade() {
      if (this.type == GeneratorType.DIAMOND || this.type == GeneratorType.EMERALD) {
         ++this.tier;
         this.currentDelay = this.getBaseDelay();
         this.updateHologram();
      }

   }

   public void upgradeForge(int level) {
      if (this.type == GeneratorType.IRON || this.type == GeneratorType.GOLD) {
         this.tier = level;
         this.currentDelay = this.getBaseDelay();
         this.updateHologram();
      }

   }

   private void spawnHologram() {
      if (this.location.getWorld() != null) {
         this.removeHologramEntities();
         this.hologramLines.clear();
         String[] lines = this.getHologramLines(this.isAtMaxStack());
         double yOffset = (double)2.0F + (double)(lines.length - 1) * 0.3;

         for(String text : lines) {
            Location lineLoc = this.location.clone().add((double)0.0F, yOffset, (double)0.0F);
            ArmorStand stand = (ArmorStand)this.location.getWorld().spawn(lineLoc, ArmorStand.class);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(text);
            this.hologramLines.add(stand);
            yOffset -= 0.3;
         }

      }
   }

   public void removeHologramEntities() {
      if (this.location.getWorld() != null) {
         this.location.getWorld().getNearbyEntities(this.location, (double)1.0F, (double)3.0F, (double)1.0F).stream().filter((e) -> e instanceof ArmorStand).map((e) -> (ArmorStand)e).filter((stand) -> !stand.isVisible() && stand.isMarker() && stand.isSmall()).forEach(Entity::remove);
      }
   }

   private void updateHologram() {
      boolean atMax = this.isAtMaxStack();
      String[] lines = this.getHologramLines(atMax);
      int needed = lines.length;

      int current;
      for(current = this.hologramLines.size(); current > needed; --current) {
         ArmorStand stand = (ArmorStand)this.hologramLines.remove(this.hologramLines.size() - 1);
         stand.remove();
      }

      while(current < needed) {
         double yOffset = (double)2.0F + (double)(needed - 1 - current) * 0.3;
         Location lineLoc = this.location.clone().add((double)0.0F, yOffset, (double)0.0F);
         ArmorStand stand = (ArmorStand)this.location.getWorld().spawn(lineLoc, ArmorStand.class);
         stand.setVisible(false);
         stand.setMarker(true);
         stand.setGravity(false);
         stand.setSmall(true);
         stand.setCustomNameVisible(true);
         this.hologramLines.add(stand);
         ++current;
      }

      for(int i = 0; i < lines.length && i < this.hologramLines.size(); ++i) {
         ((ArmorStand)this.hologramLines.get(i)).setCustomName(lines[i]);
      }

      if (atMax && this.getItemCount() < this.getMaxStack()) {
         this.lastSpawnTime = System.currentTimeMillis();
      }

   }

   private boolean isAtMaxStack() {
      int maxStack = this.getMaxStack();
      if (maxStack <= 0) {
         return false;
      } else {
         return this.getItemCount() >= maxStack;
      }
   }

   private int getItemCount() {
      if (this.location.getWorld() == null) {
         return 0;
      } else {
         ItemStack item = this.getDropItem();
         int count = 0;

         for(Entity e : this.location.getWorld().getNearbyEntities(this.location, (double)2.0F, (double)2.0F, (double)2.0F)) {
            if (e instanceof Item) {
               Item ent = (Item)e;
               if (ent.getItemStack().getType() == item.getType() && ent.getLocation().distanceSquared(this.location) < (double)4.0F) {
                  count += ent.getItemStack().getAmount();
               }
            }
         }

         return count;
      }
   }

   private String[] getHologramLines(boolean atMax) {
      String tierStr = LBedWars.getInstance().getLanguageManager().getMessage("generators.level");
      if (tierStr == null) {
         tierStr = "&7Tier {level}";
      }

      tierStr = ChatColor.translateAlternateColorCodes('&', tierStr.replace("{level}", String.valueOf(this.tier)));
      String var10000;
      switch (this.type) {
         case IRON -> var10000 = "§7";
         case GOLD -> var10000 = "§6";
         case DIAMOND -> var10000 = "§b";
         case EMERALD -> var10000 = "§a";
         default -> throw new IncompatibleClassChangeError();
      }

      String color = var10000;
      switch (this.type) {
         case IRON -> var10000 = "generators.hologram-iron";
         case GOLD -> var10000 = "generators.hologram-gold";
         case DIAMOND -> var10000 = "generators.hologram-diamond";
         case EMERALD -> var10000 = "generators.hologram-emerald";
         default -> throw new IncompatibleClassChangeError();
      }

      String nameKey = var10000;
      String name = LBedWars.getInstance().getLanguageManager().getMessage(nameKey);
      if (name == null) {
         switch (this.type) {
            case IRON -> var10000 = "Iron";
            case GOLD -> var10000 = "Gold";
            case DIAMOND -> var10000 = "Diamond";
            case EMERALD -> var10000 = "Emerald";
            default -> throw new IncompatibleClassChangeError();
         }

         name = var10000;
      }

      if (atMax) {
         String maxStr = LBedWars.getInstance().getLanguageManager().getMessage("generators.max-stack");
         if (maxStr == null) {
            maxStr = "&cMaksimum Eşya!";
         }

         maxStr = ChatColor.translateAlternateColorCodes('&', maxStr);
         return new String[]{color + "§l" + name, tierStr, maxStr};
      } else {
         long elapsed = (System.currentTimeMillis() - this.lastSpawnTime) / 1000L;
         long remaining = (long)this.currentDelay - elapsed;
         if (remaining < 0L) {
            remaining = 0L;
         }

         String countdownStr = LBedWars.getInstance().getLanguageManager().getMessage("generators.countdown");
         if (countdownStr == null) {
            countdownStr = "&7Next item: {time}s";
         }

         countdownStr = countdownStr.replace("{time}", "§e" + remaining + "§7");
         countdownStr = ChatColor.translateAlternateColorCodes('&', countdownStr);
         return new String[]{color + "§l" + name, tierStr, countdownStr};
      }
   }

   public GeneratorType getType() {
      return this.type;
   }

   public Location getLocation() {
      return this.location;
   }

   public Arena getArena() {
      return this.arena;
   }

   public int getTier() {
      return this.tier;
   }

   public void setTier(int tier) {
      this.tier = tier;
      this.currentDelay = this.getBaseDelay();
   }

   public boolean isRunning() {
      return this.running;
   }

   public int getCurrentDelay() {
      return this.currentDelay;
   }

   public boolean isHologramEnabled() {
      return this.hologramEnabled;
   }

   public void setHologramEnabled(boolean hologramEnabled) {
      this.hologramEnabled = hologramEnabled;
   }

   public String getOwnerTeam() {
      return this.ownerTeam;
   }

   public void setOwnerTeam(String ownerTeam) {
      this.ownerTeam = ownerTeam;
   }
}
