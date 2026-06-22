package com.lbedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {
   public static String serialize(Location loc) {
      if (loc == null) {
         return "";
      } else {
         String var10000 = loc.getWorld().getName();
         return var10000 + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
      }
   }

   public static Location deserialize(String data) {
      if (data != null && !data.isEmpty()) {
         String[] parts = data.split(";");
         if (parts.length < 4) {
            return null;
         } else {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
               return null;
            } else {
               double x = Double.parseDouble(parts[1]);
               double y = Double.parseDouble(parts[2]);
               double z = Double.parseDouble(parts[3]);
               float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0F;
               float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0F;
               return new Location(world, x, y, z, yaw, pitch);
            }
         }
      } else {
         return null;
      }
   }

   public static void saveLocation(ConfigurationSection section, String path, Location loc) {
      if (loc != null) {
         section.set(path + ".world", loc.getWorld().getName());
         section.set(path + ".x", loc.getX());
         section.set(path + ".y", loc.getY());
         section.set(path + ".z", loc.getZ());
         section.set(path + ".yaw", (double)loc.getYaw());
         section.set(path + ".pitch", (double)loc.getPitch());
      }
   }

   public static Location loadLocation(ConfigurationSection section, String path) {
      if (!section.contains(path + ".world")) {
         return null;
      } else {
         World world = Bukkit.getWorld(section.getString(path + ".world"));
         return world == null ? null : new Location(world, section.getDouble(path + ".x"), section.getDouble(path + ".y"), section.getDouble(path + ".z"), (float)section.getDouble(path + ".yaw"), (float)section.getDouble(path + ".pitch"));
      }
   }
}
