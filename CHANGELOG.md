# Changelog

All notable changes to the **Gemini AI Companion Bots** mod are documented in this file.

---

## [1.2.2] - 2026-10-25

### 🌊 Water Buoyancy, Anti-Drowning Reflex & Safe Respawn Placement
- **Anti-Drowning Buoyancy & Swimming Instincts:**
  - Resolved an issue where companions standing in streams or water trenches (such as 2-tile-high waterways or farm irrigation canals) failed to tread water, submerged, and drowned repeatedly without moving.
  - Companions in water now actively set jumping and receive buoyant upward momentum whenever submerged or sinking (`Math.max(motion.y, 0.08)`), keeping their head above the surface so oxygen automatically replenishes.
  - Similar buoyant lift is applied in lava (`motion.y = 0.15`).
- **Emergency Self-Rescue (`escapeWaterToDryLand`):**
  - If a companion's head is submerged or air supply drops below 200 ticks (10 seconds), or when taking drowning damage, they immediately trigger an autonomous escape instinct.
  - Scans up to a 10-block radius for the closest solid, dry bank with clear head clearance and powers towards it with horizontal velocity and leap momentum, scrambling out of trenches and waterways back onto dry land.
- **Safe Dry Ground Respawning:**
  - Fixed an issue where dying in a hazard or stream caused the companion to revive in the exact same water block in an infinite death loop.
  - Implemented `findSafeDryRespawnPos`: scans outwards in concentric rings to locate solid, non-liquid ground with two clear air blocks above, strictly rejecting water, lava, fire, magma blocks, campfires, sweet berry bushes, wither roses, and powdered snow.
  - On death, companions reset their air supply to maximum, heal to full, clear fire, gain a 3-second invulnerability shield (`setInvulnerableTime(60)`), zero their momentum, and teleport to safe dry ground near the player, their guard/farm anchor, or world spawn.

### 🌾 Wheat-to-Hay Auto-Compaction
- **Space-Saving Hay Bale Crafting:**
  - Companions can now craft 9 wheat into 1 hay bale (`Items.HAY_BLOCK`) via recipe execution and smart verbal commands.
  - Added `compactWheatInInventory()`: loops through inventory and packs all multiples of 9 wheat into compact hay blocks.
  - Added persistent companion setting `autoCompactWheatToHay` (enabled by default for farmers). Whenever a farmer holds 9+ wheat, they compress it before inventory or chests can overflow.
  - Hay blocks are now recognized as farm produce in `getFarmerDepositableCropCount()` and `depositFarmerCropsIntoChest()`, ensuring hay bales are neatly stored in chests.
- **Verbal Command Recognition:**
  - Companions recognize natural language phrases: `"combine the wheat into hay"`, `"combine wheat into hay to save space"`, `"craft wheat into hay"`, `"turn wheat into hay"`, `"compact wheat"`, and `"make hay"`.
  - Companions instantly craft any wheat on hand into hay blocks, enable auto-compaction, and confirm in chat:
    > *"Understood! I will combine wheat into hay bales to save inventory and chest space. Crafted 4x hay bales right now!"*

### ♻️ Composter Integration & Seed Inventory Limits
- **Active Composting Engine (`compostExcessSeeds`):**
  - Companions now utilize vanilla Composters (`Blocks.COMPOSTER`) to convert surplus seeds into valuable bone meal!
  - `compostExcessSeeds`: uses `ComposterBlock.insertItem` to fill composters and automatically invokes `ComposterBlock.extractProduce` to harvest and vacuum up bone meal whenever the composter reaches readiness level 8.
- **Configurable Seed Caps (Hand & Chest):**
  - Configurable persistent thresholds:
    - `maxSeedsToKeepOnHand`: Preserves a set number of seeds on hand (default: 32) so the companion always has enough to replant harvested crops.
    - `maxSeedsInChest`: Caps the number of seeds deposited into chests (default: 64) so storage chests are never cluttered with endless seed stacks.
  - When depositing into chests, the companion only deposits seeds up to `maxSeedsInChest`. Any remaining seeds above `maxSeedsToKeepOnHand` stay in inventory and are routed to the nearest composter.
  - In `tickPassiveFarmer`, once chests are stocked, the farmer paths to any nearby composter within 24m and deposits all excess seeds.
- **Natural Language Parsing & Role Routing:**
  - Companions recognize: `"only keep so many seeds in the chest and on hand, putting the rest in compost"`, `"compost excess seeds"`, `"put seeds in compost"`, and specific limits like `"keep 16 seeds on hand and 64 in the chest"`.
  - Chat targeting automatically selects the nearest farmer companion if the player speaks about crops, wheat, hay, seeds, or compost without addressing the companion by name.
