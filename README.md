# 🤖 Gemini AI Companion Bots (NeoForge 26.3)

Bring your Minecraft world to life with **true autonomous AI companion players**! Powered by Google Gemini and an intelligent offline tactical engine, these companions don't just stand around — they walk, mine, craft, farm, breed animals, build houses with beds, defend your base with shields and jump-crits, operate locomotives and tractors, excavate pits, wear custom skins from your local folder, and chat naturally with you in multiplayer chat.

Works in **Singleplayer** and **Dedicated Multiplayer Servers**!

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

### ⚡ Offline Mode & API Key Control *(New in v1.2.5)*
* **Force Offline Tactical Mode**:
  * Run `/bot offline on` or `/bot offline true` to instantly force companions into 100% Offline Tactical Mode, even if an API key is present.
  * Can also be configured directly in `config/aicompanion-common.toml` with `forceOffline = true`.
  * Zero internet requests, zero API quota usage, ultra-fast local decision making!
* **Easy API Key Removal**:
  * Run `/bot apikey clear` or `/bot apikey remove` to delete your personal API key immediately in-game.
  * Run `/bot apikey clear all` (Admins/OPs) to wipe all player keys and server default keys.

### 🛢️ Crude Oil & Hazardous Fluid Avoidance *(New in v1.2.8)*
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

### 🚪 Doorway Centering & Strict Fence Boundaries *(v1.2.6)*
* **Smooth 1x2 Doorway Navigation**:
  * Companions cleanly recognize open doors and gates as walk-through passageways rather than solid walls.
  * Automatic shoulder micro-alignment centers the bot along the 1x2 doorway axis, preventing snagging on doorframes.
  * Approaching closed wooden doors will automatically open them and politely close them behind after passing.
* **Strict Fence Boundaries & Gate Routing**:
  * Fences and stone walls are strictly preserved as impenetrable boundaries — companions will **never** replace fence blocks with dirt, place blocks on top of fences, or pillar-jump over them!
  * When reaching livestock pens or fenced areas, companions automatically navigate to the nearest fence gate or existing opening, pass through, and close the gate behind to keep animals from escaping.

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

### 📖 1. Dynamic Recipe Learning, Training & Physical Memory Books *(New in v1.2.3)*
* **Natural Dynamic Recipe Learning**:
  * Teach companions completely new crafting workflows verbally in chat or via commands (e.g. *"place 9 wheat in the crafting table, 1 in each spot. Craft a hay bale"*, or *"to craft a chest use 8 oak planks"*).
  * The companion intelligently parses the target output, quantity, and ingredients into a structured recipe (`LearnedRecipe`).
  * Autonomously crafts learned recipes through `craftSingleItemSmart` during idle or work cycles without needing hardcoded recipes!
* **Written Book & Book & Quill Training**:
  * Right-click any companion with a **Written Book** (`Items.WRITTEN_BOOK`) or **Book & Quill** (`Items.WRITABLE_BOOK`) — the companion studies the pages, extracts custom recipes and operational guidelines, and acknowledges what they learned in chat!
  * **Sneak + Right-Click** (or titling the book "Global...") teaches the book's contents to **all companions globally**.
* **Physical Memory Book Export (`/bot memory book [global|botName]`)**:
  * Hands the player an authentic in-game Minecraft Written Book containing formatted chapters of all learned custom recipes, ingredient formulas, and behavioral rules.
* **Global & Personal Memory Banks**:
  * **Global Memories & Team Manual**: Directives and recipes shared across every bot on the server.
  * **Personal Memories**: Individual instructions stored per companion.
  * **AI Telemetry Injection**: All rules and recipes are injected directly into Gemini LLM prompt telemetry for real-time compliance.
  * **Persistent Storage**: All memories and learned recipes save cleanly to disk across server reboots.

### 🎭 2. Custom Skin Folder & Role-Themed Auto-Assignment *(v1.2)*
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
* **Wheat-to-Hay Auto-Compaction**: Automatically packs 9 wheat into 1 hay bale (`Items.HAY_BLOCK`) to save inventory and chest space. Enable or trigger on demand via chat (*"combine wheat into hay to save space"*).
* **Composter Integration & Seed Inventory Limits**: Farmers keep a set seed limit on hand (default: 32) and in chests (default: 64), proactively carrying all surplus seeds to nearby Composters (`Blocks.COMPOSTER`) to produce and harvest bone meal (*"only keep so many seeds in the chest and on hand, putting the rest in compost"*).
* **Multi-Chest Storage Logistics**: Automatically locates nearby single chests, double chests, and barrels. If a chest fills up during deposit, the farmer seamlessly switches to the next available container with free space.
* **Perimeter Access**: Freely reaches perimeter and boundary fence chests within 24 blocks of the farm without hitting perimeter boundaries.

