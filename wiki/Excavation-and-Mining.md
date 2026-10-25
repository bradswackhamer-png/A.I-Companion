# Excavation & Mining

## ⛏️ Interactive Verbal Hole Digging (`EXCAVATE`)
Companions support natural verbal interaction for excavating holes, basements, and foundation pits:

### 1. Conversational Dimension Flow
1. Look at the block you want to be the corner of your hole (or stand on it) and say in public chat:
   > *"dig a hole"* or *"dig a hole here"* or *"excavate here"*
2. The companion immediately marks the target block with emerald villager and flame particles, runs over to the location, and asks:
   > *"Marked starting point at (X, Y, Z)! How big and how deep should I dig from there? (e.g. '5x5 and 4 deep' or 'width 4 depth 6')"*
3. Reply naturally in chat (no bot name prefix required):
   * `"5x5 and 4 deep"`
   * `"4 by 4, 6 deep"`
   * `"width 6 depth 8"`
   * `"5x5"` (defaults to 3 blocks deep)
   * `"4 deep"` (defaults to 3x3 footprint)
   * `"cancel"` (aborts)
   * `"start here"` (updates starting corner if you repositioned)

### 2. One-Shot Dimension Chat
You can specify dimensions directly in your initial request:
> *"gemini, dig a 5x5 hole 4 deep right here"*

### 3. Systematic Serpentine Layer Excavation
* Companions clear foliage, equip the best tool (shovels for soil/gravel, pickaxes for stone/ores), and mine layer-by-layer downward from `dy = 0` to `-(depth - 1)`.
* True item drops are spawned and automatically vacuumed into the companion's inventory.
* Internal pit-bridging and hole-jumping behaviors are suspended during excavation so bots never fill in the hole they are digging.

---

## 🎯 Radius-Constrained Block Type Mining
You can order companions to clear or harvest specific block types within a radius around you:

### Chat Command Syntax
* *"mine all the stone in this 10 block area"*
* *"mine all coal within 15 blocks"*
* *"clear all the dirt in this 8 block area"*
* *"mine all the iron ore here"*

### Top-Down & Exposed-Block Prioritization
* Instead of digging random pits, companions scan all target blocks within the spherical radius, sort by exposed surface visibility, and systematically mine from highest Y level down.
* Companions respect player-placed building blocks (masonry, polished stone, stone bricks) and only mine natural world blocks matching the query.
