package com.lbedwars.api.events;

import com.lbedwars.arena.Arena;
import com.lbedwars.arena.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerJoinArenaEvent extends PlayerEvent {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Arena arena;
   private final Team team;

   public PlayerJoinArenaEvent(@NotNull Player player, Arena arena, @Nullable Team team) {
      super(player);
      this.arena = arena;
      this.team = team;
   }

   public Arena getArena() { return this.arena; }
   @Nullable
   public Team getTeam() { return this.team; }

   @NotNull
   public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
