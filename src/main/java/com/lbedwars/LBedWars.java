package com.lbedwars;

import com.lbedwars.arena.ArenaManager;
import com.lbedwars.command.BedWarsAdminCommand;
import com.lbedwars.command.BedWarsCommand;

import com.lbedwars.command.SetupCommand;
import com.lbedwars.config.ConfigManager;
import com.lbedwars.core.GameManager;
import com.lbedwars.cosmetics.CosmeticManager;
import com.lbedwars.database.Database;
import com.lbedwars.database.MySQLDatabase;
import com.lbedwars.database.SQLiteDatabase;
import com.lbedwars.database.YamlDatabase;
import com.lbedwars.generator.GeneratorManager;
import com.lbedwars.language.LanguageManager;
import com.lbedwars.listener.ChatListener;
import com.lbedwars.listener.GameListener;
import com.lbedwars.party.PartyIntegration;
import com.lbedwars.listener.PlayerListener;
import com.lbedwars.listener.SetupListener;
import com.lbedwars.listener.ShopListener;
import com.lbedwars.manager.CompassTracker;
import com.lbedwars.manager.DailyRewardManager;
import com.lbedwars.manager.EconomyManager;
import com.lbedwars.manager.LevelManager;

import com.lbedwars.manager.PlaceholderManager;
import com.lbedwars.manager.SpectateManager;
import com.lbedwars.manager.StatsCache;
import com.lbedwars.manager.TabListManager;
import com.lbedwars.proxy.ProxyManager;
import com.lbedwars.proxy.StatusSocketClient;
import com.lbedwars.scoreboard.ScoreboardManager;
import com.lbedwars.shop.ShopManager;
import com.lbedwars.upgrade.UpgradeManager;
import com.lbedwars.util.FoliaUtil;
import java.util.List;
import org.bstats.bukkit.Metrics;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class LBedWars extends JavaPlugin {
   private static LBedWars instance;
   public static final ConcurrentMap<String, String> PENDING_SPECTATES = new ConcurrentHashMap<>();
   private ConfigManager configManager;
   private LanguageManager languageManager;
   private Database database;
   private ArenaManager arenaManager;
   private GeneratorManager generatorManager;
   private ShopManager shopManager;
   private UpgradeManager upgradeManager;

   private LevelManager levelManager;
   private DailyRewardManager dailyRewardManager;
   private SpectateManager spectateManager;
   private CompassTracker compassTracker;
   private GameManager gameManager;
   private EconomyManager economyManager;
   private StatsCache statsCache;
   private ScoreboardManager scoreboardManager;
   private CosmeticManager cosmeticManager;
   private PlaceholderManager placeholderManager;
   private ProxyManager proxyManager;
   private SetupListener setupListener;
   private TabListManager tabListManager;
   private StatusSocketClient statusSocketClient;
   private Location mainLobby;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.configManager = new ConfigManager(this);
      this.languageManager = new LanguageManager(this);
      this.proxyManager = new ProxyManager(this);
      if (this.proxyManager.isEnabled()) {
         this.proxyManager.enforceMySQL();
      }
      this.initDatabase();
      this.arenaManager = new ArenaManager(this);
      if (this.proxyManager.isEnabled()) {
         this.proxyManager.setupWorld();
         Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
          Bukkit.getMessenger().registerIncomingPluginChannel(this, "lbedwars:spectate", (channel, player, data) -> {
             String msg = new String(data, java.nio.charset.StandardCharsets.UTF_8);
             String[] parts = msg.split("\0", 2);
             if (parts.length == 2) {
                LBedWars.PENDING_SPECTATES.put(parts[0], parts[1]);
             }
          });
         List<String> lobbyAddrs = this.getConfig().getStringList("proxy-mode.lobby-addresses");
         if (lobbyAddrs.isEmpty()) {
            lobbyAddrs.add("localhost:5000");
         }
         this.statusSocketClient = new StatusSocketClient(this, lobbyAddrs);
         this.statusSocketClient.start();
      }
      PartyIntegration.init();
      if (this.getConfig().getBoolean("bstats.enabled", true)) {
         new Metrics(this, 31952);
      }
      this.generatorManager = new GeneratorManager(this);
      this.shopManager = new ShopManager(this);
      this.upgradeManager = new UpgradeManager(this);

      this.levelManager = new LevelManager(this);
      this.dailyRewardManager = new DailyRewardManager(this);
      this.spectateManager = new SpectateManager(this);
      this.compassTracker = new CompassTracker(this);
      this.gameManager = new GameManager(this);
      this.economyManager = new EconomyManager(this);
      this.statsCache = new StatsCache(this);
      this.scoreboardManager = new ScoreboardManager(this);
      this.cosmeticManager = new CosmeticManager(this);
      this.placeholderManager = new PlaceholderManager(this);
      this.placeholderManager.register();
      this.tabListManager = new TabListManager(this);
      this.tabListManager.start();
      this.loadMainLobby();
      this.scoreboardManager.startLobbyScoreboard();
      this.statsCache.startFlushTask();
      this.registerCommands();
      this.registerListeners();
      if (FoliaUtil.isFolia()) {
         this.getLogger().info(this.getLanguageManager().getMessage("console.folia-detected"));
      }

      this.getLogger().info(this.getLanguageManager().getMessage("console.enabled").replace("{version}", this.getDescription().getVersion()));
   }

   public void onDisable() {
      if (this.statusSocketClient != null) {
         this.statusSocketClient.shutdown();
      }

      if (this.statsCache != null) {
         this.statsCache.stopFlushTask();
      }

      if (this.tabListManager != null) {
         this.tabListManager.stop();
      }
      if (this.scoreboardManager != null) {
         this.scoreboardManager.stopLobbyScoreboard();
      }

      if (this.arenaManager != null) {
         this.arenaManager.shutdown();
      }

      if (this.generatorManager != null) {
         this.generatorManager.shutdown();
      }

      if (this.database != null) {
         this.database.close();
      }

      this.getLogger().info(this.getLanguageManager().getMessage("console.disabled"));
   }

   private void initDatabase() {
      switch (this.getConfig().getString("storage.type", "YAML").toUpperCase()) {
         case "MYSQL" -> this.database = new MySQLDatabase(this);
         case "SQLITE" -> this.database = new SQLiteDatabase(this);
         default -> this.database = new YamlDatabase(this);
      }

      this.database.connect();
   }

   private void registerCommands() {
      this.getCommand("bedwars").setExecutor(new BedWarsCommand(this));
      this.getCommand("bedwarsadmin").setExecutor(new BedWarsAdminCommand(this));

      this.getCommand("setup").setExecutor(new SetupCommand(this));
   }

   private void registerListeners() {
      this.getServer().getPluginManager().registerEvents(new GameListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ShopListener(this), this);
      this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
      this.getServer().getPluginManager().registerEvents(this.spectateManager, this);
      this.getServer().getPluginManager().registerEvents(this.cosmeticManager, this);
      if (this.proxyManager.isEnabled()) {
         this.getServer().getPluginManager().registerEvents(this.proxyManager, this);
      }
      this.setupListener = new SetupListener(this);
      this.getServer().getPluginManager().registerEvents(this.setupListener, this);
   }

   private void loadMainLobby() {
      if (this.getConfig().contains("main-lobby.world")) {
         World world = Bukkit.getWorld(this.getConfig().getString("main-lobby.world"));
         if (world != null) {
            this.mainLobby = new Location(world, this.getConfig().getDouble("main-lobby.x"), this.getConfig().getDouble("main-lobby.y"), this.getConfig().getDouble("main-lobby.z"), (float)this.getConfig().getDouble("main-lobby.yaw"), (float)this.getConfig().getDouble("main-lobby.pitch"));
         }
      }

   }

   public void saveMainLobby(Location loc) {
      this.mainLobby = loc;
      this.getConfig().set("main-lobby.world", loc.getWorld().getName());
      this.getConfig().set("main-lobby.x", loc.getX());
      this.getConfig().set("main-lobby.y", loc.getY());
      this.getConfig().set("main-lobby.z", loc.getZ());
      this.getConfig().set("main-lobby.yaw", (double)loc.getYaw());
      this.getConfig().set("main-lobby.pitch", (double)loc.getPitch());
      this.saveConfig();
   }

   public Location getMainLobby() {
      return this.mainLobby;
   }

   public static LBedWars getInstance() {
      return instance;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Database getDatabase() {
      return this.database;
   }

   public ArenaManager getArenaManager() {
      return this.arenaManager;
   }

   public GeneratorManager getGeneratorManager() {
      return this.generatorManager;
   }

   public ShopManager getShopManager() {
      return this.shopManager;
   }

   public UpgradeManager getUpgradeManager() {
      return this.upgradeManager;
   }

   public LevelManager getLevelManager() {
      return this.levelManager;
   }

   public DailyRewardManager getDailyRewardManager() {
      return this.dailyRewardManager;
   }

   public SpectateManager getSpectateManager() {
      return this.spectateManager;
   }

   public CompassTracker getCompassTracker() {
      return this.compassTracker;
   }

   public GameManager getGameManager() {
      return this.gameManager;
   }

   public EconomyManager getEconomyManager() {
      return this.economyManager;
   }

   public StatsCache getStatsCache() {
      return this.statsCache;
   }

   public ScoreboardManager getScoreboardManager() {
      return this.scoreboardManager;
   }

   public CosmeticManager getCosmeticManager() {
      return this.cosmeticManager;
   }

   public ProxyManager getProxyManager() {
      return this.proxyManager;
   }

   public SetupListener getSetupListener() {
      return this.setupListener;
   }

   public TabListManager getTabListManager() {
      return this.tabListManager;
   }
}
