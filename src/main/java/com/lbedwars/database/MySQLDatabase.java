package com.lbedwars.database;

import com.lbedwars.LBedWars;
import com.lbedwars.libs.hikari.HikariConfig;
import com.lbedwars.libs.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MySQLDatabase implements Database {
   private final LBedWars plugin;
   private HikariDataSource dataSource;
   private volatile boolean tablesReady;
   private boolean tablesWarningLogged;

   public MySQLDatabase(LBedWars plugin) {
      this.plugin = plugin;
   }

   public void connect() {
      try {
         String host = this.plugin.getConfig().getString("storage.mysql.host", "localhost");
         int port = this.plugin.getConfig().getInt("storage.mysql.port", 3306);
         String database = this.plugin.getConfig().getString("storage.mysql.database", "bedwars");
         String username = this.plugin.getConfig().getString("storage.mysql.username", "root");
         String password = this.plugin.getConfig().getString("storage.mysql.password", "");
         int poolSize = this.plugin.getConfig().getInt("storage.mysql.pool-size", 10);
         HikariConfig config = new HikariConfig();
         config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8");
         config.setUsername(username);
         config.setPassword(password);
         config.setMaximumPoolSize(poolSize);
         config.setMinimumIdle(2);
         config.setConnectionTimeout(5000L);
         config.setLeakDetectionThreshold(30000L);
         config.setMetricsTrackerFactory(null);
         config.addDataSourceProperty("cachePrepStmts", "true");
         config.addDataSourceProperty("prepStmtCacheSize", "250");
         config.addDataSourceProperty("useServerPrepStmts", "true");
         this.dataSource = new HikariDataSource(config);
         this.createTables();
         this.tablesReady = true;
         this.plugin.getLogger().info(this.plugin.getLanguageManager().getMessage("console.mysql-connected"));
      } catch (Exception e) {
         this.plugin.getLogger().warning("MySQL connection failed: " + e.getMessage());
      }

   }

   private void createTables() throws SQLException {
      Connection conn = this.dataSource.getConnection();

      try {
         Statement stmt = conn.createStatement();

         try {
             stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, kills INT DEFAULT 0, deaths INT DEFAULT 0, final_kills INT DEFAULT 0, final_deaths INT DEFAULT 0, wins INT DEFAULT 0, losses INT DEFAULT 0, beds_broken INT DEFAULT 0, games_played INT DEFAULT 0, xp INT DEFAULT 0, level INT DEFAULT 1, last_daily BIGINT DEFAULT 0, cosmetics TEXT, kill_effect VARCHAR(32) DEFAULT 'none', victory_dance VARCHAR(32) DEFAULT 'none', trail VARCHAR(32) DEFAULT 'none', victory_song VARCHAR(32) DEFAULT 'none')");
         } catch (Throwable var7) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (Throwable var8) {
         if (conn != null) {
            try {
               conn.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }
         }

         throw var8;
      }

      if (conn != null) {
         conn.close();
      }

   }

   public void close() {
      if (this.dataSource != null && !this.dataSource.isClosed()) {
         this.dataSource.close();
      }

   }

   private Connection getConnection() throws SQLException {
      if (!this.tablesReady) {
         if (!this.tablesWarningLogged) {
            this.tablesWarningLogged = true;
            this.plugin.getLogger().warning("Database tables not ready, returning default values");
         }
         throw new SQLException("Tables not ready");
      }
      return this.dataSource.getConnection();
   }

   public boolean hasStats(UUID uuid) {
      try {
         Connection conn = this.getConnection();

         boolean var5;
         try {
            PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM player_stats WHERE uuid = ?");

            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               try {
                  var5 = rs.next();
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
         } catch (Throwable var12) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var7) {
                  var12.addSuppressed(var7);
               }
            }

            throw var12;
         }

         if (conn != null) {
            conn.close();
         }

         return var5;
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
         return false;
      }
   }

   public void createStats(UUID uuid) {
      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO player_stats (uuid) VALUES (?)");

            try {
               ps.setString(1, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var8) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var9) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

   }

   public int getStats(UUID uuid, String stat) {
      String column = stat.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         int var7;
         label114: {
            try {
               PreparedStatement ps;
               label106: {
                  ps = conn.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

                  try {
                     ps.setString(1, uuid.toString());
                     ResultSet rs = ps.executeQuery();

                     label86: {
                        try {
                           if (rs.next()) {
                              var7 = rs.getInt(column);
                              break label86;
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
                        break label106;
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
                  break label114;
               }

               if (ps != null) {
                  ps.close();
               }
            } catch (Throwable var14) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var9) {
                     var14.addSuppressed(var9);
                  }
               }

               throw var14;
            }

            if (conn != null) {
               conn.close();
            }

            return 0;
         }

         if (conn != null) {
            conn.close();
         }

         return var7;
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
         return 0;
      }
   }

   public void addStats(UUID uuid, String stat, int amount) {
      String column = stat.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET " + column + " = " + column + " + ? WHERE uuid = ?");

            try {
               ps.setInt(1, amount);
               ps.setString(2, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var12) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

   }

   public void setStats(UUID uuid, String stat, int value) {
      String column = stat.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

            try {
               ps.setInt(1, value);
               ps.setString(2, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var12) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

   }

   public long getLong(UUID uuid, String key) {
      String column = key.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         long var7;
         label114: {
            try {
               PreparedStatement ps;
               label106: {
                  ps = conn.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

                  try {
                     ps.setString(1, uuid.toString());
                     ResultSet rs = ps.executeQuery();

                     label86: {
                        try {
                           if (rs.next()) {
                              var7 = rs.getLong(column);
                              break label86;
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
                        break label106;
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
                  break label114;
               }

               if (ps != null) {
                  ps.close();
               }
            } catch (Throwable var14) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var9) {
                     var14.addSuppressed(var9);
                  }
               }

               throw var14;
            }

            if (conn != null) {
               conn.close();
            }

            return 0L;
         }

         if (conn != null) {
            conn.close();
         }

         return var7;
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
         return 0L;
      }
   }

   public void setLong(UUID uuid, String key, long value) {
      String column = key.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

            try {
               ps.setLong(1, value);
               ps.setString(2, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var12) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var13) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

   }

   public Map<UUID, Integer> getLeaderboard(String stat, int limit) {
      Map<UUID, Integer> result = new LinkedHashMap();
      String column = stat.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("SELECT uuid, " + column + " FROM player_stats ORDER BY " + column + " DESC LIMIT ?");

            try {
               ps.setInt(1, limit);
               ResultSet rs = ps.executeQuery();

               try {
                  while(rs.next()) {
                     result.put(UUID.fromString(rs.getString("uuid")), rs.getInt(column));
                  }
               } catch (Throwable var13) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                     }
                  }

                  throw var13;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var14) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var11) {
                     var14.addSuppressed(var11);
                  }
               }

               throw var14;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var15) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var10) {
                  var15.addSuppressed(var10);
               }
            }

            throw var15;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

      return result;
   }

   public Set<String> getOwnedCosmetics(UUID uuid) {
      Set<String> result = new HashSet();

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("SELECT cosmetics FROM player_stats WHERE uuid = ?");

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
               } catch (Throwable var14) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var15) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var16) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var11) {
                  var16.addSuppressed(var11);
               }
            }

            throw var16;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

      return result;
   }

   public void addOwnedCosmetic(UUID uuid, String cosmeticKey) {
      Set<String> owned = this.getOwnedCosmetics(uuid);
      if (!owned.contains(cosmeticKey)) {
         owned.add(cosmeticKey);
         String joined = String.join(",", owned);

         try {
            Connection conn = this.getConnection();

            try {
               PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET cosmetics = ? WHERE uuid = ?");

               try {
                  ps.setString(1, joined);
                  ps.setString(2, uuid.toString());
                  ps.executeUpdate();
               } catch (Throwable var11) {
                  if (ps != null) {
                     try {
                        ps.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (ps != null) {
                  ps.close();
               }
            } catch (Throwable var12) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (conn != null) {
               conn.close();
            }
         } catch (SQLException e) {
            this.plugin.getLogger().warning("Database error: " + e.getMessage());
         }

      }
   }

   public String getSelectedCosmetic(UUID uuid, String type) {
      String column = type.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         String var8;
         label123: {
            try {
               PreparedStatement ps;
               label111: {
                  ps = conn.prepareStatement("SELECT " + column + " FROM player_stats WHERE uuid = ?");

                  try {
                     ps.setString(1, uuid.toString());
                     ResultSet rs = ps.executeQuery();

                     label91: {
                        try {
                           if (rs.next()) {
                              String val = rs.getString(column);
                              var8 = val != null ? val : "none";
                              break label91;
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
                        break label111;
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
                  break label123;
               }

               if (ps != null) {
                  ps.close();
               }
            } catch (Throwable var14) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var9) {
                     var14.addSuppressed(var9);
                  }
               }

               throw var14;
            }

            if (conn != null) {
               conn.close();
            }

            return "none";
         }

         if (conn != null) {
            conn.close();
         }

         return var8;
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
         return "none";
      }
   }

   public void setSelectedCosmetic(UUID uuid, String type, String value) {
      String column = type.replace("-", "_");

      try {
         Connection conn = this.getConnection();

         try {
            PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET " + column + " = ? WHERE uuid = ?");

            try {
               ps.setString(1, value);
               ps.setString(2, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var11) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (Throwable var12) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("Database error: " + e.getMessage());
      }

   }
}
