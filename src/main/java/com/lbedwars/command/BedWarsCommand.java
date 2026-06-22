package com.lbedwars.command;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.core.GameManager;
import com.lbedwars.party.PartyIntegration;
import com.lbedwars.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class BedWarsCommand implements TabExecutor {
   private final LBedWars plugin;
   private final List<String> subCommands;
   private final List<String> modes;

   public BedWarsCommand(LBedWars plugin) {
      this.plugin = plugin;
      this.subCommands = List.of("help", "join", "randomjoin", "rejoin", "leave", "list", "stats", "level", "dailyreward", "cosmetics");
      this.modes = List.of("solo", "duo", "trio", "quad");
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            switch (args[0].toLowerCase()) {
               case "join" -> this.handleJoin(player, args);
               case "randomjoin" -> this.handleRandomJoin(player, args);
               case "rejoin" -> this.handleRejoin(player);
               case "leave" -> this.handleLeave(player);
               case "list" -> this.handleList(player);
               case "stats" -> this.handleStats(player, args);
               case "level" -> this.handleLevel(player);
               case "dailyreward" -> this.handleDailyReward(player);
               case "cosmetics" -> this.plugin.getCosmeticManager().openCosmeticsGUI(player);
               default -> MessageUtil.sendMessage(player, "general.unknown-command");
            }

            return true;
         } else {
            this.sendHelp(player, label);
            return true;
         }
      } else {
         MessageUtil.sendMessage(sender, "general.only-player");
         return true;
      }
   }

   private void sendHelp(Player player, String label) {
      player.sendMessage(MessageUtil.get("commands.help-header"));
      player.sendMessage(MessageUtil.get("commands.help-category-player"));
      String desc = MessageUtil.get("commands.descriptions.bw-help");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " help", "description", desc != null ? desc : "Help"));
      desc = MessageUtil.get("commands.descriptions.bw-join");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " join", "arg", "arena", "description", desc != null ? desc : "Join"));
      desc = MessageUtil.get("commands.descriptions.bw-randomjoin");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " randomjoin", "arg", "mode", "description", desc != null ? desc : "Random Join"));
      desc = MessageUtil.get("commands.descriptions.bw-rejoin");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " rejoin", "description", desc != null ? desc : "Rejoin"));
      desc = MessageUtil.get("commands.descriptions.bw-leave");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " leave", "description", desc != null ? desc : "Leave"));
      desc = MessageUtil.get("commands.descriptions.bw-list");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " list", "description", desc != null ? desc : "List"));
      desc = MessageUtil.get("commands.descriptions.bw-stats");
      player.sendMessage(MessageUtil.get("commands.help-entry-opt", "command", label + " stats", "arg", "player", "description", desc != null ? desc : "Stats"));
      desc = MessageUtil.get("commands.descriptions.bw-level");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " level", "description", desc != null ? desc : "Level"));
      desc = MessageUtil.get("commands.descriptions.bw-dailyreward");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " dailyreward", "description", desc != null ? desc : "Daily Reward"));
      desc = MessageUtil.get("commands.descriptions.bw-cosmetics");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " cosmetics", "description", desc != null ? desc : "Cosmetics"));
      player.sendMessage(MessageUtil.get("commands.help-footer"));
   }

   private void handleJoin(Player player, String[] args) {
      if (args.length < 2) {
         MessageUtil.sendMessage(player, "command.usage-join");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            MessageUtil.sendMessage(player, "command.arena-not-found", "arenas", String.join(", ", this.plugin.getArenaManager().getArenaNames()));
         } else if (!arena.isEnabled()) {
            MessageUtil.sendMessage(player, "game.arena-not-enabled");
         } else {
            arena.addPlayer(player);
            if (PartyIntegration.isLeader(player.getUniqueId())) {
               this.joinPartyMembers(player, arena);
            }
         }
      }
   }

   private void handleRandomJoin(Player player, String[] args) {
      if (args.length < 2) {
         MessageUtil.sendMessage(player, "command.usage-randomjoin");
      } else {
         String mode = args[1].toLowerCase();
         if (!this.modes.contains(mode)) {
            MessageUtil.sendMessage(player, "command.invalid-mode", "modes", String.join(", ", this.modes));
         } else if (this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null) {
            MessageUtil.sendMessage(player, "general.already-in-game");
         } else {
            List<Arena> candidates = new ArrayList();

            for(Arena arena : this.plugin.getArenaManager().getEnabledArenas()) {
               if (arena.getMode().equalsIgnoreCase(mode) && arena.getSmallestTeam() != null) {
                  candidates.add(arena);
               }
            }

            if (candidates.isEmpty()) {
               MessageUtil.sendMessage(player, "command.randomjoin-no-arena", "mode", mode);
            } else {
               candidates.sort((a, b) -> Integer.compare(b.getPlayers().size(), a.getPlayers().size()));
               Arena target = (Arena)candidates.get(0);
               MessageUtil.sendMessage(player, "command.randomjoin-joined", "arena", target.getName(), "mode", mode);
               target.addPlayer(player);
               if (PartyIntegration.isLeader(player.getUniqueId())) {
                  this.joinPartyMembers(player, target);
               }
            }
         }
      }
   }

   private void handleRejoin(Player player) {
      GameManager.RejoinData data = this.plugin.getGameManager().getRejoinData(player.getUniqueId());
      if (data == null) {
         MessageUtil.sendMessage(player, "command.rejoin-no-data");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(data.arenaName());
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            Team team = arena.getTeamByName(data.teamName());
            if (team != null && team.isBedAlive()) {
               this.plugin.getGameManager().rejoinPlayer(player);
               MessageUtil.sendMessage(player, "command.rejoin-joined", "arena", arena.getName());
            } else {
               MessageUtil.sendMessage(player, "command.rejoin-bed-destroyed");
               this.plugin.getGameManager().removeRejoinData(player.getUniqueId());
            }
         } else {
            MessageUtil.sendMessage(player, "command.rejoin-game-ended");
            this.plugin.getGameManager().removeRejoinData(player.getUniqueId());
         }
      }
   }

   private void joinPartyMembers(Player leader, Arena arena) {
      Team leaderTeam = arena.getTeamByPlayer(leader.getUniqueId());
      for (UUID memberUuid : PartyIntegration.getPartyMembers(leader.getUniqueId())) {
         if (!memberUuid.equals(leader.getUniqueId())) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) {
               if (this.plugin.getArenaManager().getArenaByPlayer(memberUuid) != null) {
                  MessageUtil.sendMessage(leader, "party.join-in-game", "player", member.getName());
               } else if (!arena.isEnabled()) {
                  MessageUtil.sendMessage(leader, "party.join-disabled", "player", member.getName());
               } else {
                  Team targetTeam;
                  if (leaderTeam != null && !leaderTeam.isFull()) {
                     targetTeam = leaderTeam;
                  } else {
                     targetTeam = arena.getSmallestTeam();
                  }
                  if (targetTeam == null) {
                     MessageUtil.sendMessage(leader, "party.join-full", "player", member.getName(), "arena", arena.getName());
                  } else {
                     arena.addPlayerToTeam(member, targetTeam);
                     MessageUtil.sendMessage(leader, "party.join-join", "player", member.getName(), "arena", arena.getName());
                  }
               }
            }
         }
      }
   }

   private void handleLeave(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.plugin.getSpectateManager().isSpectating(uuid)) {
         Arena arena = this.plugin.getArenaManager().getArenaBySpectator(uuid);
         this.plugin.getSpectateManager().disableSpectate(player);
         if (arena != null) {
            arena.removePlayer(player);
         }
      } else {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(uuid);
         if (arena == null) {
            MessageUtil.sendMessage(player, "general.not-in-game");
            return;
         }
         arena.removePlayer(player);
      }
      if (this.plugin.getProxyManager() != null && this.plugin.getProxyManager().isEnabled()) {
         this.plugin.getProxyManager().sendToServer(player);
      } else {
         Location mainLobby = this.plugin.getMainLobby();
         player.teleport(mainLobby != null ? mainLobby : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
      }
   }

   private void handleList(Player player) {
      List<Arena> arenas = this.plugin.getArenaManager().getEnabledArenas();
      player.sendMessage(MessageUtil.get("command.arena-list-title", "count", String.valueOf(arenas.size())));

      for(Arena arena : arenas) {
         int totalSlots = arena.getTeamCount() * arena.getMaxTeamSize();
         player.sendMessage(MessageUtil.get("command.arena-list-entry", "arena", arena.getName(), "players", String.valueOf(arena.getPlayers().size()), "slots", String.valueOf(totalSlots)));
      }

   }

   private void handleStats(Player player, String[] args) {
      UUID target = player.getUniqueId();
      String targetName = player.getName();
      if (args.length >= 2) {
         Player targetPlayer = this.plugin.getServer().getPlayer(args[1]);
         if (targetPlayer != null) {
            target = targetPlayer.getUniqueId();
            targetName = targetPlayer.getName();
         }
      }

      int kills = this.plugin.getDatabase().getStats(target, "kills");
      int deaths = this.plugin.getDatabase().getStats(target, "deaths");
      int finalKills = this.plugin.getDatabase().getStats(target, "final-kills");
      int finalDeaths = this.plugin.getDatabase().getStats(target, "final-deaths");
      int wins = this.plugin.getDatabase().getStats(target, "wins");
      int losses = this.plugin.getDatabase().getStats(target, "losses");
      int beds = this.plugin.getDatabase().getStats(target, "beds-broken");
      int games = this.plugin.getDatabase().getStats(target, "games-played");
      int level = this.plugin.getLevelManager().getLevel(target);
      int xp = this.plugin.getLevelManager().getXp(target);
      double kdr = deaths == 0 ? (double)kills : (double)Math.round((double)kills / (double)deaths * (double)100.0F) / (double)100.0F;
      player.sendMessage(MessageUtil.get("stats.title", "player", targetName));
      player.sendMessage(MessageUtil.get("stats.kills", "kills", String.valueOf(kills)));
      player.sendMessage(MessageUtil.get("stats.deaths", "deaths", String.valueOf(deaths)));
      player.sendMessage(MessageUtil.get("stats.final-kills", "kills", String.valueOf(finalKills)));
      player.sendMessage(MessageUtil.get("stats.final-deaths", "deaths", String.valueOf(finalDeaths)));
      player.sendMessage(MessageUtil.get("stats.wins", "wins", String.valueOf(wins)));
      player.sendMessage(MessageUtil.get("stats.losses", "losses", String.valueOf(losses)));
      player.sendMessage(MessageUtil.get("stats.beds-broken", "beds", String.valueOf(beds)));
      player.sendMessage(MessageUtil.get("stats.games-played", "games", String.valueOf(games)));
      player.sendMessage(MessageUtil.get("stats.kdr", "kdr", String.valueOf(kdr)));
      player.sendMessage(MessageUtil.get("stats.level", "level", String.valueOf(level)));
      int next = this.plugin.getLevelManager().getRequiredXp(level);
      player.sendMessage(MessageUtil.get("stats.xp", "xp", String.valueOf(xp), "next", String.valueOf(next)));
   }

   private void handleLevel(Player player) {
      int level = this.plugin.getLevelManager().getLevel(player.getUniqueId());
      int xp = this.plugin.getLevelManager().getXp(player.getUniqueId());
      int required = this.plugin.getLevelManager().getRequiredXp(level);
      player.sendMessage(MessageUtil.get("level.current", "level", String.valueOf(level), "xp", String.valueOf(xp), "required", String.valueOf(required)));
      int progressBars = (int)((double)xp / (double)required * (double)20.0F);
      StringBuilder progress = new StringBuilder("&7[");

      for(int i = 0; i < 20; ++i) {
         progress.append(i < progressBars ? "&a|" : "&7|");
      }

      progress.append("&7]");
      player.sendMessage(MessageUtil.get("level.progress", "progress", progress.toString()));
   }

   private void handleDailyReward(Player player) {
      this.plugin.getDailyRewardManager().claim(player);
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!(sender instanceof Player)) {
         return List.of();
      } else if (args.length == 1) {
         return (List)this.subCommands.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else {
         if (args.length == 2) {
            switch (args[0].toLowerCase()) {
               case "join" -> {
                  return (List)this.plugin.getArenaManager().getEnabledArenas().stream().map(Arena::getName).filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
               }
               case "randomjoin" -> {
                  return (List)this.modes.stream().filter((m) -> m.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
               }
               case "stats" -> {
                  return (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
               }
            }
         }

         return List.of();
      }
   }
}
