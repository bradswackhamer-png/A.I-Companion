# Changelog

All notable changes to the **Gemini AI Companion Bots** mod are documented in this file.

---

## [1.1.0] - 2026-10-24

### 🚂 New Feature: Train Conductor Role (`conductor` / `train`)
- **Locomotive Engine Control (`miningtools` Integration):**
  - Seamless, soft-reflection compatibility with `BasicLocomotiveEntity`.
  - Automatic fuel management, active ignition handling (`handleHotkeyAction(0)` / `active = true`), and initial velocity impulses to overcome track inertia.
- **Precision Rail Navigation Commands:**
  - Distance control: `"move ahead 4 tiles"`, `"forward 10 blocks"`, `"reverse 5 blocks"` — trains measure distance traveled and halt precisely with a horn blast.
  - Rail pathfinding: `"come here"` or `"drive to me"` analyzes track orientation, rolls the locomotive down the rails directly to the player, stops within 4 blocks, and sounds the horn. If the player is far off the tracks (>100 blocks), the conductor safely dismounts and walks over on foot.
  - Station transport routes: navigates to saved stations/waypoints, loads freight items from station chests into attached freight cars, runs round trips, and delivers cargo safely into destination chests.
  - Instant brakes: `"stop"`, `"cancel that"`, `"halt"`, or `"brake"` immediately clears active rail targets and shuts off locomotive engines.
  - Fixed chat parsing to prevent direction keywords (`ahead`, `forward`, `back`, `reverse`) from mistakenly being registered as station waypoints.

### 🚜 New Feature: Tractor Operator (`janemoose` Integration)
- **Rideable Agricultural Tractor Automation:**
  - Soft-reflection support for `janemoose` tractors and attachments.
  - Companions can mount, steer, and drive tractors across tilled fields.
  - Operates tractor attachments: Soil Tiller / Plow, Seed Planter, and Mature Crop Harvester.

### 💾 Bug Fixes & Persistence Hardening
- **Server Startup Race Condition Fixed:**
  - Resolved an issue where Minecraft's early `LevelEvent.Save` on dimension initialization triggered before `ServerStartedEvent` / `loadAll()`, which previously overwrote `aicompanion_memory.json` with an empty bot list on reboot.
  - Added `hasLoadedFromDisk` gate in `BotPersistence` — saving is strictly disabled until world bot memory has finished loading into memory.
  - Added `hasExplicitlyClearedAllBots` safeguard — `saveAll` will never overwrite the save file with 0 bots unless a player explicitly ran `/bot remove` or `/bot removeall`.
  - Implemented atomic file writes via `.tmp` staging and automatic `.bak` backups with fallback recovery.
- **Forced Chunk Loading & Client Visibility:**
  - Companions now register persistent chunk tickets (`setChunkForced(chunkX, chunkZ, true)`), preventing their chunks from unloading when human players leave the area or disconnect.
  - Enhanced `onPlayerLoggedIn` to immediately synchronize entity spawn packets (`ClientboundAddEntityPacket`), head rotation, and equipment data to joining players so companions are instantly rendered without chunk border lag.
- **Chest Deposit Loop Fix:**
  - Fixed an issue where farmers deposited starter items into chests and became stuck repeatedly reporting 0 items moved.

---

## [1.0.0] - 2026-10-23
- Initial public release on CurseForge.
- Autonomous Companion bots with Google Gemini AI conversational planning + offline tactical heuristics.
- Autonomous Farmer role (tilling, maturity-only harvesting, replanting, chest management).
- Animal Husbandry & Rancher role (selective breeding, population control, drop collection).
- Miner, Lumberjack, Guard, and Courier roles.
- Interactive API key setup (`/bot apikey <key>`) with unbaked secure configuration.
- Custom mod logo and in-game NeoForge menu branding.
