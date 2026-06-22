package com.lbedwars.api;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.database.Database;
import com.lbedwars.manager.EconomyManager;
import com.lbedwars.manager.LevelManager;
import com.lbedwars.manager.StatsCache;
import com.lbedwars.party.PartyIntegration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LBedWarsAPI {
   private static LBedWarsAPI instance;
   private final LBedWars plugin;

   public LBedWarsAPI(LBedWars plugin) {
      this.plugin = plugin;
      instance = this;
   }

   public static LBedWarsAPI getInstance() {
      return instance;
   }

   // --- Arena ---

   public Arena getArena(String name) {
      return this.plugin.getArenaManager().getArena(name);
   }

   public Arena getPlayerArena(Player player) {
      return this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
   }

   public Team getPlayerTeam(Player player) {
      Arena arena = this.getPlayerArena(player);
      return arena == null ? null : arena.getTeamByPlayer(player.getUniqueId());
   }

   public List<Arena> getEnabledArenas() {
      return this.plugin.getArenaManager().getEnabledArenas();
   }

   public Collection<Arena> getAllArenas() {
      return this.plugin.getArenaManager().getAllArenas();
   }

   public boolean isPlayerInArena(Player player) {
      return this.getPlayerArena(player) != null;
   }

   public ArenaState getArenaState(String arenaName) {
      Arena arena = this.getArena(arenaName);
      return arena == null ? null : arena.getState();
   }

   public void joinArena(Player player, String arenaName) {
      Arena arena = this.getArena(arenaName);
      if (arena != null) {
         arena.addPlayer(player);
      }
   }

   public void leaveArena(Player player) {
      Arena arena = this.getPlayerArena(player);
      if (arena != null) {
         arena.removePlayer(player);
      }
   }

   public Location getArenaLobby(String arenaName) {
      Arena arena = this.getArena(arenaName);
      return arena == null ? null : arena.getLobbyLocation();
   }

   // -- Stats & Leveling ---

   public Database getDatabase() {
      return this.plugin.getDatabase();
   }

   public int getPlayerLevel(UUID uuid) {
      return this.plugin.getLevelManager().getLevel(uuid);
   }

   public int getPlayerXp(UUID uuid) {
      return this.plugin.getLevelManager().getXp(uuid);
   }

   public void addXp(UUID uuid, int amount) {
      this.plugin.getLevelManager().addXp(uuid, amount);
   }

   public int getRequiredXp(int level) {
      return this.plugin.getLevelManager().getRequiredXp(level);
   }

   public int getPlayerStat(UUID uuid, String statKey) {
      return this.plugin.getStatsCache().getStats(uuid, statKey);
   }

   public void setPlayerStat(UUID uuid, String statKey, int value) {
      this.plugin.getStatsCache().setStats(uuid, statKey, value);
   }

   public void addPlayerStat(UUID uuid, String statKey, int amount) {
      this.plugin.getStatsCache().addStats(uuid, statKey, amount);
   }

   public Map<UUID, Integer> getLeaderboard(String statKey, int limit) {
      return this.plugin.getDatabase().getLeaderboard(statKey, limit);
   }

   // --- Economy ---

   public EconomyManager getEconomyManager() {
      return this.plugin.getEconomyManager();
   }

   public double getBalance(Player player) {
      return this.plugin.getEconomyManager().getBalance(player);
   }

   public boolean depositPlayer(Player player, double amount) {
      return this.plugin.getEconomyManager().deposit(player, amount);
   }

   public boolean withdrawPlayer(Player player, double amount) {
      return this.plugin.getEconomyManager().withdraw(player, amount);
   }

   // --- Party ---

   public boolean isInParty(UUID uuid) {
      return PartyIntegration.isInParty(uuid);
   }

   public boolean isPartyLeader(UUID uuid) {
      return PartyIntegration.isLeader(uuid);
   }

   public List<UUID> getPartyMembers(UUID uuid) {
      return PartyIntegration.getPartyMembers(uuid);
   }

   // --- Misc ---

   public Location getMainLobby() {
      return this.plugin.getMainLobby();
   }

   public boolean isProxyMode() {
      return this.plugin.getProxyManager().isEnabled();
   }

   public List<String> getArenaNames() {
      return this.plugin.getArenaManager().getArenaNames();
   }

   public boolean arenaExists(String name) {
      return this.plugin.getArenaManager().arenaExists(name);
   }

   public boolean createArena(String name) {
      return this.plugin.getArenaManager().createArena(name) != null;
   }

   public boolean deleteArena(String name) {
      Arena arena = this.getArena(name);
      if (arena == null || !arena.getPlayers().isEmpty()) return false;
      this.plugin.getArenaManager().deleteArena(name);
      return true;
   }
}
