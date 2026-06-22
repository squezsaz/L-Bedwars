package com.lbedwars.database;

import com.lbedwars.LBedWars;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlDatabase implements Database {
   private final LBedWars plugin;
   private YamlConfiguration statsConfig;
   private File statsFile;

   public YamlDatabase(LBedWars plugin) {
      this.plugin = plugin;
   }

   public void connect() {
      this.statsFile = new File(this.plugin.getDataFolder(), "stats.yml");
      if (!this.statsFile.exists()) {
         try {
            this.statsFile.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      this.statsConfig = YamlConfiguration.loadConfiguration(this.statsFile);
      this.plugin.getLogger().info(this.plugin.getLanguageManager().getMessage("console.yaml-started"));
   }

   public void close() {
      this.saveStats();
   }

   private void saveStats() {
      try {
         this.statsConfig.save(this.statsFile);
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   public boolean hasStats(UUID uuid) {
      return this.statsConfig.contains(uuid.toString());
   }

   public void createStats(UUID uuid) {
      String path = uuid.toString();
      if (!this.statsConfig.contains(path)) {
         this.statsConfig.set(path + ".kills", 0);
         this.statsConfig.set(path + ".deaths", 0);
         this.statsConfig.set(path + ".final-kills", 0);
         this.statsConfig.set(path + ".final-deaths", 0);
         this.statsConfig.set(path + ".wins", 0);
         this.statsConfig.set(path + ".losses", 0);
         this.statsConfig.set(path + ".beds-broken", 0);
         this.statsConfig.set(path + ".games-played", 0);
         this.statsConfig.set(path + ".xp", 0);
         this.statsConfig.set(path + ".level", 1);
         this.statsConfig.set(path + ".last-daily", 0L);
      }

   }

   public int getStats(UUID uuid, String stat) {
      return this.statsConfig.getInt(uuid.toString() + "." + stat, 0);
   }

   public void addStats(UUID uuid, String stat, int amount) {
      String var10000 = uuid.toString();
      String path = var10000 + "." + stat;
      this.statsConfig.set(path, this.statsConfig.getInt(path) + amount);
   }

   public void setStats(UUID uuid, String stat, int value) {
      this.statsConfig.set(uuid.toString() + "." + stat, value);
   }

   public long getLong(UUID uuid, String key) {
      return this.statsConfig.getLong(uuid.toString() + "." + key, 0L);
   }

   public void setLong(UUID uuid, String key, long value) {
      this.statsConfig.set(uuid.toString() + "." + key, value);
   }

   public Set<String> getOwnedCosmetics(UUID uuid) {
      List<String> list = this.statsConfig.getStringList(uuid.toString() + ".cosmetics");
      return list != null ? new HashSet(list) : new HashSet();
   }

   public void addOwnedCosmetic(UUID uuid, String cosmeticKey) {
      String path = uuid.toString() + ".cosmetics";
      List<String> list = this.statsConfig.getStringList(path);
      if (!list.contains(cosmeticKey)) {
         list.add(cosmeticKey);
         this.statsConfig.set(path, list);
      }

   }

   public String getSelectedCosmetic(UUID uuid, String type) {
      return this.statsConfig.getString(uuid.toString() + "." + type, "none");
   }

   public void setSelectedCosmetic(UUID uuid, String type, String value) {
      this.statsConfig.set(uuid.toString() + "." + type, value);
   }

   public Map<UUID, Integer> getLeaderboard(String stat, int limit) {
      Map<UUID, Integer> result = new LinkedHashMap();
      List<Map.Entry<UUID, Integer>> entries = new ArrayList();

      for(String key : this.statsConfig.getKeys(false)) {
         try {
            UUID uuid = UUID.fromString(key);
            int value = this.statsConfig.getInt(key + "." + stat, 0);
            entries.add(new AbstractMap.SimpleEntry(uuid, value));
         } catch (IllegalArgumentException var9) {
         }
      }

      entries.sort((a, b) -> Integer.compare((Integer)b.getValue(), (Integer)a.getValue()));

      for(int i = 0; i < limit && i < entries.size(); ++i) {
         Map.Entry<UUID, Integer> entry = (Map.Entry)entries.get(i);
         result.put((UUID)entry.getKey(), (Integer)entry.getValue());
      }

      return result;
   }
}
