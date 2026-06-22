package com.lbedwars.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class ItemBuilder {
   private final ItemStack item;
   private final ItemMeta meta;

   public ItemBuilder(Material material) {
      this(material, 1);
   }

   public ItemBuilder(Material material, int amount) {
      this.item = new ItemStack(material, amount);
      this.meta = this.item.getItemMeta();
   }

   public ItemBuilder(ItemStack item) {
      this.item = item.clone();
      this.meta = item.getItemMeta();
   }

   public ItemBuilder name(String name) {
      this.meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
      return this;
   }

   public ItemBuilder lore(String... lore) {
      List<String> colored = new ArrayList();

      for(String line : lore) {
         colored.add(ChatColor.translateAlternateColorCodes('&', line));
      }

      this.meta.setLore(colored);
      return this;
   }

   public ItemBuilder lore(List<String> lore) {
      List<String> colored = new ArrayList();

      for(String line : lore) {
         colored.add(ChatColor.translateAlternateColorCodes('&', line));
      }

      this.meta.setLore(colored);
      return this;
   }

   public ItemBuilder enchant(Enchantment enchantment, int level) {
      this.meta.addEnchant(enchantment, level, true);
      return this;
   }

   public ItemBuilder enchant(Map<Enchantment, Integer> enchantments) {
      for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
         this.meta.addEnchant((Enchantment)entry.getKey(), (Integer)entry.getValue(), true);
      }

      return this;
   }

   public ItemBuilder hideFlags() {
      this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS});
      return this;
   }

   public ItemBuilder unbreakable() {
      this.meta.setUnbreakable(true);
      return this;
   }

   public ItemBuilder amount(int amount) {
      this.item.setAmount(amount);
      return this;
   }

   public ItemBuilder armorColor(Color color) {
      ItemMeta var3 = this.meta;
      if (var3 instanceof LeatherArmorMeta lam) {
         lam.setColor(color);
      }

      return this;
   }

   public ItemBuilder waterBase() {
      ItemMeta var2 = this.meta;
      if (var2 instanceof PotionMeta pm) {
         try {
            pm.setBasePotionData(new PotionData(PotionType.WATER, false, false));
         } catch (Throwable var7) {
            try {
               Class<?> potionTypeClass = Class.forName("org.bukkit.potion.PotionType");
               Object waterType = potionTypeClass.getField("WATER").get((Object)null);
               Method setType = PotionMeta.class.getMethod("setBasePotionType", potionTypeClass);
               setType.invoke(pm, waterType);
            } catch (Throwable var6) {
            }
         }
      }

      return this;
   }

   public ItemBuilder potionEffect(PotionEffectType type, int duration, int amplifier) {
      ItemMeta var5 = this.meta;
      if (var5 instanceof PotionMeta pm) {
         pm.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
      }

      return this;
   }

   public ItemBuilder customModelData(int data) {
      this.meta.setCustomModelData(data);
      return this;
   }

   public ItemBuilder potionBaseType(PotionEffectType effectType, int level) {
      ItemMeta var4 = this.meta;
      if (var4 instanceof PotionMeta pm) {
         try {
            PotionType type = null;
            if (effectType.equals(PotionEffectType.SPEED)) {
               type = PotionType.SPEED;
            } else if (effectType.equals(PotionEffectType.JUMP)) {
               type = PotionType.JUMP;
            } else if (effectType.equals(PotionEffectType.INVISIBILITY)) {
               type = PotionType.INVISIBILITY;
            } else if (effectType.equals(PotionEffectType.REGENERATION)) {
               type = PotionType.REGEN;
            } else if (effectType.equals(PotionEffectType.FIRE_RESISTANCE)) {
               type = PotionType.FIRE_RESISTANCE;
            } else if (effectType.equals(PotionEffectType.POISON)) {
               type = PotionType.POISON;
            } else if (effectType.equals(PotionEffectType.WEAKNESS)) {
               type = PotionType.WEAKNESS;
            } else if (effectType.equals(PotionEffectType.INCREASE_DAMAGE)) {
               type = PotionType.STRENGTH;
            } else if (effectType.equals(PotionEffectType.SLOW)) {
               type = PotionType.SLOWNESS;
            } else if (effectType.equals(PotionEffectType.WATER_BREATHING)) {
               type = PotionType.WATER_BREATHING;
            } else if (effectType.equals(PotionEffectType.NIGHT_VISION)) {
               type = PotionType.NIGHT_VISION;
            } else if (effectType.equals(PotionEffectType.SLOW_FALLING)) {
               type = PotionType.SLOW_FALLING;
            }

            if (type != null) {
               pm.setBasePotionData(new PotionData(type, level >= 2, false));
            }
         } catch (Throwable var10) {
            try {
               String modernName = this.getModernPotionTypeName(effectType);
               if (modernName == null) {
                  return this;
               }

               Class<?> potionTypeClass = Class.forName("org.bukkit.potion.PotionType");
               Object modernType = potionTypeClass.getField(modernName).get((Object)null);
               if (modernType == null) {
                  return this;
               }

               Method setType = PotionMeta.class.getMethod("setBasePotionType", potionTypeClass);
               setType.invoke(pm, modernType);
            } catch (Throwable var9) {
            }
         }
      }

      return this;
   }

   private String getModernPotionTypeName(PotionEffectType type) {
      if (type.equals(PotionEffectType.SPEED)) {
         return "SWIFTNESS";
      } else if (type.equals(PotionEffectType.JUMP)) {
         return "LEAPING";
      } else if (type.equals(PotionEffectType.INVISIBILITY)) {
         return "INVISIBILITY";
      } else if (type.equals(PotionEffectType.REGENERATION)) {
         return "REGENERATION";
      } else if (type.equals(PotionEffectType.FIRE_RESISTANCE)) {
         return "FIRE_RESISTANCE";
      } else if (type.equals(PotionEffectType.POISON)) {
         return "POISON";
      } else if (type.equals(PotionEffectType.WEAKNESS)) {
         return "WEAKNESS";
      } else if (type.equals(PotionEffectType.INCREASE_DAMAGE)) {
         return "STRENGTH";
      } else if (type.equals(PotionEffectType.SLOW)) {
         return "SLOWNESS";
      } else if (type.equals(PotionEffectType.WATER_BREATHING)) {
         return "WATER_BREATHING";
      } else if (type.equals(PotionEffectType.NIGHT_VISION)) {
         return "NIGHT_VISION";
      } else {
         return type.equals(PotionEffectType.SLOW_FALLING) ? "SLOW_FALLING" : null;
      }
   }

   public ItemStack build() {
      this.item.setItemMeta(this.meta);
      return this.item;
   }
}
