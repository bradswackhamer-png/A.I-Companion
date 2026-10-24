# 🤖 Gemini AI Companion Bots (NeoForge 26.3)

Bring your Minecraft world to life with **true autonomous AI companion players**! Powered by Google Gemini and an intelligent offline tactical engine, these companions don't just stand around — they walk, mine, craft, farm, breed animals, build houses with beds, defend your base with shields and jump-crits, operate locomotives and tractors, and chat naturally with you in multiplayer chat.

Works in **Singleplayer** and **Dedicated Multiplayer Servers**!

---

## ⚡ Zero Configuration Needed (Offline Tactical Engine)
**No API key? No problem!** The mod features a built-in offline tactical decision engine. The moment you install the mod and spawn a companion, they can immediately:
* Build complete village cottages with doors, windows, torches, and beds
* Open an interactive speedometer-style dashboard on right-click or **`B`** key
* Mine ores and chop trees
* Craft tools, weapons, and armor
* Guard your compound with shields and sprint-jump critical strikes
* Enforce global boundary borders set with the **Surveyor Wand**
* Farm crops, breed livestock, and operate trains and tractors

*(Optional)* Want full conversational AI personality, banter, and contextual planning? Add a 100% free Google Gemini API key in 30 seconds!

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

### 📊 1. Interactive Companion Dashboard & Speedometer HUD *(New in v1.1)*
* **Right-Click or Press `B` Key**: Open any companion's full dashboard by right-clicking them or pressing **`B`** (configurable in Controls).
* **Speedometer-Style Vital Gauges**: Clean color-coded health and hunger meters displaying real-time HP and food levels.
* **Full Inventory & Gear Management**: 41 dedicated slots covering Helmet, Chestplate, Leggings, Boots, Off-hand (Shield/Torch), and 36-slot backpack with shift-click transfer support.
* **One-Click Command Buttons**: Instantly order bots to `[Follow/Hold]`, `[Patrol]`, `[Build Cottage]`, `[Store in Chest]`, or `[Eat Rations]`.

### 🗺️ 2. Global Waypoint & Area System with Surveyor Wand *(New in v1.1)*
* **Surveyor Wand Tool (`/bot wand`)**:
  * Right-click blocks to link perimeter corners with glowing laser particle fences.
  * Sneak + Right-Click opens the **Area Configuration GUI** to name zones (`Compound`, `NorthFarm`) and select zone types (`COMPOUND`, `FARM`, `RANCH`, `SAFE_ZONE`, `RESTRICTED`).
  * Craftable in survival with **1 Compass + 2 Sticks** diagonally on a crafting table.
* **Perimeter Confinement**: Assigned companions (`/bot area assign <bot> <area>` or chat *"guard Compound"*) are locked within borders; movement vectors automatically reflect back toward the zone center.
* **AI Telemetry**: Real-time zone presence is automatically passed to Gemini AI prompts.

### 🏠 3. Autonomous Architectural Builder (`builder`) *(New in v1.1)*
* **Complete Village Cottages**: Tell your builder *"build a house"* or *"build cottage"*.
* **Procedural Construction**: Clears foliage, lays a solid 7x7 cobblestone foundation, raises Oak Log corner pillars, builds Oak Plank walls, adds glass windows, installs a working wooden door, roofs the structure, lights the interior with torches (no hostile spawns), and places a cozy bed.
* **Village Housing Registry**: Automatically tracks completed houses and available beds to support future colony expansion.

### ⚔️ 4. Player-Grade Tactical Combat & Squad Defense *(New in v1.1)*
* **Sprint-Jump Critical Strikes**: Companions jump into melee range and strike at peak downward velocity to deal guaranteed critical hits with spark particles.
* **Dynamic Shield Blocking**: Instantly raises off-hand shields when Creepers swell or Skeletons draw bows, absorbing damage before counter-attacking.
* **Squad Distress Dispatch**: When non-combat workers (farmers, miners, lumberjacks) are attacked, they call out in chat; nearby Guard companions within 48 blocks rush to neutralize the threat.
* **Pillar Jump Pit Rescue**: Detects drops $\ge 2$ blocks deep in advance; if trapped in deep holes, companions pillar-jump straight up using building blocks to climb out.

