package com.lbedwars.npc;

import com.lbedwars.arena.Arena;
import com.lbedwars.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Villager.Type;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class ShopNPC {
   private final Location location;
   private final Arena arena;
   private final String npcType;
   private Entity spawned;

   public ShopNPC(Location location, Arena arena, String npcType) {
      this.location = location;
      this.arena = arena;
      this.npcType = npcType;
   }

   public Entity spawn(Plugin plugin) {
      if (this.spawned != null && !this.spawned.isDead()) {
         return this.spawned;
      } else {
         Villager villager = (Villager)this.location.getWorld().spawnEntity(this.location, EntityType.VILLAGER);
         villager.setAI(false);
         villager.setSilent(true);
         villager.setInvulnerable(true);
         villager.setCollidable(false);
         villager.setGravity(false);
         villager.setCanPickupItems(false);
         villager.setVillagerType(Type.PLAINS);
         villager.setProfession(Profession.NONE);
         if ("SHOP".equals(this.npcType)) {
            String name = MessageUtil.get("npc.shop-name");
            if (name.isEmpty()) {
               name = "§a§lSHOP";
            }

            villager.setCustomName(name);
         } else {
            String name = MessageUtil.get("npc.upgrade-name");
            if (name.isEmpty()) {
               name = "§6§lUPGRADE";
            }

            villager.setCustomName(name);
         }

         villager.setCustomNameVisible(true);
         villager.setMetadata(this.npcType.toLowerCase() + "_npc", new FixedMetadataValue(plugin, true));
         this.spawned = villager;
         return villager;
      }
   }

   public void remove() {
      if (this.spawned != null && !this.spawned.isDead()) {
         this.spawned.remove();
      }

      this.spawned = null;
   }

   public Location getLocation() {
      return this.location;
   }

   public Arena getArena() {
      return this.arena;
   }

   public String getNpcType() {
      return this.npcType;
   }

   public Entity getSpawned() {
      return this.spawned;
   }
}