- **Status Dashboard (`/bot status`):**
  - Updated status display to show: `AutoHay: true | SeedsOnHand: 32 | ChestSeeds: 64 | CompostExcess: true`.

---

## [1.2.1] - 2026-10-25

### 🚂 Train Dismount & Anti-Reboard Loop Fix
- **Eliminated Immediate Reboarding Loop:**
  - Fixed an issue where asking a companion or non-conductor bot to dismount or get out of a train caused them to step out for a split second and immediately reboard with the message *"Boarded the locomotive! Conductor ready on the rails."*
  - Implemented `dismountFromTrain()`:
    - Stops the locomotive and executes a vehicle dismount.
    - Fully clears all active locomotive orders and follow targets (`conductorFollowingPlayer`, `conductorDestStation`, `conductorReturnStation`, `conductorTileTarget`, `conductorMoveStartPos`, and current action queue).
    - Restores the companion's prior role (e.g. companion, guard, farmer) if they boarded temporarily.
    - Applies a 5-second reboarding immunity cooldown (`trainBoardCooldownTicks = 100`) to prevent any accidental auto-mounting.
    - Offsets the companion 1.2 blocks clear of the train tracks so they are not trapped inside the train's collision volume.
  - Restricted `tickPassiveConductor` auto-boarding logic so it strictly applies only to companions currently assigned to the active conductor role with pending rail orders. Non-conductor bots will never auto-board locomotives.
- **Expanded Natural Dismount Phrases:**
  - Companions now recognize: `"get out"`, `"get out of the train"`, `"get off"`, `"get off the train"`, `"dismount"`, `"exit"`, `"exit the train"`, `"leave"`, `"leave the train"`, `"hop off"`, `"hop out"`, `"step out"`, and `"jump out"`.
  - Added dedicated vehicle dismounting support to `"STOP"` and `"DISMOUNT"` action plans.

### 🏷️ Interactive Nickname Assignment & Slash Commands
- **Natural Language Nickname Recognition:**
  - Fixed an issue where companions did not respond to nickname assignment requests in chat.
  - Expanded verbal parsing in `extractAndLearnNicknameFromPrompt` to handle:
    - `"assign nickname [to|is|:] <name>"`
    - `"set nickname [to|is|:] <name>"`
    - `"give you the nickname <name>"`
    - `"your nickname is <name>"`
    - `"nickname: <name>"`
    - `"rename to <name>"` / `"change name to <name>"`
    - `"call yourself <name>"` / `"name you <name>"`
- **Instant Verbal Confirmation & Nearest-Bot Target Fallback:**
  - Companions immediately confirm nickname changes in chat: *"Nickname set to '<name>'! I will now respond to this name."*
  - If a player gives a nickname command without addressing a specific bot by name, chat routing automatically selects the nearest companion to the player.
- **New In-Game Commands:**
  - `/bot nickname <new_nickname>` — Assigns a nickname to the nearest companion.
  - `/bot nickname <botName> <new_nickname>` — Assigns a nickname to a specific companion.
  - `/bot rename <new_nickname>` — Alias for `/bot nickname`.
  - `/bot rename <botName> <new_nickname>` — Alias for `/bot nickname`.

### 🌾 Surveyor Wand Mapped Area Farming & Multi-Chest Logistics
- **Whole Mapped Area Farming:**
  - Fixed an issue where companions asked to farm a Surveyor Wand mapped area (e.g. $149\text{ m}^2$ plots) only worked a small 5-block radius and ignored the rest of the zone.
  - Added `configureFarmForSavedArea(SavedArea area)`: when ordered to *"farm it"*, *"farm this area"*, *"farm the area"*, or *"farm <areaName>"*, companions detect the mapped area (by name, standing inside it, or nearest within range), adopt its exact perimeter bounds, and announce:
    > *"Farm duty set to mapped area '<Name>' (~149 m²)! I will systematically tend, harvest, replant, and deposit crops across the entire mapped area."*
  - Full-polygon iteration: `findNearestMatureCrop`, `findNearestBareFarmland`, and `findNearestTillableDirtInFarmBox` now scan the entire 3D bounding box of the `SavedArea`, ensuring 100% of the mapped zone is farmed, harvested, and replanted.
  - `tickFarmAction` no longer terminates prematurely after a fixed crop count when working a mapped zone.
- **Multi-Chest Storage Logistics:**
  - Companions now detect single chests, double chests (`ChestBlock.getContainer`), and barrels within 24 blocks.
  - When storing crops, `findNearestAvailableContainerBlock` actively skips full containers with no available slots.
  - If chest #1 reaches capacity during deposit, the companion automatically locates the next available container with free space and deposits remaining produce sequentially across multiple chests.
  - Alerts the player in chat if all nearby chests are full: *"My bags are full of crops and all nearby chests are full! Please place an empty chest."*
