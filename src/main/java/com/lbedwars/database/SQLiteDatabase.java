package com.lbedwars.database;

import com.lbedwars.LBedWars;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SQLiteDatabase implements Database {
   private final LBedWars plugin;
   private Connection connection;

   public SQLiteDatabase(LBedWars plugin) {
      this.plugin = plugin;
   }

   public void connect() {
      try {
         Class.forName("org.sqlite.JDBC");
         this.connection = DriverManager.getConnection("jdbc:sqlite:" + String.valueOf(this.plugin.getDataFolder()) + "/data.db");
         this.createTables();
         this.plugin.getLogger().info(this.plugin.getLanguageManager().getMessage("console.sqlite-connected"));
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private void createTables() throws SQLException {
      Statement stmt = this.connection.createStatement();

      try {
         stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, kills INT DEFAULT 0, deaths INT DEFAULT 0, final_kills INT DEFAULT 0, final_deaths INT DEFAULT 0, wins INT DEFAULT 0, losses INT DEFAULT 0, beds_broken INT DEFAULT 0, games_played INT DEFAULT 0, xp INT DEFAULT 0, level INT DEFAULT 1, last_daily BIGINT DEFAULT 0, cosmetics TEXT DEFAULT '', kill_effect VARCHAR(32) DEFAULT 'none', victory_dance VARCHAR(32) DEFAULT 'none', trail VARCHAR(32) DEFAULT 'none', victory_song VARCHAR(32) DEFAULT 'none')");
      } catch (Throwable var5) {
         if (stmt != null) {
            try {
               stmt.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stmt != null) {
         stmt.close();
      }

   }

   public void close() {
      try {
         if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public boolean hasStats(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT 1 FROM player_stats WHERE uuid = ?");

         boolean var4;
         try {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            try {
               var4 = rs.next();
            } catch (Throwable var8) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var9) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void createStats(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR IGNORE INTO player_stats (uuid) VALUES (?)");

         try {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
         } catch (Throwable var6) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public int getStats(UUID uuid, String stat) {
      String column = stat.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

         int var6;
         label84: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label78: {
                  try {
                     if (!rs.next()) {
                        break label78;
                     }

                     var6 = rs.getInt(column);
                  } catch (Throwable var10) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label84;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }

            return 0;
         }

         if (ps != null) {
            ps.close();
         }

         return var6;
      } catch (SQLException e) {
         e.printStackTrace();
         return 0;
      }
   }

   public void addStats(UUID uuid, String stat, int amount) {
      String column = stat.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("UPDATE player_stats SET " + column + " = " + column + " + ? WHERE uuid = ?");

         try {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
         } catch (Throwable var9) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public void setStats(UUID uuid, String stat, int value) {
      String column = stat.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

         try {
            ps.setInt(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
         } catch (Throwable var9) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public long getLong(UUID uuid, String key) {
      String column = key.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

         long var6;
         label84: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label78: {
                  try {
                     if (!rs.next()) {
                        break label78;
                     }

                     var6 = rs.getLong(column);
                  } catch (Throwable var10) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label84;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }

            return 0L;
         }

         if (ps != null) {
            ps.close();
         }

         return var6;
      } catch (SQLException e) {
         e.printStackTrace();
         return 0L;
      }
   }

   public void setLong(UUID uuid, String key, long value) {
      String column = key.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

         try {
            ps.setLong(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public Map<UUID, Integer> getLeaderboard(String stat, int limit) {
      Map<UUID, Integer> result = new LinkedHashMap();
      String column = stat.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT uuid, " + column + " FROM player_stats ORDER BY " + column + " DESC LIMIT ?");

         try {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            try {
               while(rs.next()) {
                  result.put(UUID.fromString(rs.getString("uuid")), rs.getInt(column));
               }
            } catch (Throwable var11) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var12) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

      return result;
   }

   public Set<String> getOwnedCosmetics(UUID uuid) {
      Set<String> result = new HashSet();

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT cosmetics FROM player_stats WHERE uuid = ?");

         try {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            try {
               if (rs.next()) {
                  String raw = rs.getString("cosmetics");
                  if (raw != null && !raw.isEmpty()) {
                     for(String s : raw.split(",")) {
                        result.add(s.trim());
                     }
                  }
               }
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

      return result;
   }

   public void addOwnedCosmetic(UUID uuid, String cosmeticKey) {
      Set<String> owned = this.getOwnedCosmetics(uuid);
      if (!owned.contains(cosmeticKey)) {
         owned.add(cosmeticKey);
         String joined = String.join(",", owned);

         try {
            PreparedStatement ps = this.connection.prepareStatement("UPDATE player_stats SET cosmetics = ? WHERE uuid = ?");

            try {
               ps.setString(1, joined);
               ps.setString(2, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

      }
   }

   public String getSelectedCosmetic(UUID uuid, String type) {
      String column = type.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

         String var7;
         label92: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label83: {
                  try {
                     if (!rs.next()) {
                        break label83;
                     }

                     String val = rs.getString(column);
                     var7 = val != null ? val : "none";
                  } catch (Throwable var10) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label92;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }

            return "none";
         }

         if (ps != null) {
            ps.close();
         }

         return var7;
      } catch (SQLException e) {
         e.printStackTrace();
         return "none";
      }
   }

   public void setSelectedCosmetic(UUID uuid, String type, String value) {
      String column = type.replace("-", "_");

      try {
         PreparedStatement ps = this.connection.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

         try {
            ps.setString(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
         } catch (Throwable var9) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }
}
