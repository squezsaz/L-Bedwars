package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BedBreakEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;
   private final Team team;
   private final Player breaker;

   public BedBreakEvent(Arena arena, Team team, @Nullable Player breaker) {
      this.arena = arena;
      this.team = team;
      this.breaker = breaker;
   }

   public Arena getArena() { return this.arena; }
   public Team getTeam() { return this.team; }
   @Nullable
   public Player getBreaker() { return this.breaker; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
