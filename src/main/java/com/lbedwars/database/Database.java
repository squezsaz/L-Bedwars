package com.lbedwars.database;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Database {
   void connect();

   void close();

   boolean hasStats(UUID var1);

   void createStats(UUID var1);

   int getStats(UUID var1, String var2);

   void addStats(UUID var1, String var2, int var3);

   void setStats(UUID var1, String var2, int var3);

   long getLong(UUID var1, String var2);

   void setLong(UUID var1, String var2, long var3);

   Map<UUID, Integer> getLeaderboard(String var1, int var2);

   Set<String> getOwnedCosmetics(UUID var1);

   void addOwnedCosmetic(UUID var1, String var2);

   String getSelectedCosmetic(UUID var1, String var2);

   void setSelectedCosmetic(UUID var1, String var2, String var3);
}
