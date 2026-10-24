# 🤖 Gemini AI Companion Bots (NeoForge 26.3)

Bring your Minecraft world to life with **true autonomous AI companion players**! Powered by Google Gemini and an intelligent offline tactical engine, these companions don't just stand around — they walk, mine, craft, farm, breed animals, build shelters, defend your base, and chat naturally with you in multiplayer chat.

Works in **Singleplayer** and **Dedicated Multiplayer Servers**!

---

## ⚡ Zero Configuration Needed (Offline Tactical Engine)
**No API key? No problem!** The mod features a built-in offline tactical decision engine. The moment you install the mod and spawn a companion, they can immediately:
* Mine ores and chop trees
* Craft tools, weapons, and armor
* Build emergency survival shelters
* Guard your compound without escaping fences
* Farm crops and breed livestock

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

### 🌾 1. Autonomous Farmer Role (`farmer`)
* **Assignable Farm Zones**: Tell your farmer where to work using natural chat (e.g. `"shawn, from this spot your farm is a 10x10 area"`).
* **Strict Maturity Harvesting**: Only harvests **100% fully mature crops**, leaving growing plants untouched.
* **Instant Replanting & Open Farmland Seeding**: Automatically replants harvested spots and plants empty tilled soil from inventory.
* **Hoe Ground Tilling**: Equips hoes, tills untilled dirt/grass into farmland inside the zone, and immediately plants seeds.
* **Smart Chest Storage & Seed Reserves**: Deposits harvested crops into nearby chests while always maintaining a reserve of seeds, carrots, and potatoes so it never runs out of planting supplies.
* **Chest Retrieval**: If out of seeds or tools, retrieves them from nearby chests automatically.

### 🐄 2. Animal Husbandry & Ranching (`husbandry` / `rancher`)
* **Autonomous Breeding**: Finds livestock (cows, sheep, pigs, chickens, horses) in the ranch zone that are ready to mate and feeds them breeding food.
* **Chest Supply Taking**: Retrieves wheat, carrots, or seeds from nearby chests if low on food.
* **Controlled Herd Culling**: Automatically manages animal populations when they exceed your stated limit:
  * **Strict Minimum Guard**: Will never cull below your minimum herd size threshold.
  * **Adults Only**: Babies are strictly protected and never attacked.
* **Drop Collection**: Vacuums drops (beef, leather, pork, mutton, wool, feathers) and deposits them into nearby storage chests.

### 🛡️ 3. Guard & Compound Defense (`guard`)
* **Compound Boundary Awareness**: Bots recognize fences, walls, and closed gates as compound perimeters. They patrol and hunt hostiles inside without jumping over fences or wandering off.
* **Deep Pit & Hazard Avoidance**: Companions actively detect 2+ block drops and pits, jumping small gaps or steering sideways rather than falling into ravines or holes.

### ⛏️ 4. Mining, Crafting & Survival
* **Multi-Step Crafting**: Tell a companion `"bring me a stone pickaxe"` — if they only have logs, they'll craft planks, sticks, a wooden pickaxe, mine stone, craft the stone pickaxe, and deliver it to you!
* **Emergency Shelters**: Type `"build a shelter"` to have the companion construct a protective 5x5 shelter or defensive wall.
* **Auto-Equip & Auto-Defend**: Automatically equips the best weapons, armor, and shields found in their inventory.

### 🚂 5. Train Conductor (`conductor` / `train`) *(New in v1.1)*
* **Full Locomotive Integration (`miningtools`)**: Automatically boards and commands locomotives, manages engine fuel, sounds horns, and controls throttle.
* **Precise Rail Distance Commands**: Say `"move ahead 4 tiles"`, `"forward 10 blocks"`, or `"reverse 5 blocks"` and your train rolls the exact distance and stops.
* **Smart Rail Pathfinding ("Come to Me")**: Say `"come here"` or `"drive to me"` while your conductor is on the train — they'll roll down the rails directly to your spot, stop within 4 blocks, and blow the whistle!
* **Automated Freight Logistics**: Tell your conductor to haul freight between stations (e.g. `"drive to StationA, pick up iron, return to StationB"`). They will load from station chests into attached freight cars, transport cargo across your rail network, and unload automatically upon arrival.
* **Instant Brakes**: Shout `"stop"`, `"brake"`, or `"cancel that"` to instantly stop the engine and clear active orders.

### 🚜 6. Tractor Automation (`tractor`) *(New in v1.1)*
* **Agricultural Machinery (`janemoose`)**: Companions can mount and steer rideable tractors equipped with plows, seeders, and mature crop harvesters across your farm fields.

### 💾 7. Bulletproof World Persistence & Chunk Loading *(Updated v1.1)*
* **Waypoints & Named Locations**: Teach companions locations in chat (e.g. `"remember this spot as Home"`) or use `/bot waypoint Base`. Bots can navigate back to saved locations anytime.
* **Aliases & Nicknames**: Companions remember what you name them (e.g. `"call yourself Bob"`).
* **Auto-Save on World Save**: All bot states, inventory, memories, farm areas, and ranch settings persist seamlessly across world reloads and server restarts.

---

## 💬 Natural Chat Command Examples

You can speak to your companions directly in public chat:

* **Farming**:
  > *"shawn. from this spot ur farm land is 10x10 area, plant wheet and carrots, put ur harvest in the chest, only harvest 100% mature crop"*
* **Husbandry**:
  > *"james, breed cows with wheat, kill adult cows when amount is over 10, only kill adults never have less then 5 cows at anytime."*
* **Mining & Crafting**:
  > *"miner, gather 20 iron ore and bring them to me"*
  > *"bob, craft 2 chests and a furnace"*
* **Waypoints & Guarding**:
  > *"stay here and guard this compound"*
  > *"remember this spot as Village"*
  > *"head to Village"*

---

## 🎮 Slash Commands

| Command | Description |
| :--- | :--- |
| `/bot spawn <name> [role]` | Spawn a new companion (`farmer`, `husbandry`, `guard`, `miner`, `lumberjack`, `builder`, `courier`) |
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
