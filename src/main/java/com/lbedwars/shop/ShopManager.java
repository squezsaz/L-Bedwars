package com.lbedwars.shop;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import com.lbedwars.util.ItemBuilder;
import com.lbedwars.util.MessageUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

public class ShopManager {
   private final LBedWars plugin;
   private final Map<String, Map<String, ShopCategory>> modeCategories;

   public ShopManager(LBedWars plugin) {
      this.plugin = plugin;
      this.modeCategories = new HashMap();
   }

   private Map<String, ShopCategory> loadCategories(String mode) {
      Map<String, ShopCategory> cats = new LinkedHashMap();
      ConfigurationSection catSection = this.plugin.getConfigManager().getShopConfig(mode).getConfigurationSection("categories");
      if (catSection == null) {
         return cats;
      } else {
         for(String key : catSection.getKeys(false)) {
            String iconStr = catSection.getString(key + ".icon", "STONE");
            Material icon = Material.getMaterial(iconStr);
            if (icon == null) {
               icon = Material.getMaterial("STONE");
               if (icon == null) {
                  icon = Material.BEDROCK;
               }
            }

            int slot = catSection.getInt(key + ".slot", 0);
            ShopCategory category = new ShopCategory(key, (String)null, icon, slot);
            List<Map<?, ?>> itemsList = catSection.getMapList(key + ".items");

            for(int i = 0; i < itemsList.size(); ++i) {
               ShopItem item = this.parseShopItem((Map)itemsList.get(i));
               if (item != null) {
                  category.addItem(item);
               }
            }

            cats.put(key, category);
         }

         return cats;
      }
   }

   private ShopItem parseShopItem(Map<?, ?> itemMap) {
      String materialStr = (String)itemMap.get("material");
      if (materialStr == null) {
         return null;
      } else {
         Material material = Material.getMaterial(materialStr);
         if (material == null) {
            return null;
         } else {
            int amount = itemMap.containsKey("amount") ? ((Number)itemMap.get("amount")).intValue() : 1;
            String displayName = (String)itemMap.get("display-name");
            String nameKey = (String)itemMap.get("name-key");
            Map<Enchantment, Integer> enchantments = new HashMap();
            if ("true".equals(String.valueOf(itemMap.get("enchanted")))) {
               enchantments.put(Enchantment.DURABILITY, 1);
            }

            boolean unbreakable = "true".equals(String.valueOf(itemMap.get("unbreakable")));
            String potionTypeStr = (String)itemMap.get("potion-type");
            PotionEffectType potionType = potionTypeStr != null ? PotionEffectType.getByName(potionTypeStr) : null;
            int potionLevel = itemMap.containsKey("potion-level") ? ((Number)itemMap.get("potion-level")).intValue() : 1;
            int potionDuration = itemMap.containsKey("potion-duration") ? ((Number)itemMap.get("potion-duration")).intValue() : -1;
            int price = 0;
            Material currency = Material.IRON_INGOT;
            if (itemMap.containsKey("price")) {
               Object priceObj = itemMap.get("price");
               if (priceObj instanceof Map) {
                  Map<?, ?> priceMap = (Map)priceObj;
                  price = ((Number)priceMap.get("amount")).intValue();
                  String currencyStr = (String)priceMap.get("material");
                  if (currencyStr != null) {
                     currency = Material.getMaterial(currencyStr);
                     if (currency == null) {
                        currency = Material.IRON_INGOT;
                     }
                  }
               } else if (priceObj instanceof Number) {
                  price = ((Number)priceObj).intValue();
                  if (itemMap.containsKey("currency")) {
                     String currencyStr = (String)itemMap.get("currency");
                     if (currencyStr != null) {
                        Material c = Material.getMaterial(currencyStr);
                        if (c != null) {
                           currency = c;
                        }
                     }
                  }
               }
            }

            return new ShopItem(material, amount, price, currency, (String)null, displayName, nameKey, enchantments, unbreakable, potionType, potionDuration, potionLevel, false);
         }
      }
   }

