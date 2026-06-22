package com.lbedwars.config;

import com.lbedwars.LBedWars;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
   public static final List<String> MODES = List.of("solo", "duo", "trio", "quad");
   private final LBedWars plugin;
   private FileConfiguration rewardsConfig;
   private File rewardsFile;
   private final Map<String, FileConfiguration> shopConfigs;
   private final Map<String, FileConfiguration> upgradesConfigs;

   public ConfigManager(LBedWars plugin) {
      this.plugin = plugin;
      this.shopConfigs = new HashMap();
      this.upgradesConfigs = new HashMap();
      this.loadConfigs();
   }

   private void loadConfigs() {
      this.rewardsFile = new File(this.plugin.getDataFolder(), "rewards.yml");
      if (!this.rewardsFile.exists()) {
         this.plugin.saveResource("rewards.yml", false);
      }
      this.rewardsConfig = YamlConfiguration.loadConfiguration(this.rewardsFile);

      File shopDir = new File(this.plugin.getDataFolder(), "shop");
      File upgradesDir = new File(this.plugin.getDataFolder(), "upgrades");
      if (!shopDir.exists()) {
         shopDir.mkdirs();
      }
      if (!upgradesDir.exists()) {
         upgradesDir.mkdirs();
      }

      for (String mode : MODES) {
         File shopFile = new File(shopDir, mode + ".yml");
         if (!shopFile.exists()) {
            try (InputStream in = this.plugin.getResource("shop.yml")) {
               if (in != null) {
                  Files.copy(in, shopFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }

         File upgradesFile = new File(upgradesDir, mode + ".yml");
         if (!upgradesFile.exists()) {
            try (InputStream in = this.plugin.getResource("upgrades.yml")) {
               if (in != null) {
                  Files.copy(in, upgradesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   public void reload() {
      this.plugin.reloadConfig();
      this.shopConfigs.clear();
      this.upgradesConfigs.clear();
      this.loadConfigs();
   }

   public FileConfiguration getShopConfig(String mode) {
      if (mode == null || mode.isEmpty()) {
         mode = "solo";
      }
      return this.shopConfigs.computeIfAbsent(mode, (m) -> {
         File file = new File(this.plugin.getDataFolder() + "/shop", m + ".yml");
         return YamlConfiguration.loadConfiguration(file);
      });
   }

   public FileConfiguration getUpgradesConfig(String mode) {
      if (mode == null || mode.isEmpty()) {
         mode = "solo";
      }
      return this.upgradesConfigs.computeIfAbsent(mode, (m) -> {
         File file = new File(this.plugin.getDataFolder() + "/upgrades", m + ".yml");
         return YamlConfiguration.loadConfiguration(file);
      });
   }

   public FileConfiguration getRewardsConfig() {
      return this.rewardsConfig;
   }

   public void saveRewardsConfig() {
      try {
         this.rewardsConfig.save(this.rewardsFile);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public String getStorageType() {
      return this.plugin.getConfig().getString("storage.type", "YAML");
   }
}