- **Perimeter Boundary Leeway:**
  - Allowed container access within 24 blocks of the farm perimeter so perimeter chests placed outside boundary fences can be accessed without triggering perimeter turnbacks.

---

## [1.2.0] - 2026-10-24

### 🧠 New Feature: Intelligent Stuck Detection, Area Analysis & Dynamic Obstacle Bypass
- **Eliminated Erroneous 1-Block Drop Bridging:**
  - Fixed a critical flaw in `isDeepPitAt` where any single 1-block drop down was erroneously evaluated as a "deep pit", causing companions to place dirt blocks to walk flat instead of simply walking/stepping down.
  - Drops of 1, 2, or 3 blocks to solid ground deal zero fall damage in Minecraft and are now recognized as 100% safe to walk and step down.
  - Pits and bridging are now strictly reserved for genuine 4+ block sheer drops (ravines, cliffs, void) or hazardous lava/fire.
- **Descending Pathing Toward Lower Targets:**
  - If the player or destination is at a lower elevation than the companion (`target.y < this.getY() - 0.2`, e.g. player is at Y=101 and companion jumped onto a block at Y=102), the companion will **never** bridge across the sky. Instead, they smoothly jump/step down off the block directly to the player.
  - Ledge jump-ups are only triggered when the target is at or above the bot's elevation, or if physically colliding with an unavoidable block.
- **Goal-Distance Stagnation Tracking:**
  - Companions actively measure progress toward their goal (`bestMoveTargetDistSq` and `moveNoProgressTicks`).
  - If a companion is unable to reduce its distance to its target over 16 ticks (~0.8s) due to walls, fences, trenches, cliffs, or obstacles, it recognizes that it is stuck and immediately executes an intelligent area analysis plan.
- **Immediate Environmental Analysis (`analyzeAndExecuteUnstuckPlan`):**
  - **Hole & Trench Trap Analysis:**
    - Accurately checks surrounding solid blocks at chest/head level on 3+ sides.
    - Probes a $5 \times 5$ perimeter for natural exit ramps or steps within 2 blocks (`findNearbyExitStep`). If found, sets an intermediate waypoint to walk up and out.
    - If trapped in a sheer vertical pit ($\ge 2$ blocks deep) with no ramp, checks ceiling clearance, equips the proper tool to destroy overhead obstructions, and executes a pillar jump (`executeHoleEscape()`).
  - **Wall / Building / Cliff Bypass Probing:**
    - When blocked by a solid wall or obstacle in front, the companion casts lateral rays to the left (`getCounterClockWise()`) and right (`getClockWise()`) up to 8 blocks away.
    - Finds the nearest standable clearance leading around the obstacle and sets an intermediate **`activeBypassWaypoint`**.
    - If facing low 1–2 block barriers (fences or low walls) with no lateral opening within 8 blocks, the bot cleanly pillars over the barrier.
    - If facing a massive high wall in a dead end, backs up and disengages.
  - **Chasm / Deep Pit Rim Search:**
    - When facing a wide drop-off ahead with no bridging materials, companions probe left and right along the rim to find contiguous ground or safe natural crossings.
- **Seamless Intermediate Waypoint Routing:**
  - `moveTowards` navigates directly toward `activeBypassWaypoint` until reached (within 1.2 blocks) or timed out, then seamlessly resumes direct pathing to the original target.
- **Eliminated Wall-Pillaring Bug:**
  - Removed old logic that caused companions to place dirt pillars under their feet whenever they walked up to any building wall or stood in a 90-degree room corner.

### 🎭 New Feature: Custom Skin Folder & Role-Themed Auto-Assignment
- **Drop-In Skins Folder (`/aicompanion/skins`):**
  - Simply drop standard **64×64** or **64×32** `.png` Minecraft skins into the `aicompanion/skins` folder (in your game instance directory or config folder)!
  - Auto-initializes the directory and generates a guide on first launch.
- **Role Subfolder & Filename Tagging:**
  - Organize skins into role subdirectories (e.g. `/guard/`, `/builder/`, `/lumberjack/`, `/rancher/`, `/miner/`, `/farmer/`, `/conductor/`) or name them with role prefixes (e.g. `guard_knight.png`).
  - When companions spawn with a role, they automatically select a matching skin from that role's category, rotating across variations so each worker/guard looks distinct!
- **Exact Name Match & Fallback Pool:**
  - Name a skin file after a companion (e.g. `gemini.png` or `bob.png`) for instant 1:1 skin assignment.
  - Any general or unassigned skins in the folder form an automatic fallback pool for companions.
