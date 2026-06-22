package com.lbedwars.util;

import com.lbedwars.LBedWars;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public static String colorize(String text) {
      if (text == null) {
         return "";
      } else {
         Matcher matcher = HEX_PATTERN.matcher(text);
         StringBuilder buffer = new StringBuilder();

         while(matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public static List<String> colorize(List<String> list) {
      return list.stream().map(MessageUtil::colorize).toList();
   }

   public static void send(CommandSender sender, String message) {
      if (message != null && !message.isEmpty()) {
         sender.sendMessage(colorize(message));
      }
   }

   public static void send(Player player, String message) {
      if (message != null && !message.isEmpty()) {
         player.sendMessage(colorize(message));
      }
   }

   public static void sendMessage(Player player, String path, String... placeholders) {
      String msg = LBedWars.getInstance().getLanguageManager().getMessage(path);
      if (msg != null && !msg.isEmpty()) {
         for(int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
               msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
         }

         String var10001 = LBedWars.getInstance().getLanguageManager().getPrefix();
         player.sendMessage(colorize(var10001 + msg));
      }
   }

   public static void sendMessage(CommandSender sender, String path, String... placeholders) {
      String msg = LBedWars.getInstance().getLanguageManager().getMessage(path);
      if (msg != null && !msg.isEmpty()) {
         for(int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
               msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
         }

         String var10001 = LBedWars.getInstance().getLanguageManager().getPrefix();
         sender.sendMessage(colorize(var10001 + msg));
      }
   }

   public static String get(String path, String... placeholders) {
      String msg = LBedWars.getInstance().getLanguageManager().getMessage(path);
      if (msg == null) {
         return "";
      } else {
         for(int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
               msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
         }

         return colorize(msg);
      }
   }

   public static void sendActionBar(Player player, String path, String... placeholders) {
      String msg = LBedWars.getInstance().getLanguageManager().getMessage(path);
      if (msg != null && !msg.isEmpty()) {
         for(int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
               msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
         }

         player.sendActionBar(colorize(msg));
      }
   }
}
