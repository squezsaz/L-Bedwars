package com.lbedwars.arena;

import com.lbedwars.LBedWars;
import com.lbedwars.api.events.ArenaStateChangeEvent;
import com.lbedwars.api.events.PlayerJoinArenaEvent;
import com.lbedwars.api.events.PlayerLeaveArenaEvent;
import com.lbedwars.generator.Generator;
import com.lbedwars.generator.GeneratorType;
import com.lbedwars.npc.ShopNPC;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.MessageUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Arena {
   private final String name;
   private final LBedWars plugin;
   private ArenaState state;
   private final List<Team> teams;
   private final List<UUID> players;
   private final List<UUID> spectators;
   private final List<Generator> generators;
   private final List<ShopNPC> shopNpcs;
   private Location lobbyLocation;
   private Location spectatorSpawn;
   private Location regionMin;
   private Location regionMax;
   private int minPlayers;
   private int maxTeamSize;
   private int teamCount;
   private int startCountdown;
   private int gameTime;
   private boolean enabled;
   private boolean dragonsSpawned;
   private final List<EnderDragon> dragons = new ArrayList();
   private final List<IronGolem> golems = new ArrayList();
   private final Map<Location, BlockData> brokenBlocks = new HashMap();
   private final Set<Location> placedBlocks = new HashSet();
   private String mode;
   private int lobbyTaskId;
   private int lobbyCountdown;
   private int startCountdownTaskId;
   private int teamSelectorTaskId;
   private File configFile;
   private YamlConfiguration arenaConfig;

   public Arena(String name, LBedWars plugin) {
      this.name = name;
      this.plugin = plugin;
      this.state = ArenaState.WAITING;
      this.teams = new ArrayList();
      this.players = new ArrayList();
      this.spectators = new ArrayList();
      this.generators = new ArrayList();
      this.shopNpcs = new ArrayList();
      this.minPlayers = 2;
      this.maxTeamSize = plugin.getConfig().getInt("game.max-team-size", 4);
      this.teamCount = 0;
      this.mode = "solo";
      this.startCountdown = plugin.getConfig().getInt("game.start-countdown", 15);
      this.gameTime = 0;
      this.enabled = false;
      this.dragonsSpawned = false;
      this.lobbyTaskId = -1;
      this.lobbyCountdown = -1;
      this.startCountdownTaskId = -1;
      this.teamSelectorTaskId = -1;
   }

   public String getModeName() {
      if (this.mode != null && !this.mode.isEmpty()) {
         return this.mode;
      } else {
         int max = this.teams.stream().mapToInt(Team::getMaxSize).max().orElse(1);
         if (max <= 1) {
            return "solo";
         } else if (max == 2) {
            return "duo";
         } else {
            return max == 3 ? "trio" : "quad";
         }
      }
   }

   public int getModeMinPlayers() {
      if (this.teams.isEmpty()) {
         return 2;
      } else {
         String mode = this.getModeName();
         return this.plugin.getConfig().getInt("game.min-players-" + mode, 4);
      }
   }

   public void loadConfig() {
      this.configFile = new File(String.valueOf(this.plugin.getDataFolder()) + "/arenas", this.name + ".yml");
      if (this.configFile.exists()) {
         this.arenaConfig = YamlConfiguration.loadConfiguration(this.configFile);
         this.enabled = this.arenaConfig.getBoolean("enabled", false);
         this.mode = this.arenaConfig.getString("mode", "solo");
         this.teamCount = this.arenaConfig.getInt("team-count", 0);
         this.lobbyLocation = this.loadLocation("lobby");
         this.spectatorSpawn = this.loadLocation("spectator-spawn");
         this.regionMin = this.loadLocation("region.min");
         this.regionMax = this.loadLocation("region.max");
         this.teams.clear();
         this.generators.clear();
         this.shopNpcs.clear();
         ConfigurationSection teamsSection = this.arenaConfig.getConfigurationSection("teams");
         if (teamsSection != null) {
            this.startCountdown = this.plugin.getConfig().getInt("game.start-countdown", 15);

            for(String key : teamsSection.getKeys(false)) {
               String section = "teams." + key;
               String teamName = this.arenaConfig.getString(section + ".name", "Team");
               String colorStr = this.arenaConfig.getString(section + ".color", "WHITE");
               int maxSize = this.arenaConfig.getInt(section + ".max-size", 4);

               ChatColor color;
               try {
                  color = ChatColor.valueOf(colorStr);
               } catch (IllegalArgumentException var13) {
                  color = ChatColor.WHITE;
               }

               Team team = new Team(teamName, color, maxSize);
               team.setSpawn(this.loadLocation(section + ".spawn"));
               team.setBedLocation(this.loadLocation(section + ".bed"));
               String facingStr = this.arenaConfig.getString(section + ".bed-facing");
               if (facingStr != null) {
                  try {
                     team.setBedFacing(BlockFace.valueOf(facingStr));
                  } catch (IllegalArgumentException var12) {
                  }
               }

               this.teams.add(team);
            }

            this.minPlayers = this.getModeMinPlayers();
         }

         ConfigurationSection genSection = this.arenaConfig.getConfigurationSection("generators");
         if (genSection != null) {
            for(String key : genSection.getKeys(false)) {
               String genPath = "generators." + key;
               String typeStr = this.arenaConfig.getString(genPath + ".type");

               GeneratorType type;
               try {
                  type = GeneratorType.valueOf(typeStr);
               } catch (IllegalArgumentException var14) {
                  continue;
               }

               Location loc = this.loadLocation(genPath + ".location");
               if (loc != null) {
                  Generator gen = new Generator(type, loc, this);
                  gen.setHologramEnabled(this.arenaConfig.getBoolean(genPath + ".hologram", true));
                  String genTeam = this.arenaConfig.getString(genPath + ".team");
                  if (genTeam != null) {
                     gen.setOwnerTeam(genTeam);
                  }

                  this.generators.add(gen);
               }
            }
         }

         for(Generator gen : this.generators) {
            if (gen.getOwnerTeam() == null && (gen.getType() == GeneratorType.IRON || gen.getType() == GeneratorType.GOLD)) {
               Team nearest = null;
               double minDist = Double.MAX_VALUE;

               for(Team team : this.teams) {
                  if (team.getSpawn() != null) {
                     double dist = gen.getLocation().distanceSquared(team.getSpawn());
                     if (dist < minDist) {
                        minDist = dist;
                        nearest = team;
                     }
                  }
               }

               if (nearest != null && minDist < (double)400.0F) {
                  gen.setOwnerTeam(nearest.getName());
               }
            }
         }

         ConfigurationSection npcSection = this.arenaConfig.getConfigurationSection("npcs");
         if (npcSection != null) {
            for(String key : npcSection.getKeys(false)) {
               String npcPath = "npcs." + key;
               String npcType = this.arenaConfig.getString(npcPath + ".type");
               Location npcLoc = this.loadLocation(npcPath + ".location");
               if (npcLoc != null) {
                  ShopNPC npc = new ShopNPC(npcLoc, this, npcType);
                  this.shopNpcs.add(npc);
               }
            }
         }

      }
   }

   public void saveConfig() {
      this.configFile = new File(String.valueOf(this.plugin.getDataFolder()) + "/arenas", this.name + ".yml");
      this.arenaConfig = new YamlConfiguration();
      this.arenaConfig.set("enabled", this.enabled);
      this.arenaConfig.set("mode", this.mode);
      this.arenaConfig.set("team-count", this.teams.size());
      this.arenaConfig.set("min-players", this.minPlayers);
      this.saveLocation("lobby", this.lobbyLocation);
      this.saveLocation("spectator-spawn", this.spectatorSpawn);
      this.saveLocation("region.min", this.regionMin);
      this.saveLocation("region.max", this.regionMax);
      int tIdx = 0;

      for(Team team : this.teams) {
         ++tIdx;
         String section = "teams.team" + tIdx;
         this.arenaConfig.set(section + ".name", team.getName());
         this.arenaConfig.set(section + ".color", team.getColor().name());
         this.arenaConfig.set(section + ".max-size", team.getMaxSize());
         this.saveLocation(section + ".spawn", team.getSpawn());
         this.saveLocation(section + ".bed", team.getBedLocation());
         this.arenaConfig.set(section + ".bed-facing", team.getBedFacing().name());
      }

      int genIdx = 0;

      for(Generator gen : this.generators) {
         ++genIdx;
         String genPath = "generators.gen" + genIdx;
         this.arenaConfig.set(genPath + ".type", gen.getType().name());
         this.saveLocation(genPath + ".location", gen.getLocation());
         this.arenaConfig.set(genPath + ".hologram", gen.isHologramEnabled());
         if (gen.getOwnerTeam() != null) {
            this.arenaConfig.set(genPath + ".team", gen.getOwnerTeam());
         } else {
            this.arenaConfig.set(genPath + ".team", (Object)null);
         }
      }

      int npcIdx = 0;

      for(ShopNPC npc : this.shopNpcs) {
         ++npcIdx;
         String npcPath = "npcs.npc" + npcIdx;
         this.arenaConfig.set(npcPath + ".type", npc.getNpcType());
         this.saveLocation(npcPath + ".location", npc.getLocation());
      }

      try {
         this.arenaConfig.save(this.configFile);
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   private void saveLocation(String path, Location loc) {
      if (loc != null && loc.getWorld() != null) {
         this.arenaConfig.set(path + ".world", loc.getWorld().getName());
         this.arenaConfig.set(path + ".x", loc.getX());
         this.arenaConfig.set(path + ".y", loc.getY());
         this.arenaConfig.set(path + ".z", loc.getZ());
         this.arenaConfig.set(path + ".yaw", (double)loc.getYaw());
         this.arenaConfig.set(path + ".pitch", (double)loc.getPitch());
      }
   }

   private Location loadLocation(String path) {
      if (!this.arenaConfig.contains(path + ".world")) {
         return null;
      } else {
         World world = Bukkit.getWorld(this.arenaConfig.getString(path + ".world"));
         return world == null ? null : new Location(world, this.arenaConfig.getDouble(path + ".x"), this.arenaConfig.getDouble(path + ".y"), this.arenaConfig.getDouble(path + ".z"), (float)this.arenaConfig.getDouble(path + ".yaw"), (float)this.arenaConfig.getDouble(path + ".pitch"));
      }
   }

   public void addTeam(String name, ChatColor color, int maxSize) {
      this.teams.add(new Team(name, color, maxSize));
      this.teamCount = this.teams.size();
   }

   public void removeTeam(String name) {
      this.teams.removeIf((t) -> t.getName().equalsIgnoreCase(name));
      this.teamCount = this.teams.size();
   }

   public Team getTeamByName(String name) {
      for(Team team : this.teams) {
         if (team.getName().equalsIgnoreCase(name)) {
            return team;
         }
      }

      return null;
   }

   public Team getTeamByPlayer(UUID uuid) {
      for(Team team : this.teams) {
         if (team.getPlayers().contains(uuid)) {
            return team;
         }
      }

      return null;
   }

   public Team getSmallestTeam() {
      Team smallest = null;
      int min = Integer.MAX_VALUE;

      for(Team team : this.teams) {
         if (!team.isFull() && team.getPlayers().size() < min) {
            min = team.getPlayers().size();
            smallest = team;
         }
      }

      return smallest;
   }

   public boolean hasPlayer(UUID uuid) {
      return this.players.contains(uuid);
   }

   public void addPlayer(Player player) {
      this.addPlayerToTeam(player, this.getSmallestTeam());
   }

   public void addPlayerToTeam(Player player, Team team) {
      UUID uuid = player.getUniqueId();
      if (!this.players.contains(uuid)) {
         if ((this.state == ArenaState.PLAYING || this.state == ArenaState.STARTING) && !player.hasPermission("bedwars.admin")) {
            MessageUtil.sendMessage(player, "game.already-started");
         } else if (team == null) {
            MessageUtil.sendMessage(player, "lobby.arena-full");
         } else {
            this.players.add(uuid);
            team.addPlayer(uuid);
            this.plugin.getArenaManager().registerPlayer(uuid, this);
            player.teleport(this.lobbyLocation != null ? this.lobbyLocation : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
            this.giveLobbyItems(player);
            this.broadcast("lobby.join", "name", player.getName(), "count", String.valueOf(this.players.size()), "max", String.valueOf(this.teams.size() * this.maxTeamSize));
            this.plugin.getScoreboardManager().startWaitingScoreboard(this);
            if (this.state == ArenaState.WAITING) {
               this.plugin.getGameManager().checkLobbyCountdown(this);
            }
            Bukkit.getPluginManager().callEvent(new PlayerJoinArenaEvent(player, this, team));

         }
      }
   }

   public void giveLobbyItems(Player player) {
      player.getInventory().clear();
      player.setGameMode(GameMode.ADVENTURE);
      ItemStack compass = new ItemStack(Material.COMPASS);
      ItemMeta compassMeta = compass.getItemMeta();
      compassMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getLanguageManager().getMessage("lobby.compass-name")));
      compass.setItemMeta(compassMeta);
      player.getInventory().setItem(4, compass);
      ItemStack bed = new ItemStack(Material.RED_BED);
      ItemMeta bedMeta = bed.getItemMeta();
      bedMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getLanguageManager().getMessage("lobby.bed-name")));
      bed.setItemMeta(bedMeta);
      player.getInventory().setItem(8, bed);
   }

   private Inventory buildTeamSelector() {
      int size = Math.max((this.teams.size() / 9 + 1) * 9, 9);
      String title = ChatColor.translateAlternateColorCodes('&', this.plugin.getLanguageManager().getMessage("lobby.team-select-title"));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, size, title);

      for(int i = 0; i < this.teams.size() && i < size; ++i) {
         Team team = (Team)this.teams.get(i);
         ItemStack wool = new ItemStack(this.getWoolMaterial(team.getColor()), 1);
         ItemMeta meta = wool.getItemMeta();
         String var10001 = String.valueOf(team.getColor());
         meta.setDisplayName(var10001 + "§l" + team.getName());
         List<String> lore = new ArrayList();
         String countMsg = this.plugin.getLanguageManager().getMessage("lobby.team-count");
         if (countMsg != null) {
            lore.add(ChatColor.translateAlternateColorCodes('&', countMsg.replace("{count}", String.valueOf(team.getPlayers().size())).replace("{max}", String.valueOf(team.getMaxSize()))));
         }

         String playerEntry = this.plugin.getLanguageManager().getMessage("lobby.team-player-entry");
         if (playerEntry != null) {
            for(UUID uuid : team.getPlayers()) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                  lore.add(ChatColor.translateAlternateColorCodes('&', playerEntry.replace("{player}", p.getName())));
               }
            }
         }

         String fullMsg = this.plugin.getLanguageManager().getMessage("lobby.team-full");
         String clickMsg = this.plugin.getLanguageManager().getMessage("lobby.click-to-select");
         if (team.isFull()) {
            if (fullMsg != null) {
               lore.add(ChatColor.translateAlternateColorCodes('&', fullMsg));
            }
         } else if (clickMsg != null) {
            lore.add(ChatColor.translateAlternateColorCodes('&', clickMsg));
         }

         meta.setLore(lore);
         wool.setItemMeta(meta);
         inv.setItem(i, wool);
      }

      return inv;
   }

   public void openTeamSelector(Player player) {
      player.openInventory(this.buildTeamSelector());
      this.startTeamSelectorRefresh();
   }

   private void startTeamSelectorRefresh() {
      if (this.teamSelectorTaskId == -1) {
         String title = ChatColor.translateAlternateColorCodes('&', this.plugin.getLanguageManager().getMessage("lobby.team-select-title"));
         this.teamSelectorTaskId = FoliaUtil.runTaskTimer(() -> {
            if (this.state != ArenaState.WAITING && this.state != ArenaState.STARTING) {
               FoliaUtil.cancelTask(this.teamSelectorTaskId);
               this.teamSelectorTaskId = -1;
            } else {
               for(UUID uuid : this.players) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && p.getOpenInventory().getTitle().equals(title)) {
                     p.openInventory(this.buildTeamSelector());
                  }
               }

            }
         }, 20L, 20L);
      }
   }

   public void stopTeamSelectorRefresh() {
      if (this.teamSelectorTaskId != -1) {
         FoliaUtil.cancelTask(this.teamSelectorTaskId);
         this.teamSelectorTaskId = -1;
      }

   }

   public void switchPlayerTeam(Player player, Team newTeam) {
      UUID uuid = player.getUniqueId();
      Team oldTeam = this.getTeamByPlayer(uuid);
      if (oldTeam != null && oldTeam.equals(newTeam)) {
         MessageUtil.sendMessage(player, "lobby.already-in-team");
      } else {
         if (oldTeam != null) {
            oldTeam.removePlayer(uuid);
         }

         if (newTeam.isFull()) {
            if (oldTeam != null) {
               oldTeam.addPlayer(uuid);
            }

            MessageUtil.sendMessage(player, "lobby.team-full-msg");
         } else {
            newTeam.addPlayer(uuid);
            MessageUtil.sendMessage(player, "lobby.joined-team", "team", newTeam.getColoredName());
            player.closeInventory();
         }
      }
   }

   public Material getWoolMaterial(ChatColor color) {
      Material var10000;
      switch (color) {
         case RED:
         case DARK_RED:
            var10000 = Material.RED_WOOL;
            break;
         case BLUE:
         case DARK_BLUE:
            var10000 = Material.BLUE_WOOL;
            break;
         case GREEN:
            var10000 = Material.GREEN_WOOL;
            break;
         case DARK_GREEN:
            var10000 = Material.LIME_WOOL;
            break;
         case YELLOW:
            var10000 = Material.YELLOW_WOOL;
            break;
         case AQUA:
         case DARK_AQUA:
            var10000 = Material.CYAN_WOOL;
            break;
         case LIGHT_PURPLE:
         case DARK_PURPLE:
            var10000 = Material.PURPLE_WOOL;
            break;
         case WHITE:
            var10000 = Material.WHITE_WOOL;
            break;
         case GRAY:
            var10000 = Material.GRAY_WOOL;
            break;
         case DARK_GRAY:
            var10000 = Material.LIGHT_GRAY_WOOL;
            break;
         case BLACK:
            var10000 = Material.BLACK_WOOL;
            break;
         case GOLD:
            var10000 = Material.ORANGE_WOOL;
            break;
         default:
            var10000 = Material.WHITE_WOOL;
      }

      return var10000;
   }

   public void removePlayer(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.players.contains(uuid)) {
         Bukkit.getPluginManager().callEvent(new PlayerLeaveArenaEvent(player, this));
         if (this.plugin.getSpectateManager().isSpectating(uuid)) {
            this.plugin.getSpectateManager().disableSpectate(player);
         }

         this.players.remove(uuid);
         this.spectators.remove(uuid);
         this.plugin.getArenaManager().unregisterPlayer(uuid);
         this.plugin.getArenaManager().unregisterSpectator(uuid);
         Team team = this.getTeamByPlayer(uuid);
         if (team != null) {
            team.removePlayer(uuid);
         }

         player.getInventory().clear();
         player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
         this.broadcast("lobby.leave", "name", player.getName(), "count", String.valueOf(this.players.size()), "max", String.valueOf(this.teams.size() * this.maxTeamSize));
         if (this.players.isEmpty()) {
            this.plugin.getScoreboardManager().stopWaitingScoreboard(this);
         }

         if (this.state == ArenaState.WAITING && this.isLobbyCountdownRunning() && this.players.size() < this.getModeMinPlayers()) {
            this.cancelLobbyCountdown();
            this.broadcast("lobby.not-enough", "player", String.valueOf(this.players.size()), "min", String.valueOf(this.getModeMinPlayers()));
         }

         if (this.state == ArenaState.PLAYING) {
            this.plugin.getGameManager().checkWinCondition(this);
         }

         if (this.players.isEmpty() && (this.state == ArenaState.PLAYING || this.state == ArenaState.STARTING)) {
            this.plugin.getGameManager().resetArena(this);
         }

      }
   }

   public void cancelLobbyCountdown() {
      this.plugin.getGameManager().cancelLobbyCountdown(this);
   }

   public boolean isInRegion(Location loc) {
      if (this.regionMin != null && this.regionMax != null) {
         return loc.getX() >= this.regionMin.getX() && loc.getX() <= this.regionMax.getX() && loc.getY() >= this.regionMin.getY() && loc.getY() <= this.regionMax.getY() && loc.getZ() >= this.regionMin.getZ() && loc.getZ() <= this.regionMax.getZ();
      } else {
         return true;
      }
   }

   public void broadcast(String path, String... placeholders) {
      for(UUID uuid : this.players) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            MessageUtil.sendMessage(player, path, placeholders);
         }
      }

   }

   public void broadcastActionBar(String path, String... placeholders) {
      for(UUID uuid : this.players) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            MessageUtil.sendActionBar(player, path, placeholders);
         }
      }

   }

   public String getName() {
      return this.name;
   }

   public ArenaState getState() {
      return this.state;
   }

   public void setState(ArenaState state) {
      ArenaState old = this.state;
      this.state = state;
      if (old != state) {
         Bukkit.getPluginManager().callEvent(new ArenaStateChangeEvent(this, old, state));
      }
   }

   public String getMode() {
      return this.mode;
   }

   public void setMode(String mode) {
      this.mode = mode;
   }

   public List<Team> getTeams() {
      return this.teams;
   }

   public List<String> getTeamNames() {
      return this.teams.stream().map(Team::getName).toList();
   }

   public List<UUID> getPlayers() {
      return this.players;
   }

   public List<UUID> getSpectators() {
      return this.spectators;
   }

   public List<Generator> getGenerators() {
      return this.generators;
   }

   public List<ShopNPC> getShopNpcs() {
      return this.shopNpcs;
   }

   public Location getLobbyLocation() {
      return this.lobbyLocation;
   }

   public void setLobbyLocation(Location loc) {
      this.lobbyLocation = loc;
   }

   public Location getSpectatorSpawn() {
      return this.spectatorSpawn;
   }

   public void setSpectatorSpawn(Location loc) {
      this.spectatorSpawn = loc;
   }

   public Location getRegionMin() {
      return this.regionMin;
   }

   public void setRegionMin(Location loc) {
      this.regionMin = loc;
   }

   public Location getRegionMax() {
      return this.regionMax;
   }

   public void setRegionMax(Location loc) {
      this.regionMax = loc;
   }

   public int getMinPlayers() {
      return this.minPlayers;
   }

   public void setMinPlayers(int min) {
      this.minPlayers = min;
   }

   public int getMaxTeamSize() {
      return this.maxTeamSize;
   }

   public void setMaxTeamSize(int size) {
      this.maxTeamSize = size;
   }

   public int getTeamCount() {
      return this.teams.size();
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public int getGameTime() {
      return this.gameTime;
   }

   public void setGameTime(int time) {
      this.gameTime = time;
   }

   public int getStartCountdown() {
      return this.startCountdown;
   }

   public int getLobbyTaskId() {
      return this.lobbyTaskId;
   }

   public void setLobbyTaskId(int id) {
      this.lobbyTaskId = id;
   }

   public int getLobbyCountdown() {
      return this.lobbyCountdown;
   }

   public void setLobbyCountdown(int sec) {
      this.lobbyCountdown = sec;
   }

   public boolean isLobbyCountdownRunning() {
      return this.lobbyTaskId != -1;
   }

   public void setStartCountdownTaskId(int id) {
      this.startCountdownTaskId = id;
   }

   public void cancelStartCountdown() {
      if (this.startCountdownTaskId != -1) {
         FoliaUtil.cancelTask(this.startCountdownTaskId);
         this.startCountdownTaskId = -1;
      }

      this.stopTeamSelectorRefresh();
   }

   public YamlConfiguration getConfig() {
      return this.arenaConfig;
   }

   public boolean hasDragonsSpawned() {
      return this.dragonsSpawned;
   }

   public void setDragonsSpawned(boolean spawned) {
      this.dragonsSpawned = spawned;
   }

   public List<EnderDragon> getDragons() {
      return this.dragons;
   }

   public List<IronGolem> getGolems() {
      return this.golems;
   }

   public void trackBlockPlace(Location loc) {
      this.placedBlocks.add(loc);
   }

   public void trackBlockBreak(Location loc, BlockData originalData) {
      this.brokenBlocks.put(loc, originalData);
   }

   public boolean isPlacedBlock(Location loc) {
      return this.placedBlocks.contains(loc);
   }

   public Map<Location, BlockData> getBrokenBlocks() {
      return this.brokenBlocks;
   }

   public Set<Location> getPlacedBlocks() {
      return this.placedBlocks;
   }
}
