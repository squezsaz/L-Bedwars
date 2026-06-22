package com.lbedwars.listener;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.shop.ShopCategory;
import com.lbedwars.shop.ShopItem;
import com.lbedwars.upgrade.TeamUpgrade;
import com.lbedwars.util.MessageUtil;
import java.util.Map;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopListener implements Listener {
   private final LBedWars plugin;

   public ShopListener(LBedWars plugin) {
      this.plugin = plugin;
   }

   private String getPlayerMode(Player player) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      return arena != null ? arena.getModeName() : "solo";
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         if (event.getCurrentItem() != null) {
            if (event.getClickedInventory() != event.getView().getBottomInventory()) {
               String title = event.getView().getTitle();
               String mode = this.getPlayerMode(player);
               Map<String, ShopCategory> cats = this.plugin.getShopManager().getCategories(mode);
               if (title.equals(MessageUtil.get("shop.title"))) {
                  event.setCancelled(true);
                  int slot = event.getSlot();

                  for(ShopCategory category : cats.values()) {
                     if (category.getSlot() == slot) {
                        this.plugin.getShopManager().openCategory(player, category.getId());
                        return;
                     }
                  }
               }

               for(ShopCategory category : cats.values()) {
                  String catTitle = MessageUtil.get("shop.category-" + category.getId());
                  if (!catTitle.isEmpty() && title.equals(catTitle)) {
                     event.setCancelled(true);
                     int slot = event.getSlot();
                     if (slot == 49) {
                        this.plugin.getShopManager().openShop(player);
                        return;
                     }

                     if (slot < category.getItems().size()) {
                        ShopItem item = (ShopItem)category.getItems().get(slot);
                        this.plugin.getShopManager().purchaseItem(player, item);
                     }

                     return;
                  }
               }

               if (title.equals(MessageUtil.get("shop.upgrade-title"))) {
                  event.setCancelled(true);
                  int slot = event.getSlot();
                  Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
                  if (arena == null) {
                     return;
                  }

                  Team team = arena.getTeamByPlayer(player.getUniqueId());
                  if (team == null) {
                     return;
                  }

                  for(TeamUpgrade upgrade : this.plugin.getUpgradeManager().getUpgrades(arena).values()) {
                     if (upgrade.getSlot() == slot) {
                        this.plugin.getUpgradeManager().purchaseUpgrade(player, team, upgrade.getId());
                        this.plugin.getUpgradeManager().openUpgradeMenu(player, arena);
                        return;
                     }
                  }

                  if (slot == this.plugin.getConfigManager().getUpgradesConfig(mode).getInt("traps.menu-slot", 25)) {
                     this.plugin.getUpgradeManager().openTrapMenu(player, arena);
                     return;
                  }
               }

               if (title.equals(MessageUtil.get("shop.trap-title"))) {
                  event.setCancelled(true);
                  Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
                  if (arena == null) {
                     return;
                  }

                  Team team = arena.getTeamByPlayer(player.getUniqueId());
                  if (team == null) {
                     return;
                  }

                  int slot = event.getSlot();
                  if (slot == 49) {
                     this.plugin.getUpgradeManager().openUpgradeMenu(player, arena);
                     return;
                  }

                  for(Map.Entry<String, TeamUpgrade> entry : this.plugin.getUpgradeManager().getTraps(mode).entrySet()) {
                     if (((TeamUpgrade)entry.getValue()).getSlot() == slot) {
                        this.plugin.getUpgradeManager().purchaseTrap(player, team, (String)entry.getKey());
                        this.plugin.getUpgradeManager().openTrapMenu(player, arena);
                        return;
                     }
                  }
               }

            }
         }
      }
   }
}
