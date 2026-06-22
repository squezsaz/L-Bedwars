package com.lbedwars.upgrade;

import java.util.List;
import org.bukkit.Material;

public class TeamUpgrade {
   private final String id;
   private final String name;
   private final Material icon;
   private final int slot;
   private final int maxLevel;
   private final List<Integer> costs;
   private final Material currency;
   private final List<String> effects;

   public TeamUpgrade(String id, String name, Material icon, int slot, int maxLevel, List<Integer> costs, Material currency, List<String> effects) {
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.slot = slot;
      this.maxLevel = maxLevel;
      this.costs = costs;
      this.currency = currency;
      this.effects = effects;
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

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public List<Integer> getCosts() {
      return this.costs;
   }

   public int getCost(int level) {
      return level < this.costs.size() ? (Integer)this.costs.get(level) : -1;
   }

   public Material getCurrency() {
      return this.currency;
   }

   public List<String> getEffects() {
      return this.effects;
   }
}
