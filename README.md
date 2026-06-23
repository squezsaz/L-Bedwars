# L-BedWars

Advanced BedWars plugin for Paper/Spigot 1.19+ with Folia support and multi-server proxy mode.

## Features

- **Full BedWars Gameplay** — Solo, Duo, Trio, and Quad modes with auto-balancing
- **Dual PvP Mode** — CLASSIC (1.8 style, no attack cooldown) or MODERN (1.9+)
- **Shop** — 6 categories with tiered items and multiple currencies
- **Team Upgrades** — Sharpness, Protection, Haste, Forge, Heal Pool, Dragon Buff + traps
- **Auto-Generators** — Iron, Gold, Diamond, Emerald with timed tier upgrades
- **Dragon System** — Ender Dragons spawn at configurable times, attack enemies
- **Bed Protection** — Effects applied on bed destruction, team elimination
- **Compass Tracker** — Purchase enemy tracking with in-game currency when teams are low; actionbar shows distance and direction
- **Proxy Mode** — Multi-server support via L-BedWarsProxy (BungeeCord/Velocity); queue system, stats database, leaderboards
- **TabList System** — Colored team-sorted player names, header/footer, PlaceholderAPI support
- **Hologram System** — Configurable leaderboard holograms (ArmorStand-based), multiple holograms per stat
- **Queue System** — Optional auto-queue when arena is full; priority permissions for VIP access
- **Scoreboards** — Lobby, waiting, and in-game scoreboards with configurable date display
- **Spectator System** — Compass teleport GUI, leave-game bed, `/bwa tp <player>` admin spectate
- **Party System** — External integration with Parties API (softdepend); party join syncs members to same arena
- **Level & XP System** — 100 levels with scaling XP requirements
- **Daily Rewards** — Claimable every N hours, configurable XP/coins/money
- **Cosmetics** — Kill effects, victory dances, trails (purchasable via Vault)
- **Database** — YAML, SQLite, or MySQL (proxy mode uses HikariCP connection pool)
- **Stats Tracking** — Kills, deaths, wins, losses, beds broken, final kills, leaderboards
- **Language System** — TR (Turkish) and EN (English) via YAML files with full message customization
- **API** — Custom Bukkit events and LBedWarsAPI for external plugin integration
- **PlaceholderAPI Support** — 30+ placeholders for stats, level, game info, team, cosmetics
- **Folia Support** — Region-scheduled tasks for Folia servers
- **Performance Optimized**
## Components

### L-BedWars (Game Server)
The core plugin running on each game server. Handles arena logic, shop, upgrades, combat, and game flow.

### L-BedWarsProxy (Proxy Plugin)
Optional companion plugin for BungeeCord/Velocity. Features:
- **ProxyDatabase** — MySQL-based stats storage shared across servers
- **Arena management** — Centralized arena state tracking via socket communication
- **Queue system** — Server-wide queue with priority permissions
- **Hologram leaderboards** — Configurable ArmorStand holograms
- **Admin commands** — Remote arena management, spectate forwarding, leaderboard GUI
- **Health check** — Configurable arena timeout detection
- **PlaceholderAPI expansion** — Proxy-side placeholders (`%lbedwars_kills%`, `%lbedwars_level%`, etc.)

## Commands

### Player Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/bedwars help` | `/bw help` | `bedwars.play` | Show help menu |
| `/bedwars join <arena>` | `/bw join` | `bedwars.play` | Join an arena |
| `/bedwars leave` | `/bw leave` | `bedwars.play` | Leave current game |
| `/bedwars randomjoin <mode>` | `/bw randomjoin` | `bedwars.play` | Join a random arena |
| `/bedwars list` | `/bw list` | `bedwars.play` | List available arenas |
| `/bedwars stats [player]` | `/bw stats` | `bedwars.play` | Show player stats |
| `/bedwars cosmetics` | `/bw cosmetics` | `bedwars.play` | Open cosmetics menu |
| `/bedwars dailyreward` | `/bw dailyreward` | `bedwars.play` | Claim daily reward |
| `/bedwars vote <map>` | `/bw vote` | `bedwars.play` | Vote for a map |
| `/bedwars rejoin` | `/bw rejoin` | `bedwars.play` | Rejoin last game |

