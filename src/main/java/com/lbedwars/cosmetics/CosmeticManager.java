package com.lbedwars.cosmetics;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.util.FoliaUtil;
import com.lbedwars.util.MessageUtil;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

public class CosmeticManager implements Listener {
   private final LBedWars plugin;
   private final Map<UUID, String> killEffects;
   private final Map<UUID, String> victoryDances;
   private final Map<UUID, String> trails;
   private final Map<UUID, String> victorySongs;
   private final Map<UUID, Set<String>> ownedCosmetics;
   private final Set<UUID> loadedPlayers;
   private final Map<String, Integer> prices;
   private final Map<String, String> displayNames;
   private final Map<UUID, RadioSongPlayer> activeSongs;
    public static final String NONE = "none";
   public static final List<String> KILL_EFFECTS = List.of("none", "explosion", "lightning", "flame", "heart", "note", "slime", "snow", "blood");
   public static final List<String> VICTORY_DANCES = List.of("none", "fireworks", "lightning", "explosion", "rainbow");
   public static final List<String> TRAILS = List.of("none", "flame", "enchantment", "cloud", "snow", "heart", "lava", "crit", "speed");
   public static final List<String> VICTORY_SONGS = new ArrayList<>();

   public CosmeticManager(LBedWars plugin) {
      this.plugin = plugin;
      this.killEffects = new ConcurrentHashMap();
      this.victoryDances = new ConcurrentHashMap();
      this.trails = new ConcurrentHashMap();
      this.victorySongs = new ConcurrentHashMap();
      this.activeSongs = new ConcurrentHashMap();
      this.ownedCosmetics = new ConcurrentHashMap();
      this.loadedPlayers = new HashSet();
      this.prices = new HashMap();
      this.displayNames = new HashMap();
      this.initDisplayNames();
      this.migrateOldData();
      this.loadPrices();
      this.initVictorySongs();
      this.startTrailTask();
      this.createSongsFolder();
   }

   private void initDisplayNames() {
      this.displayNames.put("none", "&c" + MessageUtil.get("cosmetics.disabled"));
      this.displayNames.put("explosion", "&6Explosion");
      this.displayNames.put("lightning", "&eLightning");
      this.displayNames.put("flame", "&cFlame");
      this.displayNames.put("heart", "&dHeart");
      this.displayNames.put("note", "&aNote");
      this.displayNames.put("slime", "&aSlime");
      this.displayNames.put("snow", "&bSnow");
      this.displayNames.put("blood", "&cBlood");
      this.displayNames.put("fireworks", "&6Fireworks");
      this.displayNames.put("rainbow", "&dRainbow");
      this.displayNames.put("enchantment", "&dEnchantment");
      this.displayNames.put("cloud", "&fCloud");
      this.displayNames.put("lava", "&6Lava");
      this.displayNames.put("crit", "&fCritical");
      this.displayNames.put("speed", "&bSpeed");
   }

   private void createSongsFolder() {
      File songsFolder = new File(this.plugin.getDataFolder(), "songs");
      if (!songsFolder.exists()) {
         songsFolder.mkdirs();
      }
   }

   private void initVictorySongs() {
      VICTORY_SONGS.clear();
      VICTORY_SONGS.add("none");
      YamlConfiguration config = this.getCosmeticsConfig();
      if (config.contains("victory-songs.songs")) {
         for(String key : config.getConfigurationSection("victory-songs.songs").getKeys(false)) {
            VICTORY_SONGS.add(key);
            String name = config.getString("victory-songs.songs." + key + ".name", "&f" + key);
            this.displayNames.put(key, name);
         }
      }
   }

