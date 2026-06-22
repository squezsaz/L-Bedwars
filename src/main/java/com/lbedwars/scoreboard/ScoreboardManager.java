package com.lbedwars.scoreboard;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.language.LanguageManager;
import com.lbedwars.util.FoliaUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardManager {
   private final LBedWars plugin;
   private final Map<String, Integer> arenaTasks;
   private final Map<String, Integer> waitingTasks;
   private final Map<String, Map<UUID, Integer>> kills;
   private final Map<String, Map<UUID, Integer>> finalKills;
   private final Map<String, Map<UUID, Integer>> bedsBroken;
   private int lobbyUpdateTaskId;

   public ScoreboardManager(LBedWars plugin) {
      this.plugin = plugin;
      this.arenaTasks = new HashMap();
      this.waitingTasks = new HashMap();
      this.kills = new HashMap();
      this.finalKills = new HashMap();
      this.bedsBroken = new HashMap();
      this.lobbyUpdateTaskId = -1;
   }

   public void startLobbyScoreboard() {
      int interval = Math.max(this.plugin.getConfig().getInt("scoreboard.update-interval", 2), 2);
      this.lobbyUpdateTaskId = FoliaUtil.runTaskTimer(() -> {
         if (this.plugin.getConfig().getBoolean("scoreboard.lobby.enabled", true)) {
            for(Player p : Bukkit.getOnlinePlayers()) {
               if (this.isInLobbyWorld(p)) {
                  this.updateLobbyScoreboard(p);
               }
            }

         }
      }, (long)interval * 20L, (long)interval * 20L);
   }

   public void stopLobbyScoreboard() {
      if (this.lobbyUpdateTaskId != -1) {
         FoliaUtil.cancelTask(this.lobbyUpdateTaskId);
         this.lobbyUpdateTaskId = -1;
      }

      for(Player p : Bukkit.getOnlinePlayers()) {
         if (this.isInLobbyWorld(p)) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
         }
      }

   }

   public boolean isInLobbyWorld(Player player) {
      List<String> worlds = this.plugin.getConfig().getStringList("scoreboard.lobby.worlds");
      if (worlds.isEmpty()) {
         return false;
      } else if (this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null) {
         return false;
      } else {
         return this.plugin.getArenaManager().getArenaBySpectator(player.getUniqueId()) != null ? false : worlds.contains(player.getWorld().getName());
      }
   }

   private void updateLobbyScoreboard(Player player) {
      UUID uuid = player.getUniqueId();
      LanguageManager lang = this.plugin.getLanguageManager();
      String title = lang.getMessage("lobby-scoreboard.title");
      if (title == null) {
         title = "&6&lL-BEDWARS";
      }

      int kills = this.plugin.getStatsCache().getStats(uuid, "kills");
      int deaths = this.plugin.getStatsCache().getStats(uuid, "deaths");
      int wins = this.plugin.getStatsCache().getStats(uuid, "wins");
      int level = this.plugin.getLevelManager().getLevel(uuid);
      int xp = this.plugin.getLevelManager().getXp(uuid);
      int required = this.plugin.getLevelManager().getRequiredXp(level);
      double balance = this.plugin.getEconomyManager().getBalance(player);
      String kdStr;
      if (deaths == 0) {
         kdStr = String.valueOf(kills);
      } else {
         kdStr = String.format("%.2f", (double)kills / (double)deaths);
      }

      int percent = required > 0 ? (int)((double)xp / (double)required * (double)100.0F) : 0;
      if (percent > 100) {
         percent = 100;
      }

      String bar = this.generateProgressBar(percent);
      String playerLine = lang.getMessage("lobby-scoreboard.player");
      String levelLine = lang.getMessage("lobby-scoreboard.level");
      String progressLine = lang.getMessage("lobby-scoreboard.level-progress");
      String killsLine = lang.getMessage("lobby-scoreboard.kills");
      String winsLine = lang.getMessage("lobby-scoreboard.wins");
      String kdLine = lang.getMessage("lobby-scoreboard.kd");
      String coinsLine = lang.getMessage("lobby-scoreboard.coins");
      if (playerLine == null) {
         playerLine = "&fPlayer: &a{player}";
      }

      if (levelLine == null) {
         levelLine = "&fLevel: &a{level}";
      }

      if (progressLine == null) {
         progressLine = "&7{bar} &e{percent}%";
      }

      if (killsLine == null) {
         killsLine = "&fKills: &a{kills}";
      }

      if (winsLine == null) {
         winsLine = "&fWins: &a{wins}";
      }

      if (kdLine == null) {
         kdLine = "&fK/D: &a{kd}";
      }

      if (coinsLine == null) {
         coinsLine = "&fCoins: &6{coins}";
      }

      playerLine = playerLine.replace("{player}", player.getName());
      levelLine = levelLine.replace("{level}", String.valueOf(level));
      progressLine = progressLine.replace("{bar}", bar).replace("{percent}", String.valueOf(percent));
      killsLine = killsLine.replace("{kills}", String.valueOf(kills));
      winsLine = winsLine.replace("{wins}", String.valueOf(wins));
      kdLine = kdLine.replace("{kd}", kdStr);
      coinsLine = coinsLine.replace("{coins}", this.plugin.getEconomyManager().format(balance));
      Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
      Objective obj = board.registerNewObjective("lobby", "dummy", ChatColor.translateAlternateColorCodes('&', title));
      obj.setDisplaySlot(DisplaySlot.SIDEBAR);
      List<String> lines = new ArrayList();
      lines.add(playerLine);
      lines.add("  ");
      lines.add(levelLine);
      lines.add(progressLine);
      lines.add("   ");
      lines.add(killsLine);
      lines.add(winsLine);
      lines.add(kdLine);
      lines.add("    ");
      lines.add(coinsLine);
      int score = lines.size();

      for(String line : lines) {
         String entry = ChatColor.translateAlternateColorCodes('&', line);
         if (entry.length() > 40) {
            entry = entry.substring(0, 40);
         }

         Score s = obj.getScore(entry);
         s.setScore(score--);
         if (this.plugin.getConfig().getBoolean("scoreboard.hide-numbers", false)) {
            this.hideScoreNumber(s);
         }
      }

      player.setScoreboard(board);
   }

   private String generateProgressBar(int percent) {
      int filled = percent / 10;
      StringBuilder sb = new StringBuilder("&a");

      for(int i = 0; i < 10; ++i) {
         if (i == filled) {
            sb.append("&7");
         }

         sb.append("█");
      }

      return sb.toString();
   }

   private String getModeKey(String mode, String key) {
      String specific = LBedWars.getInstance().getLanguageManager().getMessage("scoreboard." + mode + "." + key);
      return specific != null ? specific : LBedWars.getInstance().getLanguageManager().getMessage("scoreboard." + key);
   }

   public void startWaitingScoreboard(Arena arena) {
      if (this.plugin.getConfig().getBoolean("scoreboard.waiting.enabled", true)) {
         String arenaName = arena.getName();
         if (!this.waitingTasks.containsKey(arenaName)) {
            int taskId = FoliaUtil.runTaskTimer(() -> {
               if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
                  this.stopWaitingScoreboard(arena);
               } else {
                  this.updateWaitingScoreboard(arena);
               }
            }, 0L, 20L);
            this.waitingTasks.put(arenaName, taskId);
         }
      }
   }

   public void stopWaitingScoreboard(Arena arena) {
      String arenaName = arena.getName();
      Integer taskId = (Integer)this.waitingTasks.remove(arenaName);
      if (taskId != null) {
         FoliaUtil.cancelTask(taskId);
      }

      for(UUID uuid : arena.getPlayers()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
         }
      }

   }

   private void updateWaitingScoreboard(Arena arena) {
      String arenaName = arena.getName();
      int playerCount = arena.getPlayers().size();
      int maxPlayers = arena.getTeamCount() * arena.getMaxTeamSize();
      int countdown = arena.getLobbyCountdown();
      boolean countdownRunning = arena.isLobbyCountdownRunning();
      LanguageManager lang = this.plugin.getLanguageManager();
      String title = lang.getMessage("waiting-scoreboard.title");
      if (title == null) {
         title = "&6&lL-BEDWARS";
      }

      String playerLabel = lang.getMessage("waiting-scoreboard.player");
      String playersLabel = lang.getMessage("waiting-scoreboard.players");
      String timeLabel = lang.getMessage("waiting-scoreboard.time");
      String arenaLabel = lang.getMessage("waiting-scoreboard.arena");
      if (playerLabel == null) {
         playerLabel = "&fPlayer: &a{player}";
      }

      if (playersLabel == null) {
         playersLabel = "&fPlayers: &a{count}&7/&a{max}";
      }

      if (timeLabel == null) {
         timeLabel = "&fStart: &e{time}";
      }

      if (arenaLabel == null) {
         arenaLabel = "&fArena: &e{arena}";
      }

      String titleColor = ChatColor.translateAlternateColorCodes('&', title);

      for(UUID uuid : arena.getPlayers()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            String playerLine = playerLabel.replace("{player}", player.getName());
            String playersLine = playersLabel.replace("{count}", String.valueOf(playerCount)).replace("{max}", String.valueOf(maxPlayers));
            String arenaLine = arenaLabel.replace("{arena}", arenaName);
            String timeLine;
            if (countdownRunning && countdown > 0) {
               timeLine = timeLabel.replace("{time}", this.formatTime(countdown));
            } else if (arena.getState() == ArenaState.STARTING) {
               int startSec = arena.getLobbyCountdown();
               if (startSec > 0) {
                  timeLine = timeLabel.replace("{time}", this.formatTime(startSec));
               } else {
                  timeLine = timeLabel.replace("{time}", lang.getMessage("waiting-scoreboard.starting") != null ? lang.getMessage("waiting-scoreboard.starting") : "&eStarting...");
               }
            } else {
               timeLine = timeLabel.replace("{time}", lang.getMessage("waiting-scoreboard.waiting") != null ? lang.getMessage("waiting-scoreboard.waiting") : "&eWaiting...");
            }

            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("waiting", "dummy", titleColor);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            List<String> lines = new ArrayList();
            lines.add(playerLine);
            lines.add("  ");
            lines.add(playersLine);
            lines.add("   ");
            lines.add(timeLine);
            lines.add("    ");
            lines.add(arenaLine);
            int score = lines.size();

            for(String line : lines) {
               String entry = ChatColor.translateAlternateColorCodes('&', line);
               if (entry.length() > 40) {
                  entry = entry.substring(0, 40);
               }

               Score s = obj.getScore(entry);
               s.setScore(score--);
               if (this.plugin.getConfig().getBoolean("scoreboard.hide-numbers", false)) {
                  this.hideScoreNumber(s);
               }
            }

            player.setScoreboard(board);
         }
      }

   }

   private String formatTime(int seconds) {
      int minutes = seconds / 60;
      int secs = seconds % 60;
      return String.format("%02d:%02d", minutes, secs);
   }

   public void addKill(String arenaName, UUID player) {
      this.kills.computeIfAbsent(arenaName, (k) -> new HashMap<>()).merge(player, 1, Integer::sum);
   }

   public void addFinalKill(String arenaName, UUID player) {
      this.finalKills.computeIfAbsent(arenaName, (k) -> new HashMap<>()).merge(player, 1, Integer::sum);
   }

   public void addBedBreak(String arenaName, UUID player) {
      this.bedsBroken.computeIfAbsent(arenaName, (k) -> new HashMap<>()).merge(player, 1, Integer::sum);
   }

   public void startScoreboard(Arena arena) {
      String arenaName = arena.getName();
      this.kills.remove(arenaName);
      this.finalKills.remove(arenaName);
      this.bedsBroken.remove(arenaName);
      int taskId = FoliaUtil.runTaskTimer(() -> {
         if (arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.STARTING) {
            this.updateScoreboard(arena);
         }
      }, 20L, 20L);
      this.arenaTasks.put(arenaName, taskId);
   }

   public void stopScoreboard(Arena arena) {
      String arenaName = arena.getName();
      Integer taskId = (Integer)this.arenaTasks.remove(arenaName);
      if (taskId != null) {
         FoliaUtil.cancelTask(taskId);
      }

      this.kills.remove(arenaName);
      this.finalKills.remove(arenaName);
      this.bedsBroken.remove(arenaName);

      for(UUID uuid : arena.getPlayers()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            if (this.isInLobbyWorld(p)) {
               this.updateLobbyScoreboard(p);
            }
         }
      }

   }

   private int[] getUpgradeTimes(String mode, String type) {
      String path = type + "-upgrade-times." + mode;
      List<Integer> list = LBedWars.getInstance().getConfig().getIntegerList(path);
      if (list == null || list.isEmpty()) {
         list = LBedWars.getInstance().getConfig().getIntegerList(type + "-upgrade-times.solo");
      }

      return list != null && !list.isEmpty() ? list.stream().mapToInt(Integer::intValue).toArray() : new int[]{300, 600, 900};
   }

   private int getDragonSpawnTime(String mode) {
      return LBedWars.getInstance().getConfig().getInt("dragon-spawn-times." + mode, 600);
   }

   private String formatTierDisplay(int[] times, int gameTime) {
      if (times.length == 0) {
         return "---";
      } else {
         int currentTier = 1;
         int nextTier = 0;
         int remaining = -1;

         for(int i = 0; i < times.length; ++i) {
            if (gameTime >= times[i]) {
               currentTier = i + 2;
            } else if (nextTier == 0) {
               nextTier = i + 2;
               remaining = times[i] - gameTime;
            }
         }

         if (remaining >= 0) {
            return currentTier + " §7→ §e" + nextTier + " §7(" + String.format("%02d:%02d", remaining / 60, remaining % 60) + ")";
         } else {
            return currentTier + " §7(MAX)";
         }
      }
   }

   private void updateScoreboard(Arena arena) {
      String arenaName = arena.getName();
      String mode = arena.getModeName();
      int gameTime = arena.getGameTime();
      int minutes = gameTime / 60;
      int seconds = gameTime % 60;
      String timeStr = String.format("%02d:%02d", minutes, seconds);
      int[] diamondTimes = this.getUpgradeTimes(mode, "diamond");
      int[] emeraldTimes = this.getUpgradeTimes(mode, "emerald");
      String diamondDisplay = this.formatTierDisplay(diamondTimes, gameTime);
      String emeraldDisplay = this.formatTierDisplay(emeraldTimes, gameTime);
      String killsLabel = this.getModeKey(mode, "kills");
      String finalKillsLabel = this.getModeKey(mode, "final-kills");
      String bedsLabel = this.getModeKey(mode, "beds-broken");
      String diamondLabel = this.getModeKey(mode, "diamond");
      String emeraldLabel = this.getModeKey(mode, "emerald");
      String timeLabel = this.getModeKey(mode, "game-time");
      String title = this.getModeKey(mode, "title");
      if (killsLabel == null) {
         killsLabel = "Kills";
      }

      if (finalKillsLabel == null) {
         finalKillsLabel = "Final Kills";
      }

      if (bedsLabel == null) {
         bedsLabel = "Beds";
      }

      if (diamondLabel == null) {
         diamondLabel = "Diamond";
      }

      if (emeraldLabel == null) {
         emeraldLabel = "Emerald";
      }

      if (timeLabel == null) {
         timeLabel = "Game Time";
      }

      if (title == null) {
         title = "L-BEDWARS";
      }

      String finalTitle = ChatColor.translateAlternateColorCodes('&', title);

      for(UUID uuid : arena.getPlayers()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("bedwars", "dummy", finalTitle);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            int score = 15;
             Set<String> lines = new LinkedHashSet();
             String datePattern = this.plugin.getConfig().getString("scoreboard.date-format", "d MMMM yyyy HH:mm");
             String dateLocale = this.plugin.getConfig().getString("scoreboard.date-locale", "tr-TR");
             String dateLabel = this.getModeKey(mode, "date");
             if (dateLabel == null) {
                dateLabel = "§7{date}";
             }
             try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern, java.util.Locale.forLanguageTag(dateLocale));
                lines.add(ChatColor.translateAlternateColorCodes('&', dateLabel.replace("{date}", LocalDateTime.now().format(dtf))));
             } catch (Exception e) {
                lines.add(ChatColor.translateAlternateColorCodes('&', dateLabel.replace("{date}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))));
             }
             lines.add("§7╔══════════════╗");

             for(Team team : arena.getTeams()) {
               if (!team.getPlayers().isEmpty()) {
                  String bedSymbol = team.isBedAlive() ? "§a✔" : "§c✘";
                  int var10000 = team.getTotalCount();
                  String aliveStr = var10000 + "/" + team.getMaxSize();
                  String colorCode = team.getColor().toString();
                  lines.add(colorCode + team.getName() + " " + bedSymbol + " " + aliveStr);
               }
            }

            lines.add("§7╚══════════════╝");
            lines.add(" ");
            lines.add("§7" + killsLabel + ": §f" + this.getStat((Map)this.kills.get(arenaName), uuid));
            lines.add("§7" + finalKillsLabel + ": §f" + this.getStat((Map)this.finalKills.get(arenaName), uuid));
            lines.add("§7" + bedsLabel + ": §f" + this.getStat((Map)this.bedsBroken.get(arenaName), uuid));
            if (arena.getState() == ArenaState.PLAYING) {
               lines.add("  ");
               lines.add("§6" + diamondLabel + " " + diamondDisplay);
               lines.add("§2" + emeraldLabel + " " + emeraldDisplay);
               lines.add("    ");
               int dragonTime = this.getDragonSpawnTime(mode);
               if (arena.hasDragonsSpawned()) {
                  String dragonActive = this.getModeKey(mode, "dragon-active");
                  if (dragonActive == null) {
                     dragonActive = "§cDragon: §aACTIVE";
                  }

                  lines.add(ChatColor.translateAlternateColorCodes('&', dragonActive));
               } else {
                  int remaining = dragonTime - gameTime;
                  if (remaining > 0) {
                     String dragonCountdown = this.getModeKey(mode, "dragon-countdown");
                     if (dragonCountdown == null) {
                        dragonCountdown = "§cDragon: §e{time}";
                     }

                     dragonCountdown = dragonCountdown.replace("{time}", String.format("%02d:%02d", remaining / 60, remaining % 60));
                     lines.add(ChatColor.translateAlternateColorCodes('&', dragonCountdown));
                  } else {
                     String dragonActive = this.getModeKey(mode, "dragon-active");
                     if (dragonActive == null) {
                        dragonActive = "§cDragon: §aACTIVE";
                     }

                     lines.add(ChatColor.translateAlternateColorCodes('&', dragonActive));
                  }
               }

               lines.add("   ");
               lines.add("§7" + timeLabel + ": §e" + timeStr);
            }

            for(String line : lines) {
               if (!line.isEmpty()) {
                  String entryName = ChatColor.translateAlternateColorCodes('&', line);
                  if (entryName.length() > 40) {
                     entryName = entryName.substring(0, 40);
                  }

                  Score s = obj.getScore(entryName);
                  s.setScore(score--);
                  if (this.plugin.getConfig().getBoolean("scoreboard.hide-numbers", false)) {
                     this.hideScoreNumber(s);
                  }
               }
            }

            player.setScoreboard(board);
         }
      }

   }

   private int getStat(Map<UUID, Integer> map, UUID uuid) {
      return map == null ? 0 : (Integer)map.getOrDefault(uuid, 0);
   }

   public List<TopPlayer> getTopPlayers(String arenaName, int limit) {
      Map<UUID, Integer> arenaKills = (Map)this.kills.get(arenaName);
      Map<UUID, Integer> arenaFinalKills = (Map)this.finalKills.get(arenaName);
      Map<UUID, Integer> arenaBeds = (Map)this.bedsBroken.get(arenaName);
      if (arenaKills != null && !arenaKills.isEmpty()) {
         List<TopPlayer> list = new ArrayList();

         for(UUID uuid : arenaKills.keySet()) {
            int k = (Integer)arenaKills.getOrDefault(uuid, 0);
            int fk = arenaFinalKills != null ? (Integer)arenaFinalKills.getOrDefault(uuid, 0) : 0;
            int b = arenaBeds != null ? (Integer)arenaBeds.getOrDefault(uuid, 0) : 0;
            int score = k * 10 + fk * 15 + b * 25;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) {
               name = "?";
            }

            list.add(new TopPlayer(uuid, name, k, fk, b, score));
         }

         list.sort((a, bx) -> bx.score - a.score);
         return list.size() > limit ? list.subList(0, limit) : list;
      } else {
         return List.of();
      }
   }

   private void hideScoreNumber(Score score) {
      try {
         Class<?> nfClass = Class.forName("org.bukkit.scoreboard.NumberFormat");
         Object blank = nfClass.getMethod("blank").invoke((Object)null);
         score.getClass().getMethod("setNumberFormat", nfClass).invoke(score, blank);
      } catch (Exception var4) {
      }

   }

   public static record TopPlayer(UUID uuid, String name, int kills, int finalKills, int bedsBroken, int score) {
   }
}