### 🏊 13. Water Buoyancy, Anti-Drowning Reflex & Safe Respawn Placement
* **Active Water Buoyancy & Swimming Instincts**: Companions naturally tread water and swim up (`setJumping(true)` with upward lift), ensuring their eyes remain above the water level so air automatically regenerates in streams, canals, and waterways.
* **Emergency Self-Rescue (`escapeWaterToDryLand`)**: When submerged, low on oxygen (< 10 seconds remaining), or taking drowning damage, companions automatically locate the nearest dry bank and propel themselves out of water ditches and trenches onto solid ground.
* **Safe Dry Ground Respawn**: When killed, companions never respawn in water, lava, walls, or death traps. The system automatically searches for verified dry, solid, hazard-free ground near players, anchors, or world spawn, fully resets air and health, and grants 3 seconds of invulnerability.

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
  > *"goku dig a 10x10x2 box around us. starting y81 go up 2 tiles and 10 in each direction"* (clears 2-high room upwards from Y81 centered on player)
  > *"excavate here"*
* **Lumberjack & Tree Felling**:
  > *"cut this tree down"* / *"chop this tree"* (fells the entire tree you are looking at, replants matching sapling, and vacuums drops)
  > *"lumber note this spot, travel up to 100 blocks in each direction from here farming all trees, bring back logs"*
  > *"chop all the trees down in your 100x100 area save the logs"*
* **Exploration & Biome Finding**:
  > *"explorer can you go find us the closest ocean"* (scans world up to 6,400 blocks, reports compass heading & distance, saves waypoint, navigates there)
  > *"find desert"* / *"find jungle"* / *"where is the nearest mountain"*
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
* **Farming (Whole Mapped Areas, Compacting & Composting)**:
  > *"farm the area"* / *"farm this area"* / *"farm it"* (automatically farms full Surveyor Wand mapped zone)
  > *"farm FarmZone"*
  > *"combine the wheat into hay to save space"*
  > *"only keep so many seeds in the chest and on hand, putting the rest in compost"*
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
| `/bot spawn <name> [role]` | Spawn a new companion (`farmer`, `husbandry`, `guard`, `builder`, `miner`, `lumberjack`, `conductor`, `courier`) |
| `/bot help` | Display formatted categorized help guide and voice command examples |
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
| `/bot command <name> <order>` | Direct order override (follow, stop, hold, attack, resume) |
| `/bot tp` | Teleport nearest companion to your position |
| `/bot tp all` | Teleport all active companions to your position across all dimensions |
| `/bot tp <name>` | Teleport specific companion to your position |
| `/bot tp to <botName>` | Teleport player directly to the specified companion's location |
| `/bot train [botName] <instruction>` | Train a companion on a custom recipe or behavioral guideline |
| `/bot train global <instruction>` | Train all companions on a global team rule or custom recipe |
| `/bot train book [global\|botName]` | Study held Written Book or Book & Quill to learn recipes and guidelines |
| `/bot memory list [global\|botName]` | Display active personal and global memories and custom learned recipes |
| `/bot memory book [global\|botName]` | Give player an authentic physical Minecraft Written Book of recorded memories |
| `/bot memory clear <global\|botName>` | Clear recorded personal or global memories and recipes |
| `/bot ask <name> <request>` | Send a direct task/question to a companion |
| `/bot apikey [key]` | View API key status or set your personal/server Gemini key |
| `/bot apikey clear [all]` | Remove your personal API key or clear all keys from server (Admin) |
| `/bot offline [true\|false]` | Toggle or set force offline tactical mode (bypasses cloud API) |
| `/bot waypoint <name>` | Save your current coordinates as a named bot waypoint |

---

## 📋 Technical Requirements
* **Platform**: NeoForge 26.3
* **Minecraft Version**: 26.3
* **Java Version**: Java 25
* **Side**: Both (Client & Server)
* **License**: MIT
