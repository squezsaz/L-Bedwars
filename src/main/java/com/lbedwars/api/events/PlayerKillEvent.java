package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerKillEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;
   private final Player killer;
   private final Player victim;
   private final boolean finalKill;

   public PlayerKillEvent(Arena arena, Player killer, Player victim, boolean finalKill) {
      this.arena = arena;
      this.killer = killer;
      this.victim = victim;
      this.finalKill = finalKill;
   }

   public Arena getArena() { return this.arena; }
   public Player getKiller() { return this.killer; }
   public Player getVictim() { return this.victim; }
   public boolean isFinalKill() { return this.finalKill; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
