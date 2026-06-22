package com.lbedwars.listener;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.generator.Generator;
import com.lbedwars.generator.GeneratorType;
import com.lbedwars.npc.ShopNPC;
import com.lbedwars.util.MessageUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SetupListener implements Listener {
   private final LBedWars plugin;
   private final Map<UUID, String> setupMode;
   private final Map<UUID, Location> pos1;
   private final Map<UUID, Location> pos2;

   public SetupListener(LBedWars plugin) {
      this.plugin = plugin;
      this.setupMode = new HashMap();
      this.pos1 = new HashMap();
      this.pos2 = new HashMap();
   }

   public void enterSetupMode(Player player, String arenaName) {
      this.setupMode.put(player.getUniqueId(), arenaName);
      this.pos1.remove(player.getUniqueId());
      this.pos2.remove(player.getUniqueId());
   }

   public void exitSetupMode(Player player) {
      this.setupMode.remove(player.getUniqueId());
      this.pos1.remove(player.getUniqueId());
      this.pos2.remove(player.getUniqueId());
   }

   public boolean isInSetupMode(Player player) {
      return this.setupMode.containsKey(player.getUniqueId());
   }

   public String getArenaName(Player player) {
      return (String)this.setupMode.get(player.getUniqueId());
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (this.isInSetupMode(player)) {
         if (event.getHand() == EquipmentSlot.HAND) {
            if (player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
               event.setCancelled(true);
               String arenaName = (String)this.setupMode.get(player.getUniqueId());
               Arena arena = this.plugin.getArenaManager().getArena(arenaName);
               if (arena == null) {
                  player.sendMessage("§cArena not found!");
                  this.exitSetupMode(player);
               } else {
                  Block clickedBlock = event.getClickedBlock();
                  Location blockLoc = clickedBlock != null ? clickedBlock.getLocation() : player.getLocation();
                  Action action = event.getAction();
                  if (action == Action.LEFT_CLICK_BLOCK) {
                     this.pos1.put(player.getUniqueId(), blockLoc);
                     String var10001 = this.formatLoc(blockLoc);
                     player.sendMessage("§aCorner 1 selected: " + var10001);
                  } else if (action == Action.RIGHT_CLICK_BLOCK) {
                     this.pos2.put(player.getUniqueId(), blockLoc);
                     String var8 = this.formatLoc(blockLoc);
                     player.sendMessage("§aCorner 2 selected: " + var8);
                  }

               }
            }
         }
      }
   }

   public boolean handleSetupCommand(Player player, String[] args) {
      if (!this.isInSetupMode(player)) {
         if (args.length >= 1 && args[0].equalsIgnoreCase("done")) {
            player.sendMessage("§cYou are not in setup mode!");
            return true;
         } else {
            return false;
         }
      } else {
         String arenaName = (String)this.setupMode.get(player.getUniqueId());
         Arena arena = this.plugin.getArenaManager().getArena(arenaName);
         if (arena == null) {
            player.sendMessage("§cArena not found!");
            this.exitSetupMode(player);
            return true;
         } else if (args.length < 1) {
            this.sendSetupHelp(player);
            return true;
         } else {
            switch (args[0].toLowerCase()) {
               case "setspawn":
                  if (args.length < 2) {
                     player.sendMessage("§cUsage: /setup setspawn <team>");
                     return true;
                  }

                  Team team = arena.getTeamByName(args[1]);
                  if (team == null) {
                     MessageUtil.sendMessage(player, "commands.setup-invalid-team", "teams", String.join(", ", arena.getTeamNames()));
                     return true;
                  }

                  team.setSpawn(player.getLocation().clone());
                  arena.saveConfig();
                  MessageUtil.sendMessage(player, "commands.setup-team-spawn", "team", team.getName());
                  break;
               case "setbed":
                  if (args.length < 2) {
                     player.sendMessage("§cUsage: /setup setbed <team>");
                     return true;
                  }

                  Team bedTeam = arena.getTeamByName(args[1]);
                  if (bedTeam == null) {
                     MessageUtil.sendMessage(player, "commands.setup-invalid-team", "teams", String.join(", ", arena.getTeamNames()));
                     return true;
                  }

                  Block targetBlock = player.getTargetBlockExact(5);
                  if (targetBlock != null && targetBlock.getType().toString().contains("BED")) {
                     BlockData var21 = targetBlock.getBlockData();
                     if (var21 instanceof Bed) {
                        Bed bedData = (Bed)var21;
                        Block head = bedData.getPart() == Part.HEAD ? targetBlock : this.getHeadHalf(targetBlock, bedData);
                        bedTeam.setBedLocation(head.getLocation());
                        bedTeam.setBedFacing(bedData.getFacing());
                     } else {
                        bedTeam.setBedLocation(targetBlock.getLocation());
                     }

                     arena.saveConfig();
                     MessageUtil.sendMessage(player, "commands.setup-bed", "team", bedTeam.getName());
                  } else {
                     player.sendMessage("§cUse this while looking at a bed!");
                  }
                  break;
               case "setgenerator":
                  if (args.length < 2) {
                     player.sendMessage("§cUsage: /setup setgenerator <type>");
                     player.sendMessage("§7Types: IRON, GOLD, DIAMOND, EMERALD");
                     return true;
                  }

                  String type = args[1].toUpperCase();

                  GeneratorType genType;
                  try {
                     genType = GeneratorType.valueOf(type);
                  } catch (IllegalArgumentException var12) {
                     player.sendMessage("§cInvalid generator type! IRON, GOLD, DIAMOND, EMERALD");
                     return true;
                  }

                  Block target = player.getTargetBlockExact(5);
                  if (target == null || target.getType().isAir()) {
                     target = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                  }

                  Location genLoc = target.getRelative(BlockFace.UP).getLocation().add((double)0.5F, (double)0.5F, (double)0.5F);
                  Generator gen = new Generator(genType, genLoc, arena);
                  arena.getGenerators().add(gen);
                  arena.saveConfig();
                  MessageUtil.sendMessage(player, "commands.setup-generator", "type", type);
                  break;
               case "setshop": {
                  ShopNPC shopNpc = new ShopNPC(player.getLocation().clone(), arena, "SHOP");
                  arena.getShopNpcs().add(shopNpc);
                  arena.saveConfig();
                  MessageUtil.sendMessage(player, "commands.setup-shop");
                  break;
               }
               case "setupgrade": {
                  ShopNPC upgradeNpc = new ShopNPC(player.getLocation().clone(), arena, "UPGRADE");
                  arena.getShopNpcs().add(upgradeNpc);
                  arena.saveConfig();
                  MessageUtil.sendMessage(player, "commands.setup-upgrade");
                  break;
               }
               case "setregion":
                  Location p1 = (Location)this.pos1.get(player.getUniqueId());
                  Location p2 = (Location)this.pos2.get(player.getUniqueId());
                  if (p1 == null || p2 == null) {
                     player.sendMessage("§cSelect 2 corners first with the blaze rod! (Left click: 1st corner, Right click: 2nd corner)");
                     return true;
                  }

                  arena.setRegionMin(new Location(p1.getWorld(), Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ())));
                  arena.setRegionMax(new Location(p1.getWorld(), Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ())));
                  arena.saveConfig();
                  MessageUtil.sendMessage(player, "commands.setup-region-set");
                  break;
               case "done":
                  arena.saveConfig();
                  this.exitSetupMode(player);
                  MessageUtil.sendMessage(player, "commands.setup-done", "arena", arenaName);
                  break;
               default:
                  this.sendSetupHelp(player);
            }

            return true;
         }
      }
   }

   private void sendSetupHelp(Player player) {
      player.sendMessage("§6§lArena Setup Commands:");
      player.sendMessage(" §e/setup setspawn <team> §7- Set team spawn point");
      player.sendMessage(" §e/setup setbed <team> §7- Set team bed (look at bed)");
      player.sendMessage(" §e/setup setgenerator <type> §7- Add generator (IRON/GOLD/DIAMOND/EMERALD)");
      player.sendMessage(" §e/setup setshop §7- Add Shop NPC");
      player.sendMessage(" §e/setup setupgrade §7- Add Upgrade NPC");
      player.sendMessage(" §e/setup setregion §7- Select arena region (with blaze rod)");
      player.sendMessage(" §e/setup done §7- Finish setup");
      List var10002 = this.getTeamList(player);
      player.sendMessage(" §7Teams: §e" + String.join(", ", var10002));
   }

   private List<String> getTeamList(Player player) {
      String an = (String)this.setupMode.get(player.getUniqueId());
      if (an == null) {
         return List.of();
      } else {
         Arena a = this.plugin.getArenaManager().getArena(an);
         return a != null ? a.getTeamNames() : List.of();
      }
   }

   private String formatLoc(Location loc) {
      int var10000 = loc.getBlockX();
      return "X: " + var10000 + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ();
   }

   private Block getHeadHalf(Block block, Bed bedData) {
      return bedData.getPart() == Part.HEAD ? block : block.getRelative(bedData.getFacing());
   }
}
