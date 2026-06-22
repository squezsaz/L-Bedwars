package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.util.FoliaUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsCache {
   private final LBedWars plugin;
   private final Map<UUID, Map<String, Integer>> cache;
   private final Map<UUID, Map<String, Integer>> dirty;
   private int flushTaskId;

   public StatsCache(LBedWars plugin) {
      this.plugin = plugin;
      this.cache = new ConcurrentHashMap();
      this.dirty = new ConcurrentHashMap();
      this.flushTaskId = -1;
   }

   public void startFlushTask() {
      if (this.flushTaskId == -1) {
         this.flushTaskId = FoliaUtil.runTaskTimer(this::flush, 600L, 600L);
      }
   }

   public void stopFlushTask() {
      if (this.flushTaskId != -1) {
         FoliaUtil.cancelTask(this.flushTaskId);
         this.flushTaskId = -1;
      }

      this.flush();
   }

   public int getStats(UUID uuid, String key) {
      Map<String, Integer> playerStats = (Map)this.cache.get(uuid);
      if (playerStats != null) {
         Integer v = (Integer)playerStats.get(key);
         if (v != null) {
            return v;
         }
      }

      int val = this.plugin.getDatabase().getStats(uuid, key);
      ((Map)this.cache.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap())).put(key, val);
      return val;
   }

   public void setStats(UUID uuid, String key, int value) {
      ((Map)this.cache.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap())).put(key, value);
      ((Map)this.dirty.computeIfAbsent(uuid, (k) -> new ConcurrentHashMap())).put(key, value);
   }

   public void addStats(UUID uuid, String key, int amount) {
      this.cache.compute(uuid, (k, map) -> {
         if (map == null) {
            map = new ConcurrentHashMap();
         }

         map.merge(key, amount, Integer::sum);
         return map;
      });
      this.dirty.compute(uuid, (k, map) -> {
         if (map == null) {
            map = new ConcurrentHashMap();
         }

         map.merge(key, amount, Integer::sum);
         return map;
      });
   }

   public void flush() {
      for(Map.Entry<UUID, Map<String, Integer>> entry : this.dirty.entrySet()) {
         UUID uuid = entry.getKey();
         Map<String, Integer> stats = entry.getValue();

         for(Map.Entry<String, Integer> stat : stats.entrySet()) {
            String key = stat.getKey();
            int value = this.cache.getOrDefault(uuid, Map.of()).getOrDefault(key, 0);
            this.plugin.getDatabase().setStats(uuid, key, value);
         }
      }

      this.dirty.clear();
   }

   public void flush(UUID uuid) {
      Map<String, Integer> playerDirty = (Map)this.dirty.remove(uuid);
      if (playerDirty != null) {
         Map<String, Integer> playerCache = (Map)this.cache.get(uuid);

         for(String key : playerDirty.keySet()) {
            int value = playerCache != null ? (Integer)playerCache.getOrDefault(key, 0) : 0;
            this.plugin.getDatabase().setStats(uuid, key, value);
         }

      }
   }

   public void invalidate(UUID uuid) {
      this.flush(uuid);
      this.cache.remove(uuid);
   }

   public void invalidateAll() {
      this.flush();
      this.cache.clear();
   }
}
