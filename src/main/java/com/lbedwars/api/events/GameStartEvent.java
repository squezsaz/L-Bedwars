package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class GameStartEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;

   public GameStartEvent(Arena arena) {
      this.arena = arena;
   }

   public Arena getArena() { return this.arena; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
