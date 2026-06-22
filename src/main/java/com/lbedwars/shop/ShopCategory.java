package com.lbedwars.shop;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

public class ShopCategory {
   private final String id;
   private final String name;
   private final Material icon;
   private final int slot;
   private final List<ShopItem> items;

   public ShopCategory(String id, String name, Material icon, int slot) {
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.slot = slot;
      this.items = new ArrayList();
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Material getIcon() {
      return this.icon;
   }

   public int getSlot() {
      return this.slot;
   }

   public List<ShopItem> getItems() {
      return this.items;
   }

   public void addItem(ShopItem item) {
      this.items.add(item);
   }
}