   private void loadPrices() {
      File configFile = new File(this.plugin.getDataFolder(), "cosmetics.yml");
      if (!configFile.exists()) {
         this.plugin.saveResource("cosmetics.yml", false);
      }

      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      if (configFile.exists() && !config.contains("prices")) {
         this.plugin.saveResource("cosmetics.yml", true);
         config = YamlConfiguration.loadConfiguration(configFile);
      }

      if (config.contains("prices.kill-effects")) {
         for(String key : config.getConfigurationSection("prices.kill-effects").getKeys(false)) {
            this.prices.put("kill:" + key, config.getInt("prices.kill-effects." + key));
         }
      }

      if (config.contains("prices.victory-dances")) {
         for(String key : config.getConfigurationSection("prices.victory-dances").getKeys(false)) {
            this.prices.put("victory:" + key, config.getInt("prices.victory-dances." + key));
         }
      }

      if (config.contains("prices.trails")) {
         for(String key : config.getConfigurationSection("prices.trails").getKeys(false)) {
            this.prices.put("trail:" + key, config.getInt("prices.trails." + key));
         }
      }

      if (config.contains("prices.victory-songs")) {
         for(String key : config.getConfigurationSection("prices.victory-songs").getKeys(false)) {
            this.prices.put("victory-song:" + key, config.getInt("prices.victory-songs." + key));
         }
      }

   }

   public int getPrice(String cosmeticKey) {
      return (Integer)this.prices.getOrDefault(cosmeticKey, 0);
   }

   private YamlConfiguration getCosmeticsConfig() {
      File configFile = new File(this.plugin.getDataFolder(), "cosmetics.yml");
      return YamlConfiguration.loadConfiguration(configFile);
   }

   private String cosmeticKey(String category, String effect) {
      return category + ":" + effect;
   }

