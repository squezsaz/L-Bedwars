package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import com.lbedwars.arena.ArenaState;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ArenaStateChangeEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;
   private final ArenaState oldState;
   private final ArenaState newState;

   public ArenaStateChangeEvent(Arena arena, ArenaState oldState, ArenaState newState) {
      this.arena = arena;
      this.oldState = oldState;
      this.newState = newState;
   }

   public Arena getArena() { return this.arena; }
   public ArenaState getOldState() { return this.oldState; }
   public ArenaState getNewState() { return this.newState; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
