# 🤖 Gemini AI Companion Bots (NeoForge 26.3)

Bring your Minecraft world to life with **true autonomous AI companion players**! Powered by Google Gemini and an intelligent offline tactical engine, these companions don't just stand around — they walk, mine, craft, farm, breed animals, build houses with beds, defend your base with shields and jump-crits, operate locomotives and tractors, excavate pits, wear custom skins from your local folder, and chat naturally with you in multiplayer chat.

Works in **Singleplayer** and **Dedicated Multiplayer Servers**!

---

# 🚀 QUICK START GUIDE: HOW TO SPAWN YOUR FIRST BOT

## 🟢 Step 1: Spawn a Companion in Chat!
To spawn your first companion immediately, type or copy-paste this command in game chat:
# `▶ /bot spawn Alex`

> Want a specific profession or role right away? Just add the role name:
> * **Guard (Defender):** `/bot spawn Alex guard`
> * **Miner (Excavation):** `/bot spawn Steve miner`
> * **Farmer (Crops):** `/bot spawn Emma farmer`
> * **Builder (Cottages/Blueprints):** `/bot spawn Bob builder`
> * **Lumberjack (Forestry):** `/bot spawn John lumberjack`
> * **Rancher (Husbandry):** `/bot spawn Daisy husbandry`
> * **Villager (Workstations & Trades):** `/bot spawn Sarah villager`
> * **Conductor (Trains):** `/bot spawn Thomas conductor`

---

## 🧠 Step 2: Baked-In AI Model & Model Command
The mod comes pre-configured with the default baked-in model:
# `▶ /bot model gemini-3.5-flash-lite`

> **Copy & paste this into game chat to set or refresh your model:**
> ```
> /bot model gemini-3.5-flash-lite
> ```
> * **Baked-In Default:** `gemini-3.5-flash-lite` *(ultra-low latency, blazing fast responses, and minimal token/quota usage)*.
> * **Other Supported Models:**
>   * `/bot model gemini-2.5-flash`
>   * `/bot model gemini-1.5-flash`
>   * `/bot model gemini-1.5-pro`

---

## ⚡ Zero Configuration Needed (Offline Tactical Engine)
**No API key? No problem!** The mod features a built-in offline tactical decision engine. The moment you install the mod and spawn a companion, they can immediately:
* Drop custom skins into `aicompanion/skins` for automatic role-themed dressing with Alex/Steve dual-layer support
* Dig systematic layer-by-layer holes and pits on conversational demand (*"dig a hole here"*)
* Mine specific block types within an anchored radius (*"mine all the stone in this 10 block area"*)
* Dynamically analyze their environment when stuck, probing lateral paths around walls and escaping pits
* Build complete village cottages, watchtowers, barns, and forges with doors, windows, torches, and beds
* Open an interactive speedometer-style dashboard on right-click or **`B`** key
* Guard your compound with shields and sprint-jump critical strikes
* Enforce global boundary borders set with the **Surveyor Wand**
* Farm crops, breed livestock, and operate trains and tractors

*(Optional)* Want full conversational AI personality, banter, and contextual planning? Add a 100% free Google Gemini API key in 30 seconds!

---

### 👶 Authentic Villager Colony Breeding *(New in v1.2.22)*
* **Strict Two-Parent Breeding**:
  * Fixed an issue where single bots could breed alone; breeding strictly requires two adult villager companions in close proximity (within 16 blocks).
* **Colony Requirements (50-Block Radius)**:
  * Minimum of 2 adult villagers within a 50-block radius.
  * Number of beds must equal or exceed total bot population + 1, with a vacant bed ready for the newborn.
  * Both parents must have hunger at 90% or higher (`>= 18 / 20`) and breeding food (wheat, bread, carrots, potatoes, or beetroots) in their inventory. Food is consumed from both parents upon birth.

---

### 🚪 Vanilla Villager A* Pathfinding & Zero Erratic Jumping *(v1.2.21)*
* **Base 3D Voxel A* Pathfinder (`BotPathfinder`) + Hive-Mind Layer**:
  * Built on a deterministic 3D Voxel A* pathfinder modeled after vanilla Villager & Mob navigation (`WalkNodeEvaluator`), with `HiveMindBrain` learned trail pheromones and obstacle penalties layered on top.
  * Bots automatically route around exterior house walls, approach doors along their cardinal passage axis, open closed wooden doors and fence gates, and walk inside to reach beds and workstations—never pushing into outside house corners!
* **Repetitive-Loop Insanity Guard**:
  * If a companion is obstructed in the same spot for >1.2 seconds, it automatically penalizes the wall/corner in the Hive-Mind and recalculates a full A* route around the structure. Sealed/unreachable targets are automatically blacklisted so bots never push endlessly against walls.
* **Zero Erratic Ground Jumping**:
  * Utilizing native `1.06` step-height physics, bots walk smoothly up 1-block ledges and stairs without hopping. Jumping is strictly reserved for swimming in water/lava or crossing 1-block pit gaps.
  * Saying *"stop jumping"*, *"use the door"*, or *"stop"* immediately halts erratic movement, penalizes the obstacle in the Hive-Mind, and updates real-time task telemetry.

---

