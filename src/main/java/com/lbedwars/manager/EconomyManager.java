package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
   private final LBedWars plugin;
   private Economy economy;
   private boolean enabled;

   public EconomyManager(LBedWars plugin) {
      this.plugin = plugin;
      this.enabled = this.setupEconomy();
      if (this.enabled) {
         plugin.getLogger().info(plugin.getLanguageManager().getMessage("console.vault-connected").replace("{name}", this.economy.getName()));
      } else {
         plugin.getLogger().info(plugin.getLanguageManager().getMessage("console.vault-not-found"));
      }

   }

   private boolean setupEconomy() {
      if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            return false;
         } else {
            this.economy = (Economy)rsp.getProvider();
            return this.economy != null;
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public double getBalance(OfflinePlayer player) {
      return this.enabled && this.economy != null ? this.economy.getBalance(player) : (double)0.0F;
   }

   public boolean deposit(OfflinePlayer player, double amount) {
      return this.enabled && this.economy != null ? this.economy.depositPlayer(player, amount).transactionSuccess() : false;
   }

   public boolean withdraw(OfflinePlayer player, double amount) {
      return this.enabled && this.economy != null ? this.economy.withdrawPlayer(player, amount).transactionSuccess() : false;
   }

   public String format(double amount) {
      return this.enabled && this.economy != null ? this.economy.format(amount) : String.valueOf(amount);
   }
}
