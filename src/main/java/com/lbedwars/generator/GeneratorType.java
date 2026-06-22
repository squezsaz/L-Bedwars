package com.lbedwars.generator;

public enum GeneratorType {
   IRON,
   GOLD,
   DIAMOND,
   EMERALD;

    private static GeneratorType[] $values() {
      return new GeneratorType[]{IRON, GOLD, DIAMOND, EMERALD};
   }
}
