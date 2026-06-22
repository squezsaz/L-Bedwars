package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.util.MessageUtil;
import java.text.DecimalFormat;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {
   private final LBedWars plugin;
   private final DecimalFormat df = new DecimalFormat("#.##");

   public PlaceholderManager(LBedWars plugin) {
      this.plugin = plugin;
   }

   public @NotNull String getIdentifier() {
      return "lbedwars";
   }

   public @NotNull String getAuthor() {
      return "L-BedWars";
   }

   public @NotNull String getVersion() {
      return "1.0.0";
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
      if (offlinePlayer == null) {
         return "";
      } else {
         UUID uuid = offlinePlayer.getUniqueId();
         Player player = offlinePlayer.getPlayer();
         String var10000;
         switch (params.toLowerCase()) {
            case "kills":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "kills"));
               break;
            case "deaths":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "deaths"));
               break;
            case "kd":
               var10000 = this.formatRatio(this.plugin.getStatsCache().getStats(uuid, "kills"), this.plugin.getStatsCache().getStats(uuid, "deaths"));
               break;
            case "finalkills":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "final-kills"));
               break;
            case "finaldeaths":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "final-deaths"));
               break;
            case "finalkd":
               var10000 = this.formatRatio(this.plugin.getStatsCache().getStats(uuid, "final-kills"), this.plugin.getStatsCache().getStats(uuid, "final-deaths"));
               break;
            case "wins":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "wins"));
               break;
            case "losses":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "losses"));
               break;
            case "wlr":
               var10000 = this.formatRatio(this.plugin.getStatsCache().getStats(uuid, "wins"), this.plugin.getStatsCache().getStats(uuid, "losses"));
               break;
            case "bedsbroken":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "beds-broken"));
               break;
            case "gamesplayed":
               var10000 = String.valueOf(this.plugin.getStatsCache().getStats(uuid, "games-played"));
               break;
            case "level":
               var10000 = String.valueOf(this.plugin.getLevelManager().getLevel(uuid));
               break;
            case "xp":
               var10000 = String.valueOf(this.plugin.getLevelManager().getXp(uuid));
               break;
            case "xp_required":
               int lvlR = this.plugin.getLevelManager().getLevel(uuid);
               var10000 = String.valueOf(this.plugin.getLevelManager().getRequiredXp(lvlR));
               break;
            case "xp_percent":
               int curP = this.plugin.getLevelManager().getXp(uuid);
               int lvlP = this.plugin.getLevelManager().getLevel(uuid);
               int reqP = this.plugin.getLevelManager().getRequiredXp(lvlP);
               var10000 = reqP > 0 ? Math.min(curP * 100 / reqP, 100) + "%" : "0%";
               break;
            case "xp_progress":
               int curB = this.plugin.getLevelManager().getXp(uuid);
               int lvlB = this.plugin.getLevelManager().getLevel(uuid);
               int reqB = this.plugin.getLevelManager().getRequiredXp(lvlB);
               int filled = reqB > 0 ? Math.min(curB * 20 / reqB, 20) : 0;
               var10000 = "▬".repeat(filled);
               var10000 = MessageUtil.colorize("&a" + var10000 + "&7" + "▬".repeat(20 - filled));
               break;
            case "balance":
               var10000 = String.valueOf((int)this.plugin.getEconomyManager().getBalance(offlinePlayer));
               break;
            case "balance_formatted":
               var10000 = this.plugin.getEconomyManager().format(this.plugin.getEconomyManager().getBalance(offlinePlayer));
               break;
            case "active_killeffect":
               var10000 = this.plugin.getCosmeticManager().getKillEffect(uuid);
               break;
            case "active_victorydance":
               var10000 = this.plugin.getCosmeticManager().getVictoryDance(uuid);
               break;
            case "active_trail":
               var10000 = this.plugin.getCosmeticManager().getTrail(uuid);
               break;
            case "active_victorysong":
               var10000 = this.plugin.getCosmeticManager().getVictorySong(uuid);
               break;
            case "arena":
               Arena a = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = a != null ? a.getName() : "";
               break;
            case "gamestate":
               Arena as = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = as != null ? as.getState().name() : "";
               break;
            case "gametime":
               Arena at = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (at == null) {
                  var10000 = "";
               } else {
                  int t = at.getGameTime();
                  var10000 = String.format("%02d:%02d", t / 60, t % 60);
               }
               break;
            case "isplaying":
               Arena ap = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = ap != null && ap.getPlayers().contains(uuid) ? "YES" : "NO";
               break;
            case "isspectating":
               Arena asp = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = asp != null && asp.getSpectators().contains(uuid) ? "YES" : "NO";
               break;
            case "players":
               Arena apl = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = apl != null ? String.valueOf(apl.getPlayers().size()) : "0";
               break;
            case "maxplayers":
               Arena amx = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               var10000 = amx != null ? String.valueOf(amx.getTeams().stream().mapToInt(Team::getMaxSize).sum()) : "0";
               break;
            case "team":
               Arena atn = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (atn == null) {
                  var10000 = "";
               } else {
                  Team t = atn.getTeamByPlayer(uuid);
                  var10000 = t != null ? t.getName() : "";
               }
               break;
            case "teamcolor":
               Arena atc = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (atc == null) {
                  var10000 = "";
               } else {
                  Team tc = atc.getTeamByPlayer(uuid);
                  var10000 = tc != null ? "&" + tc.getColor().getChar() : "";
               }
               break;
            case "team_colored":
               Arena atcl = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (atcl == null) {
                  var10000 = "";
               } else {
                  Team tc2 = atcl.getTeamByPlayer(uuid);
                  var10000 = tc2 != null ? tc2.getColoredName() : "";
               }
               break;
            case "teammates":
               Arena atm = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (atm == null) {
                  var10000 = "";
               } else {
                  Team tm = atm.getTeamByPlayer(uuid);
                  var10000 = tm != null ? String.valueOf(tm.getTotalCount()) : "0";
               }
               break;
            case "teammates_alive":
               Arena atma = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (atma == null) {
                  var10000 = "";
               } else {
                  Team tma = atma.getTeamByPlayer(uuid);
                  var10000 = tma != null ? String.valueOf(tma.getAliveCount()) : "0";
               }
               break;
            case "hasbed":
               Arena ahb = this.plugin.getArenaManager().getArenaByPlayer(uuid);
               if (ahb == null) {
                  var10000 = "";
               } else {
                  Team thb = ahb.getTeamByPlayer(uuid);
                  var10000 = thb != null && thb.isBedAlive() ? "YES" : "NO";
               }
               break;
            default:
               var10000 = "";
         }

         return var10000;
      }
   }

   private String formatRatio(int a, int b) {
      if (b == 0) {
         return a > 0 ? this.df.format((long)a) : "0.00";
      } else {
         return this.df.format((double)a / (double)b);
      }
   }
}
