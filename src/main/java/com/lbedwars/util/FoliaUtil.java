package com.lbedwars.util;

import com.lbedwars.LBedWars;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class FoliaUtil {
   private static final boolean FOLIA;
   private static Method globalRegionSchedulerMethod;
   private static Method runMethod;
   private static Method runDelayedMethod;
   private static Method runAtFixedRateMethod;
   private static Method cancelMethod;
   private static final AtomicInteger taskCounter = new AtomicInteger(1);
   private static final Map<Integer, Object> scheduledTasks = new ConcurrentHashMap();

   public static boolean isFolia() {
      return FOLIA;
   }

   public static boolean isPaper() {
      try {
         Class.forName("com.destroystokyo.paper.PaperConfig");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   public static void runTask(Runnable task) {
      if (FOLIA) {
         try {
            Object scheduler = globalRegionSchedulerMethod.invoke((Object)null);
            Object scheduledTask = runMethod.invoke(scheduler, LBedWars.getInstance(), task);
            int id = taskCounter.getAndIncrement();
            scheduledTasks.put(id, scheduledTask);
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {
         Bukkit.getScheduler().runTask(LBedWars.getInstance(), task);
      }

   }

   public static int runTaskLater(Runnable task, long delay) {
      if (FOLIA) {
         try {
            Object scheduler = globalRegionSchedulerMethod.invoke((Object)null);
            Object scheduledTask = runDelayedMethod.invoke(scheduler, LBedWars.getInstance(), task, delay);
            int id = taskCounter.getAndIncrement();
            scheduledTasks.put(id, scheduledTask);
            return id;
         } catch (Exception e) {
            e.printStackTrace();
            return -1;
         }
      } else {
         return Bukkit.getScheduler().runTaskLater(LBedWars.getInstance(), task, delay).getTaskId();
      }
   }

   public static int runTaskTimer(Runnable task, long delay, long period) {
      if (FOLIA) {
         try {
            Object scheduler = globalRegionSchedulerMethod.invoke((Object)null);
            Object scheduledTask = runAtFixedRateMethod.invoke(scheduler, LBedWars.getInstance(), task, delay, period);
            int id = taskCounter.getAndIncrement();
            scheduledTasks.put(id, scheduledTask);
            return id;
         } catch (Exception e) {
            e.printStackTrace();
            return -1;
         }
      } else {
         return Bukkit.getScheduler().runTaskTimer(LBedWars.getInstance(), task, delay, period).getTaskId();
      }
   }

   public static void cancelTask(int taskId) {
      if (FOLIA) {
         Object scheduledTask = scheduledTasks.remove(taskId);
         if (scheduledTask != null) {
            try {
               cancelMethod.invoke(scheduledTask);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      } else {
         Bukkit.getScheduler().cancelTask(taskId);
      }

   }

   public static void runAtEntityLocation(Entity entity, Runnable task) {
      if (FOLIA) {
         try {
            Method getScheduler = entity.getClass().getDeclaredMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(entity);
            Method run = entityScheduler.getClass().getDeclaredMethod("run", Plugin.class, Runnable.class, Runnable.class);
            run.invoke(entityScheduler, LBedWars.getInstance(), task, (Runnable)null);
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {
         task.run();
      }

   }

   static {
      boolean folia = false;

      try {
         Class<?> regionizedServer = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         folia = true;
         Class<?> globalRegionScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
         Method getScheduler = Bukkit.class.getDeclaredMethod("getGlobalRegionScheduler");
         globalRegionSchedulerMethod = getScheduler;
         runMethod = globalRegionScheduler.getDeclaredMethod("run", Plugin.class, Runnable.class);
         runDelayedMethod = globalRegionScheduler.getDeclaredMethod("runDelayed", Plugin.class, Runnable.class, Long.TYPE);
         runAtFixedRateMethod = globalRegionScheduler.getDeclaredMethod("runAtFixedRate", Plugin.class, Runnable.class, Long.TYPE, Long.TYPE);
         cancelMethod = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask").getDeclaredMethod("cancel");
      } catch (Exception var4) {
      }

      FOLIA = folia;
   }
}