### Admin Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/bedwarsadmin help` | `/bwa help` | `bedwars.admin` | Admin help menu |
| `/bedwarsadmin forcestart <arena>` | `/bwa forcestart` | `bedwars.admin` | Force start an arena |
| `/bedwarsadmin addxp <player> <amount>` | `/bwa addxp` | `bedwars.admin` | Add XP to player |
| `/bedwarsadmin setlobby <arena>` | `/bwa setlobby` | `bedwars.admin` | Set arena lobby spawn |
| `/bedwarsadmin setspectator <arena>` | `/bwa setspectator` | `bedwars.admin` | Set spectator spawn |
| `/bedwarsadmin setmainlobby` | `/bwa setmainlobby` | `bedwars.admin` | Set main lobby location |
| `/bedwarsadmin setmode <arena> <mode>` | `/bwa setmode` | `bedwars.admin` | Set arena game mode |
| `/bedwarsadmin addteam <arena> <name> <color> <max>` | `/bwa addteam` | `bedwars.admin` | Add a team |
| `/bedwarsadmin removeteam <arena> <name>` | `/bwa removeteam` | `bedwars.admin` | Remove a team |
| `/bedwarsadmin enable <arena>` | `/bwa enable` | `bedwars.admin` | Enable an arena |
| `/bedwarsadmin disable <arena>` | `/bwa disable` | `bedwars.admin` | Disable an arena |
| `/bedwarsadmin reload` | `/bwa reload` | `bedwars.admin` | Reload config |
| `/bedwarsadmin tp <player>` | `/bwa tp` | `bedwars.admin` | Teleport to player as spectator |
| `/bedwarsadmin hologram set <stat>` | — | `bedwars.admin` | Create leaderboard hologram |
| `/bedwarsadmin hologram remove <id>` | — | `bedwars.admin` | Remove a hologram |
| `/bedwarsadmin hologram list` | — | `bedwars.admin` | List all holograms |

### Setup Commands

| Command | Permission | Description |
|---|---|---|
| `/setup` | `bedwars.setup` | Enter arena setup mode |
| `/setup setregion` | `bedwars.setup` | Save selected region |
| `/setup setspawn <team>` | `bedwars.setup` | Set team spawn |
| `/setup setbed <team>` | `bedwars.setup` | Set team bed |
| `/setup setgenerator <type>` | `bedwars.setup` | Set generator point |
| `/setup setshop` | `bedwars.setup` | Set shop NPC location |
| `/setup setupgrade` | `bedwars.setup` | Set upgrade NPC location |
| `/setup done` | `bedwars.setup` | Finish arena setup |

### Proxy Commands

| Command | Permission | Description |
|---|---|---|
| `/bw join <arena>` | `bedwars.play` | Join arena (queues if full) |
| `/bwa tp <player>` | `bedwars.admin` | Spectate a player across servers |
| `/bwa leaderboard <stat>` | `bedwars.admin` | Open leaderboard GUI |
| `/bwa hologram set <stat>` | `bedwars.admin` | Create leaderboard hologram |
| `/bwa hologram remove <id>` | `bedwars.admin` | Remove a hologram |
| `/bwa hologram list` | `bedwars.admin` | List holograms |

## Placeholders

### Player Stats

| Placeholder | Description |
|---|---|
| `%lbedwars_kills%` | Total kills |
| `%lbedwars_deaths%` | Total deaths |
| `%lbedwars_kdr%` | Kill/death ratio |
| `%lbedwars_finalkills%` | Final kills |
| `%lbedwars_finaldeaths%` | Final deaths |
| `%lbedwars_finalkdr%` | Final kill/death ratio |
| `%lbedwars_wins%` | Total wins |
| `%lbedwars_losses%` | Total losses |
| `%lbedwars_wlr%` | Win/loss ratio |
| `%lbedwars_bedsbroken%` | Beds broken |
| `%lbedwars_gamesplayed%` | Games played |

### Level & XP

| Placeholder | Description |
|---|---|
| `%lbedwars_level%` | Current level |
| `%lbedwars_xp%` | Current XP |
| `%lbedwars_xp_required%` | XP required for next level |
| `%lbedwars_xp_percent%` | XP progress percentage |
| `%lbedwars_xp_progress%` | XP progress bar |