   private void migrateOldData() {
      File oldFile = new File(this.plugin.getDataFolder(), "cosmetics_data.yml");
      if (!oldFile.exists()) {
         oldFile = new File(this.plugin.getDataFolder(), "cosmetics.yml");
         if (oldFile.exists()) {
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
            if (oldConfig.contains("prices")) {
               return;
            }

            boolean hasData = false;

            for(String key : oldConfig.getKeys(false)) {
               try {
                  UUID.fromString(key);
                  hasData = true;
                  break;
               } catch (IllegalArgumentException var11) {
               }
            }

            if (!hasData) {
               return;
            }

            for(String key : oldConfig.getKeys(false)) {
               try {
                  UUID uuid = UUID.fromString(key);
                  String kill = oldConfig.getString(key + ".kill-effect", "none");
                  String victory = oldConfig.getString(key + ".victory-dance", "none");
                  String trail = oldConfig.getString(key + ".trail", "none");
                  if (!kill.equals("none")) {
                     this.plugin.getDatabase().addOwnedCosmetic(uuid, "kill:" + kill);
                     this.plugin.getDatabase().setSelectedCosmetic(uuid, "kill-effect", kill);
                  }

                  if (!victory.equals("none")) {
                     this.plugin.getDatabase().addOwnedCosmetic(uuid, "victory:" + victory);
                     this.plugin.getDatabase().setSelectedCosmetic(uuid, "victory-dance", victory);
                  }

                  if (!trail.equals("none")) {
                     this.plugin.getDatabase().addOwnedCosmetic(uuid, "trail:" + trail);
                     this.plugin.getDatabase().setSelectedCosmetic(uuid, "trail", trail);
                  }
               } catch (IllegalArgumentException var10) {
               }
            }
         }

      } else {
         YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);

         for(String key : oldConfig.getKeys(false)) {
            try {
               UUID uuid = UUID.fromString(key);

               for(String cos : oldConfig.getStringList(key + ".owned")) {
                  this.plugin.getDatabase().addOwnedCosmetic(uuid, cos);
               }

               String kill = oldConfig.getString(key + ".selected.kill-effect", "none");
               String victory = oldConfig.getString(key + ".selected.victory-dance", "none");
               String trail = oldConfig.getString(key + ".selected.trail", "none");
               if (!kill.equals("none")) {
                  this.plugin.getDatabase().setSelectedCosmetic(uuid, "kill-effect", kill);
               }

               if (!victory.equals("none")) {
                  this.plugin.getDatabase().setSelectedCosmetic(uuid, "victory-dance", victory);
               }

               if (!trail.equals("none")) {
                  this.plugin.getDatabase().setSelectedCosmetic(uuid, "trail", trail);
               }
            } catch (IllegalArgumentException var12) {
            }
         }

         oldFile.delete();
      }
   }

   private void ensureLoaded(UUID uuid) {
      if (!this.loadedPlayers.contains(uuid)) {
         this.loadedPlayers.add(uuid);
         Set<String> owned = this.plugin.getDatabase().getOwnedCosmetics(uuid);
         if (!owned.isEmpty()) {
            this.ownedCosmetics.put(uuid, new HashSet(owned));
         }

         String kill = this.plugin.getDatabase().getSelectedCosmetic(uuid, "kill-effect");
         String victory = this.plugin.getDatabase().getSelectedCosmetic(uuid, "victory-dance");
         String trail = this.plugin.getDatabase().getSelectedCosmetic(uuid, "trail");
         String song = this.plugin.getDatabase().getSelectedCosmetic(uuid, "victory-song");
         if (!kill.equals("none")) {
            this.killEffects.put(uuid, kill);
         }

         if (!victory.equals("none")) {
            this.victoryDances.put(uuid, victory);
         }

         if (!trail.equals("none")) {
            this.trails.put(uuid, trail);
         }

         if (!song.equals("none")) {
            this.victorySongs.put(uuid, song);
         }

      }
   }

   public boolean isOwned(UUID uuid, String category, String effect) {
      if (effect.equals("none")) {
         return true;
      } else {
         this.ensureLoaded(uuid);
         return ((Set)this.ownedCosmetics.computeIfAbsent(uuid, (k) -> new HashSet())).contains(this.cosmeticKey(category, effect));
      }
   }

   public Set<String> getOwned(UUID uuid) {
      this.ensureLoaded(uuid);
      return (Set)this.ownedCosmetics.computeIfAbsent(uuid, (k) -> new HashSet());
   }

   public boolean purchase(Player player, String category, String effect) {
      String key = this.cosmeticKey(category, effect);
      int price = this.getPrice(key);
      if (price <= 0) {
         return true;
      } else if (!this.plugin.getEconomyManager().isEnabled()) {
         MessageUtil.sendMessage(player, "cosmetics.economy-disabled");
         return false;
      } else {
         double balance = this.plugin.getEconomyManager().getBalance(player);
         if (balance < (double)price) {
            MessageUtil.sendMessage(player, "cosmetics.insufficient-funds", "price", this.plugin.getEconomyManager().format((double)price), "balance", this.plugin.getEconomyManager().format(balance));
            return false;
         } else if (!this.plugin.getEconomyManager().withdraw(player, (double)price)) {
            MessageUtil.sendMessage(player, "cosmetics.purchase-failed");
            return false;
         } else {
            this.getOwned(player.getUniqueId()).add(key);
            this.plugin.getDatabase().addOwnedCosmetic(player.getUniqueId(), key);
            MessageUtil.sendMessage(player, "cosmetics.purchased", "cosmetic", ChatColor.translateAlternateColorCodes('&', (String)this.displayNames.getOrDefault(effect, effect)), "price", this.plugin.getEconomyManager().format((double)price));
            return true;
         }
      }
   }

   public String getKillEffect(UUID uuid) {
      this.ensureLoaded(uuid);
      return (String)this.killEffects.getOrDefault(uuid, "none");
   }

   public String getVictoryDance(UUID uuid) {
      this.ensureLoaded(uuid);
      return (String)this.victoryDances.getOrDefault(uuid, "none");
   }

   public String getTrail(UUID uuid) {
      this.ensureLoaded(uuid);
      return (String)this.trails.getOrDefault(uuid, "none");
   }

   public String getVictorySong(UUID uuid) {
      this.ensureLoaded(uuid);
      return (String)this.victorySongs.getOrDefault(uuid, "none");
   }

   public void setVictorySong(UUID uuid, String song) {
      if (song.equals("none")) {
         this.victorySongs.remove(uuid);
      } else {
         this.victorySongs.put(uuid, song);
      }

      this.plugin.getDatabase().setSelectedCosmetic(uuid, "victory-song", song);
   }

   public void playVictorySong(Player player) {
      String songName = this.getVictorySong(player.getUniqueId());
      if (songName.equals("none")) return;
      YamlConfiguration config = this.getCosmeticsConfig();
      if (!config.getBoolean("victory-songs.enabled", true)) return;
      String filePath = config.getString("victory-songs.songs." + songName + ".file", "");
      boolean songEnabled = config.getBoolean("victory-songs.songs." + songName + ".enabled", true);
      if (!songEnabled || filePath.isEmpty()) {
         return;
      }

      File songFile = new File(this.plugin.getDataFolder(), "songs/" + filePath);
      if (!songFile.exists()) {
         this.plugin.getLogger().warning("Victory song file not found: " + songFile.getPath());
         return;
      }

      Plugin noteBlockAPI = Bukkit.getPluginManager().getPlugin("NoteBlockAPI");
      if (noteBlockAPI == null || !noteBlockAPI.isEnabled()) {
         return;
      }

      try {
         FileInputStream fis = new FileInputStream(songFile);
         Song song = NBSDecoder.parse(fis);
         fis.close();
         if (song == null) {
            this.plugin.getLogger().warning("Failed to parse victory song: " + filePath);
            return;
         }

         double volume = config.getDouble("victory-songs.songs." + songName + ".volume", 1.0);
         RadioSongPlayer rsp = new RadioSongPlayer(song);
         boolean broadcast = config.getBoolean("victory-songs.broadcast", false);
         if (broadcast) {
            Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
            if (arena != null) {
               for (UUID uuid : arena.getPlayers()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null) {
                     rsp.addPlayer(p);
                  }
               }
            }
         } else {
            rsp.addPlayer(player);
         }

         rsp.setVolume((byte)(int)(volume * 100.0));
         rsp.setPlaying(true);
         rsp.setAutoDestroy(true);
         this.activeSongs.put(player.getUniqueId(), rsp);
      } catch (Exception e) {
         this.plugin.getLogger().warning("Error playing victory song: " + e.getMessage());
      }
   }

   public void stopArenaSongs(Arena arena) {
      for (UUID uuid : arena.getPlayers()) {
         RadioSongPlayer rsp = this.activeSongs.remove(uuid);
         if (rsp != null) {
            rsp.setPlaying(false);
            rsp.destroy();
         }
      }
   }

   public void setKillEffect(UUID uuid, String effect) {
      if (effect.equals("none")) {
         this.killEffects.remove(uuid);
      } else {
         this.killEffects.put(uuid, effect);
      }

      this.plugin.getDatabase().setSelectedCosmetic(uuid, "kill-effect", effect);
   }

   public void setVictoryDance(UUID uuid, String dance) {
      if (dance.equals("none")) {
         this.victoryDances.remove(uuid);
      } else {
         this.victoryDances.put(uuid, dance);
      }

      this.plugin.getDatabase().setSelectedCosmetic(uuid, "victory-dance", dance);
   }

   public void setTrail(UUID uuid, String trail) {
      if (trail.equals("none")) {
         this.trails.remove(uuid);
      } else {
         this.trails.put(uuid, trail);
      }

      this.plugin.getDatabase().setSelectedCosmetic(uuid, "trail", trail);
   }

   public void spawnKillEffect(Player killer, Location location) {
      String effect = this.getKillEffect(killer.getUniqueId());
      if (!effect.equals("none")) {
         switch (effect) {
            case "explosion":
               location.getWorld().createExplosion(location, 0.0F, false, false);
               location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 1);
               break;
            case "lightning":
               location.getWorld().strikeLightningEffect(location);
               break;
            case "flame":
               location.getWorld().spawnParticle(Particle.FLAME, location, 30, (double)0.5F, (double)0.5F, (double)0.5F, 0.05);
               break;
            case "heart":
               location.getWorld().spawnParticle(Particle.HEART, location.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 10, (double)0.5F, (double)0.5F, (double)0.5F);
               break;
            case "note":
               location.getWorld().spawnParticle(Particle.NOTE, location.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 10, (double)0.5F, (double)0.5F, (double)0.5F);
               location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
               break;
            case "slime":
               location.getWorld().spawnParticle(Particle.SLIME, location.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 20, (double)0.5F, (double)0.5F, (double)0.5F);
               break;
            case "snow":
               location.getWorld().spawnParticle(Particle.SNOWBALL, location, 30, (double)0.5F, (double)0.5F, (double)0.5F, 0.1);
               break;
            case "blood":
               location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 30, (double)0.5F, (double)0.5F, (double)0.5F, Material.REDSTONE_BLOCK.createBlockData());
         }

      }
   }

   public void spawnVictoryEffects(Player winner) {
      String dance = this.getVictoryDance(winner.getUniqueId());
      if (!dance.equals("none")) {
         Location loc = winner.getLocation();
         switch (dance) {
            case "fireworks":
               for(int i = 0; i < 3; ++i) {
                  Firework fw = (Firework)loc.getWorld().spawn(loc.clone().add(Math.random() * (double)4.0F - (double)2.0F, (double)1.0F, Math.random() * (double)4.0F - (double)2.0F), Firework.class);
                  FireworkMeta meta = fw.getFireworkMeta();
                  meta.addEffect(FireworkEffect.builder().with(Type.BALL_LARGE).withColor(Color.fromRGB((new Random()).nextInt(16777215))).withFade(Color.fromRGB((new Random()).nextInt(16777215))).build());
                  meta.setPower(0);
                  fw.setFireworkMeta(meta);
               }
               break;
            case "lightning":
               for(int i = 0; i < 5; ++i) {
                  loc.getWorld().strikeLightningEffect(loc.clone().add(Math.random() * (double)6.0F - (double)3.0F, (double)0.0F, Math.random() * (double)6.0F - (double)3.0F));
               }
               break;
            case "explosion":
               for(int i = 0; i < 5; ++i) {
                  Location eLoc = loc.clone().add(Math.random() * (double)6.0F - (double)3.0F, Math.random() * (double)2.0F, Math.random() * (double)6.0F - (double)3.0F);
                  eLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, eLoc, 1);
                  eLoc.getWorld().playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
               }
               break;
            case "rainbow":
               for(int i = 0; i < 20; ++i) {
                  Location pLoc = loc.clone().add(Math.random() * (double)4.0F - (double)2.0F, Math.random() * (double)2.0F, Math.random() * (double)4.0F - (double)2.0F);
                  pLoc.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, (double)0.0F, (double)0.0F, (double)0.0F, new Particle.DustOptions(Color.fromRGB((new Random()).nextInt(16777215)), 1.0F));
               }
         }

      }
   }

   private void spawnTrail(Player player) {
      String trail = this.getTrail(player.getUniqueId());
      if (!trail.equals("none")) {
         if (player.isOnline() && !player.isDead()) {
            String mode = this.plugin.getConfig().getString("cosmetics.trail-mode", "ARENA");
            if (!mode.equalsIgnoreCase("ARENA") || this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId()) != null || this.plugin.getArenaManager().getArenaBySpectator(player.getUniqueId()) != null) {
               Location loc = player.getLocation().clone().add((double)0.0F, 0.2, (double)0.0F);
               switch (trail) {
                  case "flame" -> loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.01);
                  case "enchantment" -> loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 5, 0.2, 0.2, 0.2, (double)0.5F);
                  case "cloud" -> loc.getWorld().spawnParticle(Particle.CLOUD, loc, 3, 0.2, 0.1, 0.2, 0.01);
                  case "snow" -> loc.getWorld().spawnParticle(Particle.SNOWBALL, loc, 3, 0.2, 0.1, 0.2, 0.01);
                  case "heart" -> loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add((double)0.0F, (double)0.5F, (double)0.0F), 2, 0.2, 0.1, 0.2);
                  case "lava" -> loc.getWorld().spawnParticle(Particle.DRIP_LAVA, loc, 2, 0.1, 0.2, 0.1);
                  case "crit" -> loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.2, 0.1, 0.2, 0.1);
                  case "speed" -> loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.1, 0.2, (double)0.5F);
               }

            }
         }
      }
   }

   private void startTrailTask() {
      FoliaUtil.runTaskTimer(() -> {
         for(Player player : Bukkit.getOnlinePlayers()) {
            this.spawnTrail(player);
         }

      }, 0L, 5L);
   }

   public void openCosmeticsGUI(Player player) {
      String title = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-title"));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, title);
      ItemStack killEffectItem = new ItemStack(Material.DIAMOND_SWORD);
      ItemMeta killMeta = killEffectItem.getItemMeta();
      killMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-kill-effect")));
      killEffectItem.setItemMeta(killMeta);
      inv.setItem(11, killEffectItem);
      ItemStack victoryDanceItem = new ItemStack(Material.FIREWORK_ROCKET);
      ItemMeta victoryMeta = victoryDanceItem.getItemMeta();
      victoryMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-victory-dance")));
      victoryDanceItem.setItemMeta(victoryMeta);
      inv.setItem(13, victoryDanceItem);
      ItemStack trailItem = new ItemStack(Material.FEATHER);
      ItemMeta trailMeta = trailItem.getItemMeta();
      trailMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-trail")));
      trailItem.setItemMeta(trailMeta);
      inv.setItem(15, trailItem);
      ItemStack victorySongItem = new ItemStack(Material.MUSIC_DISC_CAT);
      ItemMeta vsMeta = victorySongItem.getItemMeta();
      vsMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-victory-song")));
      victorySongItem.setItemMeta(vsMeta);
      inv.setItem(17, victorySongItem);
      player.openInventory(inv);
   }

   private String getSelected(UUID uuid, String category) {
      String var10000;
      switch (category) {
         case "kill" -> var10000 = this.getKillEffect(uuid);
         case "victory" -> var10000 = this.getVictoryDance(uuid);
         case "trail" -> var10000 = this.getTrail(uuid);
         case "victory-song" -> var10000 = this.getVictorySong(uuid);
         default -> var10000 = "none";
      }

      return var10000;
   }

   private void openCategoryGUI(Player player, String category) {
      List<String> effects;
      String titleBase;
      String typeName;
      switch (category) {
         case "kill":
            effects = KILL_EFFECTS;
            titleBase = MessageUtil.get("cosmetics.gui-category-kill");
            typeName = MessageUtil.get("cosmetics.gui-kill-effect");
            break;
         case "victory":
            effects = VICTORY_DANCES;
            titleBase = MessageUtil.get("cosmetics.gui-category-victory");
            typeName = MessageUtil.get("cosmetics.gui-victory-dance");
            break;
         case "trail":
            effects = TRAILS;
            titleBase = MessageUtil.get("cosmetics.gui-category-trail");
            typeName = MessageUtil.get("cosmetics.gui-trail");
            break;
         case "victory-song":
            effects = VICTORY_SONGS;
            titleBase = MessageUtil.get("cosmetics.gui-category-victory-song");
            typeName = MessageUtil.get("cosmetics.gui-victory-song");
            break;
         default:
            return;
      }

      String title = ChatColor.translateAlternateColorCodes('&', titleBase);
      int rows = Math.max(1, (int)Math.ceil((double)effects.size() / (double)9.0F));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, (rows + 1) * 9, title);
      UUID uuid = player.getUniqueId();
      String currentSelected = this.getSelected(uuid, category);
      int slot = 0;

      for(String effect : effects) {
         String displayName = (String)this.displayNames.getOrDefault(effect, "&f" + effect);
         ItemStack item = new ItemStack(effect.equals("none") ? Material.BARRIER : Material.PAPER);
         ItemMeta meta = item.getItemMeta();
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
         boolean isSelected = currentSelected.equals(effect);
         boolean isOwned = this.isOwned(uuid, category, effect);
         int price = this.getPrice(this.cosmeticKey(category, effect));
         List<String> lore = new ArrayList();
         if (effect.equals("none")) {
            if (isSelected) {
               lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-selected")));
            } else {
               lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-click-disable")));
            }
         } else if (isSelected) {
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-selected")));
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-click-disable")));
         } else if (isOwned) {
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-owned")));
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-click-select")));
         } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-not-owned")));
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-price", "price", String.valueOf(price))));
            lore.add(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-click-buy")));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
         inv.setItem(slot++, item);
      }

      ItemStack backItem = new ItemStack(Material.ARROW);
      ItemMeta backMeta = backItem.getItemMeta();
      backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-back")));
      backItem.setItemMeta(backMeta);
      inv.setItem(inv.getSize() - 1, backItem);
      player.openInventory(inv);
   }

   @EventHandler
   public void onCosmeticsGUIClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
            String title = event.getView().getTitle();
            ItemStack current = event.getCurrentItem();
            String cosmeticsTitle = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-title"));
            if (title.equals(cosmeticsTitle)) {
               event.setCancelled(true);
               int slot = event.getSlot();
               if (slot == 11) {
                  this.openCategoryGUI(player, "kill");
               } else if (slot == 13) {
                  this.openCategoryGUI(player, "victory");
               } else if (slot == 15) {
                  this.openCategoryGUI(player, "trail");
               } else if (slot == 17) {
                  this.openCategoryGUI(player, "victory-song");
               }

            } else {
               String killCat = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-category-kill"));
               String victoryCat = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-category-victory"));
               String trailCat = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-category-trail"));
               String victorySongCat = ChatColor.translateAlternateColorCodes('&', MessageUtil.get("cosmetics.gui-category-victory-song"));
               String category = null;
               if (title.equals(killCat)) {
                  category = "kill";
               } else if (title.equals(victoryCat)) {
                  category = "victory";
               } else if (title.equals(trailCat)) {
                  category = "trail";
               } else if (title.equals(victorySongCat)) {
                  category = "victory-song";
               }

               if (category != null) {
                  event.setCancelled(true);
                  if (current.getType() == Material.ARROW) {
                     this.openCosmeticsGUI(player);
                  } else {
                     String displayName = ChatColor.stripColor(current.getItemMeta().getDisplayName());

                     for(Map.Entry<String, String> entry : this.displayNames.entrySet()) {
                        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', (String)entry.getValue()));
                        if (stripped.equals(displayName)) {
                           String effect = (String)entry.getKey();
                           String var10000;
                           switch (category) {
                              case "kill" -> var10000 = MessageUtil.get("cosmetics.gui-kill-effect");
                              case "victory" -> var10000 = MessageUtil.get("cosmetics.gui-victory-dance");
                              case "trail" -> var10000 = MessageUtil.get("cosmetics.gui-trail");
                              case "victory-song" -> var10000 = MessageUtil.get("cosmetics.gui-victory-song");
                              default -> var10000 = "";
                           }

                           String typeName = var10000;
                           String cosmeticName = ChatColor.translateAlternateColorCodes('&', (String)this.displayNames.getOrDefault(effect, effect));
                           UUID uuid = player.getUniqueId();
                           String currentSelected = this.getSelected(uuid, category);
                           if (effect.equals("none")) {
                              this.setSelection(category, uuid, "none");
                              MessageUtil.sendMessage(player, "cosmetics.disabled-msg", "type", typeName);
                              this.openCategoryGUI(player, category);
                              return;
                           }

                           if (currentSelected.equals(effect)) {
                              this.setSelection(category, uuid, "none");
                              MessageUtil.sendMessage(player, "cosmetics.disabled-msg", "type", typeName);
                              this.openCategoryGUI(player, category);
                              return;
                           }

                           if (this.isOwned(uuid, category, effect)) {
                              this.setSelection(category, uuid, effect);
                              MessageUtil.sendMessage(player, "cosmetics.selected", "cosmetic", cosmeticName, "type", typeName);
                              this.openCategoryGUI(player, category);
                              return;
                           }

                           if (this.purchase(player, category, effect)) {
                              this.setSelection(category, uuid, effect);
                              MessageUtil.sendMessage(player, "cosmetics.selected", "cosmetic", cosmeticName, "type", typeName);
                           }

                           this.openCategoryGUI(player, category);
                           return;
                        }
                     }

                  }
               }
            }
         }
      }
   }

   private void setSelection(String category, UUID uuid, String effect) {
      switch (category) {
         case "kill" -> this.setKillEffect(uuid, effect);
         case "victory" -> this.setVictoryDance(uuid, effect);
         case "trail" -> this.setTrail(uuid, effect);
         case "victory-song" -> this.setVictorySong(uuid, effect);
      }

   }
}
