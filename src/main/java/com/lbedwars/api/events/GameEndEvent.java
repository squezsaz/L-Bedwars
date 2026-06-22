package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameEndEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;
   private final Team winner;

   public GameEndEvent(Arena arena, @Nullable Team winner) {
      this.arena = arena;
      this.winner = winner;
   }

   public Arena getArena() { return this.arena; }
   @Nullable
   public Team getWinner() { return this.winner; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
