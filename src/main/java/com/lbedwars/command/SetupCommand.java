package com.lbedwars.command;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.listener.SetupListener;
import com.lbedwars.util.MessageUtil;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class SetupCommand implements TabExecutor {
   private final LBedWars plugin;
   private final List<String> subCommands;
   private final List<String> genTypes;

   public SetupCommand(LBedWars plugin) {
      this.plugin = plugin;
      this.subCommands = List.of("help", "setspawn", "setbed", "setgenerator", "setshop", "setupgrade", "setregion", "done");
      this.genTypes = List.of("IRON", "GOLD", "DIAMOND", "EMERALD");
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("bedwars.setup")) {
            MessageUtil.sendMessage(player, "general.no-permission");
            return true;
         } else if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            SetupListener setupListener = this.plugin.getSetupListener();
            if (setupListener == null) {
               player.sendMessage("§cSetup listener bulunamadi!");
               return true;
            } else if (!setupListener.isInSetupMode(player) && !args[0].equals("help")) {
               player.sendMessage("§cSetup modunda degilsin! §7/bwa setup <arena> ile basla.");
               return true;
            } else {
               return setupListener.handleSetupCommand(player, args);
            }
         } else {
            this.sendSetupHelp(player, label);
            return true;
         }
      } else {
         MessageUtil.sendMessage(sender, "general.only-player");
         return true;
      }
   }

   private void sendSetupHelp(Player player, String label) {
      player.sendMessage(MessageUtil.get("commands.help-header"));
      player.sendMessage(MessageUtil.get("commands.help-category-setup"));
      String desc = MessageUtil.get("commands.descriptions.setup-setspawn");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setspawn", "arg", "team", "description", desc != null ? desc : "Set Spawn"));
      desc = MessageUtil.get("commands.descriptions.setup-setbed");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setbed", "arg", "team", "description", desc != null ? desc : "Set Bed"));
      desc = MessageUtil.get("commands.descriptions.setup-setgenerator");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setgenerator", "arg", "type", "description", desc != null ? desc : "Set Generator"));
      desc = MessageUtil.get("commands.descriptions.setup-setshop");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " setshop", "description", desc != null ? desc : "Set Shop"));
      desc = MessageUtil.get("commands.descriptions.setup-setupgrade");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " setupgrade", "description", desc != null ? desc : "Set Upgrade"));
      desc = MessageUtil.get("commands.descriptions.setup-setregion");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " setregion", "description", desc != null ? desc : "Set Region"));
      desc = MessageUtil.get("commands.descriptions.setup-done");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " done", "description", desc != null ? desc : "Done"));
      player.sendMessage(" §7Generator tipleri: §eIRON, GOLD, DIAMOND, EMERALD");
      List var10002 = this.getTeamNames(player);
      player.sendMessage(" §7Takimlar: §e" + String.join(", ", var10002));
      player.sendMessage(MessageUtil.get("commands.help-footer"));
   }

   private List<String> getTeamNames(Player player) {
      SetupListener sl = this.plugin.getSetupListener();
      if (sl != null && sl.isInSetupMode(player)) {
         String arenaName = sl.getArenaName(player);
         Arena arena = this.plugin.getArenaManager().getArena(arenaName);
         return arena == null ? List.of() : arena.getTeamNames();
      } else {
         return List.of();
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (sender instanceof Player player) {
         if (player.hasPermission("bedwars.setup")) {
            SetupListener sl = this.plugin.getSetupListener();
            if (args.length == 1) {
               return (List)this.subCommands.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }

            if (args.length == 2) {
               List var10000;
               switch (args[0].toLowerCase()) {
                  case "setspawn":
                  case "setbed":
                     var10000 = (List)this.getTeamNames(player).stream().filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                     break;
                  case "setgenerator":
                     var10000 = (List)this.genTypes.stream().filter((t) -> t.startsWith(args[1].toUpperCase())).collect(Collectors.toList());
                     break;
                  default:
                     var10000 = List.of();
               }

               return var10000;
            }

            return List.of();
         }
      }

      return List.of();
   }
}