### Economy

| Placeholder | Description |
|---|---|
| `%lbedwars_balance%` | Vault balance (integer) |
| `%lbedwars_balance_formatted%` | Vault balance (formatted) |

### Cosmetics

| Placeholder | Description |
|---|---|
| `%lbedwars_active_killeffect%` | Active kill effect |
| `%lbedwars_active_victorydance%` | Active victory dance |
| `%lbedwars_active_trail%` | Active trail effect |

### Game / Arena

| Placeholder | Description |
|---|---|
| `%lbedwars_arena%` | Current arena name |
| `%lbedwars_gamestate%` | Arena state (WAITING/STARTING/PLAYING/ENDED) |
| `%lbedwars_gametime%` | Game time in mm:ss |
| `%lbedwars_isplaying%` | Is player in-game (YES/NO) |
| `%lbedwars_isspectating%` | Is spectating (YES/NO) |
| `%lbedwars_players%` | Current player count |
| `%lbedwars_maxplayers%` | Maximum player capacity |
| `%lbedwars_arena_online%` | Total online players across all arenas (proxy) |

### Team

| Placeholder | Description |
|---|---|
| `%lbedwars_team%` | Team name |
| `%lbedwars_teamcolor%` | Team color code (e.g. `&c`) |
| `%lbedwars_team_colored%` | Colored team name |
| `%lbedwars_teammates%` | Total teammates |
| `%lbedwars_teammates_alive%` | Alive teammates |
| `%lbedwars_hasbed%` | Is bed alive (YES/NO) |

## Compass Tracker

When only 2 teams (configurable via `min-teams`) remain, players receive a compass in slot 8. Right-clicking opens a GUI showing all alive enemies:

- **Purchase** — Click an enemy to buy tracking (costs emeralds, configurable)
- **Re-select** — Already purchased enemies can be switched to freely
- **Deselect** — Click the active target to stop tracking
- **Actionbar** — Shows direction arrow + distance to tracked enemy
- **Persistence** — Tracking survives death; compass is re-given on respawn
- **Death cleanup** — When the tracked enemy dies (final kill), tracking is removed

Configuration:
```yaml
compass-tracker:
  enabled: true
  cost: 2
  currency: EMERALD
  min-teams: 2
  update-interval: 10
```

## Shop

6 categories with Hypixel-style pricing:

| Category | Icon | Slot |
|---|---|---|
| Blocks | White Wool | 10 |
| Melee | Iron Sword | 11 |
| Armor | Chainmail Boots | 12 |
| Tools | Stone Pickaxe | 13 |
| Ranged | Bow | 14 |
| Potions | Potion | 15 |

Currencies: Iron Ingot, Gold Ingot, Diamond, Emerald

## Team Upgrades

| Upgrade | Max Level | Cost (Diamonds) |
|---|---|---|
| Sharpness | 3 | 4 / 8 / 16 |
| Protection | 3 | 5 / 10 / 20 |
| Haste | 2 | 4 / 8 |
| Forge | 4 | 2 / 4 / 6 / 8 |
| Heal Pool | 1 | 3 |
| Dragon Buff | 1 | 5 |

### Traps

| Trap | Cost | Effect |
|---|---|---|
| It's a Trap | 2 Diamonds | Blindness + Slowness + Glowing (30s) |
| Miner Trap | 1 Diamond | Mining Fatigue (20s) |

## Generator System

- **Iron generators** — continuous spawning per team base
- **Gold generators** — continuous spawning per team base
- **Diamond generators** — upgrade tiers (I, II, III) at configurable game times
- **Emerald generators** — upgrade tiers (I, II, III) at configurable game times
- **Forge upgrade** — increases iron/gold output rate per level
- **Max stack limits** — configurable per item type

## Cosmetics

### Kill Effects (8)
Explosion, Lightning, Flame, Heart, Note, Slime, Snow, Blood

### Victory Dances (4)
Fireworks, Lightning, Explosion, Rainbow

### Trails (8)
Flame, Enchantment, Cloud, Snow, Heart, Lava, Critical, Speed

