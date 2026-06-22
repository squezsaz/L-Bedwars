package com.lbedwars.manager;

import com.lbedwars.LBedWars;
import com.lbedwars.util.MessageUtil;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class DailyRewardManager {
   private final LBedWars plugin;

   public DailyRewardManager(LBedWars plugin) {
      this.plugin = plugin;
   }

   public boolean isEnabled() {
      return this.plugin.getConfigManager().getRewardsConfig().getBoolean("daily-reward.enabled", true);
   }

   public boolean canClaim(UUID uuid) {
      if (!this.isEnabled()) {
         return false;
      } else {
         long lastClaim = this.plugin.getDatabase().getLong(uuid, "last-daily");
         long now = System.currentTimeMillis();
         long cooldownHours = this.plugin.getConfigManager().getRewardsConfig().getLong("daily-reward.cooldown-hours", 24L);
         return now - lastClaim >= TimeUnit.HOURS.toMillis(cooldownHours);
      }
   }

   public long getRemainingCooldown(UUID uuid) {
      long lastClaim = this.plugin.getDatabase().getLong(uuid, "last-daily");
      long cooldownHours = this.plugin.getConfigManager().getRewardsConfig().getLong("daily-reward.cooldown-hours", 24L);
      long cooldownMs = TimeUnit.HOURS.toMillis(cooldownHours);
      long remaining = cooldownMs - (System.currentTimeMillis() - lastClaim);
      return Math.max(0L, remaining);
   }

   public void claim(Player player) {
      if (!this.isEnabled()) {
         MessageUtil.sendMessage(player, "daily-reward.disabled");
      } else if (!this.canClaim(player.getUniqueId())) {
         long remaining = this.getRemainingCooldown(player.getUniqueId());
         long hours = TimeUnit.MILLISECONDS.toHours(remaining);
         long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60L;
         MessageUtil.sendMessage(player, "daily-reward.not-ready", "time", hours + "s " + minutes + "d");
      } else {
         int xp = this.plugin.getConfigManager().getRewardsConfig().getInt("daily-reward.reward-xp", 100);
         int coins = this.plugin.getConfigManager().getRewardsConfig().getInt("daily-reward.reward-coins", 50);
         double money = this.plugin.getConfigManager().getRewardsConfig().getDouble("daily-reward.reward-money", (double)0.0F);
         this.plugin.getDatabase().setLong(player.getUniqueId(), "last-daily", System.currentTimeMillis());
         if (xp > 0) {
            this.plugin.getLevelManager().addXp(player.getUniqueId(), xp);
         }

         if (money > (double)0.0F && this.plugin.getEconomyManager().isEnabled()) {
            this.plugin.getEconomyManager().deposit(player, money);
         }

         MessageUtil.sendMessage(player, "daily-reward.claimed", "xp", String.valueOf(xp), "coins", String.valueOf(coins), "money", this.plugin.getEconomyManager().isEnabled() ? this.plugin.getEconomyManager().format(money) : "0");
      }
   }

   public void resetCooldown(OfflinePlayer player) {
      this.plugin.getDatabase().setLong(player.getUniqueId(), "last-daily", 0L);
   }

   public void setXp(int xp) {
      this.plugin.getConfigManager().getRewardsConfig().set("daily-reward.reward-xp", xp);
      this.plugin.getConfigManager().saveRewardsConfig();
   }

   public void setCoins(int coins) {
      this.plugin.getConfigManager().getRewardsConfig().set("daily-reward.reward-coins", coins);
      this.plugin.getConfigManager().saveRewardsConfig();
   }

   public void setMoney(double money) {
      this.plugin.getConfigManager().getRewardsConfig().set("daily-reward.reward-money", money);
      this.plugin.getConfigManager().saveRewardsConfig();
   }

   public void setCooldownHours(long hours) {
      this.plugin.getConfigManager().getRewardsConfig().set("daily-reward.cooldown-hours", hours);
      this.plugin.getConfigManager().saveRewardsConfig();
   }

   public void setEnabled(boolean enabled) {
      this.plugin.getConfigManager().getRewardsConfig().set("daily-reward.enabled", enabled);
      this.plugin.getConfigManager().saveRewardsConfig();
   }

   public int getXp() {
      return this.plugin.getConfigManager().getRewardsConfig().getInt("daily-reward.reward-xp", 100);
   }

   public int getCoins() {
      return this.plugin.getConfigManager().getRewardsConfig().getInt("daily-reward.reward-coins", 50);
   }

   public double getMoney() {
      return this.plugin.getConfigManager().getRewardsConfig().getDouble("daily-reward.reward-money", (double)0.0F);
   }

   public long getCooldownHours() {
      return this.plugin.getConfigManager().getRewardsConfig().getLong("daily-reward.cooldown-hours", 24L);
   }
}
