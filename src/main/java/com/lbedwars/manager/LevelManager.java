package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.util.MessageUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LevelManager {
   private final LBedWars plugin;
   private final Map<Integer, Integer> levelRequirements;

   public LevelManager(LBedWars plugin) {
      this.plugin = plugin;
      this.levelRequirements = new HashMap();
      this.initLevels();
   }

   private void initLevels() {
      int xp = 100;

      for(int i = 1; i <= 100; ++i) {
         this.levelRequirements.put(i, xp);
         xp = (int)((double)xp * 1.15);
      }

   }

   public int getLevel(UUID uuid) {
      return !this.plugin.getConfig().getBoolean("levels.enabled", true) ? 1 : this.plugin.getStatsCache().getStats(uuid, "level");
   }

   public int getXp(UUID uuid) {
      return this.plugin.getStatsCache().getStats(uuid, "xp");
   }

   public void addXp(UUID uuid, int amount) {
      if (this.plugin.getConfig().getBoolean("levels.enabled", true)) {
         int currentXp = this.getXp(uuid);
         int currentLevel = this.getLevel(uuid);
         int newXp = currentXp + amount;
         int newLevel = currentLevel;
         int required = this.getRequiredXp(currentLevel);

         while(newXp >= required) {
            newXp -= required;
            ++newLevel;
            required = this.getRequiredXp(newLevel);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               MessageUtil.sendMessage(player, "level.up", "level", String.valueOf(newLevel));
            }
         }

         this.plugin.getStatsCache().setStats(uuid, "xp", newXp);
         this.plugin.getStatsCache().setStats(uuid, "level", newLevel);
      }
   }

   public int getRequiredXp(int level) {
      return (Integer)this.levelRequirements.getOrDefault(level, 10000);
   }

   public void addKillXp(UUID uuid) {
      int xp = this.plugin.getConfig().getInt("levels.xp-per-kill", 10);
      this.addXp(uuid, xp);
   }

   public void addWinXp(UUID uuid) {
      int xp = this.plugin.getConfig().getInt("levels.xp-per-win", 50);
      this.addXp(uuid, xp);
   }

   public void addBedBreakXp(UUID uuid) {
      int xp = this.plugin.getConfig().getInt("levels.xp-per-bed-break", 25);
      this.addXp(uuid, xp);
   }

   public void addFinalKillXp(UUID uuid) {
      int xp = this.plugin.getConfig().getInt("levels.xp-per-final-kill", 15);
      this.addXp(uuid, xp);
   }
}
