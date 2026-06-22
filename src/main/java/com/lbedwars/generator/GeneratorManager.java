package com.lbedwars.generator;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.util.FoliaUtil;
import java.util.HashMap;
import java.util.Map;

public class GeneratorManager {
   private final LBedWars plugin;
   private final Map<String, Integer> tasks;

   public GeneratorManager(LBedWars plugin) {
      this.plugin = plugin;
      this.tasks = new HashMap();
      this.startGlobalTasks();
   }

   private void startGlobalTasks() {
      FoliaUtil.runTaskTimer(() -> {
         for(Arena arena : this.plugin.getArenaManager().getAllArenas()) {
            if (arena.getState() == ArenaState.PLAYING) {
               arena.setGameTime(arena.getGameTime() + 1);
               int[] diamondUpgradeTimes = new int[]{300, 600, 900};
               int[] emeraldUpgradeTimes = new int[]{360, 720, 1080};

               for(int i = 0; i < diamondUpgradeTimes.length; ++i) {
                  if (arena.getGameTime() == diamondUpgradeTimes[i]) {
                     for(Generator gen : arena.getGenerators()) {
                        if (gen.getType() == GeneratorType.DIAMOND) {
                           gen.autoUpgrade();
                        }
                     }

                     String level = String.valueOf(i + 2);
                     arena.broadcast("events.diamond-upgrade", "level", level);
                     arena.broadcastActionBar("actionbar.diamond-upgrade", "level", level);
                  }
               }

               for(int i = 0; i < emeraldUpgradeTimes.length; ++i) {
                  if (arena.getGameTime() == emeraldUpgradeTimes[i]) {
                     for(Generator gen : arena.getGenerators()) {
                        if (gen.getType() == GeneratorType.EMERALD) {
                           gen.autoUpgrade();
                        }
                     }

                     String level = String.valueOf(i + 2);
                     arena.broadcast("events.emerald-upgrade", "level", level);
                     arena.broadcastActionBar("actionbar.emerald-upgrade", "level", level);
                  }
               }

               if (!arena.hasDragonsSpawned()) {
                  int dragonSpawnTime = this.plugin.getConfig().getInt("dragon-spawn-times." + arena.getModeName(), 600);
                  if (arena.getGameTime() >= dragonSpawnTime) {
                     this.plugin.getGameManager().spawnDragons(arena);
                  }
               }
            }
         }

      }, 20L, 20L);
   }

   public void startArenaGenerators(Arena arena) {
      for(Generator gen : arena.getGenerators()) {
         gen.start();
      }

   }

   public void stopArenaGenerators(Arena arena) {
      for(Generator gen : arena.getGenerators()) {
         gen.stop();
      }

   }

   public void upgradeAllForgeGenerators(Arena arena, int forgeLevel, Team team) {
      for(Generator gen : arena.getGenerators()) {
         if ((gen.getType() == GeneratorType.IRON || gen.getType() == GeneratorType.GOLD) && gen.getOwnerTeam() != null && gen.getOwnerTeam().equals(team.getName())) {
            gen.upgradeForge(forgeLevel);
         }
      }

   }

   public void shutdown() {
      for(Arena arena : this.plugin.getArenaManager().getAllArenas()) {
         this.stopArenaGenerators(arena);
      }

   }
}
