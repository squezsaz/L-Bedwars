package com.lbedwars.proxy;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.configuration.file.FileConfiguration;

public class ProxyManager implements Listener {
   private final LBedWars plugin;
   private boolean enabled;
   private String endAction;
   private final List<String> lobbyServers;
   private boolean motdEnabled;
   private String motdWaitingLine1;
   private String motdWaitingLine2;
   private int motdWaitingMax;
   private String motdFullLine1;
   private String motdFullLine2;
   private int motdFullMax;
   private String motdRunningLine1;
   private String motdRunningLine2;
   private int motdRunningMax;
   private String motdRebuildingLine1;
   private String motdRebuildingLine2;
   private int motdRebuildingMax;
   private String motdDisabledLine1;
   private String motdDisabledLine2;
   private int motdDisabledMax;
   private String worldTemplate;
   private final Random random;

   public ProxyManager(LBedWars plugin) {
      this.plugin = plugin;
      this.lobbyServers = new ArrayList();
      this.random = new Random();
      this.loadConfig();
   }

   public void loadConfig() {
      FileConfiguration config = this.plugin.getConfig();
      this.enabled = config.getBoolean("proxy-mode.enabled", false);
      this.endAction = config.getString("proxy-mode.end-action", "restart");
      this.lobbyServers.clear();
      this.lobbyServers.addAll(config.getStringList("proxy-mode.lobby-servers"));
      this.motdEnabled = config.getBoolean("proxy-mode.motd.enabled", false);
      this.motdWaitingLine1 = config.getString("proxy-mode.motd.waiting.line1", "&aL-BedWars &7- &eWaiting...");
      this.motdWaitingLine2 = config.getString("proxy-mode.motd.waiting.line2", "&7Map: {map}");
      this.motdWaitingMax = config.getInt("proxy-mode.motd.waiting.max-players", 100);
      this.motdFullLine1 = config.getString("proxy-mode.motd.waiting_full.line1", "&cL-BedWars &7- &6Full!");
      this.motdFullLine2 = config.getString("proxy-mode.motd.waiting_full.line2", "&7Map: {map}");
      this.motdFullMax = config.getInt("proxy-mode.motd.waiting_full.max-players", 100);
      this.motdRunningLine1 = config.getString("proxy-mode.motd.running.line1", "&aL-BedWars &7- &aIn Game");
      this.motdRunningLine2 = config.getString("proxy-mode.motd.running.line2", "&7Map: {map}");
      this.motdRunningMax = config.getInt("proxy-mode.motd.running.max-players", 100);
      this.motdRebuildingLine1 = config.getString("proxy-mode.motd.rebuilding.line1", "&eL-BedWars &7- &eRebuilding...");
      this.motdRebuildingLine2 = config.getString("proxy-mode.motd.rebuilding.line2", "&7Please wait...");
      this.motdRebuildingMax = config.getInt("proxy-mode.motd.rebuilding.max-players", 0);
      this.motdDisabledLine1 = config.getString("proxy-mode.motd.disabled.line1", "&cL-BedWars &7- &cDisabled");
      this.motdDisabledLine2 = config.getString("proxy-mode.motd.disabled.line2", "&7Coming soon...");
      this.motdDisabledMax = config.getInt("proxy-mode.motd.disabled.max-players", 0);
      this.worldTemplate = config.getString("proxy-mode.world-template", "world_template");
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getEndAction() {
      return this.endAction;
   }

   public List<String> getLobbyServers() {
      return this.lobbyServers;
   }

   public void sendAllToLobby(Arena arena) {
      for (UUID uuid : arena.getPlayers()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            this.sendToServer(player);
         }
      }

      for (UUID uuid : arena.getSpectators()) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null) {
            this.sendToServer(player);
         }
      }
   }

   public void sendToServer(Player player) {
      if (this.lobbyServers.isEmpty()) {
         return;
      }

      String server = (String)this.lobbyServers.get(this.random.nextInt(this.lobbyServers.size()));
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(b);

      try {
         out.writeUTF("Connect");
         out.writeUTF(server);
      } catch (IOException var6) {
         return;
      }

      player.sendPluginMessage(this.plugin, "BungeeCord", b.toByteArray());
   }

   public void executeEndAction() {
      switch (this.endAction.toLowerCase()) {
         case "restart":
            Bukkit.spigot().restart();
            break;
         case "stop":
            Bukkit.shutdown();
      }
   }

   @EventHandler
   public void onServerListPing(ServerListPingEvent event) {
      if (!this.enabled || !this.motdEnabled) {
         return;
      }

      Arena arena = null;
      for (Arena a : this.plugin.getArenaManager().getAllArenas()) {
         if (a.isEnabled()) {
            arena = a;
            break;
         }
      }

      if (arena == null) {
         event.setMotd(this.colorize(this.motdDisabledLine1 + "\n" + this.motdDisabledLine2));
         event.setMaxPlayers(this.motdDisabledMax);
         return;
      }

      ArenaState state = arena.getState();
      String mapName = arena.getName();
      int online = Bukkit.getOnlinePlayers().size();

      switch (state) {
         case WAITING:
         case STARTING: {
            int maxPlayers = arena.getTeamCount() * arena.getMaxTeamSize();
            if (online >= maxPlayers) {
               event.setMotd(this.colorize(this.motdFullLine1.replace("{map}", mapName) + "\n" + this.motdFullLine2.replace("{map}", mapName)));
               event.setMaxPlayers(this.motdFullMax);
            } else {
               event.setMotd(this.colorize(this.motdWaitingLine1.replace("{map}", mapName) + "\n" + this.motdWaitingLine2.replace("{map}", mapName)));
               event.setMaxPlayers(this.motdWaitingMax);
            }
            break;
         }
         case PLAYING:
         case ENDING: {
            event.setMotd(this.colorize(this.motdRunningLine1.replace("{map}", mapName) + "\n" + this.motdRunningLine2.replace("{map}", mapName)));
            event.setMaxPlayers(this.motdRunningMax);
            break;
         }
         case RESTARTING: {
            event.setMotd(this.colorize(this.motdRebuildingLine1.replace("{map}", mapName) + "\n" + this.motdRebuildingLine2.replace("{map}", mapName)));
            event.setMaxPlayers(this.motdRebuildingMax);
            break;
         }
      }
   }

   public boolean enforceMySQL() {
      if (!this.enabled) {
         return false;
      }

      String currentType = this.plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
      if (!currentType.equals("MYSQL")) {
         this.plugin.getLogger().warning("Proxy mode requires MySQL! Changing storage type to MySQL.");
         this.plugin.getConfig().set("storage.type", "MySQL");
         this.plugin.saveConfig();
         return true;
      }

      return false;
   }

   public void setupWorld() {
      if (!this.enabled) {
         return;
      }

      File templateDir = new File(this.worldTemplate);
      if (!templateDir.exists() || !templateDir.isDirectory()) {
         templateDir = new File(this.plugin.getDataFolder(), this.worldTemplate);
         if (!templateDir.exists() || !templateDir.isDirectory()) {
            this.plugin.getLogger().warning("World template not found: " + this.worldTemplate + ". Skipping world reset.");
            return;
         }
      }

      Arena arena = null;
      for (Arena a : this.plugin.getArenaManager().getAllArenas()) {
         if (a.isEnabled()) {
            arena = a;
            break;
         }
      }

      if (arena == null) {
         return;
      }

      String worldName = null;
      if (arena.getLobbyLocation() != null && arena.getLobbyLocation().getWorld() != null) {
         worldName = arena.getLobbyLocation().getWorld().getName();
      }

      if (worldName == null) {
         for (com.lbedwars.arena.Team team : arena.getTeams()) {
            if (team.getSpawn() != null && team.getSpawn().getWorld() != null) {
               worldName = team.getSpawn().getWorld().getName();
               break;
            }
         }
      }

      if (worldName == null) {
         this.plugin.getLogger().warning("Could not determine arena world name. Skipping world reset.");
         return;
      }

      World world = Bukkit.getWorld(worldName);
      if (world != null) {
         Bukkit.unloadWorld(world, false);
      }

      File worldDir = new File(worldName);
      if (worldDir.exists()) {
         this.deleteDirectory(worldDir);
      }

      this.copyDirectory(templateDir, worldDir);
      this.plugin.getLogger().info("World reset from template: " + this.worldTemplate + " -> " + worldName);
      (new WorldCreator(worldName)).createWorld();
   }

   private void copyDirectory(File source, File target) {
      if (source.isDirectory()) {
         if (!target.exists()) {
            target.mkdirs();
         }

         File[] files = source.listFiles();
         if (files != null) {
            for (File f : files) {
               File dest = new File(target, f.getName());
               if (f.isDirectory()) {
                  this.copyDirectory(f, dest);
               } else {
                  this.copyFile(f, dest);
               }
            }
         }
      }
   }

   private void copyFile(File source, File target) {
      try {
         InputStream in = new FileInputStream(source);
         OutputStream out = new FileOutputStream(target);
         byte[] buf = new byte[8192];

         int len;
         while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
         }

         in.close();
         out.close();
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to copy file: " + source.getPath());
      }
   }

   private void deleteDirectory(File path) {
      if (path.isDirectory()) {
         File[] files = path.listFiles();
         if (files != null) {
            for (File f : files) {
               if (f.isDirectory()) {
                  this.deleteDirectory(f);
               } else {
                  f.delete();
               }
            }
         }
      }

      path.delete();
   }

   private String colorize(String text) {
      return text.replace("&", "§");
   }
}