All cosmetic prices are configurable in `cosmetics.yml`. Uses Vault economy.

## Database

| Type | Description |
|---|---|
| `YAML` | Flat-file storage in `stats.yml` |
| `SQLite` | Local SQLite database (`data.db`) |
| `MySQL` | Remote MySQL via HikariCP connection pool |

In proxy mode, the proxy plugin connects directly to MySQL (`player_stats` table) for shared stats across servers.

## Configuration Files

| File | Purpose |
|---|---|
| `config.yml` | Main plugin configuration |
| `shop.yml` | Shop item definitions and prices |
| `upgrades.yml` | Team upgrade definitions and costs |
| `rewards.yml` | Daily reward + game reward values |
| `cosmetics.yml` | Cosmetic price definitions |
| `holograms.yml` | Proxy leaderboard hologram data |
| `arenas/*.yml` | Per-arena configuration (created via setup) |
| `languages/messages_en.yml` | English language file |
| `languages/messages_tr.yml` | Turkish language file |



## Arena Setup

1. **Select region** — `/setup` to get wand, left/right click corners
2. **Save region** — `/setup setregion`
3. **Set mode** — `/bwa setmode <arena> <solo/duo/trio/quad>`
4. **Add teams** — `/bwa addteam <arena> <name> <color> <max>`
5. **Set spawns** — `/setup setspawn <team>` at each spawn point
6. **Set beds** — Look at the bed, `/setup setbed <team>`
7. **Add generators** — `/setup setgenerator <IRON/GOLD/DIAMOND/EMERALD>`
8. **Add NPCs** — `/setup setshop` and `/setup setupgrade`
9. **Set lobby** — `/bwa setlobby <arena>`
10. **Set spectator** — `/bwa setspectator <arena>`
11. **Set main lobby** — `/bwa setmainlobby`
12. **Finish** — `/setup done`, then `/bwa enable <arena>`

## Proxy Mode Setup

1. Install `L-BedWarsProxy.jar` in BungeeCord/Velocity `plugins/`
2. Configure `config.yml` on proxy (database, queue, holograms, health-check)
3. Configure `config.yml` on game servers (`proxy-mode: true`, `proxy-socket-address`)
4. Ensure `player_stats` MySQL table is accessible from both proxy and game servers
5. Restart all servers

## TabList System

- **Header/Footer** — Configurable per-arena
- **Player name format** — `{team-color}{player}` with team color prefix
- **Team sorting** — Players grouped by team (configurable, default: enabled)
- **PlaceholderAPI** — Full placeholder support in header/footer

## Chat System

- Normal messages are **team-only** (colored by team)
- Messages starting with `!` are **global** (visible to all players in the arena)
- Waiting lobby messages are public with `[Lobby]` prefix

## Scoreboard System

| Type | Description |
|---|---|
| **Lobby Scoreboard** | Stats, level, K/D, coins — configurable worlds |
| **Waiting Scoreboard** | Player count, start timer, arena name |
| **Game Scoreboard** | Kills, final kills, beds, tiers, game time, configurable date |

## Level System

- 100 levels with scaling XP requirements (1.15x multiplier per level)
- XP sources: kills, wins, bed breaks, final kills, daily rewards
- Level-up announcements with title/subtitle

## Daily Rewards

- Configurable cooldown (default: 24 hours)
- Rewards: XP, coins (Vault money)



## Dependencies

| Dependency | Type | Required |
|---|---|---|
| Paper/Spigot 1.19+ | Server | Yes |
| Vault | Plugin | No (economy limited without) |
| PlaceholderAPI | Plugin | No (placeholders unavailable without) |
| Parties | Plugin | No (party features disabled without) |
| NoteBlockAPI | Plugin | |

## Installation

1. Place `L-BedWars.jar` in your game server's `plugins/` folder
2. (Optional) Place `L-BedWarsProxy.jar` in your proxy's `plugins/` folder
3. Restart the server
4. Configure `config.yml` (language, storage, game settings)
5. Set up arenas using the setup commands
6. Optionally install Vault, PlaceholderAPI, and/or Parties for extended features

Discord: squezsaz