### 🧠 Shared Hive-Mind Neural Network & Reinforcement Learning *(v1.2.19)*
* **Collective Hive-Mind Brain (`24 -> 32 -> 16 -> 9` Pure-Java MLP)**:
  * Every companion shares a persistent **Hive-Mind Neural Network**, **3D Spatial Pheromone Waypoint Graph**, **Modded Block Walkability Table**, and **Task Preference Q-Table** saved in `aicompanion_memory.json`.
  * When one bot learns how to navigate a tricky staircase (like Macaw's narrow stairs/railings), avoid a trap, or optimize a task, **every bot on the server immediately benefits**.
* **Verbal & Command Reinforcement (`"good job"` / `"bad job"`)**:
  * Praise a bot in chat (*"good job"*, *"good bot"*, *"well done"*, *"nice pathing"*) or run `/bot reward [name|all]` to send a `+15.0` reward signal backward through its recent 60-step Eligibility Trace, permanently reinforcing that route and task!
  * Correct a bot in chat (*"bad job"*, *"bad bot"*, *"wrong way"*, *"stop getting stuck"*) or run `/bot punish [name|all]` to penalize its recent path (`-15.0`), mark the obstacle in the spatial hazard graph, and force an immediate reroute.
* **Player Footstep Imitation (`/bot learnpath`)**:
  * Run `/bot learnpath start` (or say *"learn this path"* / *"watch how I walk"*) and walk up complex stairs, bridges, or doorways. Your footsteps and jumps are recorded as **Golden Breadcrumb Waypoints** that bots automatically follow!
* **3D Brain Visualizer (`/bot brain`)**:
  * Run `/bot brain stats` to view your squad's **Hive-Mind IQ**, training steps, learned modded blocks, and top task Q-values, or `/bot brain show` to visualize learned trails and hazard zones with 3D particles in-world.

---

### 👥 Global Squad Commands & Traditional Trader GUI *(New in v1.2.17)*
* **Global Squad Commands (`/bot all`)**:
  * Added dedicated `/bot all` commands for fast management of all active companion bots at once:
    * `/bot all remove` - Despawn and remove all active companion bots across the server in one command.
    * `/bot all come` / `/bot all come here` / `/bot all follow` - Order all active companions to immediately rush to and follow you.
    * `/bot all stop` - Put all active companions into immediate Absolute Standby Mode.
    * `/bot all stay` / `/bot all standby` / `/bot all hold` - Tether all active companions to hold position at their current location within a 4m perimeter.
    * `/bot all sentry` - Set all active companions into stationary 360° Sentry Sniper mode.
    * `/bot all resume` / `/bot all dismiss` - Release tethers and order all companions to resume their normal autonomous role duties.
    * `/bot all shelter [true|false]` - Enable or disable night shelter and bed sleeping for all companions at once.
    * `/bot all tp` - Teleport all active companions directly to your position.
* **Traditional Trader GUI (`MerchantMenu`)**:
  * **Sneak + Right-Click** (Shift + Right-Click) any companion in the `"villager"` role to open the authentic vanilla Minecraft Merchant trading interface!
  * Displays tiered trades tailored to the companion's claimed profession trait (Farmer, Librarian, Armorer, Weaponsmith, Toolsmith, Cleric, Fletcher, Butcher, Fisherman, Shepherd, Mason, Cartographer, Leatherworker, or Unemployed/Wandering).
  * Baby villager companions shake their head (`VILLAGER_NO`) and remind you they must grow up before trading.
* **Villager Merchant Leveling & Progression System**:
  * Companions gain trading experience (`villagerXp`) with every trade executed in the GUI.
  * Progress through all 5 authentic vanilla tiers: **Novice** (Level 1) $\to$ **Apprentice** (Level 2) $\to$ **Journeyman** (Level 3) $\to$ **Expert** (Level 4) $\to$ **Master** (Level 5).
  * Leveling up plays `PLAYER_LEVELUP` and `VILLAGER_YES` sound effects, displays green sparkles, announces promotion in chat, and dynamically unlocks new tier trades directly in the open trading screen!
  * Trading level and experience points are saved and restored seamlessly across server reboots via `BotPersistence`.
* **Exploit Protection (Zero Starter Emeralds)**:
  * Villager companions spawn and breed with zero emeralds (starting only with basic bread and wheat), protecting survival economies from free emerald spawning exploits.

---

### 🏛️ Workstation Searching & Villager Profession Sub-Roles *(New in v1.2.16)*
* **Workstation Searching & Profession Sub-Role Adoption**:
  * Companions in the `"villager"` role autonomously search within a 48-block radius for unclaimed villager work tables (like vanilla villagers).
  * Upon finding an available work table, the companion claims the block and adopts that table's profession trait as an authentic sub-role under their Villager role (e.g. `Role: Villager`, `Sub-Role: Librarian`).
  * Supports all 13 vanilla work tables:
    * `Composter` -> **Farmer** | `Lectern` -> **Librarian** | `Blast Furnace` -> **Armorer** | `Smoker` -> **Butcher**
    * `Cartography Table` -> **Cartographer** | `Brewing Stand` -> **Cleric** | `Fletching Table` -> **Fletcher**
    * `Cauldron` -> **Leatherworker** | `Loom` -> **Shepherd** | `Smithing Table` -> **Weaponsmith**
    * `Stonecutter` -> **Mason** | `Grindstone` -> **Toolsmith** | `Barrel` -> **Fisherman**
* **Single Workstation Claim Enforcement**:
  * Companions ensure a work table isn't already claimed by another companion before taking it, ensuring every villager companion has their own unique job site.
* **Dynamic Workstation Loss & Re-Employment**:
  * If a work table is broken or replaced, the companion sighs (`VILLAGER_NO`), resets to Unemployed, announces the loss in chat, and automatically resumes searching for an available work table.
* **Authentic Work Hours & Handheld Tools**:
  * During work hours (2000-9000 ticks), companions walk to their claimed work table, face it, hold their profession item (Librarian holds a Book, Weaponsmith holds an Iron Sword, Fletcher holds a Bow, Farmer holds Wheat, Mason holds Stone Bricks, etc.), and play profession-specific ambient work audio with green happy sparkles.
* **Profession-Specific Emerald Trades**:
  * Right-clicking a companion with an Emerald grants authentic trades matching their profession trait (e.g. Books from Librarians, Iron Ingots from Armorers, Arrows from Fletchers, Redstone from Clerics, Cooked Beef from Butchers, Stone Bricks from Masons).
* **Commands & GUI Status**:
  * Configure or inspect via `/bot profession <name> [trait|clear|search]` or `/bot workstation <name> [assign|clear|search]`, or chat (*"search for a work table"*, *"claim this table"*, *"become a librarian"*, *"clear workstation"*).
  * Displays `Role: Villager (Sub-Role: Librarian)` and assigned workstation coordinates in `/bot status` and the companion dashboard GUI.

---

### 👁️ Enhanced Perception, Sight Cones & Crosshair Targeting *(New in v1.2.15)*
* **Player Crosshair Raycast Perception**:
  * Companions and the Gemini LLM engine directly track what entity the player/speaker is looking at via line-of-sight raycasting up to 48 blocks away.
  * Crosshairs targets are highlighted in telemetry and prioritized in combat orders, allowing you to simply look at a mob and say *"attack!"* or *"take that out!"* for instant target acquisition.
* **Player Front Vision Cone Detection**:
  * Companions now analyze a wide ~140-degree field of view in front of the player up to 40 blocks away.
  * When you say *"zombie in front of me"*, *"watch out ahead"*, or *"can you see that zombie?"*, the companion immediately confirms the mob's presence, location, and distance instead of claiming it can't see it.
* **Expanded 48m Surroundings Perception Volume**:
  * Perception volume expanded from 24m horizontal / 12m vertical to **48m horizontal and 28m vertical**, scanning around both the bot and the player.
  * Telemetry enriches all nearby entities with relative cardinal directions (North, South, East, West, etc.) and player-relative tags (`[IN FRONT OF SPEAKER]`, `[DIRECT CROSSHAIR TARGET]`, `[VISIBLE TO BOT]`).
* **Fixed Vertical Elevation Blindness & Fence Rejections**:
  * Removed restrictive 6-block vertical search caps (`vertDiff > 6.0`), allowing companions to spot and target mobs on hills, cliffs, terraces, and staircases up to 28+ blocks vertically.
  * Fixed attack commands being rejected if a fence or railing was situated along the line of sight.
* **Expanded Bodyguard Auto-Defend Range**:
  * Melee bodyguard engagement range around guarded players increased from 8m to **20m** (and up to 36m for archers), proactively neutralizing threats within 14 blocks before they reach the player.

---

### 👶 Baby Bot Villagers & Colony Life Cycle *(New in v1.2.14)*
* **Newborns Start as Babies**:
  * All new companion villagers born through breeding or spawned with the `"villager"` role start out as adorable baby bot villagers.
  * Scaled to authentic baby size (`Attributes.SCALE = 0.55`) using vanilla Minecraft attribute scaling, proportionally shrinking their model, reach, eye level, and hitbox.
* **Playful Schedule & Bed Jumping**:
  * Baby villagers don't work adult jobs; during daytime work hours (2000-9000 ticks) they play tag with nearby friends and jump joyfully up and down on beds with authentic wool bounce sounds and sparkles!
* **Natural Growth & Feeding**:
  * Baby companions age over 20 minutes (24000 ticks) into full adults. Right-clicking a baby bot with Wheat, Bread, Carrots, or Potatoes feeds them, accelerating their growth by 10% (2 minutes) per feeding.
* **Baby Commands & Status**:
  * Toggle baby/adult stage via `/bot baby <name> [true|false]` or chat (*"make <name> a baby"*, *"<name> grow up"*). View developmental stage and remaining growth time in `/bot status <name>`.

---

### 🪜 Macaw's Stairs & Modded Staircase Pathfinding *(New in v1.2.13)*
* **Full Compatibility with Macaw's Stairs & Modded Stairs**:
  * Companions natively recognize all Macaw's Stairs variants (Loft Stairs, Compact Stairs, Bulk Stairs, Terrace Stairs, Skyline Stairs, Balcony stairs, and platforms) as walkable paths.
  * Eliminates false wall detection where ascending stair steps were previously misclassified as solid 2-block tall walls.
* **1-Block Wide Stair Corridor Navigation & Shoulder Centering**:
  * When ascending or descending narrow 1-block wide staircases flanked by side railings, walls, or balustrades, companions automatically align and steer along the exact center axis of the staircase corridor.
  * Prevents 0.6-block wide player collision hitboxes from scraping or getting snagged against tall railing hitboxes.
* **Smart Stair Railing & Balustrade Classification**:
  * Stair railings, balustrades, handrails, and balconies are recognized as stair accessories rather than impenetrable perimeter fence walls, preventing companions from halting or getting distracted searching for fence gates.
* **Native 1.06m Step-Up Capacity & Ascension Boost**:
  * Companions now feature `Attributes.STEP_HEIGHT` configured to 1.06 blocks with smooth vertical momentum assist (`0.28` lift) when climbing stairs, providing seamless ascent even over steep modded steps.

---

### 🌾 Villager Role, Bed & House Assignment, Breeding & "I Command" Absolute Mode *(New in v1.2.12)*
* **"Villager" Role & 100% Vanilla Villager Simulation**:
  * Set a companion to the `"villager"` role via `/bot role <name> villager` or chat (*"you are now a villager"*).
  * Follows authentic vanilla villager schedules: morning strolls, job site workstation employment with ambient sounds (composter, lectern, blast furnace, smoker, smithing table) and green sparkles (`HAPPY_VILLAGER`), afternoon socializing and gossiping at bells, and night sleep in beds.
  * Flees from hostile monsters (zombies, drowneds, illagers) and rings village bells to warn the colony.
  * Trade with villager companions by right-clicking with Emeralds!
  * Retains 100% of companion features: chat, memory, commands, and ability to switch roles back to anything else at any time (*"change role to guard"*).
* **Housing, Bed Assignment & Night Sleep Routine**:
  * Assign a personal bed via `/bot bed assign <name> [pos]` or chat (*"this is your bed"*, *"assign bed"*).
  * Assign a home house via `/bot house assign <name> [areaName]` or chat (*"this is your house"*, *"set house"*).
  * At night and during thunderstorms, companions automatically seek shelter and sleep in their assigned bed until morning dawn.
  * Toggle nocturnal shelter via `/bot shelter <name> [true|false]` or chat (*"night watch"*, *"stay awake"*).
* **Villager-Style Colony Breeding Logic**:
  * When hunger is good (food level >= 16 and holding food), safe (no nearby threats, healthy), and total beds in a 100x100 block radius exceed bot population (`beds >= bots + 1`), companions can breed.
  * Consumes 3 raw wheat (or bread/carrots), emits heart particles, plays level-up and happy villager sounds, and spawns a newborn companion with a random name and the `"villager"` role.
* **Strict "I Command" Gating for Absolute Command Mode**:
  * Absolute Command Mode is now strictly gated: it is only enabled when a player says *"i command"* (e.g. *"i command you to follow me"*, *"i command stop"*) or uses `/bot command <name> <order>`.
  * Phrases like *"stop combining the wheat into haybales"* or regular *"stop"* no longer lock the companion into Absolute Standby Mode.
* **Wheat-to-Hay Auto-Compaction Fix & Hay Uncrafting**:
  * Asking a bot to stop compacting wheat (*"stop combining wheat into haybales"*, *"keep wheat as wheat"*, *"need wheat to breed with"*) now properly disables auto-compaction and preserves raw wheat.
  * Companions can also uncraft existing hay bales back into 9x raw wheat upon player request (*"uncraft hay"*, *"turn hay into wheat"*).

---

### 🛡️ Area Guarding & Farming Intent Disambiguation *(v1.2.11)*
* **Strict Guarding vs Farming Disambiguation**:
  * Fixed an issue where ordering a companion to stand in or guard a mapped area containing "farm" in its name (e.g. `Wheat_Farm`) caused the companion to override their role to "farmer" and begin farming crops.
  * When any mapped area is mentioned, the mod now checks player intent: directives containing guard, defend, protect, stand, stay, hold, or sentry (*"stay here in wheat_farm and guard it"*, *"stand in wheat_farm"*) assign the companion strictly as an area guard, holding position without farming.
  * Area names are sanitized so naming an area `Wheat_Farm` or `tree_farm` will never trigger agricultural farming loops unless explicitly ordered to farm.
  * Added robust recognition for negative farming instructions (*"do no farm it"*, *"not a farmer"*, *"don't farm"*).

---

### 🚪 Fence Gate Traversal, Passing-Through & Collision Fix *(v1.2.10)*
* **Seamless Fence Gate Navigation**:
  * Fixed an issue where open fence gates were classified as solid fences because their identifier contained `"fence"`, causing companions to freeze in front of open gates.
  * Companions now push forward continuously through gates without their destination resetting to the center of the gate.
  * Gates dynamically swing open *away* from the companion so the gate panel never collides with their hitbox.
  * Auto-close logic checks bounding box collision and requires clearing 2.1m before shutting, ensuring gates never slam shut on the companion's body.
  * Detects gates approaching from any angle or diagonally within 2.5m and features dedicated unstuck recovery.

---

### 🏹 Sentry Mode, Archery Engine & Strict Fence Combat *(v1.2.9)*
* **Dedicated Sentry Mode (`/bot sentry [name]` or Chat `"sentry"`)**:
  * Stations companions as stationary sentinels on watchtowers, fortress ramparts, roofs, or base perimeter walls.
  * Sentry companions lock onto their exact post, continuously survey the surroundings in a smooth 360° panoramic scan, and snipe hostile monsters up to 48m away (56m if elevated) with pinpoint accuracy.
  * Status and coordinates persist across server reboots via `BotPersistence`.
* **Zero Fence-Hopping & Strict Perimeter Defense**:
  * Upgraded 3D vertical column fence perception: companions inside fenced compounds will **never** open gates or jump fences to pursue distant monsters in the wilderness.
  * Archers safely stay behind fence perimeters and fire arrows in parabolic trajectories over the fence at monsters outside.
  * Melee guards hold ground inside the fence and attack over the fence only if enemies close into direct reach (<3m).
* **Extended Archery Range & Infinite Fallback Reserve**:
  * Archery range expanded up to 48m with enhanced gravity/arc compensation; companions stand ground and shoot instead of charging across 30 blocks of wilderness into melee.
  * AI companions maintain an infinite standard arrow fallback reserve so bow users are never forced into swinging a bow as a club when out of inventory arrows.
  * Auto-defend detection range for archers expanded up to 48m to immediately return fire against distant skeletons.

---

### 🛢️ Crude Oil & Hazardous Fluid Avoidance *(v1.2.8)*
* **Lava-Grade Hazard Classification**:
  * Companions now treat all modded crude oil, fuel, petroleum, tar, diesel, acid, sludge, and toxic fluids as lethal hazards identical to molten lava.
  * Companions will **never** step into crude oil pools, walk through oil puddles, or choose bypass waypoints inside oil. They cleanly route along solid shorelines or detour around oil lakes.
* **Emergency Shoreline Escape**:
  * If pushed or knocked into crude oil, companions immediately activate emergency vertical propulsion (`0.28` velocity), continuous swimming jumps, and seek the nearest solid bank within 12 blocks to climb out onto dry land.
* **Item Retrieval Safety**:
  * Item vacuuming strictly avoids chasing drops that have fallen into crude oil or lava, keeping companions safe on dry land.

---

### 🏹 Archery, Absolute Command Mode & Leash Tethering *(v1.2.7)*
* **True Bow & Archery Combat**:
  * Companions holding or carrying bows and arrows now dynamically fight at range (up to 25+ blocks), drawing their bow with charging animations, computing ballistic trajectory arcs, and firing precision arrows with vanilla shoot sounds.
  * Seamlessly switches to melee and raises a shield if hostiles breach within 4 blocks; tactically backpedals when enemies attempt to corner them while drawing.
* **Absolute Command Mode**:
  * Immediate fast-path execution of core player orders (*"come here"*, *"follow me"*, *"stop"*, *"stay here"*, *"resume"*) on tick 0 without LLM latency.
  * In Absolute Mode, player commands unconditionally override background autonomous loops (farming, tree felling, breeding, distant mob hunting).
* **Smart Aggro Filtering ("Leave Them Alone")**:
  * Companions strictly ignore ambient, unprovoked monsters minding their own business 20 blocks away.
  * Hostiles are only engaged if actively attacking the bot, attacking the protected player, swelling as a Creeper in close quarters, or explicitly targeted via an attack order.
* **Leash Tethering (`/bot tether`, `/bot hold`, `/bot untether`)**:
  * Tether a companion to any position with a custom radius (*"stay here"*, *"stand here"*, or `/bot tether Goku 4`).
  * Tethered companions defend with bow and melee but will **never** chase monsters outside their tether boundary, returning immediately to their post.

---

### 🚪 Doorway Centering & Strict Fence Boundaries *(v1.2.6)*
* **Smooth 1x2 Doorway Navigation**:
  * Companions cleanly recognize open doors and gates as walk-through passageways rather than solid walls.
  * Automatic shoulder micro-alignment centers the bot along the 1x2 doorway axis, preventing snagging on doorframes.
  * Approaching closed wooden doors will automatically open them and politely close them behind after passing.
* **Strict Fence Boundaries & Gate Routing**:
  * Fences and stone walls are strictly preserved as impenetrable boundaries — companions will **never** replace fence blocks with dirt, place blocks on top of fences, or pillar-jump over them!
  * When reaching livestock pens or fenced areas, companions automatically navigate to the nearest fence gate or existing opening, pass through, and close the gate behind to keep animals from escaping.

---

### ⚡ Offline Mode & API Key Control *(v1.2.5)*
* **Force Offline Tactical Mode**:
  * Run `/bot offline on` or `/bot offline true` to instantly force companions into 100% Offline Tactical Mode, even if an API key is present.
  * Can also be configured directly in `config/aicompanion-common.toml` with `forceOffline = true`.
* **Easy API Key Removal**:
  * Run `/bot apikey clear` or `/bot apikey remove` to delete your personal API key immediately in-game.
  * Run `/bot apikey clear all` (Admins/OPs) to wipe all player keys and server default keys.

---

## 🔑 How to Get a Free Gemini API Key in 30 Seconds
Google provides free Gemini API access with **no credit card or billing required**:

1. **Get your Key**: Go to **[https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)** and sign in with your standard Google account.
2. **Create Key**: Click the blue **"Create API Key"** button and copy the generated key.
3. **Add in Minecraft**: Open chat and run:
   ```text
   /bot apikey YOUR_API_KEY_HERE
   ```
   *That's it! Your key is saved locally to your server/client configuration.*

---

## 🌟 Key Features

### 🎭 1. Custom Skin Folder & Role-Themed Auto-Assignment *(New in v1.2)*
* **Drop-In Skins Folder (`/aicompanion/skins`)**:
  * Simply drop standard **64×64** or **64×32** `.png` Minecraft skins into the `aicompanion/skins` folder (in your game instance directory or config folder)!
  * Auto-initializes the directory and generates a guide on first launch.
* **Role Subfolders & Auto-Assignment**:
  * Organize skins into role subdirectories (e.g. `/guard/`, `/builder/`, `/lumberjack/`, `/rancher/`, `/miner/`, `/farmer/`, `/conductor/`) or name them with role prefixes (e.g. `guard_knight.png`).
  * When companions spawn with a role, they automatically select a matching skin from that role's category, rotating across variations so each worker/guard looks distinct!
* **Exact Companion Name Match & Fallback Pool**:
  * Name a skin file after a companion (e.g. `gemini.png` or `bob.png`) for instant 1:1 skin assignment.
  * Any general or unassigned skins in the folder form an automatic fallback pool for companions.
* **Dual Layering & Slim (Alex 3px) / Wide (Steve 4px) Support**:
  * Full dual-layer rendering: outer accessory layers (hardhats, leather satchels, toolbelts, chaps, braids, fleece vests) render in high fidelity.
  * Automatically detects Alex 3px slim arm models via alpha transparency or `_slim`/`alex` filename tags.
* **In-Game Skin Commands**:
  * `/bot skin list` — Lists all detected skins in the folder along with their detected model types.
  * `/bot skin reload` — Hot-reloads all skin textures from disk in real-time without restarting Minecraft.
  * `/bot skin set <botName> <skinName>` — Assigns a specific skin to a companion and persists across world restarts.

### ⛏️ 2. Interactive Verbal Hole Digging & Systematic Excavation (`EXCAVATE`) *(New in v1.2)*
* **Natural Conversational Follow-Up**:
  * Tell your companion *"dig a hole"*, *"dig a hole here"*, *"excavate here"*, or *"dig here"*.
  * The companion marks the exact ground block you are looking at (or standing on) with emerald villager and flame particles, walks over to the spot, and asks in chat:
    > *"Marked starting point at (X, Y, Z)! How big and how deep should I dig from there? (e.g. '5x5 and 4 deep' or 'width 4 depth 6')"*
  * Reply naturally without needing to repeat the bot's name (e.g., `"5x5 and 4 deep"`, `"4 by 4, 6 deep"`, `"width 4 depth 8"`, `"5x5"`, or `"4 deep"`).
  * Say `"cancel"` to abort or `"start here"` to update the anchor point if you reposition.
* **One-Shot Dimension Commands**:
  * Include dimensions in your prompt (e.g. *"dig a 5x5 hole 4 deep here"*), and the companion immediately locks onto the spot and begins digging.
* **Systematic Serpentine Layer-by-Layer Excavation**:
  * Companions dig the volume systematically from the surface level down layer-by-layer (`dy = 0` down to `-(depth - 1)`).
  * Automatically equips the optimal tool (shovel for dirt/gravel/sand, pickaxe for stone/ores), breaks blocks with true drops, and vacuums all excavated items directly into the companion's inventory.

### 🎯 3. Radius-Constrained Block Type Mining *(New in v1.2)*
* **Anchored Area Mining Commands**:
  * Natural chat commands: *"mine all the stone in this 10 block area"*, *"mine all coal within 15 blocks"*, *"clear all the dirt in this 8 block area"*, or *"mine all the stone here"*.
  * Locks the mining zone to the player's anchor point with the specified block radius.
* **Top-Down & Exposed-Block Prioritization**:
  * Mining systematically prioritizes **exposed surface blocks** and works from the **highest Y level down**, smoothly peeling blocks layer-by-layer.
  * Distinguishes natural terrain (stone, deepslate, andesite, diorite, granite, tuff, dirt, ores) from player-placed building blocks.

### 🧠 4. Intelligent Stuck Detection, Area Analysis & Dynamic Obstacle Bypass *(New in v1.2)*
* **Distance-to-Goal Stagnation Tracking**:
  * Companions measure forward progress toward their destination (`moveNoProgressTicks`). If unable to decrease distance to the goal over 16 ticks (~0.8s), they recognize that they are stuck and analyze their surroundings.
* **Immediate Environmental Analysis (`analyzeAndExecuteUnstuckPlan`)**:
  * **Hole & Trench Trap Analysis**: Checks surrounding blocks at chest/head level on 3+ sides. Probes for natural exit ramps or steps within 2 blocks (`findNearbyExitStep`). If trapped in a vertical pit ($\ge 2$ blocks deep) with no ramp, clears overhead obstructions with proper tools and executes a pillar jump (`executeHoleEscape()`).
  * **Wall / Building / Cliff Bypass Probing**: Probes lateral rays left and right up to 8 blocks away to find the nearest corner or opening around obstacles, setting an intermediate **`activeBypassWaypoint`**.
  * **Chasm / Deep Pit Rim Search**: When facing a wide drop-off ahead with no bridging materials, companions probe left and right along the rim to find contiguous ground or safe natural crossings.
* **Seamless Intermediate Waypoint Routing**:
  * Navigates directly toward `activeBypassWaypoint` until reached or timed out, then seamlessly resumes direct pathing to the original target.
* **Eliminated Wall-Pillaring Bug**:
  * Companions no longer place dirt pillars under their feet whenever they touch a building wall or room corner.

### 📐 5. Subterranean Pit Support & Height Ranges *(New in v1.2)*
* **Negative Height Pit Excavation**:
  * `/bot area height <name> <height>` (and `/bot area cube`) accepts negative values (down to `-384`) to define subterranean pits and underground quarries.
* **Two-Number Explicit Y Ranges**:
  * Added syntax `/bot area height <name> <minY> <maxY>` to define cuboid volumes between two explicit world Y levels.

### 🌟 6. Shader Bloom Fix on Companion Name Tags *(New in v1.2)*
* **Iris & Complementary Shaders Bloom Fix**:
  * Routes nametag rendering through standard non-emissive buffers, preventing blinding white glowing rectangles on shader packs and matching human player nametags perfectly.

### 📊 7. Interactive Companion Dashboard & Speedometer HUD *(v1.1)*
* **Right-Click or Press `B` Key**: Open any companion's full dashboard by right-clicking them or pressing **`B`** (configurable in Controls).
* **Speedometer-Style Vital Gauges**: Clean color-coded health and hunger meters displaying real-time HP and food levels.
* **Full Inventory & Gear Management**: 41 dedicated slots covering Helmet, Chestplate, Leggings, Boots, Off-hand (Shield/Torch), and 36-slot backpack with shift-click transfer support.
* **One-Click Command Buttons**: Instantly order bots to `[Follow/Hold]`, `[Patrol]`, `[Build Cottage]`, `[Store in Chest]`, or `[Eat Rations]`.

### 🗺️ 8. Global Waypoint & Area System with Surveyor Wand *(v1.1)*
* **Surveyor Wand Tool (`/bot wand`)**:
  * Right-click blocks to link perimeter corners with glowing laser particle fences (supports 2-corner diagonal boxes or multi-corner polygons).
  * Sneak + Right-Click opens the **Area Configuration GUI** to name zones (`Compound`, `NorthFarm`) and select zone types (`COMPOUND`, `FARM`, `RANCH`, `SAFE_ZONE`, `RESTRICTED`).
  * Craftable in survival with **1 Compass + 2 Sticks** diagonally on a crafting table.
* **3D Cuboid Volume Extrusion (`/bot area height` / `/bot area cube`)**:
  * Assign a vertical height or min/max Y range to any surveyed area.
  * Instantly projects a 3D holographic wireframe volume with vertical corner beacons and upper/lower particle boundaries.
* **Perimeter Confinement**: Assigned companions (`/bot area assign <bot> <area>` or chat *"guard Compound"*) are locked within borders; movement vectors automatically reflect back toward the zone center.
* **AI Telemetry**: Real-time zone presence is automatically passed to Gemini AI prompts.

### 📐 9. In-World Blueprint Scanner & Custom Builder Catalog *(v1.1)*
* **Scan & Capture Any Building**:
  * Run `/bot blueprint capture <areaName> <blueprintName>` to scan everything inside a 3D cubed area into a persistent blueprint.
  * Records exact block IDs, rotation properties, and relative coordinates.
  * Check material bills and dimensions anytime with `/bot blueprint info <blueprintName>`.
* **Builder Bot Remakes**:
  * Blueprints are instantly registered into the bot's building catalog!
  * Tell any builder companion in chat: `"builder, build <blueprintName>"` or type `/bot build <bot> <blueprintName>`.
  * The bot automatically rotates the structure to match their facing direction, clears the footprint, requisitions materials from workshop chests (crafting raw logs into planks if needed), and constructs the entire build block by block!

### 🏠 10. Autonomous Architectural Builder & Expanded Catalog (`builder`) *(v1.1)*
* **Multi-Archetype Architectural Catalog**:
  * **Defensive Guard Watchtower ($5 \times 5$, 10 blocks tall)**: Multi-floor outpost with stone brick base, ladder climbing shaft, mid-floor guard deck, and open battlements with arrow slits for guards (`"build watchtower"` / `"build tower"`).
  * **Farmhouse Barn & Granary ($9 \times 9$, high-vaulted ceiling)**: High vaulted plank roof, double barn doors (Macaw's Barn Doors compatible), decorative hay bales, and deposit chests for farmers (`"build barn"` / `"build granary"`).
  * **Blacksmith Forge & Workshop ($7 \times 7$)**: Open-air industrial stone pavilion with chimney, Blast Furnace, standard Furnace, Crafting Table, and Anvil (`"build forge"` / `"build blacksmith"`).
  * **Frontier Residential Cottage ($7 \times 7$)**: Starter home with foundation, log pillars, glass windows, wooden door, torch lighting, and bed (`"build house"` / `"build cottage"`).
* **Autonomous Workshop Crafting & Supply Logistics**:
  * Builders automatically scan workshop chests within 16 blocks of the construction site.
  * If supplies run low, raw logs found in chests are autonomously crafted into wooden planks and sticks without manual player hand-feeding.
* **Adaptive Modded Palette Engine**:
  * Dynamically scans nearby storage and mod registries for Macaw's packs (`mcwdoors`, `mcwroofs`, `mcwwindows`, `mcwlights`, `mcwfurnitures`) and harmonizes matching wood themes across all structures.
* **Village Housing Registry**: Automatically tracks completed houses and available beds to support future colony expansion.

### ⚔️ 11. Tactical Combat Mastery & Squad Defense
* **Sprint-Jump Critical Strikes**: Companions jump into melee range and strike at peak downward velocity to deal guaranteed critical hits with spark particles.
* **Dynamic Shield Blocking**: Instantly raises off-hand shields when Creepers swell or Skeletons draw bows, absorbing damage before counter-attacking.
* **Squad Distress Dispatch**: When non-combat workers (farmers, miners, lumberjacks) are attacked, they call out in chat; nearby Guard companions within 48 blocks rush to neutralize the threat.
* **Reverse Mob A\* Pathfinding & Anti-Spin Targeting**: Companions utilize reverse pathfinding to navigate around cover toward mobs, vertical targeting is clamped within $\pm 6$ blocks with line-of-sight verification, and unreachable targets time out cleanly.

### 🌾 12. Autonomous Farmer Role (`farmer`)
* **Surveyor Wand Mapped Area Farming**: Tell your farmer `"farm it"`, `"farm this area"`, `"farm the area"`, or `"farm <name>"` — they automatically link to the mapped zone and systematically tend 100% of the area (e.g. $149\text{ m}^2$ plots) without skipping outer corners.
* **Assignable Dimensions**: Specify arbitrary dimensions in chat (e.g. `"shawn, from this spot your farm is a 10x10 area"`).
* **Strict Maturity Harvesting**: Only harvests **100% fully mature crops**, leaving growing plants untouched.
* **Instant Replanting & Open Farmland Seeding**: Automatically replants harvested spots and plants empty tilled soil from inventory.
* **Hoe Ground Tilling**: Equips hoes, tills untilled dirt/grass into farmland inside the zone, and immediately plants seeds.
* **Multi-Chest Storage Logistics**: Automatically locates nearby single chests, double chests, and barrels. If a chest fills up during deposit, the farmer seamlessly switches to the next available container with free space.
* **Perimeter Access**: Freely reaches perimeter and boundary fence chests within 24 blocks of the farm without hitting perimeter boundaries.

### 🐄 13. Animal Husbandry & Ranching (`husbandry` / `rancher`)
* **Autonomous Breeding**: Finds livestock in the ranch zone ready to mate and feeds them breeding food.
* **Controlled Herd Culling**: Automatically manages animal populations when they exceed your stated limit without ever harming babies or dropping below your minimum threshold.
* **Drop Collection**: Collects meat, leather, wool, and feathers and deposits them into storage chests.

### 🚂 14. Train Conductor & Clean Dismount (`conductor` / `train`) (`miningtools` Integration)
* **Locomotive Operation**: Automatically boards locomotives, manages fuel, controls throttle, and sounds whistles.
* **Precision Distance Control**: Say `"move ahead 4 tiles"`, `"forward 10 blocks"`, or `"reverse 5 blocks"`.
* **Rail Pathfinding**: Say `"come here"` or `"drive to me"` and the conductor rolls the train down tracks directly to you.
* **Reliable Dismount & Anti-Reboard Protection**: Tell any companion riding a train to `"get out"`, `"dismount"`, `"get off"`, or `"exit"` — they cleanly dismount, cancel all train orders, restore their previous role, and step clear of tracks without any immediate re-boarding loops.
* **Automated Freight Logistics**: Transports cargo between stations, loading and unloading chests autonomously.

### 🚜 15. Tractor Automation (`tractor`) (`janemoose` Integration)
* Companions mount and steer rideable tractors equipped with plows, seeders, and harvesters across farm fields.

### 💾 16. Bulletproof World Persistence & Chunk Loading
* **Persistent Chunk Tickets**: Companions keep their chunks loaded so they remain active even when human players are far away.
* **Instant Client Rendering**: Immediate packet synchronization on player login eliminates invisible companion lag.

---

## 💬 Natural Chat Command Examples

You can speak to your companions directly in public chat:

* **Excavation & Hole Digging**:
  > *"dig a hole here"* → Companion responds: *"Marked starting point at (X, Y, Z)! How big and how deep should I dig from there?"* → Reply: *"5x5 and 4 deep"*
  > *"dig a 4x4 hole 6 deep here"*
  > *"excavate here"*
* **Radius-Constrained Mining**:
  > *"mine all the stone in this 10 block area"*
  > *"mine all coal within 15 blocks"*
  > *"clear all the dirt in this 8 block area"*
* **Building**:
  > *"builder, build a house right here"*
  > *"build a watchtower"*
  > *"build a barn"*
  > *"build a forge"*
* **Areas & Guarding**:
  > *"guard Compound"*
  > *"stay inside Base and watch the perimeter"*
* **Nicknames & Renaming**:
  > *"assign nickname Charlie"*
  > *"set your nickname to Bob"*
  > *"your nickname is now Steve"*
* **Farming (Whole Mapped Areas & Custom Dimensions)**:
  > *"farm the area"* / *"farm this area"* / *"farm it"* (automatically farms full Surveyor Wand mapped zone)
  > *"farm FarmZone"*
  > *"shawn, from this spot ur farm land is 10x10 area, plant wheat and carrots, put ur harvest in the chest, only harvest 100% mature crop"*
* **Husbandry**:
  > *"james, breed cows with wheat, kill adult cows when amount is over 10, only kill adults never have less then 5 cows at anytime."*
* **Trains**:
  > *"conductor, move ahead 10 blocks"*
  > *"drive to me"*
  > *"stop train"*
  > *"get out"* / *"dismount"* / *"get off the train"*

---

## 🎮 Slash Commands

| Command | Description |
| :--- | :--- |
| `/bot spawn <name> [role]` | Spawn a new companion (`farmer`, `husbandry`, `guard`, `builder`, `miner`, `lumberjack`, `conductor`, `courier`, `villager`) |
| `/bot all <remove\|come\|stop\|stay\|sentry\|resume\|shelter\|tp>` | Global squad commands: remove all, come here, stop, stay/hold, sentry sniper, resume duties, toggle shelter, or teleport all |
| `/bot role <name> <role>` | Switch companion role (e.g. `villager`, `guard`, `farmer`, `miner`) |
| `/bot bed assign <name> [pos]` | Assign personal bed for nightly sleep |
| `/bot bed clear <name>` | Clear assigned bed |
| `/bot house assign <name> [area]` | Assign home house / compound area for shelter |
| `/bot house clear <name>` | Clear assigned house |
| `/bot shelter <name> [true\|false]` | Enable or disable night shelter and bed sleeping |
| `/bot farm autohay <name> [true\|false]` | Toggle automatic wheat-to-hay compaction |
| `/bot nickname [botName] <nickname>` | Assign an active nickname/alias to a companion (or nearest companion) |
| `/bot rename [botName] <nickname>` | Alias for `/bot nickname` |
| `/bot skin list` | List all detected skins in the `aicompanion/skins` folder and their model types |
| `/bot skin reload` | Hot-reload all skin textures from disk in real-time |
| `/bot skin set <botName> <skinName>` | Manually assign a specific skin to a companion (persisted across restarts) |
| `/bot gui [name]` | Open the interactive Speedometer Dashboard & Inventory for a bot |
| `/bot wand` | Obtain the Surveyor Wand for defining global boundary areas |
| `/bot area list` | List all saved global areas and compound zones |
| `/bot area height <name> <height\|minY> [maxY]` | Set 3D extrusion height (supports negative pits) or explicit min/max Y range |
| `/bot area cube <name> <height\|minY> [maxY]` | Alias for `/bot area height` |
| `/bot area show <name>` | Display glowing laser boundary particles around an area |
| `/bot area assign <bot> <area>` | Confine a companion's patrol area strictly to a saved zone |
| `/bot area remove <name>` | Delete a saved global area |
| `/bot blueprint capture <area> <name>` | Scan in-world structure within a cubed area into a saved blueprint |
| `/bot blueprint list` | List all custom saved blueprints |
| `/bot blueprint info <name>` | View dimensions, block count, and material bill for a blueprint |
| `/bot blueprint build <bot> <name>` | Order a builder companion to construct the blueprint |
| `/bot remove <name>` | Despawn a companion |
| `/bot list` | List all active companions and their current tasks |
| `/bot status <name>` | View HP, hunger, farm/ranch zones, and inventory |
| `/bot stop [name\|all]` | Stop tasks immediately into Absolute Standby Mode (nearest bot if omitted) |
| `/bot follow [name]` | Order nearest or named companion to follow closely in Absolute Mode |
| `/bot hold [name]` | Tether nearest or named companion at current spot with a 4m perimeter |
| `/bot tether <name> [radius]` | Leash companion to their post; defends with bow/melee without wandering |
| `/bot untether <name>` | Release tether and resume normal autonomous duties |
| `/bot sentry [name]` | Lock companion to post: stationary sniper scanning 360° up to 48m |
| `/bot command <name> <order>` | Direct order override (follow, stop, hold, attack, resume) |
| `/bot reward [name\|all] [amount]` | Reinforce a companion's recent pathing & task in the shared Hive-Mind Neural Net |
| `/bot punish [name\|all] [amount]` | Penalize a companion's recent route/behavior and trigger immediate rerouting |
| `/bot learnpath [start\|stop]` | Record your footsteps & jumps as Golden Breadcrumbs in the Hive-Mind path graph |
| `/bot brain [stats\|show\|reset]` | View Hive-Mind IQ & neural stats, render 3D trail particles, or reset the brain |
| `/bot tp <name>` | Teleport companion to your position |
| `/bot ask <name> <request>` | Send a direct task/question to a companion |
| `/bot apikey [key]` | View API key status or set your personal/server Gemini key |
| `/bot model <modelName>` | Set the active Gemini model (default baked-in: `gemini-3.5-flash-lite`) |
| `/bot waypoint <name>` | Save your current coordinates as a named bot waypoint |

---

## 📋 Technical Requirements
* **Platform**: NeoForge 26.3
* **Minecraft Version**: 26.3
* **Java Version**: Java 25
* **Side**: Both (Client & Server)
* **License**: MIT
