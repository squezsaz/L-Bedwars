package com.lbedwars.language;

import com.lbedwars.LBedWars;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageManager {
   private final LBedWars plugin;
   private final Map<String, String> messages = new HashMap();
   private String prefix;
   private String lang;

   public LanguageManager(LBedWars plugin) {
      this.plugin = plugin;
      this.loadLanguage();
   }

   private void loadLanguage() {
      this.messages.clear();
      this.lang = this.plugin.getConfig().getString("language", "tr");
      File langFile = new File(String.valueOf(this.plugin.getDataFolder()) + "/languages", "messages_" + this.lang + ".yml");
      if (!langFile.exists()) {
         this.plugin.saveResource("languages/messages_" + this.lang + ".yml", true);
      }

      YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
      this.prefix = config.getString("prefix", "&8[&6L-BedWars&8] &7");
      this.flattenConfig(config, "");
   }

   private void flattenConfig(YamlConfiguration config, String basePath) {
      for(String key : config.getKeys(false)) {
         String fullKey = basePath.isEmpty() ? key : basePath + "." + key;
         if (config.isConfigurationSection(key)) {
            for(String subKey : config.getConfigurationSection(key).getKeys(true)) {
               String subFullKey = fullKey + "." + subKey;
               if (config.getConfigurationSection(key).isString(subKey)) {
                  this.messages.put(subFullKey, config.getConfigurationSection(key).getString(subKey));
               }
            }
         } else if (config.isString(key)) {
            this.messages.put(fullKey, config.getString(key));
         }
      }

   }

   public String getMessage(String path) {
      return (String)this.messages.getOrDefault(path, null);
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void reload() {
      this.loadLanguage();
   }
}