### 🌾 5. Autonomous Farmer Role (`farmer`)
* **Assignable Farm Zones**: Tell your farmer where to work using natural chat (e.g. `"shawn, from this spot your farm is a 10x10 area"`).
* **Strict Maturity Harvesting**: Only harvests **100% fully mature crops**, leaving growing plants untouched.
* **Instant Replanting & Open Farmland Seeding**: Automatically replants harvested spots and plants empty tilled soil from inventory.
* **Hoe Ground Tilling**: Equips hoes, tills untilled dirt/grass into farmland inside the zone, and immediately plants seeds.
* **Smart Chest Storage & Seed Reserves**: Deposits harvested crops into nearby chests while always maintaining seed reserves.

### 🐄 6. Animal Husbandry & Ranching (`husbandry` / `rancher`)
* **Autonomous Breeding**: Finds livestock in the ranch zone ready to mate and feeds them breeding food.
* **Controlled Herd Culling**: Automatically manages animal populations when they exceed your stated limit without ever harming babies or dropping below your minimum threshold.
* **Drop Collection**: Collects meat, leather, wool, and feathers and deposits them into storage chests.

### 🚂 7. Train Conductor (`conductor` / `train`) (`miningtools` Integration)
* **Locomotive Operation**: Automatically boards locomotives, manages fuel, controls throttle, and sounds whistles.
* **Precision Distance Control**: Say `"move ahead 4 tiles"`, `"forward 10 blocks"`, or `"reverse 5 blocks"`.
* **Rail Pathfinding**: Say `"come here"` or `"drive to me"` and the conductor rolls the train down tracks directly to you.
* **Automated Freight Logistics**: Transports cargo between stations, loading and unloading chests autonomously.

### 🚜 8. Tractor Automation (`tractor`) (`janemoose` Integration)
* Companions mount and steer rideable tractors equipped with plows, seeders, and harvesters across farm fields.

### 💾 9. Bulletproof World Persistence & Chunk Loading
* **Persistent Chunk Tickets**: Companions keep their chunks loaded so they remain active even when human players are far away.
* **Instant Client Rendering**: Immediate packet synchronization on player login eliminates invisible companion lag.

---

## 💬 Natural Chat Command Examples

You can speak to your companions directly in public chat:

* **Building**:
  > *"builder, build a house right here"*
  > *"build a cottage for the village"*
* **Areas & Guarding**:
  > *"guard Compound"*
  > *"stay inside Base and watch the perimeter"*
* **Farming**:
  > *"shawn, from this spot ur farm land is 10x10 area, plant wheat and carrots, put ur harvest in the chest, only harvest 100% mature crop"*
* **Husbandry**:
  > *"james, breed cows with wheat, kill adult cows when amount is over 10, only kill adults never have less then 5 cows at anytime."*
* **Trains**:
  > *"conductor, move ahead 10 blocks"*
  > *"drive to me"*
  > *"stop train"*

---

## 🎮 Slash Commands

| Command | Description |
| :--- | :--- |
| `/bot spawn <name> [role]` | Spawn a new companion (`farmer`, `husbandry`, `guard`, `builder`, `miner`, `lumberjack`, `conductor`, `courier`) |
| `/bot gui [name]` | Open the interactive Speedometer Dashboard & Inventory for a bot |
| `/bot wand` | Obtain the Surveyor Wand for defining global boundary areas |
| `/bot area list` | List all saved global areas and compound zones |
| `/bot area show <name>` | Display glowing laser boundary particles around an area |
| `/bot area assign <bot> <area>` | Confine a companion's patrol area strictly to a saved zone |
| `/bot area remove <name>` | Delete a saved global area |
| `/bot remove <name>` | Despawn a companion |
| `/bot list` | List all active companions and their current tasks |
| `/bot status <name>` | View HP, hunger, farm/ranch zones, and inventory |
| `/bot stop <name\|all>` | Stop all active bot tasks immediately |
| `/bot tp <name>` | Teleport companion to your position |
| `/bot ask <name> <request>` | Send a direct task/question to a companion |
| `/bot apikey [key]` | View API key status or set your personal/server Gemini key |
| `/bot waypoint <name>` | Save your current coordinates as a named bot waypoint |

---

## 📋 Technical Requirements
* **Platform**: NeoForge 26.3
* **Minecraft Version**: 26.3
* **Java Version**: Java 25
* **Side**: Both (Client & Server)
* **License**: MIT
