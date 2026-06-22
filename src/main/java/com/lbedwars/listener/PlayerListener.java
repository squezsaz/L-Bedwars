package com.lbedwars.listener;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import com.lbedwars.npc.ShopNPC;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.MessageUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {
   private final LBedWars plugin;
   private final Map<UUID, Long> borderWarnings;
   private final Map<UUID, Long> trapCooldowns;
   private final Map<UUID, UUID> tntOwners;
   private final Map<UUID, Integer> tntFuseTasks;
   private final Map<UUID, ItemStack[]> savedArmor;
   private final Set<UUID> effectGuard;

   public PlayerListener(LBedWars plugin) {
      this.plugin = plugin;
      this.borderWarnings = new HashMap();
      this.trapCooldowns = new HashMap();
      this.tntOwners = new HashMap();
      this.tntFuseTasks = new HashMap();
      this.savedArmor = new HashMap();
      this.effectGuard = new HashSet();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (!this.plugin.getDatabase().hasStats(uuid)) {
         this.plugin.getDatabase().createStats(uuid);
      }

      event.setJoinMessage((String)null);
      if (!this.plugin.getProxyManager().isEnabled()) {
         Location mainLobby = this.plugin.getMainLobby();
         if (mainLobby != null) {
            player.teleport(mainLobby);
         }
         return;
      }

      String spectateTarget = LBedWars.PENDING_SPECTATES.remove(player.getName());
      if (spectateTarget != null) {
         for (Arena arena : this.plugin.getArenaManager().getAllArenas()) {
            for (UUID pUuid : arena.getPlayers()) {
               Player p = Bukkit.getPlayer(pUuid);
               if (p != null && p.getName().equals(spectateTarget)) {
                  this.plugin.getSpectateManager().enableSpectate(player, arena);
                  player.teleport(p.getLocation());
                  MessageUtil.sendMessage(player, "spectate.controls");
                  return;
               }
            }
         }
         for (Arena arena : this.plugin.getArenaManager().getAllArenas()) {
            for (UUID sUuid : arena.getSpectators()) {
               Player p = Bukkit.getPlayer(sUuid);
               if (p != null && p.getName().equals(spectateTarget)) {
                  this.plugin.getSpectateManager().enableSpectate(player, arena);
                  player.teleport(p.getLocation());
                  MessageUtil.sendMessage(player, "spectate.controls");
                  return;
               }
            }
         }
      }

      if (this.plugin.getGameManager().getRejoinData(uuid) != null) {
         this.plugin.getGameManager().rejoinPlayer(player);
         return;
      }

      List<Arena> candidates = new ArrayList();
      for (Arena arena : this.plugin.getArenaManager().getEnabledArenas()) {
         if (arena.getSmallestTeam() != null) {
            candidates.add(arena);
         }
      }
      if (!candidates.isEmpty()) {
         candidates.sort((a, b) -> Integer.compare(b.getPlayers().size(), a.getPlayers().size()));
         Arena target = candidates.get(0);
         target.addPlayer(player);
      } else {
         Location mainLobby = this.plugin.getMainLobby();
         player.teleport(mainLobby != null ? mainLobby : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
      }
   }

   @EventHandler
   public void onPotionEffect(EntityPotionEffectEvent event) {
      Entity var3 = event.getEntity();
      if (var3 instanceof Player player) {
         UUID var7 = player.getUniqueId();
         if (!this.effectGuard.contains(var7)) {
            PotionEffect newEffect = event.getNewEffect();
            PotionEffectType type = event.getModifiedType();
            if (type.equals(PotionEffectType.INVISIBILITY)) {
               switch (event.getAction()) {
                  case ADDED:
                  case CHANGED:
                     this.savedArmor.putIfAbsent(var7, player.getInventory().getArmorContents());
                     player.getInventory().setArmorContents(new ItemStack[4]);
                     if (newEffect != null && newEffect.hasParticles()) {
                        event.setCancelled(true);
                        this.effectGuard.add(var7);
                        player.addPotionEffect(new PotionEffect(type, newEffect.getDuration(), newEffect.getAmplifier(), newEffect.isAmbient(), false, newEffect.hasIcon()));
                        this.effectGuard.remove(var7);
                     }
                     break;
                  case REMOVED:
                  case CLEARED:
                     ItemStack[] armor = (ItemStack[])this.savedArmor.remove(var7);
                     if (armor != null) {
                        player.getInventory().setArmorContents(armor);
                     }
               }
            } else if ((type.equals(PotionEffectType.SPEED) || type.equals(PotionEffectType.JUMP)) && (event.getAction() == Action.ADDED || event.getAction() == Action.CHANGED) && newEffect != null && newEffect.hasParticles()) {
               event.setCancelled(true);
               this.effectGuard.add(var7);
               player.addPotionEffect(new PotionEffect(type, newEffect.getDuration(), newEffect.getAmplifier(), newEffect.isAmbient(), false, newEffect.hasIcon()));
               this.effectGuard.remove(var7);
            }

         }
      }
   }

   @EventHandler
   public void onWorldChange(PlayerChangedWorldEvent event) {
   }

   @EventHandler
   public void onItemConsume(PlayerItemConsumeEvent event) {
      ItemStack item = event.getItem();
      if (item != null && item.getType() == Material.POTION && this.plugin.getArenaManager().getArenaByPlayer(event.getPlayer().getUniqueId()) != null) {
         FoliaUtil.runTaskLater(() -> event.getPlayer().getInventory().removeItem(new ItemStack[]{new ItemStack(Material.GLASS_BOTTLE)}), 1L);
      }

   }

   @EventHandler
   public void onFoodLevelChange(FoodLevelChangeEvent event) {
      HumanEntity var3 = event.getEntity();
      if (var3 instanceof Player player) {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            if (!this.plugin.getConfig().getBoolean("arena.hunger", false)) {
               event.setCancelled(true);
               player.setFoodLevel(20);
               player.setSaturation(20.0F);
            }

         }
      }
   }

   @EventHandler
   public void onPlayerBedEnter(PlayerBedEnterEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null || this.plugin.getArenaManager().getArenaBySpectator(player.getUniqueId()) != null) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void onCraftItem(CraftItemEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         if (this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
         }
      }

   }

   @EventHandler
   public void onPortalCreate(PortalCreateEvent event) {
      Entity var3 = event.getEntity();
      if (var3 instanceof Player player) {
         if (this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
         }
      }

   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(uuid);
      if (arena != null) {
         if (arena.getState() == ArenaState.PLAYING) {
            Team team = arena.getTeamByPlayer(uuid);
            if (team != null && team.isBedAlive()) {
                this.plugin.getGameManager().saveRejoinData(uuid, arena.getName(), team.getName(), player.getName());
            }
         }

         arena.removePlayer(player);
      }

      if (this.plugin.getSpectateManager().isSpectating(uuid)) {
         this.plugin.getSpectateManager().disableSpectate(player);
      }

      this.plugin.getCompassTracker().cleanup(uuid);
      this.plugin.getStatsCache().invalidate(uuid);
      this.savedArmor.remove(uuid);
      this.effectGuard.remove(uuid);
      event.setQuitMessage((String)null);
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena != null) {
         if (arena.getState() == ArenaState.STARTING) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
               event.setCancelled(true);
            }

         } else if (arena.getState() == ArenaState.PLAYING) {
            if (arena.getRegionMin() != null && arena.getRegionMax() != null) {
               Location to = event.getTo();
               if (to != null) {
                  if (!arena.isInRegion(to)) {
                     long now = System.currentTimeMillis();
                     UUID uuid = player.getUniqueId();
                     Long lastWarning = (Long)this.borderWarnings.get(uuid);
                     if (lastWarning == null || now - lastWarning > 5000L) {
                        this.borderWarnings.put(uuid, now);
                     }
                  }

                  Location from = event.getFrom();
                  if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
                     long now = System.currentTimeMillis();
                     Long lastTrapCheck = (Long)this.trapCooldowns.get(player.getUniqueId());
                     if (lastTrapCheck == null || now - lastTrapCheck > 500L) {
                        this.trapCooldowns.put(player.getUniqueId(), now);
                        this.plugin.getUpgradeManager().checkAndTriggerTrap(player, arena);
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena != null) {
         if (this.isOutsideRegion(player, arena)) {
            event.setCancelled(true);
         } else if (arena.getState() != ArenaState.PLAYING) {
            event.setCancelled(true);
         } else if (block.getType().name().endsWith("_BED")) {
            event.setCancelled(true);
            Team playerTeam = arena.getTeamByPlayer(player.getUniqueId());
            if (playerTeam != null) {
               for(Team t : arena.getTeams()) {
                  if (t.getBedLocation() != null && t.isBedBlock(block)) {
                     if (t.equals(playerTeam)) {
                        MessageUtil.sendMessage(player, "game.cant-break-own-bed");
                        return;
                     }

                     Location otherHalf = t.getOtherHalfLocation();
                     block.setType(Material.AIR);
                     if (otherHalf != null) {
                        otherHalf.getBlock().setType(Material.AIR);
                     }

                     for(Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), (double)1.0F, (double)1.0F, (double)1.0F)) {
                        if (entity instanceof Item) {
                           Item item = (Item)entity;
                           if (item.getItemStack().getType().name().endsWith("_BED")) {
                              entity.remove();
                           }
                        }
                     }

                     this.plugin.getGameManager().handleBedBreak(player, t, arena);
                     return;
                  }
               }

            }
         } else {
            if (!arena.isPlacedBlock(block.getLocation()) && !player.hasPermission("bedwars.bypass")) {
               event.setCancelled(true);
            } else {
               BlockData originalData = block.getBlockData().clone();
               arena.trackBlockBreak(block.getLocation(), originalData);
            }

         }
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena != null) {
         if (this.isOutsideRegion(player, arena)) {
            event.setCancelled(true);
         } else if (arena.getState() != ArenaState.PLAYING) {
            event.setCancelled(true);
         } else {
            Material blockType = event.getBlock().getType();
            if (this.plugin.getConfig().getStringList("game.blocked-blocks").contains(blockType.name())) {
               event.setCancelled(true);
               MessageUtil.sendMessage(player, "game.blocked-block", "block", blockType.name());
            } else {
               arena.trackBlockPlace(event.getBlock().getLocation());
               if (blockType == Material.TNT) {
                  Block tntBlock = event.getBlock();
                  tntBlock.setType(Material.AIR);
                  TNTPrimed tnt = (TNTPrimed)tntBlock.getWorld().spawn(tntBlock.getLocation().add((double)0.5F, (double)0.5F, (double)0.5F), TNTPrimed.class);
                  tnt.setFuseTicks(60);
                  this.tntOwners.put(tnt.getUniqueId(), player.getUniqueId());
                  this.startTntCountdown(tnt, player);
               }

            }
         }
      }
   }

   @EventHandler
   public void onBlockPhysics(BlockPhysicsEvent event) {
      Block block = event.getBlock();
      if (block.getType().name().endsWith("_BED")) {
         Arena arena = this.plugin.getArenaManager().getArenaByWorld(block.getWorld());
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onEntityDamage(EntityDamageEvent event) {
      Entity var3 = event.getEntity();
      if (var3 instanceof Player player) {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null) {
            if (arena.getState() != ArenaState.PLAYING) {
               event.setCancelled(true);
            } else {
               if (player.getGameMode() == GameMode.SPECTATOR || this.plugin.getSpectateManager().isSpectating(player.getUniqueId())) {
                  event.setCancelled(true);
               }

            }
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (!event.getEntity().hasMetadata("shop_npc") && !event.getEntity().hasMetadata("upgrade_npc")) {
         Entity var3 = event.getEntity();
         if (var3 instanceof Player) {
            Player victim = (Player)var3;
            Object var17 = null;
            Entity damagerEntity = event.getDamager();
            if (damagerEntity instanceof Player) {
               Player damager = (Player)damagerEntity;
               Arena var23 = this.plugin.getArenaManager().getArenaByPlayer(damager.getUniqueId());
               if (var23 == null || var23.getState() != ArenaState.PLAYING) {
                  return;
               }

               if (this.isOutsideRegion(damager, var23)) {
                  event.setCancelled(true);
                  return;
               }

               Team damagerTeam = var23.getTeamByPlayer(damager.getUniqueId());
               Team victimTeam = var23.getTeamByPlayer(victim.getUniqueId());
               if (damagerTeam != null && victimTeam != null && damagerTeam.equals(victimTeam)) {
                  event.setCancelled(true);
               } else {
                  this.plugin.getGameManager().recordDamage(victim, damager);
               }
            } else {
               label166: {
                  damagerEntity = event.getDamager();
                  if (damagerEntity instanceof Projectile) {
                     Projectile proj = (Projectile)damagerEntity;
                     ProjectileSource shooterTeam = proj.getShooter();
                     if (shooterTeam instanceof Player) {
                        Player shooter = (Player)shooterTeam;
                        Arena var22 = this.plugin.getArenaManager().getArenaByPlayer(victim.getUniqueId());
                        if (var22 == null || var22.getState() != ArenaState.PLAYING) {
                           return;
                        }

                        this.plugin.getGameManager().recordDamage(victim, shooter);
                        double remaining = Math.max((double)0.0F, victim.getHealth() - event.getFinalDamage());
                        String health = String.format("%.1f", remaining);
                        MessageUtil.sendMessage(shooter, "game.damage-indicator", "player", victim.getName(), "health", health);
                        this.plugin.getGameManager().playConfigSound(shooter, "sounds.damage-indicator");
                        break label166;
                     }
                  }

                  damagerEntity = event.getDamager();
                  if (damagerEntity instanceof Fireball) {
                     Fireball fireball = (Fireball)damagerEntity;
                     ProjectileSource ownerTeam = fireball.getShooter();
                     if (ownerTeam instanceof Player) {
                        Player shooter = (Player)ownerTeam;
                        Arena var21 = this.plugin.getArenaManager().getArenaByPlayer(shooter.getUniqueId());
                        if (var21 == null) {
                           return;
                        }

                        Team shooterTeam = var21.getTeamByPlayer(shooter.getUniqueId());
                        Team victimTeam = var21.getTeamByPlayer(victim.getUniqueId());
                        if (shooterTeam != null && victimTeam != null && shooterTeam.equals(victimTeam)) {
                           event.setCancelled(true);
                        } else {
                           this.plugin.getGameManager().recordDamage(victim, shooter);
                        }
                        break label166;
                     }
                  }

                  damagerEntity = event.getDamager();
                  if (damagerEntity instanceof EnderDragon) {
                     EnderDragon dragon = (EnderDragon)damagerEntity;
                     Team ownerTeam = this.plugin.getGameManager().getDragonTeam(dragon.getUniqueId());
                     if (ownerTeam == null) {
                        return;
                     }

                     Arena var18 = this.plugin.getArenaManager().getArenaByPlayer(victim.getUniqueId());
                     if (var18 == null) {
                        return;
                     }

                     Team victimTeam = var18.getTeamByPlayer(victim.getUniqueId());
                     if (victimTeam != null && victimTeam.equals(ownerTeam)) {
                        event.setCancelled(true);
                        return;
                     }

                     int buffLevel = (Integer)ownerTeam.getUpgrades().getOrDefault("dragon-buff", 0);
                     double multiplier = this.plugin.getUpgradeManager().getDragonDamageMultiplier(var18.getModeName(), buffLevel);
                     event.setDamage(event.getDamage() * multiplier);
                  } else {
                     damagerEntity = event.getDamager();
                     if (damagerEntity instanceof IronGolem) {
                        IronGolem golem = (IronGolem)damagerEntity;
                        Team ownerTeam = this.plugin.getGameManager().getGolemTeam(golem.getUniqueId());
                        if (ownerTeam == null) {
                           return;
                        }

                        Arena var19 = this.plugin.getArenaManager().getArenaByPlayer(victim.getUniqueId());
                        if (var19 == null) {
                           return;
                        }

                        Team victimTeam = var19.getTeamByPlayer(victim.getUniqueId());
                        if (victimTeam != null && victimTeam.equals(ownerTeam)) {
                           event.setCancelled(true);
                        } else if (victimTeam != null) {
                           event.setDamage(this.plugin.getConfig().getDouble("golem.damage", (double)4.0F));
                        }
                     } else {
                        damagerEntity = event.getDamager();
                        if (damagerEntity instanceof TNTPrimed) {
                            TNTPrimed tnt = (TNTPrimed)damagerEntity;
                           Arena var20 = this.plugin.getArenaManager().getArenaByPlayer(victim.getUniqueId());
                           if (var20 == null || var20.getState() != ArenaState.PLAYING) {
                              return;
                           }

                           UUID owner = (UUID)this.tntOwners.get(tnt.getUniqueId());
                           if (owner != null && this.plugin.getConfig().getBoolean("tnt-jump.enabled", true) && !this.plugin.getConfig().getBoolean("tnt-jump.take-damage", false) && owner.equals(victim.getUniqueId())) {
                              event.setCancelled(true);
                              return;
                           }

                           if (owner != null) {
                              Team ownerTeam = var20.getTeamByPlayer(owner);
                              Team victimTeam = var20.getTeamByPlayer(victim.getUniqueId());
                              if (ownerTeam != null && victimTeam != null && ownerTeam.equals(victimTeam)) {
                                 event.setCancelled(true);
                                 return;
                              }
                           }

                           event.setDamage(this.plugin.getConfig().getDouble("tnt.damage", (double)4.0F));
                        }
                     }
                  }
               }
            }

            if (!event.isCancelled() && victim.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
               victim.removePotionEffect(PotionEffectType.INVISIBILITY);
               this.plugin.getGameManager().playConfigSound(victim, "sounds.invisibility-removed");
            }

         }
      } else {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onPlayerInteractItem(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
         ItemStack item = event.getItem();
         if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null) {
               if (arena.getState() == ArenaState.STARTING) {
                  event.setCancelled(true);
                } else if (arena.getState() != ArenaState.WAITING) {
                   if (this.plugin.getSpectateManager().isSpectating(player.getUniqueId())) {
                      event.setCancelled(true);
                      if (item.getType() == Material.COMPASS) {
                         this.plugin.getSpectateManager().openSpectatorGUI(player, arena);
                       } else if (item.getType() == Material.RED_BED) {
                          this.plugin.getSpectateManager().disableSpectate(player);
                          if (this.plugin.getProxyManager() != null && this.plugin.getProxyManager().isEnabled()) {
                             this.plugin.getProxyManager().sendToServer(player);
                          }
                       }
                   } else if (this.plugin.getCompassTracker().isCompassItem(item)) {
                      event.setCancelled(true);
                      this.plugin.getCompassTracker().openTrackerGUI(player);
                   }

                } else {
                  if (item.getType() == Material.COMPASS) {
                     event.setCancelled(true);
                     arena.openTeamSelector(player);
               } else if (item.getType() == Material.RED_BED) {
                      event.setCancelled(true);
                      arena.removePlayer(player);
                      if (this.plugin.getProxyManager() != null && this.plugin.getProxyManager().isEnabled()) {
                         this.plugin.getProxyManager().sendToServer(player);
                      } else {
                         player.teleport(this.plugin.getMainLobby() != null ? this.plugin.getMainLobby() : ((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
                      }
                   }

               }
            }
         }
      }
   }

   @EventHandler
   public void onShieldBlock(PlayerInteractEvent event) {
      if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
         Player player = event.getPlayer();
         ItemStack item = event.getItem();
         if (item != null && item.getType() == Material.SHIELD) {
            Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null && arena.getState() == ArenaState.PLAYING) {
               if (this.plugin.getConfig().getString("pvp-mode", "MODERN").equalsIgnoreCase("CLASSIC")) {
                  event.setCancelled(true);
               }

            }
         }
      }
   }

   @EventHandler
   public void onFireballUse(PlayerInteractEvent event) {
      if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
         Player player = event.getPlayer();
         ItemStack item = event.getItem();
         if (item != null && item.getType() == Material.FIRE_CHARGE) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
               Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
               if (arena != null && arena.getState() == ArenaState.PLAYING) {
                  event.setCancelled(true);
                  Fireball fireball = (Fireball)player.launchProjectile(Fireball.class);
                  fireball.setYield(2.0F);
                  fireball.setIsIncendiary(false);
                  fireball.setVelocity(player.getEyeLocation().getDirection().normalize().multiply((double)1.5F));
                  item.setAmount(item.getAmount() - 1);
               }
            }
         }
      }
   }

   @EventHandler
   public void onExplosion(EntityExplodeEvent event) {
      Arena arena = this.plugin.getArenaManager().getArenaByWorld(event.getLocation().getWorld());
      if (arena != null && arena.getState() == ArenaState.PLAYING) {
         if (arena.getRegionMin() != null && arena.getRegionMax() != null) {
            Entity var4 = event.getEntity();
            if (var4 instanceof TNTPrimed) {
               TNTPrimed tnt = (TNTPrimed)var4;
               Integer taskId = (Integer)this.tntFuseTasks.remove(tnt.getUniqueId());
               if (taskId != null) {
                  FoliaUtil.cancelTask(taskId);
               }

               if (this.plugin.getConfig().getBoolean("tnt-jump.enabled", true)) {
                  UUID owner = (UUID)this.tntOwners.remove(tnt.getUniqueId());
                  double radius = this.plugin.getConfig().getDouble("tnt-jump.radius", (double)4.0F);
                  double power = this.plugin.getConfig().getDouble("tnt-jump.power", (double)2.5F);

                  for(Entity entity : tnt.getNearbyEntities(radius, radius, radius)) {
                     if (entity instanceof Player) {
                        Player target = (Player)entity;
                        if (arena.getPlayers().contains(target.getUniqueId())) {
                           if (owner != null && target.getUniqueId().equals(owner)) {
                              target.setVelocity(target.getVelocity().setY(power));
                           } else {
                              Vector away = target.getLocation().toVector().subtract(tnt.getLocation().toVector()).normalize();
                              away.setY(0.6);
                              target.setVelocity(away.multiply(0.8));
                           }
                        }
                     }
                  }
               }
            }

            Iterator<Block> iterator = event.blockList().iterator();

            while(iterator.hasNext()) {
               Block block = (Block)iterator.next();
               if (!arena.isInRegion(block.getLocation()) || !arena.isPlacedBlock(block.getLocation())) {
                  iterator.remove();
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onEggThrow(ProjectileLaunchEvent event) {
      Projectile var3 = event.getEntity();
      if (var3 instanceof Egg egg) {
         ProjectileSource var4 = egg.getShooter();
         if (var4 instanceof Player player) {
            ItemStack var14 = player.getInventory().getItemInMainHand();
            if (this.isGolemEgg(var14)) {
               this.plugin.getGameManager().spawnGolem(player);
               event.setCancelled(true);
            } else {
               if (!this.isBridgeEgg(var14)) {
                  var14 = player.getInventory().getItemInOffHand();
                  if (this.isGolemEgg(var14)) {
                     this.plugin.getGameManager().spawnGolem(player);
                     event.setCancelled(true);
                     return;
                  }

                  if (!this.isBridgeEgg(var14)) {
                     return;
                  }
               }

               Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
               if (arena != null && arena.getState() == ArenaState.PLAYING) {
                  Team team = arena.getTeamByPlayer(player.getUniqueId());
                  if (team != null) {
                     this.plugin.getGameManager().playConfigSound(player, "sounds.bridge-egg");
                     Material wool = arena.getWoolMaterial(team.getColor());
                     int startY = player.getLocation().getBlockY();
                     int[] lastX = new int[]{player.getLocation().getBlockX()};
                     int[] lastZ = new int[]{player.getLocation().getBlockZ()};
                     int[] ticks = new int[]{0};
                     int[] taskId = new int[]{-1};
                     taskId[0] = FoliaUtil.runTaskTimer(() -> {
                        int var10002 = ticks[0]++;
                        if (egg.isValid() && ticks[0] <= 200) {
                           Location loc = egg.getLocation();
                           int ex = loc.getBlockX();
                           int ez = loc.getBlockZ();
                           int stepX = ex > lastX[0] ? 1 : (ex < lastX[0] ? -1 : 0);
                           int stepZ = ez > lastZ[0] ? 1 : (ez < lastZ[0] ? -1 : 0);
                           int dist = Math.max(Math.abs(ex - lastX[0]), Math.abs(ez - lastZ[0]));

                           for(int s = 1; s <= dist; ++s) {
                              int bx = lastX[0] + stepX * s;
                              int bz = lastZ[0] + stepZ * s;
                              Block top = loc.getWorld().getBlockAt(bx, startY, bz);
                              Block bottom = top.getRelative(0, -1, 0);
                              if (top.getType() == Material.AIR || top.isLiquid()) {
                                 top.setType(wool);
                                 arena.trackBlockPlace(top.getLocation());
                              }

                              if (bottom.getType() == Material.AIR || bottom.isLiquid()) {
                                 bottom.setType(wool);
                                 arena.trackBlockPlace(bottom.getLocation());
                              }
                           }

                           lastX[0] = ex;
                           lastZ[0] = ez;
                        } else {
                           FoliaUtil.cancelTask(taskId[0]);
                        }
                     }, 0L, 1L);
                  }
               }
            }
         }
      }
   }

   private boolean isBridgeEgg(ItemStack item) {
      if (item != null && item.getType() == Material.EGG && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            String name = ChatColor.stripColor(meta.getDisplayName());
            return name.toLowerCase().contains("bridge egg") || name.toLowerCase().contains("köprü yumurtası");
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isGolemEgg(ItemStack item) {
      if (item != null && item.getType() == Material.EGG && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            String name = ChatColor.stripColor(meta.getDisplayName());
            return name.toLowerCase().contains("demir golem") || name.toLowerCase().contains("iron golem");
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void startTntCountdown(TNTPrimed tnt, Player player) {
      if (this.plugin.getConfig().getBoolean("tnt-jump.actionbar", true)) {
         int[] fuse = new int[]{60};
         int[] id = new int[]{-1};
         id[0] = FoliaUtil.runTaskTimer(() -> {
            int var10002 = fuse[0]--;
            if (fuse[0] < 0) {
               FoliaUtil.cancelTask(id[0]);
               this.tntFuseTasks.remove(tnt.getUniqueId());
            } else {
               int millis = fuse[0] * 50;
               String time = String.format("%d.%02d", millis / 1000, millis % 1000 / 10);
               MessageUtil.sendActionBar(player, "actionbar.tnt-countdown", "time", time);
            }
         }, 0L, 1L);
         this.tntFuseTasks.put(tnt.getUniqueId(), id[0]);
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         if (this.plugin.getCompassTracker().isGuiTitle(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getBottomInventory() && event.getCurrentItem() != null) {
               this.plugin.getCompassTracker().handleGuiClick(player, event.getSlot());
            }
         } else if (event.getView().getTitle().equals(MessageUtil.get("lobby.team-select-title"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
               if (event.getClickedInventory() != event.getView().getBottomInventory()) {
                  Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
                  if (arena != null && arena.getState() == ArenaState.WAITING) {
                     int slot = event.getSlot();
                     if (slot < arena.getTeams().size()) {
                        Team team = (Team)arena.getTeams().get(slot);
                        if (team.isFull()) {
                           MessageUtil.sendMessage(player, "lobby.team-full-msg");
                        } else {
                           arena.switchPlayerTeam(player, team);
                        }
                     }
                  } else {
                     player.closeInventory();
                  }
               }
            }
         } else {
            if (this.isLockedArmorSlot(event, player)) {
               event.setCancelled(true);
            }
            if (!event.isCancelled() && this.isCompassSlotClick(event, player)) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         PlayerInventory var8 = player.getInventory();

         for(int slot : event.getRawSlots()) {
            if (this.isArmorRawSlot(slot)) {
               ItemStack newItem = (ItemStack)event.getNewItems().get(slot);
               if (newItem != null && this.isLockedArmorItem(newItem, player)) {
                  event.setCancelled(true);
                  return;
               }

               ItemStack currentArmor = var8.getItem(slot);
               if (currentArmor != null && this.isLockedArmorItem(currentArmor, player)) {
                  event.setCancelled(true);
                  return;
               }
            }
            if (slot == 44) {
               ItemStack newItem = event.getNewItems().get(slot);
               if (newItem != null && this.plugin.getCompassTracker().isCompassItem(newItem)) {
                  event.setCancelled(true);
                  return;
               }
               ItemStack current = var8.getItem(slot);
               if (current != null && this.plugin.getCompassTracker().isCompassItem(current)) {
                  event.setCancelled(true);
                  return;
               }
            }
         }

      }
   }

   private boolean isLockedArmorSlot(InventoryClickEvent event, Player player) {
      if (event.getSlotType() == SlotType.ARMOR) {
         ItemStack current = event.getCurrentItem();
         if (current != null && this.isLockedArmorItem(current, player)) {
            return true;
         }
      }

      if (event.getClick() == ClickType.NUMBER_KEY) {
         Inventory var4 = event.getView().getBottomInventory();
         if (var4 instanceof PlayerInventory) {
            PlayerInventory inv = (PlayerInventory)var4;
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = inv.getItem(hotbarSlot);
            if (hotbarItem != null && this.isLockedArmorItem(hotbarItem, player)) {
               return true;
            }

            if (event.getSlotType() == SlotType.ARMOR) {
               ItemStack armorItem = inv.getItem(event.getSlot());
               if (armorItem != null && this.isLockedArmorItem(armorItem, player)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isCompassSlotClick(InventoryClickEvent event, Player player) {
      if (!this.plugin.getCompassTracker().isEnabled()) return false;
      int slot = event.getSlot();
      PlayerInventory inv = player.getInventory();
      if (slot >= 0 && slot <= 8 && inv.getItem(slot) != null && this.plugin.getCompassTracker().isCompassItem(inv.getItem(slot))) {
         return true;
      }
      if (event.getClick() == ClickType.NUMBER_KEY) {
         int hotbarSlot = event.getHotbarButton();
         ItemStack hotbarItem = inv.getItem(hotbarSlot);
         if (hotbarItem != null && this.plugin.getCompassTracker().isCompassItem(hotbarItem)) {
            return true;
         }
         ItemStack target = inv.getItem(slot);
         if (target != null && this.plugin.getCompassTracker().isCompassItem(target)) {
            return true;
         }
      }
      return false;
   }

   private boolean isLockedArmorItem(ItemStack item, Player player) {
      if (item == null) {
         return false;
      } else {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         return arena != null && arena.getState() == ArenaState.PLAYING ? this.isArmorItem(item) : false;
      }
   }

   private boolean isArmorRawSlot(int rawSlot) {
      return rawSlot >= 5 && rawSlot <= 8;
   }

   private boolean isArmorItem(ItemStack item) {
      Material type = item.getType();
      return type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") || type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS");
   }

   @EventHandler
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      ItemStack dropped = event.getItemDrop().getItemStack();
      if (dropped.getType() == Material.WOODEN_SWORD) {
         event.setCancelled(true);
      } else if (this.isLockedArmorItem(dropped, player)) {
         event.setCancelled(true);
      } else if (this.plugin.getCompassTracker().isCompassItem(dropped)) {
         event.setCancelled(true);
      } else {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null && arena.getState() != ArenaState.PLAYING) {
            event.setCancelled(true);
         } else {
            if (dropped.getType().name().endsWith("_SWORD") && !event.isCancelled()) {
               this.checkAndGiveWoodenSword(player);
            }

         }
      }
   }

   @EventHandler
   public void onPickupItem(EntityPickupItemEvent event) {
      LivingEntity var3 = event.getEntity();
      if (var3 instanceof Player player) {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            ItemStack item = event.getItem().getItemStack();
            if (item.getType().name().endsWith("_SWORD") && item.getType() != Material.WOODEN_SWORD) {
               player.getInventory().remove(Material.WOODEN_SWORD);
            }

         }
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      if (event.getRightClicked() instanceof Villager) {
         Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena != null && arena.getState() == ArenaState.PLAYING) {
            if (this.isOutsideRegion(player, arena)) {
               event.setCancelled(true);
            } else {
               Location npcLoc = event.getRightClicked().getLocation();

               for(ShopNPC npc : arena.getShopNpcs()) {
                  if (npc.getLocation().distance(npcLoc) < (double)2.0F) {
                     event.setCancelled(true);
                     if ("SHOP".equals(npc.getNpcType())) {
                        this.plugin.getShopManager().openShop(player);
                     } else if ("UPGRADE".equals(npc.getNpcType())) {
                        this.plugin.getUpgradeManager().openUpgradeMenu(player, arena);
                     }

                     return;
                  }
               }

            }
         }
      }
   }

   private void checkAndGiveWoodenSword(Player player) {
      if (!player.getInventory().contains(Material.WOODEN_SWORD)) {
         for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().name().endsWith("_SWORD") && item.getType() != Material.WOODEN_SWORD) {
               return;
            }
         }

         player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WOODEN_SWORD)});
      }
   }

   private boolean isOutsideRegion(Player player, Arena arena) {
      Location loc = player.getLocation();
      return arena.getRegionMin() != null && arena.getRegionMax() != null && !arena.isInRegion(loc);
   }
}
