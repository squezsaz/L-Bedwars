package com.lbedwars.shop;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public class ShopItem {
   private final Material material;
   private final int amount;
   private final int price;
   private final Material currency;
   private final String configPath;
   private final String displayName;
   private final String nameKey;
   private final Map<Enchantment, Integer> enchantments;
   private final boolean unbreakable;
   private final PotionEffectType potionType;
   private final int potionDuration;
   private final int potionLevel;
   private final boolean armor;

   public ShopItem(Material material, int amount, int price, Material currency, String configPath, String displayName, Map<Enchantment, Integer> enchantments, boolean unbreakable, PotionEffectType potionType, int potionDuration, int potionLevel, boolean armor) {
      this(material, amount, price, currency, configPath, displayName, (String)null, enchantments, unbreakable, potionType, potionDuration, potionLevel, armor);
   }

   public ShopItem(Material material, int amount, int price, Material currency, String configPath, String displayName, String nameKey, Map<Enchantment, Integer> enchantments, boolean unbreakable, PotionEffectType potionType, int potionDuration, int potionLevel, boolean armor) {
      this.material = material;
      this.amount = amount;
      this.price = price;
      this.currency = currency;
      this.configPath = configPath;
      this.displayName = displayName;
      this.nameKey = nameKey;
      this.enchantments = enchantments != null ? enchantments : Collections.emptyMap();
      this.unbreakable = unbreakable;
      this.potionType = potionType;
      this.potionDuration = potionDuration;
      this.potionLevel = potionLevel;
      this.armor = armor;
   }

   public Material getMaterial() {
      return this.material;
   }

   public int getAmount() {
      return this.amount;
   }

   public int getPrice() {
      return this.price;
   }

   public Material getCurrency() {
      return this.currency;
   }

   public String getConfigPath() {
      return this.configPath;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getNameKey() {
      return this.nameKey;
   }

   public Map<Enchantment, Integer> getEnchantments() {
      return this.enchantments;
   }

   public boolean isUnbreakable() {
      return this.unbreakable;
   }

   public PotionEffectType getPotionType() {
      return this.potionType;
   }

   public int getPotionDuration() {
      return this.potionDuration;
   }

   public int getPotionLevel() {
      return this.potionLevel;
   }

   public boolean isArmor() {
      return this.armor;
   }
}
