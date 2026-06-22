package com.lbedwars.core;

import com.lbedwars.LBedWars;
import com.lbedwars.api.events.BedBreakEvent;
import com.lbedwars.api.events.GameEndEvent;
import com.lbedwars.api.events.GameStartEvent;
import com.lbedwars.api.events.PlayerKillEvent;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.generator.Generator;
import com.lbedwars.npc.ShopNPC;
import com.lbedwars.scoreboard.ScoreboardManager;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.MessageUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public class GameManager {
   private final LBedWars plugin;
   private final Map<String, Integer> countdownTasks;
   private final Map<String, Integer> dragonTargetTasks;
   private final Map<UUID, Team> dragonTeams;
   private final Map<UUID, LastDamager> lastDamagers;
   private final Map<UUID, RejoinData> rejoinData;
   private final Map<UUID, Team> golemTeams;
   private final Map<String, Integer> golemTargetTasks;
   private final Map<UUID, Long> golemCooldowns;

   public GameManager(LBedWars plugin) {
      this.plugin = plugin;
      this.countdownTasks = new HashMap();
      this.dragonTargetTasks = new HashMap();
      this.dragonTeams = new HashMap();
      this.lastDamagers = new HashMap();
      this.rejoinData = new HashMap();
      this.golemTeams = new HashMap();
      this.golemTargetTasks = new HashMap();
      this.golemCooldowns = new HashMap();
   }

   public void checkLobbyCountdown(Arena arena) {
      if (arena.getState() == ArenaState.WAITING) {
         if (!arena.isLobbyCountdownRunning()) {
            int playerCount = arena.getPlayers().size();
            int needed = arena.getModeMinPlayers();
            if (playerCount >= needed) {
               arena.setLobbyCountdown(arena.getStartCountdown());
               int taskId = FoliaUtil.runTaskTimer(() -> {
                  int sec = arena.getLobbyCountdown();
                  if (sec <= 0) {
                     this.cancelLobbyCountdown(arena);
                     this.startGame(arena);
                  } else if (arena.getPlayers().size() < needed) {
                     arena.broadcast("lobby.not-enough", "player", String.valueOf(arena.getPlayers().size()), "min", String.valueOf(needed));
                     this.cancelLobbyCountdown(arena);
                  } else {
                     if (sec % 5 == 0 || sec <= 3) {
                        arena.broadcast("lobby.start-countdown", "seconds", String.valueOf(sec));
                        this.playConfigSoundToAll(arena, "sounds.countdown");
                     }

                     arena.setLobbyCountdown(sec - 1);
                  }
               }, 0L, 20L);
               arena.setLobbyTaskId(taskId);
            }

         }
      }
   }

   public void cancelLobbyCountdown(Arena arena) {
      if (arena.getLobbyTaskId() != -1) {
         FoliaUtil.cancelTask(arena.getLobbyTaskId());
         arena.setLobbyTaskId(-1);
         arena.setLobbyCountdown(-1);
      }

   }

   public void startGame(Arena arena) {
      if (arena.getState() == ArenaState.WAITING) {
         arena.setState(ArenaState.STARTING);
         arena.setGameTime(0);

         for(UUID uuid : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               Team team = arena.getTeamByPlayer(uuid);
               if (team != null && team.getSpawn() != null) {
                  player.teleport(team.getSpawn());
               }

               player.setGameMode(GameMode.SURVIVAL);
               player.setHealth((double)20.0F);
               player.setFoodLevel(20);
               player.getInventory().clear();
               player.setWalkSpeed(0.0F);
               if (team != null) {
                  Color color = this.getArmorColor(team.getColor());
                  ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                  LeatherArmorMeta helmetMeta = (LeatherArmorMeta)helmet.getItemMeta();
                  helmetMeta.setColor(color);
                  helmetMeta.setUnbreakable(true);
                  helmetMeta.addEnchant(Enchantment.WATER_WORKER, 1, true);
                  helmet.setItemMeta(helmetMeta);
                  player.getInventory().setHelmet(helmet);
                  ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                  LeatherArmorMeta chestMeta = (LeatherArmorMeta)chest.getItemMeta();
                  chestMeta.setColor(color);
                  chestMeta.setUnbreakable(true);
                  chest.setItemMeta(chestMeta);
                  player.getInventory().setChestplate(chest);
                  ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
                  LeatherArmorMeta legsMeta = (LeatherArmorMeta)legs.getItemMeta();
                  legsMeta.setColor(color);
                  legsMeta.setUnbreakable(true);
                  legs.setItemMeta(legsMeta);
                  player.getInventory().setLeggings(legs);
                  ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                  LeatherArmorMeta bootsMeta = (LeatherArmorMeta)boots.getItemMeta();
                  bootsMeta.setColor(color);
                  bootsMeta.setUnbreakable(true);
                  boots.setItemMeta(bootsMeta);
                  player.getInventory().setBoots(boots);
               }

                player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WOODEN_SWORD)});
               this.plugin.getCompassTracker().giveCompass(player);
               this.applyClassicPvp(player);
            }
         }

         this.plugin.getScoreboardManager().stopWaitingScoreboard(arena);
         this.plugin.getScoreboardManager().startScoreboard(arena);

         for(Team team : arena.getTeams()) {
            if (team.getPlayers().isEmpty()) {
               team.setBedAlive(false);
               Location footLoc = team.getOtherHalfLocation();
               if (team.getBedLocation() != null && team.getBedLocation().getWorld() != null) {
                  team.getBedLocation().getBlock().setType(Material.AIR);
                  if (footLoc != null) {
                     footLoc.getBlock().setType(Material.AIR);
                  }
               }
            }
         }

         int[] preCount = new int[]{5};
         int[] taskId = new int[]{-1};
         taskId[0] = FoliaUtil.runTaskTimer(() -> {
            if (arena.getState() != ArenaState.STARTING) {
               FoliaUtil.cancelTask(taskId[0]);
            } else {
               int sec = preCount[0];
               if (sec > 0) {
                  String title = MessageUtil.get("game.countdown-title", "seconds", String.valueOf(sec));
                  String subtitle = MessageUtil.get("game.countdown-subtitle");

                  for(UUID uuid : arena.getPlayers()) {
                     Player p = Bukkit.getPlayer(uuid);
                     if (p != null) {
                        p.sendTitle(title, subtitle, 0, 20, 5);
                     }
                  }

                  int var10002 = preCount[0]--;
               } else {
                  String fightTitle = MessageUtil.get("game.fight-title");
                  String fightSubtitle = MessageUtil.get("game.fight-subtitle");

                  for(UUID uuid : arena.getPlayers()) {
                     Player p = Bukkit.getPlayer(uuid);
                     if (p != null) {
                        p.sendTitle(fightTitle, fightSubtitle, 5, 30, 10);
                        p.setWalkSpeed(0.2F);
                     }
                  }

                  this.plugin.getGeneratorManager().startArenaGenerators(arena);

                  for(ShopNPC npc : arena.getShopNpcs()) {
                     npc.spawn(this.plugin);
                  }

                   arena.setState(ArenaState.PLAYING);
                   arena.setGameTime(0);
                   arena.broadcast("game.started");
                   this.playConfigSoundToAll(arena, "sounds.game-start");
                   Bukkit.getPluginManager().callEvent(new GameStartEvent(arena));
                   FoliaUtil.cancelTask(taskId[0]);
               }

            }
         }, 0L, 20L);
         arena.setStartCountdownTaskId(taskId[0]);
      }
   }

   public void endGame(Arena arena, Team winner) {
      if (arena.getState() != ArenaState.ENDING) {
         arena.setState(ArenaState.ENDING);
         Bukkit.getPluginManager().callEvent(new GameEndEvent(arena, winner));
         this.plugin.getGeneratorManager().stopArenaGenerators(arena);

         for(ShopNPC npc : arena.getShopNpcs()) {
            npc.remove();
         }

         String topTitle = MessageUtil.get("game.top-title");
         String topEntry = MessageUtil.get("game.top-entry");
         if (topTitle != null && topEntry != null) {
            List<ScoreboardManager.TopPlayer> top = this.plugin.getScoreboardManager().getTopPlayers(arena.getName(), 3);
            if (!top.isEmpty()) {
               List<String> lines = new ArrayList();
               lines.add(ChatColor.translateAlternateColorCodes('&', topTitle));

               for(int i = 0; i < top.size(); ++i) {
                  ScoreboardManager.TopPlayer tp = (ScoreboardManager.TopPlayer)top.get(i);
                  String line = topEntry.replace("{pos}", String.valueOf(i + 1)).replace("{player}", tp.name()).replace("{kills}", String.valueOf(tp.kills())).replace("{final}", String.valueOf(tp.finalKills())).replace("{beds}", String.valueOf(tp.bedsBroken()));
                  lines.add(ChatColor.translateAlternateColorCodes('&', line));
               }

               for(UUID uuid : arena.getPlayers()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null) {
                     Objects.requireNonNull(p);
                     lines.forEach(p::sendMessage);
                  }
               }
            }
         }

         this.plugin.getScoreboardManager().stopScoreboard(arena);
         arena.broadcast("game.won", "team", winner.getColoredName());
         String winTitle = MessageUtil.get("game.win-title");
         String winSubtitle = MessageUtil.get("game.win-subtitle");
         if (winTitle != null || winSubtitle != null) {
            String title = winTitle != null ? ChatColor.translateAlternateColorCodes('&', winTitle) : "";
            String subtitle = winSubtitle != null ? ChatColor.translateAlternateColorCodes('&', winSubtitle) : "";

            for(UUID uuid : winner.getPlayers()) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                   p.sendTitle(title, subtitle, 5, 60, 10);
                   this.plugin.getCosmeticManager().spawnVictoryEffects(p);
                   this.plugin.getCosmeticManager().playVictorySong(p);
               }
            }
         }

         for(UUID uuid : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               this.plugin.getLevelManager().addWinXp(uuid);
               this.plugin.getStatsCache().addStats(uuid, "wins", 1);
               player.getInventory().clear();
               player.setGameMode(GameMode.SPECTATOR);
            }
         }

         int lobbyDelay = this.plugin.getConfig().getInt("game.end-lobby-delay", 20);
         if (this.plugin.getProxyManager().isEnabled()) {
            FoliaUtil.runTaskLater(() -> {
               this.plugin.getCosmeticManager().stopArenaSongs(arena);
               this.plugin.getProxyManager().sendAllToLobby(arena);
               FoliaUtil.runTaskLater(() -> this.plugin.getProxyManager().executeEndAction(), 20L);
            }, (long)lobbyDelay * 20L);
         } else {
            FoliaUtil.runTaskLater(() -> this.resetArena(arena), (long)lobbyDelay * 20L);
         }
      }
   }

   private Location getMainLobby() {
      Location mainLobby = this.plugin.getMainLobby();
      return mainLobby != null ? mainLobby : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation();
   }

   public void resetArena(Arena arena) {
      this.plugin.getCompassTracker().clear();
      Location mainLobby = this.getMainLobby();
      this.plugin.getScoreboardManager().stopScoreboard(arena);
      this.plugin.getScoreboardManager().stopWaitingScoreboard(arena);
      this.plugin.getGeneratorManager().stopArenaGenerators(arena);
      this.plugin.getUpgradeManager().stopAllHealPools(arena);
      arena.cancelStartCountdown();
      this.plugin.getCosmeticManager().stopArenaSongs(arena);

      for(UUID uuid : arena.getPlayers()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(mainLobby);
         }
      }

      for(UUID uuid : arena.getSpectators()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(mainLobby);

            for(UUID pUuid : arena.getPlayers()) {
               Player p = Bukkit.getPlayer(pUuid);
               if (p != null) {
                  p.showPlayer(this.plugin, player);
               }
            }
         }
      }

      if (!this.plugin.getProxyManager().isEnabled()) {
         this.restoreWorld(arena);
      }

      for(Team team : arena.getTeams()) {
         if (team.getSpawn() != null && team.getSpawn().getWorld() != null) {
            Location spawn = team.getSpawn();

            for(Entity entity : spawn.getWorld().getNearbyEntities(spawn, (double)50.0F, (double)50.0F, (double)50.0F)) {
               if (entity instanceof Item) {
                  entity.remove();
               }
            }
         }
      }

      for(Generator gen : arena.getGenerators()) {
         gen.stop();
         gen.removeHologramEntities();
         if (gen.getLocation().getWorld() != null) {
            for(Entity entity : gen.getLocation().getWorld().getNearbyEntities(gen.getLocation(), (double)1.0F, (double)1.0F, (double)1.0F)) {
               if (entity instanceof Item) {
                  entity.remove();
               }
            }
         }
      }

      this.removeDragons(arena);
      this.removeGolems(arena);
      this.removeNpcEntities(arena);

      for(ShopNPC npc : arena.getShopNpcs()) {
         npc.remove();
      }

      for(UUID uuid : arena.getPlayers()) {
         this.plugin.getArenaManager().unregisterPlayer(uuid);
      }

      for(UUID uuid : arena.getSpectators()) {
         this.plugin.getArenaManager().unregisterSpectator(uuid);
      }

      arena.getPlayers().clear();
      arena.getSpectators().clear();

      for(Team team : arena.getTeams()) {
         team.getPlayers().clear();
         team.getAlivePlayers().clear();
         team.setBedAlive(true);
         team.getUpgrades().clear();
      }

      arena.getBrokenBlocks().clear();
      arena.getPlacedBlocks().clear();
      arena.getGenerators().clear();
      arena.loadConfig();
      arena.setState(ArenaState.WAITING);
      arena.setGameTime(0);
      arena.broadcast("lobby.waiting", "player", "0", "max", String.valueOf(arena.getTeamCount() * arena.getMaxTeamSize()));
   }

   private void removeNpcEntities(Arena arena) {
      for(Team team : arena.getTeams()) {
         if (team.getSpawn() != null && team.getSpawn().getWorld() != null) {
            for(Entity entity : team.getSpawn().getWorld().getNearbyEntities(team.getSpawn(), (double)50.0F, (double)50.0F, (double)50.0F)) {
               if (entity instanceof Villager && (entity.hasMetadata("shop_npc") || entity.hasMetadata("upgrade_npc"))) {
                  entity.remove();
               }
            }
         }
      }

   }

   private void restoreWorld(Arena arena) {
      for(Map.Entry<Location, BlockData> entry : arena.getBrokenBlocks().entrySet()) {
         Location loc = (Location)entry.getKey();
         if (loc.getWorld() != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR || block.getType().toString().contains("BED")) {
               block.setType(((BlockData)entry.getValue()).getMaterial());
               block.setBlockData((BlockData)entry.getValue(), false);
            }
         }
      }

      arena.getBrokenBlocks().clear();

      for(Location loc : arena.getPlacedBlocks()) {
         if (loc.getWorld() != null) {
            loc.getBlock().setType(Material.AIR);
         }
      }

      arena.getPlacedBlocks().clear();

      for(Team team : arena.getTeams()) {
         if (team.getBedLocation() != null && team.getBedLocation().getWorld() != null) {
            Location headLoc = team.getBedLocation();
            Location footLoc = team.getOtherHalfLocation();
            if (footLoc != null) {
               Block headBlock = headLoc.getBlock();
               Block footBlock = footLoc.getBlock();
               headBlock.setType(Material.AIR);
               footBlock.setType(Material.AIR);
               BlockFace facing = team.getBedFacing();
               headBlock.setType(Material.RED_BED);
               BlockData var10 = headBlock.getBlockData();
               if (var10 instanceof Bed) {
                  Bed bedData = (Bed)var10;
                  bedData.setPart(Part.HEAD);
                  bedData.setFacing(facing);
                  headBlock.setBlockData(bedData);
               }

               footBlock.setType(Material.RED_BED);
               var10 = footBlock.getBlockData();
               if (var10 instanceof Bed) {
                  Bed bedData = (Bed)var10;
                  bedData.setPart(Part.FOOT);
                  bedData.setFacing(facing);
                  footBlock.setBlockData(bedData);
               }
            }
         }
      }

   }

   public void recordDamage(Player victim, Player damager) {
      this.lastDamagers.put(victim.getUniqueId(), new LastDamager(damager.getUniqueId(), System.currentTimeMillis()));
   }

   public Player getLastDamager(Player victim) {
      LastDamager record = (LastDamager)this.lastDamagers.remove(victim.getUniqueId());
      if (record == null) {
         return null;
      } else {
         return System.currentTimeMillis() - record.time > 5000L ? null : Bukkit.getPlayer(record.uuid);
      }
   }

   public void saveRejoinData(UUID uuid, String arenaName, String teamName, String playerName) {
      this.rejoinData.put(uuid, new RejoinData(arenaName, teamName, playerName));
   }

   public RejoinData getRejoinData(UUID uuid) {
      return (RejoinData)this.rejoinData.get(uuid);
   }

   public void removeRejoinData(UUID uuid) {
      this.rejoinData.remove(uuid);
   }

   public Map<UUID, RejoinData> getRejoinDataMap() {
      return this.rejoinData;
   }

   public void rejoinPlayer(Player player) {
      RejoinData data = (RejoinData)this.rejoinData.remove(player.getUniqueId());
      if (data != null) {
         Arena arena = this.plugin.getArenaManager().getArena(data.arenaName);
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            Team team = arena.getTeamByName(data.teamName);
            if (team != null && team.isBedAlive()) {
               UUID uuid = player.getUniqueId();
               arena.getPlayers().add(uuid);
               team.addPlayer(uuid);
               this.plugin.getArenaManager().registerPlayer(uuid, arena);
               player.getInventory().clear();
               if (team.getSpawn() != null) {
                  player.teleport(team.getSpawn());
               }

               player.setGameMode(GameMode.SPECTATOR);
               MessageUtil.sendMessage(player, "game.rejoin-respawning");
               int[] respawnCount = new int[]{5};
               int[] taskId = new int[]{-1};
               taskId[0] = FoliaUtil.runTaskTimer(() -> {
                  int sec = respawnCount[0];
                  if (sec > 0) {
                     String title = "§c§l" + sec;
                     String subtitle = MessageUtil.get("game.respawn-in", "seconds", String.valueOf(sec));
                     player.sendTitle(title, subtitle, 0, 25, 5);
                     this.playConfigSound(player, "sounds.respawn-tick");
                     int var10002 = respawnCount[0]--;
                  } else {
                     FoliaUtil.cancelTask(taskId[0]);
                     if (arena.getState() == ArenaState.PLAYING && team.getSpawn() != null) {
                        this.playConfigSound(player, "sounds.respawn-done");
                        FoliaUtil.runAtEntityLocation(player, () -> {
                           this.respawnPlayer(player);
                           player.teleport(team.getSpawn());
                            player.getInventory().clear();
                            this.plugin.getCompassTracker().giveCompass(player);
                            player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WOODEN_SWORD)});
                            player.setGameMode(GameMode.SURVIVAL);
                            MessageUtil.sendMessage(player, "game.respawned");
                            MessageUtil.sendActionBar(player, "actionbar.respawned");
                        });
                     }
                  }

               }, 0L, 20L);
            }
         }
      }
   }

   public void handlePlayerDeath(Player victim, Player killer, Arena arena, boolean voidDeath) {
      Team victimTeam = arena.getTeamByPlayer(victim.getUniqueId());
      if (killer != null && victimTeam != null) {
         boolean isFinal = !victimTeam.isBedAlive();
         Bukkit.getPluginManager().callEvent(new PlayerKillEvent(arena, killer, victim, isFinal));
      }
      if (victimTeam != null) {
         if (killer == null) {
            killer = this.getLastDamager(victim);
         }

         for(PotionEffect effect : victim.getActivePotionEffects()) {
            victim.removePotionEffect(effect.getType());
         }

         UUID victimUUID = victim.getUniqueId();
         this.plugin.getStatsCache().addStats(victimUUID, "deaths", 1);
         if (killer != null) {
            UUID killerUUID = killer.getUniqueId();
            this.plugin.getStatsCache().addStats(killerUUID, "kills", 1);
            this.plugin.getLevelManager().addKillXp(killerUUID);
            this.plugin.getScoreboardManager().addKill(arena.getName(), killerUUID);
            MessageUtil.sendActionBar(killer, "actionbar.kill", "player", victim.getName());
            this.plugin.getCosmeticManager().spawnKillEffect(killer, victim.getLocation());
         }

         String deathKey = voidDeath ? "game.void-died" : "game.died";
         String actionKey = voidDeath ? "actionbar.void-killed" : "actionbar.killed";
         if (!victimTeam.isBedAlive()) {
            victimTeam.killPlayer(victimUUID);
            this.plugin.getCompassTracker().removeTarget(victimUUID);
            this.plugin.getStatsCache().addStats(victimUUID, "final-deaths", 1);
            if (killer != null) {
               this.plugin.getStatsCache().addStats(killer.getUniqueId(), "final-kills", 1);
               this.plugin.getLevelManager().addFinalKillXp(killer.getUniqueId());
               this.plugin.getScoreboardManager().addFinalKill(arena.getName(), killer.getUniqueId());
               MessageUtil.sendActionBar(killer, "actionbar.final-kill", "player", victim.getName());
               this.plugin.getCosmeticManager().spawnKillEffect(killer, victim.getLocation());
            }

            MessageUtil.sendActionBar(victim, actionKey, "player", killer != null ? killer.getName() : "?");
            arena.broadcast("game.final-died", "player", victim.getName());
            this.plugin.getSpectateManager().enableSpectate(victim, arena);
            this.giveSpectatorItems(victim);
            if (victimTeam.isEliminated()) {
               this.checkWinCondition(arena);
            }
         } else {
            arena.broadcast(deathKey, "player", victim.getName());
            MessageUtil.sendActionBar(victim, actionKey, "player", killer != null ? killer.getName() : "?");
            ItemStack[] savedArmor = victim.getInventory().getArmorContents();
            Material[] resources = new Material[]{Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD};
            if (killer != null) {
               for(ItemStack item : victim.getInventory().getContents()) {
                  if (item != null) {
                     for(Material res : resources) {
                        if (item.getType() == res) {
                           killer.getInventory().addItem(new ItemStack[]{item.clone()});
                           item.setAmount(0);
                           break;
                        }
                     }
                  }
               }
            } else {
               for(ItemStack item : victim.getInventory().getContents()) {
                  if (item != null) {
                     for(Material res : resources) {
                        if (item.getType() == res) {
                           item.setAmount(0);
                           break;
                        }
                     }
                  }
               }
            }

            victim.setGameMode(GameMode.SPECTATOR);
            Location deathTeleport = arena.getSpectatorSpawn();
            if (deathTeleport == null && victimTeam.getSpawn() != null) {
               deathTeleport = victimTeam.getSpawn();
            }

            if (deathTeleport == null) {
               deathTeleport = victim.getWorld().getSpawnLocation();
            }

            if (deathTeleport != null) {
               Location finalLoc = deathTeleport.clone();
               FoliaUtil.runTaskLater(() -> victim.teleport(finalLoc), 1L);
            }

            int[] respawnCount = new int[]{5};
            int[] taskId = new int[]{-1};
            taskId[0] = FoliaUtil.runTaskTimer(() -> {
               int sec = respawnCount[0];
               if (sec > 0) {
                  String title = "§c§l" + sec;
                  String subtitle = MessageUtil.get("game.respawn-in", "seconds", String.valueOf(sec));
                  victim.sendTitle(title, subtitle, 0, 25, 5);
                  this.playConfigSound(victim, "sounds.respawn-tick");
                  int var10002 = respawnCount[0]--;
               } else {
                  FoliaUtil.cancelTask(taskId[0]);
                  if (arena.getState() == ArenaState.PLAYING && victimTeam.getSpawn() != null) {
                     this.playConfigSound(victim, "sounds.respawn-done");
                     FoliaUtil.runAtEntityLocation(victim, () -> {
                        this.respawnPlayer(victim);
                        victim.teleport(victimTeam.getSpawn());
                         victim.getInventory().clear();
                         victim.getInventory().setArmorContents(savedArmor);
                         victim.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WOODEN_SWORD)});
                         this.plugin.getCompassTracker().giveCompass(victim);
                         victim.setGameMode(GameMode.SURVIVAL);
                         this.applyClassicPvp(victim);
                        MessageUtil.sendMessage(victim, "game.respawned");
                        MessageUtil.sendActionBar(victim, "actionbar.respawned");
                     });
                  }
               }

            }, 0L, 20L);
         }

      }
   }

   public void handleBedBreak(Player breaker, Team team, Arena arena) {
      team.setBedAlive(false);
      Bukkit.getPluginManager().callEvent(new BedBreakEvent(arena, team, breaker));
      arena.broadcast("game.bed-destroyed-team", "team", team.getColoredName(), "player", breaker.getName());
      String bedTitle = MessageUtil.get("game.bed-destroyed-title");
      String bedSubtitle = MessageUtil.get("game.bed-destroyed-subtitle");
      String title = bedTitle != null ? ChatColor.translateAlternateColorCodes('&', bedTitle) : "";
      String subtitle = bedSubtitle != null ? ChatColor.translateAlternateColorCodes('&', bedSubtitle) : "";

      for(UUID uuid : team.getPlayers()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            MessageUtil.sendMessage(player, "game.bed-destroyed");
            MessageUtil.sendActionBar(player, "actionbar.bed-destroyed");
            player.sendTitle(title, subtitle, 0, 40, 10);
            this.playConfigSound(player, "sounds.bed-destroyed");
         }
      }

      for(Team t : arena.getTeams()) {
         if (t == team) continue;
         for(UUID uuid : t.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
               this.playConfigSound(player, "sounds.bed-break-enemy");
            }
         }
      }

      if (breaker != null) {
         MessageUtil.sendActionBar(breaker, "actionbar.bed-broken", "team", team.getColoredName());
         this.plugin.getStatsCache().addStats(breaker.getUniqueId(), "beds-broken", 1);
         this.plugin.getLevelManager().addBedBreakXp(breaker.getUniqueId());
         this.plugin.getScoreboardManager().addBedBreak(arena.getName(), breaker.getUniqueId());
         this.playConfigSound(breaker, "sounds.bed-break");
      }

      boolean allBedsBroken = true;

      for(Team t : arena.getTeams()) {
         if (t.isBedAlive() && t.getAliveCount() > 0) {
            allBedsBroken = false;
            break;
         }
      }

      if (allBedsBroken) {
         arena.broadcast("events.bed-destroy-all");
      }

   }

   public void applyClassicPvp(Player player) {
      if (this.plugin.getConfig().getString("pvp-mode", "MODERN").equalsIgnoreCase("CLASSIC")) {
         player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue((double)100.0F);
      }
   }

   public void playConfigSound(Player player, String path) {
      if (this.plugin.getConfig().getBoolean(path + ".enabled", true)) {
         String soundName = this.plugin.getConfig().getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
         double volume = this.plugin.getConfig().getDouble(path + ".volume", (double)1.0F);
         double pitch = this.plugin.getConfig().getDouble(path + ".pitch", (double)1.0F);

         try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, (float)volume, (float)pitch);
         } catch (IllegalArgumentException var9) {
            this.plugin.getLogger().warning("Invalid sound name in config: " + soundName);
         }

      }
   }

   public void playConfigSoundToAll(Arena arena, String path) {
      for(UUID uuid : arena.getPlayers()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            this.playConfigSound(p, path);
         }
      }

   }

   public void playConfigSoundAt(Location location, String path) {
      if (location != null && location.getWorld() != null) {
         if (this.plugin.getConfig().getBoolean(path + ".enabled", true)) {
            String soundName = this.plugin.getConfig().getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            double volume = this.plugin.getConfig().getDouble(path + ".volume", (double)1.0F);
            double pitch = this.plugin.getConfig().getDouble(path + ".pitch", (double)1.0F);

            try {
               Sound sound = Sound.valueOf(soundName);
               location.getWorld().playSound(location, sound, (float)volume, (float)pitch);
            } catch (IllegalArgumentException var9) {
               this.plugin.getLogger().warning("Invalid sound name in config: " + soundName);
            }

         }
      }
   }

   private void respawnPlayer(Player player) {
      try {
         player.getClass().getMethod("respawn").invoke(player);
      } catch (Exception var5) {
         try {
            player.spigot().respawn();
         } catch (Exception var4) {
         }
      }

   }

   public void spawnGolem(Player player) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena != null && arena.getState() == ArenaState.PLAYING) {
         Team team = arena.getTeamByPlayer(player.getUniqueId());
         if (team != null) {
            int delay = this.plugin.getConfig().getInt("golem.delay", 30);
            if (delay > 0) {
               long lastSpawn = (Long)this.golemCooldowns.getOrDefault(player.getUniqueId(), 0L);
               long remaining = lastSpawn + (long)delay * 1000L - System.currentTimeMillis();
               if (remaining > 0L) {
                  MessageUtil.sendMessage(player, "events.golem-cooldown", "time", String.valueOf(remaining / 1000L + 1L));
                  return;
               }
            }

            Location spawnLoc = player.getLocation();
            IronGolem golem = (IronGolem)player.getWorld().spawn(spawnLoc, IronGolem.class);
            golem.setPlayerCreated(true);
            double health = this.plugin.getConfig().getDouble("golem.health", (double)80.0F);
            golem.setMaxHealth(health);
            golem.setHealth(health);
            double speed = this.plugin.getConfig().getDouble("golem.speed", (double)0.25F);
            if (golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
               golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
            }

            boolean showName = this.plugin.getConfig().getBoolean("golem.show-name", true);
            String[] var10002 = new String[]{"team", null};
            String var10005 = String.valueOf(team.getColor());
            var10002[1] = var10005 + team.getName();
            golem.setCustomName(MessageUtil.get("events.golem-name", var10002));
            golem.setCustomNameVisible(showName);
            boolean collidable = this.plugin.getConfig().getBoolean("golem.collidable", true);
            golem.setCollidable(collidable);
            arena.getGolems().add(golem);
            this.golemTeams.put(golem.getUniqueId(), team);
            if (delay > 0) {
               this.golemCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }

            for(UUID uuid : team.getPlayers()) {
               Player member = Bukkit.getPlayer(uuid);
               if (member != null) {
                  var10002 = new String[]{"team", null};
                  var10005 = String.valueOf(team.getColor());
                  var10002[1] = var10005 + team.getName();
                  MessageUtil.sendMessage(member, "events.golem-spawn", var10002);
               }
            }

            if (!this.golemTargetTasks.containsKey(arena.getName())) {
               int taskId = FoliaUtil.runTaskTimer(() -> this.updateGolemTargets(arena), 0L, 20L);
               this.golemTargetTasks.put(arena.getName(), taskId);
            }

         }
      }
   }

   public Team getGolemTeam(UUID golemUUID) {
      return (Team)this.golemTeams.get(golemUUID);
   }

   private void updateGolemTargets(Arena arena) {
      arena.getGolems().removeIf((g) -> !g.isValid() || g.isDead());
      if (arena.getGolems().isEmpty()) {
         Integer taskId = (Integer)this.golemTargetTasks.remove(arena.getName());
         if (taskId != null) {
            FoliaUtil.cancelTask(taskId);
         }

      } else {
         double followRange = this.plugin.getConfig().getDouble("golem.follow-range", (double)16.0F);
         double followRangeSq = followRange <= (double)0.0F ? Double.MAX_VALUE : followRange * followRange;

         for(IronGolem golem : arena.getGolems()) {
            Team ownerTeam = (Team)this.golemTeams.get(golem.getUniqueId());
            if (ownerTeam != null && !ownerTeam.isEliminated()) {
               Player nearest = null;
               double nearestDist = Double.MAX_VALUE;

               for(UUID uuid : arena.getPlayers()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && p.isOnline() && !p.isDead()) {
                     Team playerTeam = arena.getTeamByPlayer(uuid);
                     if (playerTeam != null && !playerTeam.equals(ownerTeam) && golem.getWorld().equals(p.getWorld())) {
                        double dist = golem.getLocation().distanceSquared(p.getLocation());
                        if (dist < nearestDist && dist <= followRangeSq) {
                           nearestDist = dist;
                           nearest = p;
                        }
                     }
                  }
               }

               if (nearest != null) {
                  golem.setTarget(nearest);
               } else {
                  golem.setTarget((LivingEntity)null);
               }
            } else {
               golem.remove();
               this.golemTeams.remove(golem.getUniqueId());
            }
         }

      }
   }

   public void removeGolems(Arena arena) {
      Integer taskId = (Integer)this.golemTargetTasks.remove(arena.getName());
      if (taskId != null) {
         FoliaUtil.cancelTask(taskId);
      }

      for(IronGolem golem : arena.getGolems()) {
         this.golemTeams.remove(golem.getUniqueId());
         golem.remove();
      }

      arena.getGolems().clear();
   }

   public void spawnDragons(Arena arena) {
      if (!arena.hasDragonsSpawned()) {
         arena.setDragonsSpawned(true);

         for(Team team : arena.getTeams()) {
            if (!team.isEliminated() && team.getSpawn() != null && team.getUpgradeLevel("dragon-buff") > 0) {
               Location spawn = team.getSpawn();
               EnderDragon dragon = (EnderDragon)spawn.getWorld().spawn(spawn, EnderDragon.class);
               String var10001 = String.valueOf(team.getColor());
               dragon.setCustomName(var10001 + "\ud83d\udc09 " + team.getName() + "'s Dragon");
               dragon.setCustomNameVisible(true);
               dragon.setMaxHealth((double)200.0F);
               dragon.setHealth((double)200.0F);
               dragon.setInvulnerable(false);
               arena.getDragons().add(dragon);
               this.dragonTeams.put(dragon.getUniqueId(), team);
            }
         }

         arena.broadcast("events.dragon-spawn");
         arena.broadcastActionBar("actionbar.dragon-spawn");

         for(EnderDragon dragon : arena.getDragons()) {
            try {
               Method setAI = dragon.getClass().getMethod("setAI", Boolean.TYPE);
               setAI.invoke(dragon, false);
            } catch (Exception var6) {
            }
         }

         int targetTaskId = FoliaUtil.runTaskTimer(() -> this.updateDragonTargets(arena), 0L, 20L);
         this.dragonTargetTasks.put(arena.getName(), targetTaskId);
      }
   }

   private void updateDragonTargets(Arena arena) {
      arena.getDragons().removeIf((d) -> !d.isValid() || d.isDead());

      for(EnderDragon dragon : arena.getDragons()) {
         Team ownerTeam = (Team)this.dragonTeams.get(dragon.getUniqueId());
         if (ownerTeam != null && !ownerTeam.isEliminated()) {
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for(UUID uuid : arena.getPlayers()) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null && p.isOnline() && !p.isDead()) {
                  Team playerTeam = arena.getTeamByPlayer(uuid);
                  if (playerTeam != null && !playerTeam.equals(ownerTeam) && dragon.getWorld().equals(p.getWorld())) {
                     double dist = dragon.getLocation().distance(p.getLocation());
                     if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                     }
                  }
               }
            }

            if (nearest != null) {
               Location dLoc = dragon.getLocation();
               Location pLoc = nearest.getLocation();
               Vector dir = pLoc.toVector().subtract(dLoc.toVector());
               double dist = dir.length();
               if (dist > (double)0.5F) {
                  dir.normalize();
                  double speed = Math.min(dist * 0.3, (double)2.0F);
                  dir.multiply(speed);
                  dir.setY(dir.getY() * 0.3 + 0.4);
                  dragon.setVelocity(dir);
               }

               dragon.setTarget(nearest);
            } else {
               Location spawn = ownerTeam.getSpawn();
               if (spawn != null && dragon.getWorld().equals(spawn.getWorld())) {
                  Vector dir = spawn.toVector().subtract(dragon.getLocation().toVector());
                  double dist = dir.length();
                  if (dist > (double)3.0F) {
                     dir.normalize().multiply(Math.min(dist * 0.1, (double)1.0F));
                     dir.setY(dir.getY() * 0.3 + 0.3);
                     dragon.setVelocity(dir);
                  }
               }
            }
         } else {
            dragon.remove();
            this.dragonTeams.remove(dragon.getUniqueId());
         }
      }

   }

   public void removeDragons(Arena arena) {
      Integer taskId = (Integer)this.dragonTargetTasks.remove(arena.getName());
      if (taskId != null) {
         FoliaUtil.cancelTask(taskId);
      }

      for(EnderDragon dragon : arena.getDragons()) {
         this.dragonTeams.remove(dragon.getUniqueId());
         dragon.remove();
      }

      arena.getDragons().clear();
      arena.setDragonsSpawned(false);
   }

   public Team getDragonTeam(UUID dragonUUID) {
      return (Team)this.dragonTeams.get(dragonUUID);
   }

   private void giveSpectatorItems(Player player) {
      ItemStack[] armor = player.getInventory().getArmorContents();
      player.getInventory().clear();
      player.getInventory().setArmorContents(armor);
      ItemStack compass = new ItemStack(Material.COMPASS);
      ItemMeta compassMeta = compass.getItemMeta();
      compassMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.compass-name")));
      compass.setItemMeta(compassMeta);
      player.getInventory().setItem(4, compass);
      ItemStack bed = new ItemStack(Material.RED_BED);
      ItemMeta bedMeta = bed.getItemMeta();
      bedMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("spectator.bed-name")));
      bed.setItemMeta(bedMeta);
      player.getInventory().setItem(8, bed);
   }

   private Color getArmorColor(ChatColor chatColor) {
      Color var10000;
      switch (chatColor) {
         case RED:
         case DARK_RED:
            var10000 = Color.RED;
            break;
         case BLUE:
         case DARK_BLUE:
            var10000 = Color.BLUE;
            break;
         case GREEN:
            var10000 = Color.GREEN;
            break;
         case DARK_GREEN:
            var10000 = Color.fromRGB(43520);
            break;
         case YELLOW:
            var10000 = Color.YELLOW;
            break;
         case AQUA:
         case DARK_AQUA:
            var10000 = Color.AQUA;
            break;
         case LIGHT_PURPLE:
            var10000 = Color.fromRGB(16733695);
            break;
         case DARK_PURPLE:
            var10000 = Color.PURPLE;
            break;
         case WHITE:
            var10000 = Color.WHITE;
            break;
         case GRAY:
            var10000 = Color.GRAY;
            break;
         case DARK_GRAY:
            var10000 = Color.fromRGB(5592405);
            break;
         case BLACK:
            var10000 = Color.BLACK;
            break;
         case GOLD:
            var10000 = Color.ORANGE;
            break;
         default:
            var10000 = Color.WHITE;
      }

      return var10000;
   }

   public void checkWinCondition(Arena arena) {
      List<Team> aliveTeams = new ArrayList();

      for(Team team : arena.getTeams()) {
         if (!team.isEliminated()) {
            aliveTeams.add(team);
         }
      }

      if (aliveTeams.size() <= 1) {
         Team winner = aliveTeams.isEmpty() ? null : (Team)aliveTeams.get(0);
         if (winner != null) {
            this.endGame(arena, winner);
         }
      }

   }

   private static record LastDamager(UUID uuid, long time) {
   }

   public static record RejoinData(String arenaName, String teamName, String playerName) {
   }
}