   public Map<String, ShopCategory> getCategories(String mode) {
      if (mode == null) {
         mode = "solo";
      }

      return (Map)this.modeCategories.computeIfAbsent(mode, this::loadCategories);
   }

   private String getPlayerMode(Player player) {
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      return arena != null ? arena.getModeName() : "solo";
   }

   public void openShop(Player player) {
      String mode = this.getPlayerMode(player);
      Map<String, ShopCategory> cats = this.getCategories(mode);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, MessageUtil.get("shop.title"));

      for(ShopCategory category : cats.values()) {
         String catName = MessageUtil.get("shop.category-" + category.getId());
         if (catName.isEmpty()) {
            String var10000 = category.getId().substring(0, 1).toUpperCase();
            catName = MessageUtil.colorize("&7" + var10000 + category.getId().substring(1));
         }

         ItemStack icon = (new ItemBuilder(category.getIcon())).name(catName).build();
         inv.setItem(category.getSlot(), icon);
      }

      player.openInventory(inv);
   }

   public void openCategory(Player player, String categoryId) {
      String mode = this.getPlayerMode(player);
      ShopCategory category = (ShopCategory)this.getCategories(mode).get(categoryId);
      if (category != null) {
         String title = MessageUtil.get("shop.category-" + categoryId);
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, title.isEmpty() ? "&6Shop" : title);
         List<ShopItem> items = category.getItems();

         for(int i = 0; i < items.size() && i < 45; ++i) {
            ShopItem shopItem = (ShopItem)items.get(i);
            Material displayMat = this.resolveWoolMaterial(player, shopItem.getMaterial());
            ItemBuilder builder = new ItemBuilder(displayMat, shopItem.getAmount());
            String itemName = shopItem.getDisplayName();
            if (itemName == null) {
               if (shopItem.getNameKey() != null) {
                  itemName = MessageUtil.get(shopItem.getNameKey());
                  if (itemName.isEmpty()) {
                     itemName = MessageUtil.get("shop.item-name", "item", this.formatMaterial(shopItem.getMaterial()));
                  }
               } else if (shopItem.getPotionType() != null) {
                  itemName = MessageUtil.get("shop.potion-name", "type", this.getPotionEffectName(shopItem.getPotionType()), "level", this.toRoman(shopItem.getPotionLevel()));
               } else {
                  itemName = MessageUtil.get("shop.item-name", "item", this.formatMaterial(shopItem.getMaterial()));
               }
            }

            builder.name(itemName);
            List<String> lore = new ArrayList();
            lore.add(MessageUtil.get("shop.price", "price", String.valueOf(shopItem.getPrice()), "currency", this.formatMaterial(shopItem.getCurrency())));
            lore.add("");
            if (!shopItem.getEnchantments().isEmpty()) {
               for(Map.Entry<Enchantment, Integer> entry : shopItem.getEnchantments().entrySet()) {
                  lore.add(MessageUtil.get("shop.enchantment-line", "name", this.getEnchantmentName((Enchantment)entry.getKey()), "level", this.toRoman((Integer)entry.getValue())));
               }

               lore.add("");
            }

            lore.add(MessageUtil.get("shop.click-buy"));
            builder.lore(lore);
            if (shopItem.isUnbreakable()) {
               builder.unbreakable();
            }

            if (shopItem.getPotionType() != null) {
               builder.potionBaseType(shopItem.getPotionType(), shopItem.getPotionLevel());
            }

            if (!shopItem.getEnchantments().isEmpty() || shopItem.getPotionType() != null) {
               builder.hideFlags();
            }

            ItemStack display = builder.build();
            inv.setItem(i, display);
         }

         ItemStack back = (new ItemBuilder(Material.ARROW)).name(MessageUtil.get("shop.back")).build();
         inv.setItem(49, back);
         player.openInventory(inv);
      }
   }

   public boolean purchaseItem(Player player, ShopItem shopItem) {
      Material currency = shopItem.getCurrency();
      int price = shopItem.getPrice();
      if (!this.hasEnough(player, currency, price)) {
         MessageUtil.sendMessage(player, "shop.cannot-afford", "price", String.valueOf(price), "currency", this.formatMaterial(currency));
         return false;
      } else {
         PlayerInventory inv = player.getInventory();
         Material type = this.resolveWoolMaterial(player, shopItem.getMaterial());
         if (this.isHelmet(type)) {
            String slotType = "helmet";
            ItemStack current = inv.getHelmet();
            if (current != null) {
               int currentTier = this.getArmorTier(current.getType());
               int newTier = this.getArmorTier(type);
               if (currentTier > newTier) {
                  MessageUtil.sendMessage(player, "shop.armor-downgrade", "slot", MessageUtil.get("shop.armor-slot-" + slotType));
                  return false;
               }

               if (currentTier == newTier) {
                  MessageUtil.sendMessage(player, "shop.already-owned");
                  return false;
               }
            }

            this.removeItems(player, currency, price);
            this.removeAllFromInventory(inv, Material.LEATHER_HELMET);
            this.removeAllFromInventory(inv, Material.CHAINMAIL_HELMET);
            this.removeAllFromInventory(inv, Material.IRON_HELMET);
            this.removeAllFromInventory(inv, Material.DIAMOND_HELMET);
            ItemStack armor = this.buildPurchaseItem(shopItem, type);
            this.applyProtectionUpgrade(player, armor);
            inv.setHelmet(armor);
            MessageUtil.sendMessage(player, "shop.purchased", "item", this.getItemName(shopItem), "x", String.valueOf(shopItem.getAmount()), "price", String.valueOf(price));
            this.plugin.getGameManager().playConfigSound(player, "sounds.item-buy");
            return true;
         } else if (this.isChestplate(type)) {
            String slotType = "chestplate";
            ItemStack current = inv.getChestplate();
            if (current != null) {
               int currentTier = this.getArmorTier(current.getType());
               int newTier = this.getArmorTier(type);
               if (currentTier > newTier) {
                  MessageUtil.sendMessage(player, "shop.armor-downgrade", "slot", MessageUtil.get("shop.armor-slot-" + slotType));
                  return false;
               }

               if (currentTier == newTier) {
                  MessageUtil.sendMessage(player, "shop.already-owned");
                  return false;
               }
            }

            this.removeItems(player, currency, price);
            this.removeAllFromInventory(inv, Material.LEATHER_CHESTPLATE);
            this.removeAllFromInventory(inv, Material.CHAINMAIL_CHESTPLATE);
            this.removeAllFromInventory(inv, Material.IRON_CHESTPLATE);
            this.removeAllFromInventory(inv, Material.DIAMOND_CHESTPLATE);
            ItemStack armor = this.buildPurchaseItem(shopItem, type);
            this.applyProtectionUpgrade(player, armor);
            inv.setChestplate(armor);
            MessageUtil.sendMessage(player, "shop.purchased", "item", this.getItemName(shopItem), "x", String.valueOf(shopItem.getAmount()), "price", String.valueOf(price));
            this.plugin.getGameManager().playConfigSound(player, "sounds.item-buy");
            return true;
         } else if (this.isBoots(type)) {
            Material leggingsMat = this.getLeggingsFor(type);
            if (leggingsMat == null) {
               return false;
            } else {
               int newTier = this.getArmorTier(type);
               int bootsTier = inv.getBoots() != null ? this.getArmorTier(inv.getBoots().getType()) : 0;
               int leggingsTier = inv.getLeggings() != null ? this.getArmorTier(inv.getLeggings().getType()) : 0;
               int currentMaxTier = Math.max(bootsTier, leggingsTier);
               boolean bootsCovered = bootsTier >= newTier;
               boolean leggingsCovered = leggingsTier >= newTier;
               if (bootsCovered && leggingsCovered) {
                  if (currentMaxTier > newTier) {
                     MessageUtil.sendMessage(player, "shop.armor-downgrade", "slot", MessageUtil.get("shop.armor-slot-armor"));
                     return false;
                  } else {
                     MessageUtil.sendMessage(player, "shop.already-owned");
                     return false;
                  }
               } else {
                  this.removeItems(player, currency, price);
                  this.removeAllFromInventory(inv, Material.LEATHER_BOOTS);
                  this.removeAllFromInventory(inv, Material.CHAINMAIL_BOOTS);
                  this.removeAllFromInventory(inv, Material.IRON_BOOTS);
                  this.removeAllFromInventory(inv, Material.DIAMOND_BOOTS);
                  this.removeAllFromInventory(inv, Material.LEATHER_LEGGINGS);
                  this.removeAllFromInventory(inv, Material.CHAINMAIL_LEGGINGS);
                  this.removeAllFromInventory(inv, Material.IRON_LEGGINGS);
                  this.removeAllFromInventory(inv, Material.DIAMOND_LEGGINGS);
                  ItemStack boots = this.buildPurchaseItem(shopItem, type);
                  this.applyProtectionUpgrade(player, boots);
                  inv.setBoots(boots);
                  ItemStack leggings = new ItemStack(leggingsMat);
                  ItemMeta leggingsMeta = leggings.getItemMeta();
                  leggingsMeta.setUnbreakable(true);
                  leggings.setItemMeta(leggingsMeta);
                  this.applyProtectionUpgrade(player, leggings);
                  inv.setLeggings(leggings);
                  MessageUtil.sendMessage(player, "shop.purchased", "item", this.getItemName(shopItem), "x", "1", "price", String.valueOf(price));
                  this.plugin.getGameManager().playConfigSound(player, "sounds.item-buy");
                  return true;
               }
            }
         } else {
            if (this.isSword(type)) {
               this.removeWoodenSword(inv);
            }

            ItemStack item = this.buildPurchaseItem(shopItem, type);
            this.applyEfficiencyUpgrade(player, item);
            this.applySharpnessUpgrade(player, item);
            HashMap<Integer, ItemStack> leftover = inv.addItem(new ItemStack[]{item});
            if (!leftover.isEmpty()) {
               MessageUtil.sendMessage(player, "shop.inventory-full");
               return false;
            } else {
               this.removeItems(player, currency, price);
               MessageUtil.sendMessage(player, "shop.purchased", "item", this.getItemName(shopItem), "x", String.valueOf(shopItem.getAmount()), "price", String.valueOf(price));
               this.plugin.getGameManager().playConfigSound(player, "sounds.item-buy");
               return true;
            }
         }
      }
   }

   private ItemStack buildPurchaseItem(ShopItem shopItem, Material material) {
      ItemBuilder builder = new ItemBuilder(material, shopItem.getAmount());
      if (shopItem.getDisplayName() != null) {
         builder.name(shopItem.getDisplayName());
      } else if (shopItem.getNameKey() != null) {
         String name = MessageUtil.get(shopItem.getNameKey());
         if (!name.isEmpty()) {
            builder.name(name);
         }
      } else if (shopItem.getPotionType() != null) {
         String name = MessageUtil.get("shop.potion-name", "type", this.getPotionEffectName(shopItem.getPotionType()), "level", this.toRoman(shopItem.getPotionLevel()));
         builder.name(name);
      }

      if (!shopItem.getEnchantments().isEmpty()) {
         builder.enchant(shopItem.getEnchantments());
      }

      if (shopItem.isUnbreakable()) {
         builder.unbreakable().hideFlags();
      }

      if (shopItem.getPotionType() != null) {
         builder.waterBase();
         builder.potionEffect(shopItem.getPotionType(), shopItem.getPotionDuration(), shopItem.getPotionLevel() - 1);
      }

      return builder.build();
   }

   private void applyEfficiencyUpgrade(Player player, ItemStack item) {
      if (item != null) {
         Material type = item.getType();
         if (type.name().endsWith("_PICKAXE")) {
            Arena arena = LBedWars.getInstance().getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null) {
               Team team = arena.getTeamByPlayer(player.getUniqueId());
               if (team != null) {
                  int level = team.getUpgradeLevel("haste");
                  if (level > 0) {
                     item.addUnsafeEnchantment(Enchantment.DIG_SPEED, level);
                  }
               }
            }
         }
      }
   }

   private void applySharpnessUpgrade(Player player, ItemStack item) {
      if (item != null) {
         Material type = item.getType();
         if (type.name().endsWith("_SWORD")) {
            Arena arena = LBedWars.getInstance().getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null) {
               Team team = arena.getTeamByPlayer(player.getUniqueId());
               if (team != null) {
                  int level = team.getUpgradeLevel("sharpness");
                  if (level > 0) {
                     Enchantment sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
                     if (sharpness != null) {
                        item.addUnsafeEnchantment(sharpness, level);
                     }

                  }
               }
            }
         }
      }
   }

   private void applyProtectionUpgrade(Player player, ItemStack item) {
      if (item != null) {
         Material type = item.getType();
         if (type.name().endsWith("_CHESTPLATE") || type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) {
            Arena arena = LBedWars.getInstance().getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null) {
               Team team = arena.getTeamByPlayer(player.getUniqueId());
               if (team != null) {
                  int level = team.getUpgradeLevel("protection");
                  if (level > 0) {
                     Enchantment protection = Enchantment.getByKey(NamespacedKey.minecraft("protection"));
                     if (protection != null) {
                        item.addUnsafeEnchantment(protection, level);
                     }

                  }
               }
            }
         }
      }
   }

   private String getPotionEffectName(PotionEffectType type) {
      String key = "potioneffect." + type.getName().toLowerCase();
      String translated = LBedWars.getInstance().getLanguageManager().getMessage(key);
      if (translated != null) {
         return translated;
      } else {
         String name = type.getName().toLowerCase().replace("_", " ");
         StringBuilder result = new StringBuilder();
         boolean nextUpper = true;

         for(char c : name.toCharArray()) {
            if (c == ' ') {
               result.append(' ');
               nextUpper = true;
            } else if (nextUpper) {
               result.append(Character.toUpperCase(c));
               nextUpper = false;
            } else {
               result.append(c);
            }
         }

         return result.toString();
      }
   }

   private Material resolveWoolMaterial(Player player, Material base) {
      if (base != Material.WHITE_WOOL) {
         return base;
      } else {
         Arena arena = LBedWars.getInstance().getArenaManager().getArenaByPlayer(player.getUniqueId());
         if (arena == null) {
            return base;
         } else {
            Team team = arena.getTeamByPlayer(player.getUniqueId());
            return team == null ? base : arena.getWoolMaterial(team.getColor());
         }
      }
   }

   private int getArmorTier(Material material) {
      String name = material.name();
      if (name.startsWith("LEATHER_")) {
         return 1;
      } else if (name.startsWith("CHAINMAIL_")) {
         return 2;
      } else if (name.startsWith("IRON_")) {
         return 3;
      } else if (name.startsWith("DIAMOND_")) {
         return 4;
      } else {
         return name.startsWith("NETHERITE_") ? 5 : 0;
      }
   }

   private Material getLeggingsFor(Material boots) {
      String name = boots.name();
      if (name.startsWith("LEATHER_")) {
         return Material.LEATHER_LEGGINGS;
      } else if (name.startsWith("CHAINMAIL_")) {
         return Material.CHAINMAIL_LEGGINGS;
      } else if (name.startsWith("IRON_")) {
         return Material.IRON_LEGGINGS;
      } else if (name.startsWith("DIAMOND_")) {
         return Material.DIAMOND_LEGGINGS;
      } else {
         return name.startsWith("NETHERITE_") ? Material.NETHERITE_LEGGINGS : null;
      }
   }

   private boolean isHelmet(Material material) {
      return material.name().endsWith("_HELMET");
   }

   private boolean isChestplate(Material material) {
      return material.name().endsWith("_CHESTPLATE");
   }

   private boolean isBoots(Material material) {
      return material.name().endsWith("_BOOTS");
   }

   private boolean isSword(Material material) {
      return material.name().endsWith("_SWORD");
   }

   private void removeWoodenSword(PlayerInventory inv) {
      for(ItemStack item : inv.getContents()) {
         if (item != null && item.getType() == Material.WOODEN_SWORD) {
            item.setAmount(0);
            return;
         }
      }

   }

   private void removeAllFromInventory(PlayerInventory inv, Material type) {
      for(ItemStack item : inv.getContents()) {
         if (item != null && item.getType() == type) {
            item.setAmount(0);
         }
      }

   }

   private boolean hasEnough(Player player, Material currency, int amount) {
      int count = 0;

      for(ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.getType() == currency) {
            count += item.getAmount();
         }
      }

      return count >= amount;
   }

   private void removeItems(Player player, Material type, int amount) {
      for(ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.getType() == type) {
            int remove = Math.min(amount, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            amount -= remove;
            if (amount <= 0) {
               break;
            }
         }
      }

   }

   private String getItemName(ShopItem shopItem) {
      if (shopItem.getDisplayName() != null) {
         return shopItem.getDisplayName();
      } else {
         if (shopItem.getNameKey() != null) {
            String name = LBedWars.getInstance().getLanguageManager().getMessage(shopItem.getNameKey());
            if (name != null && !name.isEmpty()) {
               return name;
            }
         }

         return shopItem.getPotionType() != null ? MessageUtil.get("shop.potion-name", "type", this.getPotionEffectName(shopItem.getPotionType()), "level", this.toRoman(shopItem.getPotionLevel())) : this.formatMaterial(shopItem.getMaterial());
      }
   }

   private String formatMaterial(Material material) {
      String key = "material." + material.name();
      String translated = LBedWars.getInstance().getLanguageManager().getMessage(key);
      if (translated != null) {
         return translated;
      } else {
         String name = material.name().replace("_", " ").toLowerCase();
         StringBuilder result = new StringBuilder();
         boolean nextUpper = true;

         for(char c : name.toCharArray()) {
            if (c == ' ') {
               result.append(' ');
               nextUpper = true;
            } else if (nextUpper) {
               result.append(Character.toUpperCase(c));
               nextUpper = false;
            } else {
               result.append(c);
            }
         }

         return result.toString();
      }
   }

   private String getEnchantmentName(Enchantment ench) {
      String key = "enchantment." + ench.getKey().getKey();
      String translated = LBedWars.getInstance().getLanguageManager().getMessage(key);
      if (translated != null) {
         return translated;
      } else {
         String name = ench.getKey().getKey();
         name = name.replace("_", " ").toLowerCase();
         StringBuilder result = new StringBuilder();
         boolean nextUpper = true;

         for(char c : name.toCharArray()) {
            if (c == ' ') {
               result.append(' ');
               nextUpper = true;
            } else if (nextUpper) {
               result.append(Character.toUpperCase(c));
               nextUpper = false;
            } else {
               result.append(c);
            }
         }

         return result.toString();
      }
   }

   private String toRoman(int n) {
      String var10000;
      switch (n) {
         case 1 -> var10000 = "I";
         case 2 -> var10000 = "II";
         case 3 -> var10000 = "III";
         case 4 -> var10000 = "IV";
         case 5 -> var10000 = "V";
         default -> var10000 = String.valueOf(n);
      }

      return var10000;
   }

   public Map<String, ShopCategory> getCategories() {
      return this.getCategories("solo");
   }
}
