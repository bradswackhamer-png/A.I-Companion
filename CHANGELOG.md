# Changelog

All notable changes to the **Gemini AI Companion Bots** mod are documented in this file.

---

## [1.2.20] - 2026-10-29

### Fixed
- **Doorway & Fence Gate Navigation & Shoulder Collision:**
  - Resolved an issue where companion bots became stuck or wedged in doorways, open fence gates, and 1-block chokepoints and hopped continuously in place.
  - **Strict Lateral-Only Alignment**: Replaced bidirectional center attractor spring forces with pure lateral alignment along the doorway cross-axis. Centering forces now only align the bot to the doorway centerline without applying backward drag or halting forward walking momentum.
  - **Passage Axis Corridor Alignment**: When entering, standing inside, or clearing a doorway or fence gate, bots now orient and walk strictly parallel to the opening's orientation (getDoorwayPassageAxis) rather than turning prematurely toward off-axis/diagonal targets. This preserves their narrow 0.6-block profile through the 0.8125-block door clearance and prevents shoulder clipping against doorframes and fence posts.
  - **Doorway Jump Suppression**: Jumping is now strictly suppressed while the bot is inside or traversing doorway and fence gate bounding boxes, preventing head collisions with door lintels and repetitive jumping against 1.5-block high fence collision boundaries.
  - **Straight-Through Unstuck Bypass Waypoints**: Unstuck routine now projects bypass waypoints 2 blocks straight through the opening along the passage corridor rather than at the bot's angled facing yaw, preventing waypoints from landing inside adjacent solid walls or fence posts.
  - **Auto-Close Clearance Buffer**: Adjusted doorway and gate auto-close logic so doors and fence gates do not swing shut until the bot is completely clear of the passage threshold.

## [1.2.19] - 2026-10-29

