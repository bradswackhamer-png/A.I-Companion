# Changelog

All notable changes to the **Gemini AI Companion Bots** mod are documented in this file.

---

## [1.1.0] - 2026-10-24

### 📊 New Feature: Interactive Companion Dashboard & Speedometer HUD
- **Right-Click, Sneak-Click & Hotkey (`B` key) Access:**
  - Open any companion's full dashboard by right-clicking them in-world, pressing **`B`** (configurable in Controls), or using `/bot gui [name]`.
- **Speedometer-Style Vital Gauges:**
  - High-contrast, color-graded health meter (Green → Amber → Red) showing current vs maximum HP.
  - Satiety and hunger meter tracking companion food levels in real time.
  - Active task and assigned zone badges.
- **Direct Equipment & Full Inventory Management:**
  - 41 dedicated slots: Helmet, Chestplate, Leggings, Boots, and Off-Hand (Shield/Torch) plus full 36-slot backpack.
  - Integrated player inventory with shift-click quick-transfer support.
- **One-Click Tactical Command Buttons:**
  - Instant buttons for `[Follow/Hold]`, `[Patrol]`, `[Build Cottage]`, `[Store in Chest]`, and `[Eat Rations]`.

### 🗺️ New Feature: Global Waypoint & Area System (`Surveyor Wand`)
- **Surveyor Wand Tool (`/bot wand`):**
  - Right-click blocks in-world to connect multi-point perimeter vertices with glowing laser particle fences.
  - Sneak + Right-Click opens the in-game **Area Configuration GUI** to name zones (`Compound`, `NorthFarm`) and select zone types (`COMPOUND`, `FARM`, `RANCH`, `SAFE_ZONE`, `RESTRICTED`).
  - Added dedicated **AI Companion** Creative Tab and diagonal survival crafting recipe (1 Compass + 2 Sticks).
- **Perimeter Confinement & AI Telemetry:**
  - Assigned companions (`/bot area assign <bot> <area>` or chat *"guard Compound"*) are locked within boundaries; movement vectors reflect off borders back to the zone center.
  - Gemini AI prompts receive real-time location telemetry indicating which named area the player and companion occupy.
- **Area Commands:** `/bot wand`, `/bot area list`, `/bot area show <name>`, `/bot area assign <bot> <area>`, `/bot area remove <name>`, `/bot area clear`.

### 🏠 New Feature: Autonomous Architectural Builder (`builder`)
- **Voxel Cottage Construction Pipeline:**
  - Say *"build a house"*, *"build home"*, or *"build cottage"* to initiate complete architectural construction.
  - **Site Leveling:** Automatically clears foliage, grass, and obstruction blocks.
  - **Cobblestone Foundation:** Lays a level 7x7 stone floor.
  - **Framing & Walls:** Raises corner Oak Log pillars and 3-block tall Oak Plank walls.
  - **Doors & Windows:** Inserts glass windows and a functional wooden door with proper lower/upper halves.
  - **Overhanging Roof:** Builds a protective plank roof keeping out rain and phantoms.
  - **Lighting & Furnishing:** Mounts interior torches (preventing hostile mob spawns) and places a cozy bed.
- **Village Housing Registry (`VillageManager`):**
  - Tracks completed houses, total beds, and bed vacancies per compound to power colony growth and apprentice recruitment.

### ⚔️ Tactical Combat Mastery & Squad Distress Coordination
- **Sprint-Jump Critical Hits:** Companions sprint forward and jump into melee strikes, landing falling critical attacks with spark particles.
- **Dynamic Shield Blocking:**
  - Detects Creeper swelling (`swellDir > 0`) within 7 blocks and instantly raises off-hand shields to absorb explosions.
  - Detects Skeleton and Pillager bow/crossbow draw animations, raising shields to deflect arrows and dropping them only to counter-attack.
- **Worker Distress & Guard Dispatch:**
  - Non-combat workers (farmers, lumberjacks, miners, builders) shout in chat when damaged: `"[Distress] Under attack by <Monster>! Requesting backup!"`
  - Nearby Guard companions within 48 blocks immediately drop low-priority tasks and charge to defend the worker.

### 🧗 Advanced Path Tracing, Pit Avoidance & Hole Escape
- **Pit Hazard Avoidance (>1 Block Deep):**
  - Detects drops $\ge 2$ blocks deep along the bot's travel vector in advance, steering around edges or bridging instead of falling into ravines or pits.
- **Eliminated Wall-Jumping Bug:** Distinguishes 1-block steps from 2+ block solid walls; bots no longer pointlessly jump against solid walls.
- **Pillar Jump Self-Rescue:** If trapped in a hole 2+ blocks deep, companions check ceiling clearance, jump straight up, and place blocks beneath their feet until level with the surface.

### 🚂 Train Conductor Role (`conductor` / `train`) (`miningtools` Integration)
- Autonomous locomotive operation: fuel loading, ignition, and initial impulse to start rolling.
- **Precision Rail Navigation Commands:**
  - Distance commands: *"move ahead 4 tiles"*, *"forward 10 blocks"*, *"reverse 5 blocks"*.
  - Rail pathfinding: *"come here"* / *"drive to me"* rolls down rails to the player and sounds the horn.
  - Station transport: automated round trips between stations to load freight and deliver cargo into chests.
  - Instant brakes on *"stop"*, *"halt"*, or *"cancel that"*.

### 🚜 Tractor Operator Role (`janemoose` Integration)
- Companions can mount, steer, and operate rideable tractors.
- Automatically operates tractor attachments: Soil Tiller / Plow, Seed Planter, and Mature Crop Harvester.

### 💾 Persistence Hardening & Reboot Fixes
- **Startup Save Wipe Bug Fixed:** Added load-state verification (`hasLoadedFromDisk` gate) preventing early dimension save events from overwriting `aicompanion_memory.json` on server boot.
- **Forced Chunk Loading:** Companions register persistent chunk tickets (`setChunkForced`), keeping their chunks active when human players leave.
- **Instant Client Render:** Immediate spawn packet sync on player join eliminates invisible companion lag across chunk borders.
- **Chest Deposit Loop Fix:** Fixed loop where companions repeatedly deposited starter items into chests.

---

## [1.0.0] - 2026-10-23
- Initial public release on CurseForge.
- Autonomous Companion bots with Google Gemini AI conversational planning + offline tactical heuristics.
- Autonomous Farmer role (tilling, maturity-only harvesting, replanting, chest management).
- Animal Husbandry & Rancher role (selective breeding, population control, drop collection).
- Miner, Lumberjack, Guard, and Courier roles.
- Interactive API key setup (`/bot apikey <key>`) with unbaked secure configuration.
- Custom mod logo and in-game NeoForge menu branding.