- **Dual Layering & Slim (Alex 3px) / Wide (Steve 4px) Support:**
  - Full dual-layer rendering: outer accessory layers (hardhats, leather satchels, toolbelts, chaps, cowgirl braids, fleece vests) render in high fidelity.
  - Automatically detects Alex 3px slim arm models via alpha transparency at `(55, 20)` or by naming files with `_slim` / `alex`.
- **In-Game Skin Commands:**
  - `/bot skin list` — Lists all detected skins in the folder along with their detected model types.
  - `/bot skin reload` — Hot-reloads all skin textures from disk in real-time without restarting Minecraft.
  - `/bot skin set <botName> <skinName>` — Assigns a specific skin to a companion and persists across world restarts.

### ⛏️ New Feature: Interactive Verbal Hole Digging & Systematic Excavation (`EXCAVATE`)
- **Natural Conversational Follow-Up:**
  - Tell your companion *"dig a hole"*, *"dig a hole here"*, *"excavate here"*, or *"dig here"*.
  - The companion marks the exact ground block you are looking at (or standing on) with emerald villager and flame particles, walks over to the spot, and asks in chat:
    > *"Marked starting point at (X, Y, Z)! How big and how deep should I dig from there? (e.g. '5x5 and 4 deep' or 'width 4 depth 6')"*
  - Reply naturally without needing to repeat the bot's name (e.g., `"5x5 and 4 deep"`, `"4 by 4, 6 deep"`, `"width 4 depth 8"`, `"5x5"`, or `"4 deep"`).
  - Say `"cancel"` to abort or `"start here"` to update the anchor point if you reposition.
- **One-Shot Dimension Commands:**
  - If dimensions are already included in your initial prompt (e.g. *"dig a 5x5 hole 4 deep here"*), the companion immediately locks onto the spot and begins digging.
- **Systematic Serpentine Layer-by-Layer Excavation:**
  - Companions dig the volume systematically from the surface level down layer-by-layer (`dy = 0` down to `-(depth - 1)`).
  - Clears surface obstruction/foliage, automatically equips the optimal tool (shovel for dirt/gravel/sand, pickaxe for stone/ores), breaks blocks with true drops, and vacuums all excavated items directly into the companion's inventory.
  - Temporarily bypasses pit-bridging and self-rescue pillar jumps so companions never fill in the hole they are actively excavating.

### 🎯 New Feature: Radius-Constrained Block Type Mining
- **Anchored Area Mining Commands:**
  - Commands such as *"mine all the stone in this 10 block area"*, *"mine all coal within 15 blocks"*, *"clear all the dirt in this 8 block area"*, or *"mine all the stone here"*.
  - Locks the mining zone to the player's anchor point with the specified block radius.
- **Top-Down & Exposed-Block Prioritization:**
  - Companions no longer dig random holes or wander off into dark caves.
  - Mining systematically prioritizes **exposed surface blocks** and works from the **highest Y level down**, smoothly peeling blocks layer-by-layer.
  - Distinguishes natural terrain (stone, deepslate, andesite, diorite, granite, tuff, dirt, ores) from player-placed building blocks (stone bricks, polished variants, masonry).

### ⚔️ Enhanced Mob Targeting, Line-of-Sight & Anti-Spin Pathfinding
- **Eliminated Underground Spinning:**
  - Fixed issue where companions stood on the surface spinning in circles trying to reach unreachable mobs deep underground in caves.
  - Clamped vertical target search to $\pm 6$ blocks and requires direct line of sight or valid navigation pathing.
- **Reverse Mob A\* Pathfinding:**
  - If a monster is around a corner or behind cover, companions query the mob's own vanilla navigation path in reverse to cleanly navigate around obstacles and corners.
- **Target Unreachable Timeout & Blacklist:**
  - If a companion cannot reach a combat target within 4 seconds (80 ticks), they disengage and place the entity on a temporary 10-second blacklist so they don't get stuck indefinitely.

### 🌟 Fix: Shader Bloom on Companion Name Tags
- **Iris & Complementary Shaders Bloom Fix:**
  - Fixed an issue in Minecraft 26.3 where Iris + Complementary Shaders caused companion name tags to bloom into blinding solid white glowing rectangles.
  - Added a client render event hook (`RenderNameTagEvent.DoRender`) to route nametag rendering through standard non-emissive buffers, matching human player nametags perfectly.

### 📐 Subterranean Pit Support & Explicit Height Ranges for Saved Areas
- **Negative Height Pit Excavation:**
  - `/bot area height <name> <height>` (and `/bot area cube`) now accepts negative values (down to `-384`) to represent subterranean pits and underground quarries.
- **Two-Number Explicit Y Ranges:**
  - Added syntax `/bot area height <name> <minY> <maxY>` to define cuboid volumes between two explicit world Y levels.

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