### Added
- **Shared Hive-Mind Neural Network & Reinforcement Learning (`HiveMindBrain`):**
  - Implemented a pure-Java Multi-Layer Perceptron (`24 sensory inputs -> 32 hidden -> 16 hidden -> 9 micro-steering actions`) with `LeakyReLU` activations, `tanh` Q-outputs, Eligibility Traces (`γ = 0.92`), Experience Replay, and online backpropagation shared across all companion bots on the server.
  - Added a persistent **3D Spatial Pheromone & Waypoint Graph** (`DIMENSION_NAV_GRAPHS`) that records successful block traversals, directed neighbor edges, jump requirements, and hazard/stuck zones across dimensions.
  - Added a **Modded Block Walkability Knowledge Base** (`BLOCK_KNOWLEDGE`) that dynamically learns `walkabilityScore`, `centerAlignmentNeed` (shoulder-centering for narrow 1-block stairs/bridges), and `jumpAssistNeed` per Block Registry ID (such as Macaw's stairs and modded furniture).
  - Added a **Role & Task Preference Q-Table** (`TASK_Q_VALUES`) that tracks reinforcement scores for `role:actionType` pairs and injects top learned preferences and Hive-Mind IQ telemetry into Gemini AI prompts.
- **Player Verbal & Command Reinforcement (`/bot reward` & `/bot punish`):**
  - Players can praise or correct bots naturally in chat (*"good job"*, *"good bot"*, *"well done"*, *"nice pathing"* vs. *"bad job"*, *"bad bot"*, *"wrong way"*, *"bad path"*, *"stop getting stuck"*) or via `/bot reward [name|all] [amount]` and `/bot punish [name|all] [amount]`.
  - Positive feedback propagates `+15.0` reward backward through the bot's recent 60-step eligibility trace, strengthening neural weights, boosting spatial trail pheromones, and playing villager celebration sounds + happy villager particles.
  - Negative feedback propagates `-15.0` punishment across the recent trajectory, marking stuck coordinates as hazard zones, triggering immediate lateral rerouting, and playing villager headshake feedback + angry villager particles.
- **Command-Enabled Player Footstep Imitation (`/bot learnpath`):**
  - Added `/bot learnpath [start|stop]` (and natural chat triggers *"learn this path"*, *"watch how I walk"*, *"stop learning path"*) allowing players to demonstrate tricky routes, stairs, and jumps. Recorded footsteps are stored as golden breadcrumb nodes (`+35.0` score) in the shared Hive-Mind graph for all bots to follow.
- **Hive-Mind Diagnostics & 3D Trail Visualizer (`/bot brain`):**
  - Added `/bot brain [stats|show|reset]` to inspect Hive-Mind IQ, training updates, learned modded block walkability, and top Q-value tasks, or render 3D particles in-world (`END_ROD` for player-taught golden paths, `HAPPY_VILLAGER` for learned trails, and `FLAME` for avoided stuck/hazard zones).

## [1.2.18] - 2026-10-28

### Fixed
- **Emerald Farming Exploit Protection:**
  - Removed initial starter emeralds from the Villager role starter kit (`applyRoleStarterKit`).
  - Newly spawned, converted, or bred Villager companions now start only with food and farming supplies (bread and wheat) and zero emeralds, preventing players from repeatedly spawning bots to harvest free emeralds.

### Changed
- **Documentation & Quick Start Guide:**
  - Added a high-contrast, prominent Quick Start Guide to `CURSEFORGE.md`, `CURSEFORGE_PAGE.md`, and `README.md` highlighting the `/bot spawn <name> [role]` command.
  - Documented the baked-in default AI model (`gemini-3.5-flash-lite`) with copy-paste syntax for `/bot model gemini-3.5-flash-lite`.

## [1.2.17] - 2026-10-28

### Added
- **Global Squad Command Suite (`/bot all`):**
  - Added dedicated `/bot all` commands for fast management of all active companion bots at once:
    - `/bot all remove`: Despawns and clears all active AI companion bots across the server in a single command, updating persistent storage.
    - `/bot all come` / `/bot all come here` / `/bot all follow`: Commands all active companions to immediately rush to and follow the executing player in Absolute Follow mode.
    - `/bot all stop`: Places all active companions into immediate Absolute Standby Mode, clearing lingering actions.
    - `/bot all stay` / `/bot all standby` / `/bot all hold`: Tethers all active companions to hold position at their current locations within a 4-block perimeter.
    - `/bot all sentry`: Sets all active companions into stationary 360° Sentry Sniper mode.
    - `/bot all resume` / `/bot all dismiss`: Releases tethers and orders all active companions to resume their normal autonomous role duties.
    - `/bot all shelter [true|false]`: Toggles or sets night shelter and bed-seeking behaviors for all companions simultaneously.
    - `/bot all tp`: Teleports all active companions directly to the player's location.
- **Authentic Vanilla Merchant Trading GUI (`MerchantMenu`):**
  - Sneak right-clicking (Shift + Right-Click) any adult companion in the `"villager"` role now opens the authentic vanilla Minecraft Merchant trade interface.
  - Generates full authentic tiered trade offers matching the companion's claimed profession trait (Farmer, Librarian, Armorer, Weaponsmith, Toolsmith, Cleric, Fletcher, Butcher, Fisherman, Shepherd, Mason, Cartographer, Leatherworker, or Unemployed/Wandering).
  - Baby villager companions reject trades with a villager headshake (`VILLAGER_NO`) and remind the player they must grow up before trading.
- **Villager Merchant Leveling & Progression System:**
  - Companions earn trading experience (`villagerXp`) with every completed trade in the trader GUI.
  - Progression spans all 5 authentic vanilla tiers:
    - **Level 1 (Novice):** 0 XP
    - **Level 2 (Apprentice):** 10 XP
    - **Level 3 (Journeyman):** 70 XP
    - **Level 4 (Expert):** 150 XP
    - **Level 5 (Master):** 250 XP
  - Leveling up triggers `PLAYER_LEVELUP` and `VILLAGER_YES` sound effects, happy villager particle sparkles, announces promotion in chat, unlocks the next tier of trades, and dynamically updates the player's active merchant menu.
  - Trading level (`villagerLevel`) and experience points (`villagerXp`) are saved to and loaded from `BotPersistence`.

## [1.2.16] - 2026-10-28

### Added
- **Workstation Searching & Profession Sub-Role Assignment:**
  - Adult companions in the `"villager"` role now search in a 48-block radius for unclaimed villager workstations/work tables (like vanilla villagers).
  - Upon discovering an unclaimed work table, the companion claims the block position and adopts that table's villager trait as an authentic sub-role under their Villager role (e.g. `Role: Villager`, `Sub-Role: Librarian`).
  - Supported 13 Workstation Professions:
    - `Composter` -> **Farmer**
    - `Lectern` -> **Librarian**
    - `Blast Furnace` -> **Armorer**
    - `Smoker` -> **Butcher**
    - `Cartography Table` -> **Cartographer**
    - `Brewing Stand` -> **Cleric**
    - `Fletching Table` -> **Fletcher**
    - `Cauldron` -> **Leatherworker**
    - `Loom` -> **Shepherd**
    - `Smithing Table` -> **Weaponsmith**
    - `Stonecutter` -> **Mason**
    - `Grindstone` -> **Toolsmith**
    - `Barrel` -> **Fisherman**
- **Single Workstation Claim Enforcement:**
  - Companions verify that a workstation is not already claimed by another companion before taking it, ensuring each companion has their own unique job site table.
- **Dynamic Workstation Loss & Re-Employment:**
  - If a companion's work table is broken or replaced, they emit a sad villager sigh (`VILLAGER_NO`), reset their sub-role to Unemployed, announce the loss in chat, and resume searching for an available work table.
- **Authentic Work Hours & Handheld Profession Items:**
  - During work hours (2000-9000 ticks), companions walk to their claimed work table, face it, hold their profession item (Librarian holds a Book, Weaponsmith holds an Iron Sword, Fletcher holds a Bow, Farmer holds Wheat, Mason holds Stone Bricks, etc.), and play profession-specific ambient work audio with green happy sparkles.
- **Profession-Specific Emerald Trading:**
  - Right-clicking a villager companion with an Emerald now rewards trades authentic to their profession (e.g. Librarian offers Books, Armorer offers Iron Ingots, Fletcher offers Arrows, Cleric offers Redstone, Weaponsmith offers Iron Swords).
- **Commands & Chat Integration:**
  - Added `/bot profession <name> [trait|clear|search]` and `/bot workstation <name> [assign|clear|search]`.
  - Companions respond to conversational requests (*"search for a work table"*, *"claim this table"*, *"become a librarian"*, *"clear workstation"*).
  - `/bot status` and the companion GUI dashboard now display `Role: Villager (Sub-Role: Librarian)` and the assigned workstation coordinates.
- **Full Persistence:**
  - Sub-roles and assigned workstation block coordinates are fully serialized and restored via `BotPersistence`.

---

## [1.2.15] - 2026-10-28

### Added
- **Speaker Crosshair & Line-of-Sight Raycast Perception:**
  - Companions and the Gemini LLM engine now track exact entities in the player/speaker's crosshairs with raycast collision detection up to 48 blocks away.
  - Crosshair targets are explicitly highlighted in the telemetry prompt (`>>> SPEAKER DIRECT CROSSHAIR TARGET`), informing the companion of the player's exact visual focus.
- **Speaker Front Vision Cone Detection:**
  - Companions now detect and catalog all entities situated in front of the speaker within a wide ~140-degree front vision cone up to 40 blocks away.
  - When a player says *"zombie in front of me"*, *"watch out ahead"*, or points out a mob, the bot's perception instantly confirms the mob's presence, location, and distance.
- **Relative Cardinal Directions & Spatial Awareness:**
  - Nearby entities in telemetry are enriched with cardinal directions (North, South, East, West, North-East, etc.) relative to the companion and relative tags to the player (`[IN FRONT OF SPEAKER]`, `[BEHIND SPEAKER]`, `[VISIBLE TO BOT]`).
- **Player Crosshair Priority Targeting:**
  - When a companion receives an attack or hunt command, it first checks if the commanding player is looking directly at a matching mob in their crosshairs or front vision cone, immediately locking onto that target.
- **Enhanced Offline Combat & Threat Intent Recognition:**
  - The offline rule-based parser now understands conversational mob alerts (*"zombie in front of me"*, *"there's a zombie ahead"*, *"kill that zombie"*, *"can you see that zombie?"*), immediately triggering attack and bodyguard engagement.

### Changed
- **Expanded Perception & Surroundings Radius:**
  - Entity scan volume expanded from 24m horizontal / 12m vertical to **48m horizontal and 28m vertical**, scanning both around the bot and around the speaker.
- **Bodyguard Auto-Defend Engagement Range:**
  - Increased melee bodyguard threat engagement radius around the guarded player from 8.0m to **20.0m** (and up to 36.0m for archers).
- **Proactive Threat Assessment:**
  - Hostile monsters within 14 blocks of the guarded player are now classified as imminent threats even before the mob's vanilla target acquisition delay completes.

### Fixed
- **"I can't see any zombies" Blind Spot:**
  - Fixed an issue where companions claimed they could not see zombies or mobs situated 10 blocks away in front of the player due to limited 24m/12m bot-centric bounding boxes and missing player view telemetry.
- **Vertical Elevation Blindness (`vertDiff > 6.0`):**
  - Fixed `findNearestTargetMob`, `findNearestMonsterNear`, and `findNearestProvokedThreatNear` whose vertical search box was restricted to only 6.0 blocks, which caused mobs on stairs, terraces, hills, or in ravines to be completely ignored.
- **Reachable Mob Validation Failure (`isMobReachableAndValid`):**
  - Removed flawed mob-to-bot path navigation checks that caused bots to reject targets whenever the mob could not pathfind through closed gates or railings.
- **Fence Line-of-Sight Rejection in Combat:**
  - Removed strict fence blockage rejection from `findNearestTargetMob`, allowing companions to engage and approach mobs across fences, railings, or through gates when ordered to fight.

---

## [1.2.14] - 2026-10-28

### Added
- **Baby Bot Villagers & Authentic Colony Life Cycle:**
  - **Newborn Baby Villagers**: All newly spawned companion villagers (from villager breeding or spawned with the `"villager"` role) start out as adorable baby bot villagers!
  - **Vanilla Attribute Scaling (`Attributes.SCALE`)**: Baby bots feature proportional scaling via vanilla Minecraft's `generic.scale` attribute set to `0.55`, shrinking model size, eye height, reach, and collision boxes to true baby proportions.
  - **Playful Daytime Routine**: Baby villagers do not work adult workstations; during work hours (2000-9000 ticks) they play tag with nearby companions, joyfully jump on nearby beds with wool bounce audio, and emit ambient baby villager chirps at higher pitch (`1.35f`).
  - **Natural Aging into Adulthood**: Baby companions age over 20 minutes (24000 ticks, 1 in-game day) just like vanilla baby villagers. Upon maturing, they emit `HAPPY_VILLAGER` sparkles, play level-up sounds, scale back to adult size (1.0), announce their coming of age in chat, and begin taking on adult villager duties.
  - **Feeding & Growth Acceleration**: Right-clicking a baby bot with Wheat, Bread, Carrots, or Potatoes feeds them, emitting sparkles and accelerating their growth by 10% (2400 ticks / 2 minutes) per feeding.
  - **Baby Commands & Status**: Added `/bot baby <name> [true|false]` and chat commands (*"make <name> a baby"*, *"<name> grow up"*). `/bot status` displays the companion's developmental stage and remaining minutes until adulthood.
  - **Trading & Breeding Safeguards**: Baby villagers cannot trade emeralds or breed until reaching adulthood, politely declining emerald trades with high-pitched audio.
  - **Full Persistence**: Baby state and remaining age ticks are fully saved and restored across world reloads via `BotPersistence`.

---

## [1.2.13] - 2026-10-28

### Added
- **Comprehensive Stair, Railing & Balustrade Recognition (`isStairBlock`, `isRailingOrBalustrade`, `isStairOrPlatform`):**
  - Built-in recognition for all vanilla and modded stairs (Macaw's Stairs including Loft Stairs, Compact Stairs, Bulk Stairs, Terrace Stairs, Skyline Stairs, platforms, and balconies).
  - Explicit recognition for stair railings, balustrades, balconies, handrails, banisters, and guardrails across modded stairs and architectural mods.
- **1-Block Wide Stair Corridor Alignment & Shoulder Centering:**
  - Added dynamic corridor alignment and shoulder centering when navigating staircases. When traversing a 1-block wide staircase flanked by railings, walls, or balustrades, companions automatically align and steer along the exact centerline of the stair axis, preventing their 0.6-block wide player collision box from snagging on tall railing collision boundaries.
- **Enhanced Step-Up Capacity & Smooth Ascension Momentum:**
  - Configured companion `Attributes.STEP_HEIGHT` to `1.06` blocks, allowing bots to natively step up 1-block elevation changes, complex stair profiles, and modded stair lips without stutter.
  - Added gentle upward momentum assist (`0.28` vertical boost) when ascending stairs under horizontal pressure, guaranteeing fluid, continuous stair ascension even on steep or compact stairs.

### Fixed
- **Macaw's Stairs & Modded Staircase Navigation & Pathing:**
  - Resolved an issue where companions were unable to pathfind or climb Macaw's Stairs (`mcw-stairs`) and modded stair blocks.
  - Fixed `isTallWall` and `stepUpPossible` checks that previously misidentified the next ascending stair step as an impassable 2-block high solid wall, which suppressed jumping and caused bots to walk directly into steps and halt.
  - Fixed `analyzeAndExecuteUnstuckPlan` treating stair steps as obstacles and attempting sideways detours into railings or retreating backwards down the stairs. The unstuck engine now recognizes active stair ascent and boosts the bot up and over the step.
- **Stair Railings Misclassified as Impenetrable Fence Barriers:**
  - Fixed `isFenceOrWall` treating stair railings and balustrades as perimeter fences, which previously caused companions to halt (`stopMovement()`) or abort to search for distant fence gates.
  - Fixed `isLineBlockedByFence` flagging false-positive line blockage when navigating stair corridors bordered by railings or banisters.
  - Fixed `isEnclosedInHole` and hole-escape pillar jumping falsely detecting stair corridors flanked by railings as trapped holes.

---

## [1.2.12] - 2026-10-28

### Added
- **"Villager" Role & 100% Vanilla Villager Simulation:**
  - Added new role `"villager"`. When set to this role, companions fully emulate vanilla villager behavior and schedules while retaining all companion capabilities (chat, memory, status, and role-switching).
  - **Daily Schedule & Workstations**: Villagers stroll around their home in early morning, work at assigned workstations during work hours (2000-9000 ticks) producing job site sounds (composter filling, page turning, blast furnace crackle, smithing hammering) and green sparkles (`HAPPY_VILLAGER`), gather at bells or with other companions in the afternoon (9000-11500 ticks) to socialize and gossip, and return home in the evening.
  - **Panic & Flee Reactions**: Villager companions flee in panic from hostile monsters (zombies, drowneds, illagers), sprint towards safety or their house, and ring nearby village bells to alert defenders.
  - **Emerald Trading**: Right-clicking a villager companion with Emeralds initiates authentic villager trading (1 emerald for 4 bread or items), accompanied by cheerful villager trade audio.
- **Housing & Bed Assignment (`/bot bed`, `/bot house`) with Nocturnal Sleep:**
  - **Bed Assignment**: Assign personal beds via `/bot bed assign <name> [pos]` or chat (*"this is your bed"*, *"assign bed"*, *"claim this bed"*). Companions remember their bed location across restarts.
  - **House Assignment**: Assign home houses via `/bot house assign <name> [areaName]` or chat (*"this is your house"*, *"set home"*, *"house Cottage"*).
  - **Nocturnal Shelter & Sleep**: Unless commanded otherwise, companions automatically pathfind to their bed and sleep until morning (`startSleeping` and wake at AM dawn). If no bed is found, they shelter inside their assigned house or covered roofing.
  - **Night Shelter Controls**: Toggle nocturnal sleep via `/bot shelter <name> [true|false]` or chat (*"night watch"*, *"stay awake"*, *"seek shelter at night"*).
- **Villager-Style Colony Breeding Logic:**
  - When hunger is good (food level >= 16 and holding breeding food), safe (no hostile threats within 16m, healthy), and total beds in a 100x100 block area exceed current bot population (`beds >= bots + 1`), companions can breed.
  - Consumes 3 raw wheat (or bread/carrots), emits heart particles (`HEART`), plays level-up and happy villager sounds, and spawns a newborn companion with a friendly random name and the `"villager"` role.
  - Newborn automatically claims the vacant bed in the colony.
- **Dynamic Role Management & Farm Commands:**
  - Added `/bot role <name> <role>` and chat command (*"you are now a [role]"*, *"change role to [role]"*) allowing seamless role switching back and forth at any time.
  - Added `/bot farm autohay <name> [true|false]` to toggle automatic wheat-to-hay compaction.

### Changed
- **Absolute Command Mode Gated by "i command":**
  - Absolute Command Mode is now strictly gated and only activated when a player explicitly prefaces their request with *"i command"* (e.g. *"i command you to follow me"*, *"i command stop"*, *"i command stay here"*) or uses `/bot command <name> <order>`.
  - Standard chat instructions without *"i command"* run with normal companion priority without locking into Absolute Mode.

### Fixed
- **Wheat-to-Hay Auto-Compaction & Negation Fix:**
  - Resolved an issue where asking a companion to *"stop combining the wheat into haybales"* triggered `autoCompactWheatToHay = true` and crafted all wheat into hay bales.
  - Added negative intent detection (*"stop"*, *"don't"*, *"keep wheat as wheat"*, *"need wheat as wheat to breed with"*), disabling auto-compaction so players can save raw wheat for breeding.
  - Added support for uncrafting existing hay bales back into 9x raw wheat upon player request (*"uncraft hay"*, *"turn hay back into wheat"*).
- **"stop something" Absolute Mode Hijack Fix:**
  - Fixed `cmdClean.startsWith("stop ")` in `BotManager` which previously intercepted any command starting with "stop " (e.g. *"stop combining wheat"*, *"stop mining"*) and put the companion into Absolute Standby Mode.
  - Standalone stop words (*"stop"*, *"halt"*, *"cancel"*) now stop current actions cleanly without entering Absolute Mode unless preceded by *"i command"*.

---

## [1.2.11] - 2026-10-26

### Fixed
- **Area Guarding vs Farming Role Hijacking Fix:**
  - Resolved an issue where ordering a companion to stand in, stay in, or guard a mapped zone with "farm" in its name (e.g. `Wheat_Farm`, `farm1`) triggered automatic farming duty and hijacked the companion's role to "farmer" against player orders.
  - **Intent Disambiguation for Mapped Areas**: When a registered area name is detected in player chat, the system now distinguishes between guarding/standing vs farming. If the message contains guard, defend, protect, watch, patrol, stand, stay, hold, or sentry directives, or contains negative farming phrases (*"do no farm it"*, *"not a farmer"*, *"don't farm"*), the companion strictly anchors as a guard, preserves their guard/warrior role, and will **never** farm.
  - **Area Name Keyword Sanitization**: `extractAndLearnFarmConfigFromPrompt` now strips all registered area names before checking for agricultural keywords. An area named `Wheat_Farm` or `tree_farm` will never falsely trigger agricultural farming just because the area's name contains `"farm"`.
  - Added comprehensive negative farming phrases (`"do no farm"`, `"don't farm"`, `"not farm"`, `"not a farmer"`, `"no farm"`, `"never farm"`).

---

## [1.2.10] - 2026-10-26

### Fixed
- **Fence Gate Traversal, Collision & Passing-Through Overhaul:**
  - Resolved an issue where companions became stuck attempting to pass through fence gates.
  - **Open Gate Classification**: Fixed `isFenceOrWall(state)` which previously classified open fence gates as solid impassable barriers because `oak_fence_gate` matched `"fence"`. Open gates and doorways are now recognized as clear walkthrough openings.
  - **Continuous Forward Momentum**: Fixed navigation resetting `target = gateCenter` every tick, which previously caused companions to navigate to the gate center and stop dead. Companions now apply shoulder centering nudges into the gateway opening while keeping their forward motion vector driving completely through to the other side.
  - **Directional Gate Swing**: When companions open a fence gate, the gate's `FACING` property is aligned with the companion's movement direction so the gate always swings open *away* from them, preventing the gate panel from swinging into their hitbox and physically blocking passage.
  - **Hitbox Safety Gate Auto-Close**: Fence gates and doors now check `!this.getBoundingBox().intersects(gateBox)` and require the companion to be 2.1m clear (`distSq > 4.5`) before auto-closing, ensuring gates never slam shut on the companion's body.
  - **Diagonal & Angled Approach Detection**: Expanded gate detection to all 4 horizontal adjacent blocks within 2.5m, ensuring companions approaching at angles smoothly detect, open, and pass through gates.
  - **Unstuck Gate Recovery**: Added dedicated fence gate recovery in `analyzeAndExecuteUnstuckPlan`: if a companion ever experiences forward resistance before a gate, they immediately open it and set their bypass waypoint straight through to the far side of the opening.

---

## [1.2.9] - 2026-10-26

### Added
- **Dedicated Sentry Mode (`/bot sentry [name]` & Chat Trigger):**
  - Added **Sentry Mode** allowing companions to be assigned as stationary sentinels on watchtowers, fortress walls, roofs, or base perimeters.
  - Activated via `/bot sentry [name]`, `/bot command <name> sentry`, or natural chat phrasing (`"sentry"`, `"sentry mode"`, `"stand sentry"`, `"guard sentry"`, `"sentry duty"`, `"sniper mode"`, `"be a sentry"`, `"hold sentry"`).
  - Sentry companions hold their exact post coordinates, continuously survey the surrounding area in a smooth 360° panoramic scan, and snipe approaching hostiles up to 48m away (56m if elevated) with pinpoint accuracy.
  - Sentry mode status and coordinates are preserved across server restarts and world reloads via `BotPersistence`.
- **Infinite Companion Archery Reserve & Extended Weapon Support:**
  - Companions equipped with a bow or crossbow maintain a continuous archery reserve and will never be forced to charge into melee to club enemies with a bow due to missing inventory arrows.
  - Compatible with vanilla bows, crossbows, and modded bows.

### Fixed
- **Fence Boundary Enforcement in Combat (No Fence Hopping or Gate Leaving):**
  - Resolved an issue where eye-level line-of-sight ray checks traveled above 1.5m tall fence collision boxes, failing to register fences and leading companions to hop fences or open fence gates to chase monsters outside.
  - Upgraded `isLineBlockedByFence` to evaluate 3D vertical columns across ground and foot elevations, accurately detecting fences, walls, and closed gates.
  - During combat, sentry duty, or standby, companions strictly treat fences as impenetrable boundaries and will never open gates or jump fences to pursue enemies in the wilderness.
- **Ranged Combat Over Fences (`canShootAtTarget`):**
  - Archers stationed behind fences or on walls can now detect and fire arrows in parabolic arcs over fence perimeters at hostile targets outside while remaining in complete safety behind the fence.
  - Melee-only guards hold their ground safely inside the fence; if a hostile approaches directly adjacent to the fence (within 3m), the guard attacks over the fence without opening the gate.
- **Archery Engagement Distance & Bow Clubbing Fix:**
  - Fixed an issue where archery distance was capped at 16 blocks (`distSq > 256.0`), causing companions holding bows to sprint across 30 blocks of wilderness towards skeletons and melee them with the bow frame.
  - Archery combat now operates effectively from 4.5m up to 48m (56m in sentry mode). Archers stand ground and shoot, kite backwards if enemies close within 4.5m, and incorporate advanced parabolic trajectory elevation arc compensation.
  - Auto-defend detection range for archers expanded up to 48m, ensuring companions immediately return fire when targeted by distant skeletons.

---

## [1.2.8] - 2026-10-26

### Fixed
- **Crude Oil & Hazardous Mod Fluid Avoidance (Lava-Grade Hazard Classification):**
  - Resolved an issue where companions treated modded crude oil pools and lakes (from Create: Diesel Generators, BuildCraft, Ad Astra, Tech Reborn, Thermal, PneumaticCraft, etc.) as safe, walkable terrain and stepped in, becoming trapped and suffocating or drowning.
  - Implemented dynamic hazardous fluid detection identifying all crude oil, fuel, petroleum, tar, diesel, acid, sludge, and toxic fluids across block and fluid registries.
  - Upgraded pit and hazard pathfinding (`isDeepPitAt`, `isPassable`, `isStandable`): companions now treat crude oil and hazardous fluids with identical severity to molten lava. Companions will never step into crude oil, walk across oil puddles, or select bypass waypoints in or over oil.
  - When encountering oil pools or lakes, the dynamic environment analysis engine navigates along the solid rim or detours cleanly around the perimeter.
  - Item vacuuming (`findNearestDrop`) strictly ignores items that fell into crude oil or lava, preventing companions from diving into oil pools to retrieve dropped items.

### Added
- **Emergency Crude Oil Self-Preservation & Land Escape:**
  - Added emergency buoyancy impulse (`escapeHazardousFluidToDryLand`): if a companion is pushed or knocked into crude oil, they immediately apply high vertical propulsion (`0.28` velocity), continuous swimming jumps, and navigate directly to the nearest solid bank within 12 blocks to climb out onto dry land.
  - Companions taking damage while submerged in crude oil or hazardous fluids prioritize immediate emergency shoreline escape.
  - Safe respawn scanning (`findSafeDryRespawnPos`) strictly rejects positions with or adjacent to crude oil.

---

## [1.2.7] - 2026-10-26

### Added
- **True Archery & Bow Shooting System:**
  - Companions equipped with bows and arrows now dynamically engage hostile targets at range (up to 25+ blocks) rather than sprinting into melee to swing the bow as a club.
  - Automatically draws bows with item animations (`startUsingItem`), holds a 20-tick charging cycle for full 1.0 velocity draw, applies ballistic trajectory elevation arc compensation, and shoots high-precision arrows.
  - Tactical range switching: if enemies close to within 4 blocks and the companion possesses a melee weapon (sword/axe/mace/trident), the companion seamlessly switches to melee and raises their shield; if the enemy retreats, the companion redraws the bow.
  - Tactically backpedals when enemies attempt to close into point-blank range while drawing the bow.
  - Plays authentic vanilla bow release audio (`SoundEvents.ARROW_SHOOT`) and consumes arrows from inventory (infinite in Creative mode).
- **Absolute Command Mode & Fast-Path Intent Interception:**
  - Introduced **Absolute Command Mode** ensuring player orders unconditionally override autonomous role duties (farming, logging, breeding, compound patrols, distant combat).
  - Fast-path chat commands executed immediately on tick 0 without LLM round-trip latency:
    - `"come here"`, `"follow me"`, `"to me"`, `"regroup"`: Instantly puts companion into Absolute Follow Mode, clearing background queues and following closely at 2.5m distance.
    - `"stop"`, `"halt"`, `"cancel"`, `"pause"`, `"freeze"`: Instantly halts all movement, clears current actions, cancels passive loops, and puts companion into Absolute Standby Mode.
    - `"stay here"`, `"stand here"`, `"hold position"`, `"camp here"`: Instantly tethers companion at their current coordinates with a 4m leash radius.
    - `"resume"`, `"continue"`, `"back to work"`, `"dismiss"`, `"free roam"`: Disengages Absolute Command Mode, clears tethers, and resumes normal autonomous role duties.
- **Tether Leash & Guard Post Perimeter (`/bot tether`, `/bot untether`, `/bot hold`):**
  - Added strict position tethering (`/bot tether <name> [radius]`, default 4 blocks).
  - Tethered companions strictly hold their post: if pushed or knocked back outside their tether radius, they immediately navigate back to the tether anchor.
  - In combat, tethered companions will never pursue enemies beyond their tether perimeter. If holding a bow and arrows, they will snipe distant targets from within their designated area; melee-only companions will disengage if the monster moves outside the tether boundary.
- **In-Game Direct Command Overrides:**
  - Added `/bot stop [name|all]`: Argumentless execution halts the nearest companion (or all active companions) into Absolute Standby.
  - Added `/bot follow [name]`: Commands nearest or named companion to follow caller closely in Absolute Mode.
  - Added `/bot hold [name]`: Tethers nearest or named companion at current coordinates with a 4m perimeter.
  - Added `/bot tether <name> [radius]` and `/bot untether <name>`.
  - Added `/bot command <name> <order>`: Direct command invocation.

### Changed
- **Strict Mob Threat Aggro Filter ("Leave Mobs Alone"):**
  - Completely reworked aggro detection so companions will **never** charge unprovoked, distant monsters minding their own business 20 blocks away.
  - Companions will only engage hostiles if:
    1. The monster is actively targeting the companion (`target == bot`).
    2. The monster recently damaged the companion (`lastHurtByMob == monster`).
    3. The monster is targeting or damaging the protected player.
    4. The monster is a primed Creeper actively swelling in close proximity (< 8 blocks).
    5. The player explicitly orders an attack (e.g. `"attack that zombie"`, `/bot command <name> attack <mob>`).
  - Ambient monsters wandering outside the perimeter are completely ignored, eliminating chain-aggro loops and companions running away from base.

---

## [1.2.6] - 2026-10-25

### Fixed
- **1x2 Doorway Navigation & Passage Fix:**
  - Fixed an issue where companions were unable to walk through 1x2 doorways when a door was present even if it was open.
  - Updated `isPassable` and `isStandable` to correctly classify open doors (`DoorBlock.OPEN == true`), open gates, and hand-openable doors as walkable passageways rather than solid, impenetrable walls.
  - Doorways are no longer misidentified as 2-block-tall solid walls (`isTallWall`), preventing the bot from refusing movement or triggering lateral exterior detour loops.
  - Implemented doorway axis alignment and shoulder micro-centering so the companion's 0.6-wide bounding box aligns with the center of the 1x2 aperture without snagging on the doorframe or door leaf.
  - Companions approaching closed wooden doors automatically open them and politely close them behind after passing through.

- **Strict Fence Boundaries & Animal Pen Protection:**
  - Resolved an issue where companions stuck against fences would destroy or replace fence blocks with dirt/building blocks and pillar-jump over them.
  - Fences and stone walls are now treated as strict, impenetrable boundaries: companions will **never** pillar-jump over them, place blocks next to or on top of them, or break them.
  - Hole escape pillaring is now strictly confined to true subterranean pits/trenches with solid 3-sided terrain, and explicitly refuses execution if standing on or next to fences or walls.
  - Building block placement (`placeBuildingBlockAt`) completely rejects placing blocks onto, adjacent to, or above fence and wall blocks.

### Changed
- **Intelligent Gate & Fence Opening Routing:**
  - When a companion's destination (such as cattle, sheep, crops, or players) is separated by a fence line, the companion automatically locates the nearest `FenceGateBlock` or existing opening in the fence perimeter within 24 blocks.
  - Companions route to the gate, open the gate to enter or leave, and automatically close the gate behind them once through so livestock cannot escape.
  - If a fenced pen has no gates or openings, the companion respects the perimeter and stands by cleanly without attempting to vault or damage the fence.

---

## [1.2.5] - 2026-10-25

### ⚡ Offline Mode & API Key Management
- **Force Offline Tactical Mode (`/bot offline [true|false]`):**
  - Added full support to force all companions into 100% Offline Tactical Mode at any time, bypassing cloud Gemini API calls and conserving API quotas even when an API key is present in configs or environment variables.
  - Added new configuration entry `forceOffline = true|false` in `config/aicompanion-common.toml`.
  - In-game toggle `/bot offline` or `/bot offline on|off` instantly switches between cloud personality and zero-latency local tactical execution.
- **In-Game API Key Removal (`/bot apikey clear [all]` / `/bot apikey remove [all]`):**
  - Added `/bot apikey clear` and `/bot apikey remove` allowing players to remove their personal Gemini API key from the server pool without manually opening configuration files.
  - Added `/bot apikey clear all` (requires Operator Level 2 / singleplayer) to purge all player keys and server default keys simultaneously.
  - Recognizes keywords (`clear`, `remove`, `reset`, `none`, `off`) directly via `/bot apikey <argument>` to prevent accidentally saving them as key strings.
- **Enhanced Status & Setup Guide (`/bot apikey`):**
  - Dynamically indicates current AI engine state (`Connected to Cloud Gemini`, `Forced Offline Mode`, or `Offline Tactical Mode`).
  - Lists quick commands for managing keys and toggling offline operation.

---

## [1.2.4] - 2026-10-25

### 🌲 Full Lumberjack Role & Tree Felling
- **Lumberjack Intent Decoupling:**
  - Resolved an issue where natural commands containing words like `"farm"` (e.g., *"farming all trees"* or *"your not 'farming' go chop all the trees down in your 100x100 area save the logs"*) were mistakenly captured by crop farming logic and assigned farm duty.
  - Crop farming now strictly ignores tree, wood, log, chop, timber, and lumber requests.
- **Single-Tree Felling (`"cut this tree down"` / `"chop this tree"`):**
  - Raycasts the exact block the player is looking at (if log or leaf), traces down to the root base, and fells the entire tree trunk and branches.
  - Auto-replants matching saplings (oak, birch, spruce, jungle, acacia, dark oak, cherry, mangrove, crimson/warped fungus) on dirt/grass.
  - Vacuums up dropped logs, sticks, apples, and saplings automatically.
- **Area Deforestation & Log Storing:**
  - Recognizes prompts such as *"note this spot, travel up to 100 blocks in each direction from here farming all trees, bring back logs"* or *"chop all the trees down in your 100x100 area save the logs"*.
  - Configures lumberjack duty with an origin anchor and custom radius (e.g. 100 blocks), continuously clears standing trees, and deposits all logs/wood into nearby chests.
  - Broadened log block matching so bots recognize all vanilla and modded wood types without failing when encountering non-oak trees.
- **Persistent Lumberjack Settings:**
  - Saves and restores lumberjack origin, radius, active area mode, and container deposit settings in `bot_data.json`.

### 🧭 Exploration & Native Biome Finder
- **True Biome Locating (`findClosestBiome3d`):**
  - Fixed an issue where explorer companions asked to find an ocean (*"explorer can you go find us the closest ocean"*) instantly reported "Arrived at destination!" due to empty GOTO targets.
  - Implemented `handleExplorationChatCommand`: searches `ServerLevel` up to 6,400 blocks away for oceans, deserts, mountains, jungles, swamps, badlands, cherry groves, and forests.
  - Reports exact compass direction and distance (e.g., *"Found an Ocean ~450 blocks away to the North-West at (-380, ~, 1420)! Starting journey there now."*), saves a named waypoint, and navigates there.

### ⛏️ Advanced Excavation (Starting Y & Upward Room Clearance)
- **Starting Y & Upwards Room Clearance:**
  - Supports prompts like *"goku dig a 10x10x2 box around us. starting y81 go up 2 tiles and 10 in each direction"*.
  - Explicitly parses starting Y coordinates (`starting y81`, `at y 81`, `y=81`).
  - Parses centering (`around us`, `around me`, `in each direction`, `centered`) to center the box around the player.
  - Parses upward room clearing (`go up 2 tiles`, `up 2`, `height 2`) to excavate layer-by-layer upwards from Y 81 instead of digging down into a pit.

### ⚔️ Distress Call Progress Counter Fix
- **Eliminated Instant Elimination Bug:**
  - Fixed a bug where a companion responding to a distress call immediately announced *"Finished eliminating 16x Skeleton!"* on tick 0 because `actionProgressCount` wasn't reset to 0.

### 📋 `/bot help` Command & Categorized Menu
- Added `/bot help` (and bare `/bot` / `/aibot` command execution) displaying a clean, comprehensive command reference and natural chat voice command examples.

---

## [1.2.3] - 2026-10-25

### 📖 Companion Training, Memory Books & Dynamic Recipe Learning
- **Trainable Custom Recipes & Dynamic Workflows:**
  - Companions can now be taught new crafting recipes and workflows dynamically through in-game chat, commands, or training books (e.g., *"place 9 wheat in the crafting table, 1 in each spot. Craft a hay bale"*).
  - Implemented `LearnedRecipe`: dynamically maps item outputs, counts, and ingredient ratios parsed directly from plain English instructions or books.
  - Integrated with `craftSingleItemSmart`: companions check personal and global learned recipes before standard recipes, autonomously crafting learned items on demand.
- **Physical Memory Books & Interactive Training:**
  - **In-Game Book Studying:** Right-clicking any companion while holding a Written Book (`Items.WRITTEN_BOOK`) or Book & Quill (`Items.WRITABLE_BOOK`) makes the bot study the book, parsing every page into persistent rules and recipes. Sneak/shift right-clicking or titling the book with "Global" applies the training to all companions worldwide.
  - **Memory Book Export (`/bot memory book [botName|global]`):** Generates and hands the player an authentic in-game Minecraft Written Book containing formatted chapters of learned recipes, ingredients, and operational rules.
- **Global & Personal Memory Architecture:**
  - **Global Memory:** Team-wide directives and recipes applied across all companion bots simultaneously.
  - **Personal Memory:** Dedicated memory banks tailored to individual companions and roles.
  - **LLM Context Injection:** Memory guidelines and learned recipes are injected into Gemini system prompt telemetry so AI decision-making strictly adheres to player training.
  - **Full Persistence:** All learned recipes and memories are serialized to `bot_data.json` across server restarts.

### ⚡ Teleportation Upgrades (`/bot tp`)
- **Fixed `/bot tp` Command:**
  - `/bot tp`: Teleports the nearest companion directly to the player (resolving the previous syntax exception when called with no arguments).
  - `/bot tp all`: Teleports all active companions to the caller's position.
  - `/bot tp <botName>`: Teleports the specified companion to the caller.
  - `/bot tp to <botName>`: Teleports the player to the specified companion's location.
- **Cross-Dimension Teleportation:**
  - Teleportation now properly switches dimensions (`ServerLevel`) and loads target chunks using `teleportTo(destLevel, x, y, z, Set.of(), yRot, xRot, true)`, enabling smooth inter-dimensional travel across the Overworld, Nether, and End.

### 🛠️ New Commands
- `/bot train <instruction>`: Trains the nearest companion on a new rule or recipe.
- `/bot train global <instruction>`: Trains all companions globally on a team rule or recipe.
- `/bot train <botName> <instruction>`: Trains a specific companion.
- `/bot train book [global|<botName>]`: Studies the held Written Book or Book & Quill.
- `/bot memory list [global|<botName>]`: Displays all active personal and global memories and custom recipes in chat.
- `/bot memory book [global|<botName>]`: Gives the player a physical in-game Written Book of memories and recipes.
- `/bot memory clear [global|<botName>]`: Clears personal or global memories and custom recipes.

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
