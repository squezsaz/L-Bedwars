package com.lbedwars.arena;

import com.lbedwars.LBedWars;
import com.lbedwars.generator.Generator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.World;

public class ArenaManager {
   private final LBedWars plugin;
   private final Map<String, Arena> arenas;
   private final Map<UUID, Arena> playerArenas;
   private final Map<UUID, Arena> spectatorArenas;

   public ArenaManager(LBedWars plugin) {
      this.plugin = plugin;
      this.arenas = new HashMap();
      this.playerArenas = new HashMap();
      this.spectatorArenas = new HashMap();
      this.loadArenas();
   }

   public void registerPlayer(UUID uuid, Arena arena) {
      this.playerArenas.put(uuid, arena);
   }

   public void unregisterPlayer(UUID uuid) {
      this.playerArenas.remove(uuid);
   }

   public void registerSpectator(UUID uuid, Arena arena) {
      this.spectatorArenas.put(uuid, arena);
   }

   public void unregisterSpectator(UUID uuid) {
      this.spectatorArenas.remove(uuid);
   }

   private void loadArenas() {
      File arenasDir = new File(this.plugin.getDataFolder(), "arenas");
      if (!arenasDir.exists()) {
         arenasDir.mkdirs();
      }

      File[] files = arenasDir.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files != null) {
         for(File file : files) {
            String arenaName = file.getName().replace(".yml", "");
            Arena arena = new Arena(arenaName, this.plugin);
            arena.loadConfig();
            this.arenas.put(arenaName, arena);
         }

         this.plugin.getLogger().info(this.plugin.getLanguageManager().getMessage("console.arenas-loaded").replace("{count}", String.valueOf(this.arenas.size())));
      }
   }

   public Arena createArena(String name) {
      if (this.arenas.containsKey(name)) {
         return null;
      } else {
         Arena arena = new Arena(name, this.plugin);
         arena.saveConfig();
         this.arenas.put(name, arena);
         return arena;
      }
   }

   public void deleteArena(String name) {
      Arena arena = (Arena)this.arenas.remove(name);
      if (arena != null) {
         File file = new File(String.valueOf(this.plugin.getDataFolder()) + "/arenas", name + ".yml");
         if (file.exists()) {
            file.delete();
         }
      }

   }

   public boolean addTeam(String arenaName, String teamName, String colorStr, int maxSize) {
      Arena arena = (Arena)this.arenas.get(arenaName);
      if (arena == null) {
         return false;
      } else if (arena.getTeamByName(teamName) != null) {
         return false;
      } else {
         ChatColor color;
         try {
            color = ChatColor.valueOf(colorStr.toUpperCase());
         } catch (IllegalArgumentException var8) {
            return false;
         }

         arena.addTeam(teamName, color, maxSize);
         arena.saveConfig();
         return true;
      }
   }

   public boolean removeTeam(String arenaName, String teamName) {
      Arena arena = (Arena)this.arenas.get(arenaName);
      if (arena == null) {
         return false;
      } else {
         Team team = arena.getTeamByName(teamName);
         if (team == null) {
            return false;
         } else {
            arena.removeTeam(teamName);
            arena.saveConfig();
            return true;
         }
      }
   }

   public Arena getArena(String name) {
      return (Arena)this.arenas.get(name);
   }

   public Arena getArenaByPlayer(UUID uuid) {
      return (Arena)this.playerArenas.get(uuid);
   }

   public Arena getArenaBySpectator(UUID uuid) {
      return (Arena)this.spectatorArenas.get(uuid);
   }

   public List<Arena> getEnabledArenas() {
      return this.arenas.values().stream().filter(Arena::isEnabled).toList();
   }

   public List<String> getArenaNames() {
      return new ArrayList(this.arenas.keySet());
   }

   public boolean arenaExists(String name) {
      return this.arenas.containsKey(name);
   }

   public void shutdown() {
      for(Arena arena : this.arenas.values()) {
         arena.saveConfig();
      }

   }

   public Collection<Arena> getAllArenas() {
      return this.arenas.values();
   }

   public void reloadArenas() {
      for(Arena arena : this.arenas.values()) {
         for(Generator gen : arena.getGenerators()) {
            if (gen.isRunning()) {
               gen.stop();
            }
         }
      }

      this.arenas.clear();
      this.loadArenas();
   }

   public Arena getArenaByWorld(World world) {
      for(Arena arena : this.arenas.values()) {
         if (arena.getRegionMin() != null && arena.getRegionMin().getWorld() != null && arena.getRegionMin().getWorld().equals(world)) {
            return arena;
         }
      }

      return null;
   }
}
