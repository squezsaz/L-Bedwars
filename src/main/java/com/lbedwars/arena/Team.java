package com.lbedwars.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class Team {
   private final String name;
   private final ChatColor color;
   private int maxSize;
   private final List<UUID> players;
   private final List<UUID> alivePlayers;
   private Location spawn;
   private Location bedLocation;
   private BlockFace bedFacing;
   private boolean bedAlive;
   private final Map<String, Integer> upgrades;
   private int healPoolTaskId = -1;

   public Team(String name, ChatColor color, int maxSize) {
      this.name = name;
      this.color = color;
      this.maxSize = maxSize;
      this.players = new ArrayList();
      this.alivePlayers = new ArrayList();
      this.bedAlive = true;
      this.bedFacing = BlockFace.SOUTH;
      this.upgrades = new HashMap();
   }

   public String getName() {
      return this.name;
   }

   public ChatColor getColor() {
      return this.color;
   }

   public int getMaxSize() {
      return this.maxSize;
   }

   public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
   }

   public List<UUID> getPlayers() {
      return this.players;
   }

   public List<UUID> getAlivePlayers() {
      return this.alivePlayers;
   }

   public Location getSpawn() {
      return this.spawn;
   }

   public void setSpawn(Location spawn) {
      this.spawn = spawn;
   }

   public Location getBedLocation() {
      return this.bedLocation;
   }

   public void setBedLocation(Location bedLocation) {
      this.bedLocation = bedLocation;
   }

   public BlockFace getBedFacing() {
      return this.bedFacing;
   }

   public void setBedFacing(BlockFace bedFacing) {
      this.bedFacing = bedFacing;
   }

   public Location getOtherHalfLocation() {
      return this.bedLocation != null && this.bedFacing != null ? this.bedLocation.clone().add(this.bedFacing.getOppositeFace().getDirection()) : null;
   }

   public boolean isBedBlock(Block block) {
      if (this.bedLocation == null) {
         return false;
      } else {
         Location loc = block.getLocation();
         return loc.getBlockX() == this.bedLocation.getBlockX() && loc.getBlockY() == this.bedLocation.getBlockY() && loc.getBlockZ() == this.bedLocation.getBlockZ() || loc.equals(this.getOtherHalfLocation());
      }
   }

   public boolean isBedAlive() {
      return this.bedAlive;
   }

   public void setBedAlive(boolean bedAlive) {
      this.bedAlive = bedAlive;
   }

   public Map<String, Integer> getUpgrades() {
      return this.upgrades;
   }

   public void addPlayer(UUID uuid) {
      if (!this.players.contains(uuid)) {
         this.players.add(uuid);
         this.alivePlayers.add(uuid);
      }

   }

   public void removePlayer(UUID uuid) {
      this.players.remove(uuid);
      this.alivePlayers.remove(uuid);
   }

   public boolean isAlive(UUID uuid) {
      return this.alivePlayers.contains(uuid);
   }

   public void killPlayer(UUID uuid) {
      this.alivePlayers.remove(uuid);
   }

   public boolean isEliminated() {
      return this.alivePlayers.isEmpty();
   }

   public boolean isFull() {
      return this.players.size() >= this.maxSize;
   }

   public int getAliveCount() {
      return this.alivePlayers.size();
   }

   public int getTotalCount() {
      return this.players.size();
   }

   public boolean hasUpgrade(String upgrade) {
      return (Integer)this.upgrades.getOrDefault(upgrade, 0) > 0;
   }

   public int getUpgradeLevel(String upgrade) {
      return (Integer)this.upgrades.getOrDefault(upgrade, 0);
   }

   public void setUpgradeLevel(String upgrade, int level) {
      this.upgrades.put(upgrade, level);
   }

   public int getHealPoolTaskId() {
      return this.healPoolTaskId;
   }

   public void setHealPoolTaskId(int id) {
      this.healPoolTaskId = id;
   }

   public String getColoredName() {
      String var10000 = String.valueOf(this.color);
      return var10000 + this.name;
   }
}
