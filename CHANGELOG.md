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
  - Supports 2-corner diagonal rectangular bounding boxes as well as multi-corner polygons.
  - Sneak + Right-Click opens the in-game **Area Configuration GUI** to name zones (`Compound`, `NorthFarm`) and select zone types (`COMPOUND`, `FARM`, `RANCH`, `SAFE_ZONE`, `RESTRICTED`).
  - Added dedicated **AI Companion** Creative Tab and diagonal survival crafting recipe (1 Compass + 2 Sticks).
- **3D Cuboid Volume Extrusion (`/bot area height` / `/bot area cube`):**
  - Assign custom height to any surveyed area (`/bot area height <name> <height>` or `/bot area cube <name> <height>`).
  - Renders a glowing 3D holographic wireframe volume with vertical `END_ROD` pillars and top/bottom shimmering particle boundaries.
  - Saves 3D volume dimensions persistently to world memory.
- **Perimeter Confinement & AI Telemetry:**
  - Assigned companions (`/bot area assign <bot> <area>` or chat *"guard Compound"*) are locked within boundaries; movement vectors reflect off borders back to the zone center.
  - Gemini AI prompts receive real-time location telemetry indicating which named area the player and companion occupy.
- **Area Commands:** `/bot wand`, `/bot area list`, `/bot area height <name> <h>`, `/bot area cube <name> <h>`, `/bot area show <name>`, `/bot area assign <bot> <area>`, `/bot area remove <name>`, `/bot area clear`.

### 📐 New Feature: In-World Blueprint Scanner & Custom Builder Catalog (`/bot blueprint`)
- **Capture In-World Structures as Blueprints:**
  - Run `/bot blueprint capture <areaName> <blueprintName>` (or `/bot blueprint scan`) to scan and record all blocks within a cubed area.
  - Captures exact block states, rotation properties (`FACING`, `HALF`, `PART`, `AXIS`, `WATERLOGGED`), and relative coordinates.
  - Automatically calculates material bills and dimensions (`/bot blueprint info <name>`).
- **Autonomous Builder Reconstruction:**
  - Saved blueprints are immediately added to the bot's building catalog.
  - Tell your builder companion in chat: `"builder, build <blueprintName>"` or run `/bot build <bot> <blueprintName>`.
  - Builders automatically rotate the structure to match their current facing direction, clear foliage/air obstructions, pull materials from workshop chests, and place every block voxel by voxel.
- **Blueprint Commands:**
  - `/bot blueprint capture <areaName> <blueprintName>` - Scan cubed area into a saved blueprint.
  - `/bot blueprint list` - List all custom blueprints and their dimensions.
  - `/bot blueprint info <blueprintName>` - Display block count, dimensions, and material requirements.
  - `/bot blueprint remove <blueprintName>` - Delete a custom blueprint.
  - `/bot blueprint build <botName> <blueprintName>` / `/bot build <botName> <blueprintName>` - Order bot to construct the blueprint.

### 🏠 New Feature: Autonomous Architectural Builder & Expanded Catalog (`builder`)
- **Multi-Archetype Architectural Catalog:**
  - **Defensive Guard Watchtower ($5 \times 5$, 10 blocks tall):** Stone brick foundation, internal ladder climbing shaft, mid-floor guard outpost, and open-air crenelated roof battlements with arrow slits for guards to snipe hostile monsters (`"build watchtower"` / `"build tower"`).
  - **Farmhouse Barn & Granary ($9 \times 9$, high-vaulted ceiling):** Cobblestone foundation, high vaulted plank ceiling, double barn doors (Macaw's Barn Doors compatible), decorative hay bales, and built-in deposit chests for farmers (`"build barn"` / `"build granary"`).
  - **Blacksmith Forge & Workshop ($7 \times 7$):** Open-air industrial stone pavilion with chimney, Blast Furnace, standard Furnace, Crafting Table, and Anvil (`"build forge"` / `"build blacksmith"`).
  - **Frontier Residential Cottage ($7 \times 7$):** Classic starter cottage with stone foundation, wooden log pillars, glass windows, wooden door, torch lighting, and bed (`"build house"` / `"build cottage"`).
- **Autonomous Workshop Crafting & Supply Logistics:**
  - Builders automatically scan workshop chests within 16 blocks of the construction site for building materials (cobblestone, stone bricks, logs, glass, planks).
  - Raw logs in chests are autonomously processed into wood planks and sticks if building supplies run short, eliminating tedious manual hand-feeding.
- **Adaptive Modded Palette Engine (Macaw's Modpack Support):**
  - Dynamically inspects player chests and mod block registries for Macaw's mods (`mcwdoors`, `mcwroofs`, `mcwwindows`, `mcwlights`, `mcwfurnitures`).
  - Automatically harmonizes wood types (Spruce, Birch, Acacia, Dark Oak, Mangrove, Cherry, Bamboo) across roofs, doors, windows, and furniture.
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
