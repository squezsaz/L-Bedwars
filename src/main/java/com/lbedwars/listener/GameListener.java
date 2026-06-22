package com.lbedwars.listener;

import com.lbedwars.LBedWars;
import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import com.lbedwars.arena.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameListener implements Listener {
   private final LBedWars plugin;

   public GameListener(LBedWars plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(victim.getUniqueId());
      if (arena != null && arena.getState() == ArenaState.PLAYING) {
         Player killer = victim.getKiller();
         event.setCancelled(true);
         EntityDamageEvent lastDamage = victim.getLastDamageCause();
         boolean voidDeath = lastDamage != null && lastDamage.getCause() == DamageCause.VOID;
         this.plugin.getGameManager().handlePlayerDeath(victim, killer, arena, voidDeath);
      }
   }

   @EventHandler
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      Arena arena = this.plugin.getArenaManager().getArenaByPlayer(player.getUniqueId());
      if (arena != null) {
         Team team = arena.getTeamByPlayer(player.getUniqueId());
         if (team != null && team.getSpawn() != null) {
            event.setRespawnLocation(team.getSpawn());
         }

      }
   }
}
