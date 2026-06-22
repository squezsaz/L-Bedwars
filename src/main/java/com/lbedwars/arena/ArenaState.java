package com.lbedwars.arena;

public enum ArenaState {
   WAITING,
   STARTING,
   PLAYING,
   ENDING,
   RESTARTING;

    private static ArenaState[] $values() {
      return new ArenaState[]{WAITING, STARTING, PLAYING, ENDING, RESTARTING};
   }
}
