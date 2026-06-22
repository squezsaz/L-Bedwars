package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.util.FoliaUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;

public class TabListManager {
   private final LBedWars plugin;
   private int taskId = -1;
   private static final String TEAM_PREFIX = "lbw_";

   public TabListManager(LBedWars plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (!this.plugin.getConfig().getBoolean("tablist.enabled", false)) return;
      int interval = Math.max(this.plugin.getConfig().getInt("tablist.update-interval", 20), 10);
      this.taskId = FoliaUtil.runTaskTimer(() -> {
         for (Player p : Bukkit.getOnlinePlayers()) {
            this.updateTabList(p);
            this.updatePlayerListName(p);
         }
         if (this.plugin.getConfig().getBoolean("tablist.sort-by-team", false)) {
            this.updateTeamSorting();
         }
      }, 0L, (long) interval);
   }

   public void stop() {
      if (this.taskId != -1) {
         FoliaUtil.cancelTask(this.taskId);
         this.taskId = -1;
      }
   }

   public void updateTabList(Player player) {
      String header = this.plugin.getConfig().getString("tablist.header", "&6&lL-BedWars");
      String footer = this.plugin.getConfig().getString("tablist.footer", "&7Online: &a{online}");
      header = this.formatText(player, header);
      footer = this.formatText(player, footer);
      Component headerComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(header);
      Component footerComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(footer);
      player.sendPlayerListHeaderAndFooter(headerComponent, footerComponent);
   }

   public void updatePlayerListName(Player player) {
      String format = this.plugin.getConfig().getString("tablist.player-name-format", "");
      if (format.isEmpty()) return;
      String formatted = this.formatText(player, format);
      Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
      player.playerListName(nameComponent);
   }

   public void updateTeamSorting() {
      Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
      java.util.Map<ChatColor, java.util.List<Player>> groups = new java.util.HashMap<>();
      for (Player p : Bukkit.getOnlinePlayers()) {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(p.getUniqueId());
         ChatColor color = null;
         if (arena != null) {
            Team team = arena.getTeamByPlayer(p.getUniqueId());
            if (team != null) {
               color = team.getColor();
            }
         }
         if (color == null) color = ChatColor.WHITE;
         groups.computeIfAbsent(color, k -> new java.util.ArrayList<>()).add(p);
      }
      for (org.bukkit.scoreboard.Team t : board.getTeams()) {
         if (t.getName().startsWith(TEAM_PREFIX)) {
            t.unregister();
         }
      }
      int index = 0;
      for (ChatColor color : ChatColor.values()) {
         java.util.List<Player> players = groups.get(color);
         if (players == null || players.isEmpty()) continue;
         String teamName = TEAM_PREFIX + String.format("%02d", index++);
         org.bukkit.scoreboard.Team team = board.registerNewTeam(teamName);
         for (Player p : players) {
            team.addPlayer(p);
         }
      }
   }

   private String formatText(Player player, String text) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      text = text.replace("{player}", player.getName());
      text = text.replace("{display-name}", player.getDisplayName());
      text = text.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
      text = text.replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
      text = text.replace("{ping}", String.valueOf(player.getPing()));
      String teamColor = "";
      if (arena != null) {
         Team team = arena.getTeamByPlayer(player.getUniqueId());
         if (team != null) {
            teamColor = team.getColor().toString();
         }
         text = text.replace("{map}", arena.getName());
         text = text.replace("{state}", arena.getState().name());
         text = text.replace("{players}", String.valueOf(arena.getPlayers().size()));
         text = text.replace("{max-players}", String.valueOf(arena.getTeams().size() * arena.getMaxTeamSize()));
      }
      text = text.replace("{team-color}", teamColor);
      Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
      if (papi != null && papi.isEnabled()) {
         text = PlaceholderAPI.setPlaceholders(player, text);
      }
      return text;
   }
}
