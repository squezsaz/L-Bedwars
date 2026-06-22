package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerLeaveArenaEvent extends PlayerEvent {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;

   public PlayerLeaveArenaEvent(@NotNull Player player, Arena arena) {
      super(player);
      this.arena = arena;
   }

   public Arena getArena() { return this.arena; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
