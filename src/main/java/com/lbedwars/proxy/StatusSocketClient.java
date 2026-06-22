package com.lbedwars.proxy;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bukkit.Bukkit;

public class StatusSocketClient {
   private static final int PROTOCOL_VERSION = 1;
   private final LBedWars plugin;
   private final String token;
   private final String serverName;
   private final List<LobbyConnection> connections;
   private ScheduledExecutorService executor;

   public StatusSocketClient(LBedWars plugin, List<String> addressStrings) {
      this.plugin = plugin;
      this.token = plugin.getConfig().getString("proxy-mode.socket-token", "");
      this.serverName = plugin.getConfig().getString("proxy-mode.server-name", "server-1");
      this.connections = new ArrayList<>();
      for (String addr : addressStrings) {
         String[] parts = addr.split(":");
         String host = parts[0].trim();
         int port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 5000;
         this.connections.add(new LobbyConnection(host, port));
      }
   }

   public void start() {
      this.executor = Executors.newSingleThreadScheduledExecutor();
      this.executor.scheduleWithFixedDelay(this::tick, 1L, 5L, TimeUnit.SECONDS);
   }

   public void shutdown() {
      if (this.executor != null) {
         this.executor.shutdownNow();
      }
      for (LobbyConnection conn : this.connections) {
         conn.close();
      }
   }

   private void tick() {
      Arena arena = this.findArena();
      if (arena == null) return;

      String payload = this.buildPayload(arena);
      String signature = this.hmac(payload);
      String message = "{\"type\":\"UPDATE\",\"data\":" + payload + ",\"signature\":\"" + signature + "\"}";

      for (LobbyConnection conn : this.connections) {
         if (!conn.isAuthenticated()) {
            conn.connect();
         }
         if (conn.isAuthenticated()) {
            conn.send(message);
         }
      }
   }

   private String buildPayload(Arena arena) {
      String stateName = arena.getState().name();
      int spectatorCount = arena.getSpectators().size();
      int online = arena.getPlayers().size() + spectatorCount;
      int max = arena.getTeamCount() * arena.getMaxTeamSize();
      String mode = arena.getModeName();
      String arenaName = arena.getName();
      int maxInTeam = arena.getMaxTeamSize();

      int gameTime = arena.getGameTime() / 20;

      StringBuilder playersJson = new StringBuilder("[");
      boolean first = true;
      for (UUID uuid : arena.getPlayers()) {
         org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            if (!first) playersJson.append(",");
            playersJson.append("\"").append(escape(p.getName())).append("\"");
            first = false;
         }
      }
      for (UUID uuid : arena.getSpectators()) {
         org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            if (!first) playersJson.append(",");
            playersJson.append("\"").append(escape(p.getName())).append("\"");
            first = false;
         }
      }
      playersJson.append("]");

      StringBuilder rejoinJson = new StringBuilder("[");
      boolean rejoinFirst = true;
      for (java.util.Map.Entry<java.util.UUID, com.lbedwars.core.GameManager.RejoinData> entry : this.plugin.getGameManager().getRejoinDataMap().entrySet()) {
         if (!rejoinFirst) rejoinJson.append(",");
         rejoinJson.append("\"").append(escape(entry.getValue().playerName())).append("\"");
         rejoinFirst = false;
      }
      rejoinJson.append("]");

      return "{\"server_name\":\"" + escape(this.serverName)
         + "\",\"arena_identifier\":\"" + escape(arenaName)
         + "\",\"arena_name\":\"" + escape(arenaName)
         + "\",\"arena_group\":\"" + escape(mode)
         + "\",\"arena_status\":\"" + escape(stateName)
         + "\",\"arena_max_players\":" + max
         + ",\"arena_current_players\":" + online
         + ",\"arena_max_in_team\":" + maxInTeam
         + ",\"arena_spectators\":" + spectatorCount
         + ",\"arena_time\":" + gameTime
         + ",\"arena_map\":\"" + escape(arenaName)
         + "\",\"arena_players\":" + playersJson.toString()
         + ",\"rejoin_players\":" + rejoinJson.toString()
         + ",\"spectate\":true}";
   }

   private Arena findArena() {
      for (Arena arena : this.plugin.getArenaManager().getAllArenas()) {
         if (arena.isEnabled()) return arena;
      }
      for (Arena arena : this.plugin.getArenaManager().getAllArenas()) {
         return arena;
      }
      return null;
   }

   private String hmac(String data) {
      try {
         Mac mac = Mac.getInstance("HmacSHA256");
         SecretKeySpec keySpec = new SecretKeySpec(this.token.getBytes("UTF-8"), "HmacSHA256");
         mac.init(keySpec);
         byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
         StringBuilder hex = new StringBuilder();
         for (byte b : raw) {
            hex.append(String.format("%02x", b & 0xFF));
         }
         return hex.toString();
      } catch (Exception e) {
         return "";
      }
   }

   private static String escape(String s) {
      if (s == null) return "";
      return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
   }

   private class LobbyConnection {
      private final String host;
      private final int port;
      private Socket socket;
      private PrintWriter writer;
      private Scanner reader;
      private boolean authenticated;

      LobbyConnection(String host, int port) {
         this.host = host;
         this.port = port;
      }

      boolean isAuthenticated() {
         return this.authenticated;
      }

      void connect() {
         this.close();
         try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(this.host, this.port), 3000);
            this.socket.setSoTimeout(5000);
            this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"), true);
            this.reader = new Scanner(this.socket.getInputStream(), "UTF-8");
            this.handshake();
            StatusSocketClient.this.plugin.getLogger().info("Connected to lobby proxy at " + this.host + ":" + this.port);
         } catch (IOException e) {
            StatusSocketClient.this.plugin.getLogger().info("Could not connect to lobby proxy at " + this.host + ":" + this.port + " (proxy not running?)");
            this.close();
         }
      }

      void send(String message) {
         if (this.writer == null) return;
         this.writer.println(message);
         if (this.writer.checkError()) {
            StatusSocketClient.this.plugin.getLogger().warning("Lost connection to " + this.host + ":" + this.port);
            this.authenticated = false;
            this.close();
         }
      }

      private void handshake() throws IOException {
         this.writer.println("{\"type\":\"HELLO\",\"protocol\":" + PROTOCOL_VERSION + "}");
         String helloResponse = this.reader.nextLine();
         if (helloResponse == null || !helloResponse.contains("\"HELLO_OK\"")) {
            throw new IOException("Invalid HELLO response from " + this.host);
         }

         this.writer.println("{\"type\":\"AUTH\",\"token\":\"" + StatusSocketClient.this.token + "\",\"server\":\"" + StatusSocketClient.this.serverName + "\"}");
         String authResponse = this.reader.nextLine();
         if (authResponse != null && authResponse.contains("\"AUTH_OK\"")) {
            this.authenticated = true;
         } else {
            throw new IOException("Authentication failed at " + this.host);
         }
      }

      private void close() {
         try {
            if (this.reader != null) this.reader.close();
            if (this.writer != null) this.writer.close();
            if (this.socket != null) this.socket.close();
         } catch (IOException ignored) {}
         this.reader = null;
         this.writer = null;
         this.socket = null;
      }
   }
}
