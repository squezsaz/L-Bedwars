package com.lbedwars.command;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BedWarsAdminCommand implements TabExecutor {
   private final LBedWars plugin;
   private final List<String> mainCommands;
   private final List<String> rewardSubCommands;

   public BedWarsAdminCommand(LBedWars plugin) {
      this.plugin = plugin;
      this.mainCommands = List.of("help", "setup", "create", "delete", "enable", "disable", "reload", "setlobby", "setspectator", "setmainlobby", "reward", "addteam", "removeteam", "forcestart", "setmode", "spectator", "addxp");
      this.rewardSubCommands = List.of("help", "setxp", "setcoins", "setmoney", "setcooldown", "reset", "info", "toggle");
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("bedwars.admin")) {
            MessageUtil.sendMessage(player, "general.no-permission");
            return true;
         } else if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            switch (args[0].toLowerCase()) {
               case "setup" -> this.handleSetup(player, args);
               case "create" -> this.handleCreate(player, args);
               case "delete" -> this.handleDelete(player, args);
               case "enable" -> this.handleEnable(player, args);
               case "disable" -> this.handleDisable(player, args);
               case "reload" -> this.handleReload(player);
               case "setlobby" -> this.handleSetLobby(player, args);
               case "setspectator" -> this.handleSetSpectator(player, args);
               case "reward" -> this.handleReward(player, args);
               case "addteam" -> this.handleAddTeam(player, args);
               case "removeteam" -> this.handleRemoveTeam(player, args);
               case "setmainlobby" -> this.handleSetMainLobby(player);
               case "forcestart" -> this.handleForceStart(player, args);
               case "setmode" -> this.handleSetMode(player, args);
               case "spectator" -> this.handleSpectator(player, args);
               case "addxp" -> this.handleAddExp(player, args);
               default -> this.sendAdminHelp(player, label);
            }

            return true;
         } else {
            this.sendAdminHelp(player, label);
            return true;
         }
      } else {
         MessageUtil.sendMessage(sender, "general.only-player");
         return true;
      }
   }

   private void sendAdminHelp(Player player, String label) {
      player.sendMessage(MessageUtil.get("commands.help-header"));
      player.sendMessage(MessageUtil.get("commands.help-category-admin"));
      String desc = MessageUtil.get("commands.descriptions.bwa-help");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " help", "description", desc != null ? desc : "Help"));
      desc = MessageUtil.get("commands.descriptions.bwa-create");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " create", "arg", "isim", "description", desc != null ? desc : "Create"));
      desc = MessageUtil.get("commands.descriptions.bwa-setup");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setup", "arg", "arena", "description", desc != null ? desc : "Setup"));
      desc = MessageUtil.get("commands.descriptions.bwa-addteam");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " addteam", "arg", "arena isim renk max-oyuncu", "description", desc != null ? desc : "Add team"));
      desc = MessageUtil.get("commands.descriptions.bwa-removeteam");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " removeteam", "arg", "arena isim", "description", desc != null ? desc : "Remove team"));
      desc = MessageUtil.get("commands.descriptions.bwa-delete");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " delete", "arg", "arena", "description", desc != null ? desc : "Delete"));
      desc = MessageUtil.get("commands.descriptions.bwa-enable");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " enable", "arg", "arena", "description", desc != null ? desc : "Enable"));
      desc = MessageUtil.get("commands.descriptions.bwa-disable");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " disable", "arg", "arena", "description", desc != null ? desc : "Disable"));
      desc = MessageUtil.get("commands.descriptions.bwa-reload");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " reload", "description", desc != null ? desc : "Reload"));
      desc = MessageUtil.get("commands.descriptions.bwa-setlobby");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setlobby", "arg", "arena", "description", desc != null ? desc : "Set Lobby"));
      desc = MessageUtil.get("commands.descriptions.bwa-setspectator");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setspectator", "arg", "arena", "description", desc != null ? desc : "Set Spectator"));
      desc = MessageUtil.get("commands.descriptions.bwa-reward");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " reward", "description", desc != null ? desc : "Reward"));
      desc = MessageUtil.get("commands.descriptions.bwa-setmainlobby");
      player.sendMessage(MessageUtil.get("commands.help-entry", "command", label + " setmainlobby", "description", desc != null ? desc : "Set Main Lobby"));
      desc = MessageUtil.get("commands.descriptions.bwa-forcestart");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " forcestart", "arg", "arena", "description", desc != null ? desc : "Force Start"));
      desc = MessageUtil.get("commands.descriptions.bwa-setmode");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " setmode", "arg", "arena mod", "description", desc != null ? desc : "Set Mode"));
      desc = MessageUtil.get("commands.descriptions.bwa-spectator");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " spectator", "arg", "arena", "description", desc != null ? desc : "Spectate"));
      desc = MessageUtil.get("commands.descriptions.bwa-addexp");
      player.sendMessage(MessageUtil.get("commands.help-entry-arg", "command", label + " addxp", "arg", "player amount", "description", desc != null ? desc : "Add XP"));
      player.sendMessage(MessageUtil.get("commands.help-footer"));
   }

   private void handleSetup(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa setup <arena>");
      } else {
         String arenaName = args[1];
         if (!this.plugin.getArenaManager().arenaExists(arenaName)) {
            player.sendMessage("§cArena not found! §7Arenas: " + String.join(", ", this.plugin.getArenaManager().getArenaNames()));
         } else {
            this.plugin.getSetupListener().enterSetupMode(player, arenaName);
            player.getInventory().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
            MessageUtil.sendMessage(player, "commands.setup-wand");
            player.sendMessage(MessageUtil.get("admin.setup-entered", "arena", arenaName));
            player.sendMessage(MessageUtil.get("admin.setup-info-commands"));
         }
      }
   }

   private void handleCreate(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa create <name>");
      } else {
         String name = args[1];
         if (this.plugin.getArenaManager().arenaExists(name)) {
            player.sendMessage("§cAn arena with that name already exists!");
         } else {
            this.plugin.getArenaManager().createArena(name);
            this.plugin.getSetupListener().enterSetupMode(player, name);
            player.getInventory().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
            player.sendMessage(MessageUtil.get("admin.setup-entered", "arena", name));
            String guide = MessageUtil.get("admin.setup-guide", "arena", name);

            for(String line : guide.split("\n")) {
               player.sendMessage(line);
            }

         }
      }
   }

   private void handleDelete(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa delete <arena>");
      } else {
         String name = args[1];
         if (!this.plugin.getArenaManager().arenaExists(name)) {
            player.sendMessage("§cArena not found!");
         } else {
            this.plugin.getArenaManager().deleteArena(name);
            player.sendMessage(MessageUtil.get("commands.arena-delete", "arena", name));
         }
      }
   }

   private void handleEnable(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa enable <arena>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else {
            this.plugin.getGameManager().resetArena(arena);
            arena.setEnabled(true);
            arena.saveConfig();
            player.sendMessage(MessageUtil.get("commands.arena-enabled", "arena", args[1]));
         }
      }
   }

   private void handleDisable(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa disable <arena>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else {
            this.plugin.getGameManager().resetArena(arena);
            arena.setEnabled(false);
            arena.saveConfig();
            player.sendMessage(MessageUtil.get("commands.arena-disabled", "arena", args[1]));
         }
      }
   }

   private void handleReload(Player player) {
      this.plugin.getConfigManager().reload();
      this.plugin.getLanguageManager().reload();
      this.plugin.getArenaManager().reloadArenas();
      MessageUtil.sendMessage(player, "admin.config-reloaded");
   }

   private void handleSetLobby(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa setlobby <arena>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else {
            arena.setLobbyLocation(player.getLocation());
            arena.saveConfig();
            MessageUtil.sendMessage(player, "admin.lobby-set");
         }
      }
   }

   private void handleSetSpectator(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa setspectator <arena>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else {
            arena.setSpectatorSpawn(player.getLocation());
            arena.saveConfig();
            MessageUtil.sendMessage(player, "admin.spectator-set");
         }
      }
   }

   private void handleAddTeam(Player player, String[] args) {
      if (args.length < 5) {
         player.sendMessage(MessageUtil.get("admin.addteam-usage"));
         player.sendMessage(MessageUtil.get("admin.setup-info-colors"));
      } else {
         String arenaName = args[1];
         String teamName = args[2];
         String colorStr = args[3].toUpperCase();

         int maxSize;
         try {
            maxSize = Integer.parseInt(args[4]);
            if (maxSize < 1) {
               MessageUtil.sendMessage(player, "admin.addteam-min-size");
               return;
            }
         } catch (NumberFormatException var11) {
            player.sendMessage("§cInvalid number! Usage: /bwa addteam <arena> <name> <color> <max-players>");
            return;
         }

         ChatColor color;
         try {
            color = ChatColor.valueOf(colorStr);
         } catch (IllegalArgumentException var10) {
            MessageUtil.sendMessage(player, "admin.addteam-invalid-color");
            return;
         }

         boolean success = this.plugin.getArenaManager().addTeam(arenaName, teamName, colorStr, maxSize);
         if (!success) {
            Arena arena = this.plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
               player.sendMessage("§cArena not found!");
            } else {
               player.sendMessage("§cA team with that name already exists!");
            }

         } else {
            String[] var10002 = new String[]{"team", teamName, "arena", arenaName, "color", null, null, null};
            String var10005 = String.valueOf(color);
            var10002[5] = var10005 + color.name();
            var10002[6] = "max";
            var10002[7] = String.valueOf(maxSize);
            player.sendMessage(MessageUtil.get("admin.addteam-success", var10002));
         }
      }
   }

   private void handleRemoveTeam(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("admin.removeteam-usage"));
      } else {
         boolean success = this.plugin.getArenaManager().removeTeam(args[1], args[2]);
         if (!success) {
            Arena arena = this.plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
               player.sendMessage("§cArena not found!");
            } else {
               player.sendMessage(MessageUtil.get("admin.removeteam-not-found", "teams", String.join(", ", arena.getTeamNames())));
            }

         } else {
            player.sendMessage(MessageUtil.get("admin.removeteam-success", "team", args[2]));
         }
      }
   }

   private void handleSetMainLobby(Player player) {
      this.plugin.saveMainLobby(player.getLocation());
      MessageUtil.sendMessage(player, "admin.main-lobby-set");
   }

   private void handleForceStart(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa forcestart <arena>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else if (arena.getState() != ArenaState.WAITING) {
            player.sendMessage(MessageUtil.get("admin.forcestart-cant", "state", arena.getState().name()));
         } else if (arena.getPlayers().size() < 1) {
            player.sendMessage(MessageUtil.get("admin.forcestart-need-players"));
         } else {
            this.plugin.getGameManager().cancelLobbyCountdown(arena);
            this.plugin.getGameManager().startGame(arena);
            player.sendMessage(MessageUtil.get("admin.forcestart-started", "arena", args[1]));
         }
      }
   }

   private void handleSetMode(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage("§cUsage: /bwa setmode <arena> <solo|duo|trio|quad>");
      } else {
         Arena arena = this.plugin.getArenaManager().getArena(args[1]);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
         } else {
            String mode = args[2].toLowerCase();
            if (!List.of("solo", "duo", "trio", "quad").contains(mode)) {
               player.sendMessage("§cInvalid mode! Usage: /bwa setmode <arena> <solo|duo|trio|quad>");
            } else {
               arena.setMode(mode);
               arena.saveConfig();
               player.sendMessage("§aArena mode set to: §e" + mode);
            }
         }
      }
   }

   private void handleSpectator(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /bwa spectator <arena|player>");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target != null) {
            Arena arena = this.plugin.getArenaManager().getArenaByPlayer(target.getUniqueId());
            if (arena == null) {
               MessageUtil.sendMessage(player, "admin.spectator-player-not-in-game");
               return;
            }

            this.plugin.getSpectateManager().enableSpectate(player, arena);
            player.teleport(target.getLocation());
            MessageUtil.sendMessage(player, "admin.spectator-joined-player", "arena", arena.getName(), "player", target.getName());
         } else {
            Arena arena = this.plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
               player.sendMessage(MessageUtil.get("admin.spectator-not-found", "arena", args[1]));
               return;
            }

            if (!arena.isEnabled()) {
               MessageUtil.sendMessage(player, "admin.spectator-not-enabled");
               return;
            }

            this.plugin.getSpectateManager().enableSpectate(player, arena);
         }

      }
   }

   private void handleAddExp(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("admin.addexp-usage"));
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            for(Player online : Bukkit.getOnlinePlayers()) {
               if (online.getName().equalsIgnoreCase(args[1])) {
                  target = online;
                  break;
               }
            }
         }

         if (target == null) {
            player.sendMessage(MessageUtil.get("admin.addexp-player-not-found"));
         } else {
            int amount;
            try {
               amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException var6) {
               player.sendMessage(MessageUtil.get("admin.addexp-invalid-number"));
               return;
            }

            this.plugin.getLevelManager().addXp(target.getUniqueId(), amount);
            this.plugin.getStatsCache().invalidate(target.getUniqueId());
            player.sendMessage(MessageUtil.get("admin.addexp-success", "player", target.getName(), "amount", String.valueOf(amount)));
         }
      }
   }

   private String getColorNames() {
      StringBuilder sb = new StringBuilder();

      for(ChatColor c : ChatColor.values()) {
         if (c.isColor()) {
            sb.append(c.name()).append(", ");
         }
      }

      return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
   }

   private void handleReward(Player player, String[] args) {
      if (args.length >= 2 && !args[1].equalsIgnoreCase("help")) {
         switch (args[1].toLowerCase()) {
            case "setxp" -> this.handleRewardSetXp(player, args);
            case "setcoins" -> this.handleRewardSetCoins(player, args);
            case "setmoney" -> this.handleRewardSetMoney(player, args);
            case "setcooldown" -> this.handleRewardSetCooldown(player, args);
            case "reset" -> this.handleRewardReset(player, args);
            case "info" -> this.handleRewardInfo(player);
            case "toggle" -> this.handleRewardToggle(player);
            default -> this.sendRewardHelp(player);
         }

      } else {
         this.sendRewardHelp(player);
      }
   }

   private void sendRewardHelp(Player player) {
      player.sendMessage(MessageUtil.get("reward.title"));
      player.sendMessage(MessageUtil.get("reward.help-setxp"));
      player.sendMessage(MessageUtil.get("reward.help-setcoins"));
      player.sendMessage(MessageUtil.get("reward.help-setmoney"));
      player.sendMessage(MessageUtil.get("reward.help-setcooldown"));
      player.sendMessage(MessageUtil.get("reward.help-reset"));
      player.sendMessage(MessageUtil.get("reward.help-info"));
      player.sendMessage(MessageUtil.get("reward.help-toggle"));
      player.sendMessage(MessageUtil.get("reward.vault-status", "status", MessageUtil.get(this.plugin.getEconomyManager().isEnabled() ? "reward.vault-connected" : "reward.vault-disconnected")));
   }

   private void handleRewardSetXp(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("reward.usage-setxp"));
      } else {
         try {
            int xp = Integer.parseInt(args[2]);
            if (xp < 0) {
               player.sendMessage(MessageUtil.get("reward.negative"));
               return;
            }

            this.plugin.getDailyRewardManager().setXp(xp);
            player.sendMessage(MessageUtil.get("reward.xp-set", "xp", String.valueOf(xp)));
         } catch (NumberFormatException var4) {
            player.sendMessage(MessageUtil.get("reward.invalid-number"));
         }

      }
   }

   private void handleRewardSetCoins(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("reward.usage-setcoins"));
      } else {
         try {
            int coins = Integer.parseInt(args[2]);
            if (coins < 0) {
               player.sendMessage(MessageUtil.get("reward.negative"));
               return;
            }

            this.plugin.getDailyRewardManager().setCoins(coins);
            player.sendMessage(MessageUtil.get("reward.coins-set", "coins", String.valueOf(coins)));
         } catch (NumberFormatException var4) {
            player.sendMessage(MessageUtil.get("reward.invalid-number"));
         }

      }
   }

   private void handleRewardSetMoney(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("reward.usage-setmoney"));
      } else if (!this.plugin.getEconomyManager().isEnabled()) {
         player.sendMessage(MessageUtil.get("reward.vault-disabled"));
      } else {
         try {
            double money = Double.parseDouble(args[2]);
            if (money < (double)0.0F) {
               player.sendMessage(MessageUtil.get("reward.negative"));
               return;
            }

            this.plugin.getDailyRewardManager().setMoney(money);
            player.sendMessage(MessageUtil.get("reward.money-set", "money", this.plugin.getEconomyManager().format(money)));
         } catch (NumberFormatException var5) {
            player.sendMessage(MessageUtil.get("reward.invalid-number"));
         }

      }
   }

   private void handleRewardSetCooldown(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("reward.usage-setcooldown"));
      } else {
         try {
            long hours = Long.parseLong(args[2]);
            if (hours < 1L) {
               player.sendMessage(MessageUtil.get("reward.min-hours"));
               return;
            }

            this.plugin.getDailyRewardManager().setCooldownHours(hours);
            player.sendMessage(MessageUtil.get("reward.cooldown-set", "hours", String.valueOf(hours)));
         } catch (NumberFormatException var5) {
            player.sendMessage(MessageUtil.get("reward.invalid-number"));
         }

      }
   }

   private void handleRewardReset(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(MessageUtil.get("reward.usage-reset"));
      } else {
         OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[2]);
         if (target == null) {
            for(Player online : Bukkit.getOnlinePlayers()) {
               if (online.getName().equalsIgnoreCase(args[2])) {
                  target = online;
                  break;
               }
            }
         }

         if (target == null) {
            player.sendMessage(MessageUtil.get("reward.player-not-found"));
         } else {
            this.plugin.getDailyRewardManager().resetCooldown(target);
            player.sendMessage(MessageUtil.get("reward.reset", "player", target.getName()));
         }
      }
   }

   private void handleRewardInfo(Player player) {
      boolean enabled = this.plugin.getDailyRewardManager().isEnabled();
      player.sendMessage(MessageUtil.get("reward.info-title"));
      player.sendMessage(MessageUtil.get("reward.info-status", "status", MessageUtil.get(enabled ? "reward.active" : "reward.inactive")));
      player.sendMessage(" §7XP: §e" + this.plugin.getDailyRewardManager().getXp());
      player.sendMessage(" §7Coin: §e" + this.plugin.getDailyRewardManager().getCoins());
      if (this.plugin.getEconomyManager().isEnabled()) {
         player.sendMessage(" §7Money: §e" + this.plugin.getEconomyManager().format(this.plugin.getDailyRewardManager().getMoney()));
      }

      player.sendMessage(MessageUtil.get("reward.info-cooldown", "hours", String.valueOf(this.plugin.getDailyRewardManager().getCooldownHours())));
      player.sendMessage(MessageUtil.get("reward.info-vault", "status", MessageUtil.get(this.plugin.getEconomyManager().isEnabled() ? "reward.vault-connected" : "reward.vault-disconnected")));
   }

   private void handleRewardToggle(Player player) {
      boolean current = this.plugin.getDailyRewardManager().isEnabled();
      this.plugin.getDailyRewardManager().setEnabled(!current);
      player.sendMessage(MessageUtil.get("reward.toggled", "status", MessageUtil.get(!current ? "reward.active" : "reward.inactive")));
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (sender instanceof Player && sender.hasPermission("bedwars.admin")) {
         if (args.length == 1) {
            return (List)this.mainCommands.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
         } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reward")) {
               return (List)this.rewardSubCommands.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            } else if (!args[0].equalsIgnoreCase("addteam") && !args[0].equalsIgnoreCase("removeteam")) {
               List var10000;
               switch (args[0].toLowerCase()) {
                  case "setup":
                  case "delete":
                  case "enable":
                  case "disable":
                  case "setlobby":
                  case "setspectator":
                  case "forcestart":
                  case "setmode":
                     var10000 = (List)this.plugin.getArenaManager().getArenaNames().stream().filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                     break;
                  case "spectator":
                     List<String> suggestions = new ArrayList<String>(this.plugin.getArenaManager().getArenaNames());
                     Stream<String> var10 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName);
                     Objects.requireNonNull(suggestions);
                     var10.forEach(suggestions::add);
                     var10000 = (List)suggestions.stream().filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                     break;
                  case "addxp":
                     var10000 = (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                     break;
                  default:
                     var10000 = List.of();
               }

               return var10000;
            } else {
               return (List)this.plugin.getArenaManager().getArenaNames().stream().filter((n) -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
         } else {
            if (args.length == 3 && args[0].equalsIgnoreCase("removeteam")) {
               Arena arena = this.plugin.getArenaManager().getArena(args[1]);
               if (arena != null) {
                  return (List)arena.getTeamNames().stream().filter((n) -> n.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
               }
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("setmode")) {
               return (List)List.of("solo", "duo", "trio", "quad").stream().filter((m) -> m.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("reset")) {
               return (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((n) -> n.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            } else if (args.length == 4 && args[0].equalsIgnoreCase("addteam")) {
               List<String> colors = List.of("RED", "BLUE", "GREEN", "YELLOW", "AQUA", "WHITE", "BLACK", "DARK_RED", "DARK_BLUE", "DARK_GREEN", "LIGHT_PURPLE", "GOLD", "GRAY", "DARK_GRAY", "DARK_PURPLE");
               return (List)colors.stream().filter((c) -> c.startsWith(args[3].toUpperCase())).collect(Collectors.toList());
            } else {
               return List.of();
            }
         }
      } else {
         return List.of();
      }
   }
}
