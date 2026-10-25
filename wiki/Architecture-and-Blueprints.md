# Architecture & Blueprints

## 🏠 Autonomous Architectural Builder (`builder`)
Builder companions construct multi-block architectural buildings autonomously:

### Built-in Structural Catalog
* **Watchtower ($5 \times 5$, 10 blocks tall)**: Multi-floor outpost with stone foundation, internal ladder climbing shaft, mid-floor guard deck, and open battlements with arrow slits (`"build watchtower"` / `"build tower"`).
* **Farmhouse Barn ($9 \times 9$, high-vaulted ceiling)**: High vaulted plank roof, double barn doors (Macaw's compatible), decorative hay bales, and deposit chests for farmers (`"build barn"` / `"build granary"`).
* **Blacksmith Forge ($7 \times 7$)**: Open-air industrial stone pavilion with chimney, Blast Furnace, standard Furnace, Crafting Table, and Anvil (`"build forge"` / `"build blacksmith"`).
* **Residential Cottage ($7 \times 7$)**: Starter home with foundation, log pillars, glass windows, wooden door, torch lighting, and bed (`"build house"` / `"build cottage"`).

### Workshop Chest Logistics
* Builders scan chests within 16 blocks of the construction site.
* If materials run short, raw logs in chests are autonomously crafted into wooden planks and sticks.
* Adaptive Modded Palette Engine: Dynamically adopts Macaw's doors, roofs, windows, lights, and furnitures if installed.

---

## 📐 In-World Blueprint Scanner (`/bot blueprint`)
Scan and clone any player-made or generated building in your world:

### 1. Capture In-World Buildings
1. Define a 3D cubed area around your building using the Surveyor Wand and `/bot area height`.
2. Run `/bot blueprint capture <areaName> <blueprintName>`.
3. The structure is saved with all block states, rotations, and relative offsets.

### 2. Inspecting Blueprints
* `/bot blueprint list` — List all registered blueprints.
* `/bot blueprint info <name>` — Display total block count, dimensions, and bill of materials.

### 3. Autonomous Builder Construction
Order any builder companion to build the scanned blueprint:
* Chat: `"builder, build <blueprintName>"`
* Command: `/bot blueprint build <botName> <blueprintName>` (or `/bot build <botName> <blueprintName>`)

The companion rotates the build to match their facing direction, clears the footprint, gathers materials from nearby chests, and constructs the structure block by block!
