package com.lbedwars.listener;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener implements Listener {
   private final LBedWars plugin;

   public ChatListener(LBedWars plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null) {
            ArenaState state = arena.getState();
            if (state == ArenaState.PLAYING || state == ArenaState.WAITING) {
               if (this.plugin.getConfig().getBoolean("chat.enabled", true)) {
                  String message = event.getMessage();
                  event.setCancelled(true);
                  if (state == ArenaState.WAITING) {
                      String rawFormat = this.plugin.getConfig().getString("chat.waiting-format", "&7[Lobby] {player}&f: {message}");
                     rawFormat = this.setPlaceholders(player, rawFormat);
                     String format = ChatColor.translateAlternateColorCodes('&', rawFormat);
                     Team team = arena.getTeamByPlayer(player.getUniqueId());
                     String playerColor = team != null ? team.getColor().toString() : ChatColor.WHITE.toString();
                     String var27 = format.replace("{player}", playerColor + player.getName());
                     String var28 = String.valueOf(ChatColor.WHITE);
                     String formatted = var27.replace("{message}", var28 + message);

                     for(Player p : Bukkit.getOnlinePlayers()) {
                        Arena pArena = this.plugin.getArenaManager().getArenaByPlayer(p.getUniqueId());
                        if (pArena != null && pArena.equals(arena)) {
                           p.sendMessage(formatted);
                        }
                     }

                  } else {
                     Team team = arena.getTeamByPlayer(player.getUniqueId());
                     if (team != null) {
                        String globalPrefix = this.plugin.getConfig().getString("chat.global-prefix", "!");
                        boolean isGlobal = message.startsWith(globalPrefix);
                        if (isGlobal) {
                           message = message.substring(globalPrefix.length()).trim();
                        }

                        String formatKey = isGlobal ? "chat.global-format" : "chat.team-format";
                        String rawFormat = this.plugin.getConfig().getString(formatKey, "{player}&f: {message}");
                        rawFormat = this.setPlaceholders(player, rawFormat);
                        String format = ChatColor.translateAlternateColorCodes('&', rawFormat);
                        String var10000 = String.valueOf(team.getColor());
                        String displayName = var10000 + player.getName();
                        var10000 = format.replace("{player}", displayName);
                        String var10002 = String.valueOf(ChatColor.WHITE);
                        String formatted = var10000.replace("{message}", var10002 + message);

                        for(Player p : Bukkit.getOnlinePlayers()) {
                           Arena pArena = this.plugin.getArenaManager().getArenaByPlayer(p.getUniqueId());
                           if (pArena != null && pArena.equals(arena)) {
                              if (!isGlobal) {
                                 Team pTeam = arena.getTeamByPlayer(p.getUniqueId());
                                 if (pTeam == null || !pTeam.equals(team)) {
                                    continue;
                                 }
                              }

                              p.sendMessage(formatted);
                           }
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private String setPlaceholders(Player player, String text) {
      Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
      if (papi != null && papi.isEnabled()) {
         return PlaceholderAPI.setPlaceholders(player, text);
      }
      return text;
   }
}
