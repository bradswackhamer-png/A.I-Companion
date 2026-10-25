# Surveyor Wand & Saved Areas

## 🪄 The Surveyor Wand (`/bot wand`)
The Surveyor Wand allows players to visually mark 2D and 3D boundary zones in their Minecraft worlds.

### Crafting Recipe
* Craftable on a standard crafting table using **1 Compass** and **2 Sticks** arranged diagonally:
  * Top-Left: Compass
  * Center: Stick
  * Bottom-Right: Stick

---

## 📐 Marking Boundaries
1. **Corner Linkage**: Right-click block surfaces to set boundary vertices. Glowing laser particle fences immediately connect each corner.
   * Two corners create a 2-point diagonal rectangular box.
   * Three or more corners form a multi-point polygon.
2. **Configuration GUI**: Sneak + Right-Click with the wand in hand to open the Area Configuration GUI:
   * Enter a zone name (e.g. `Compound`, `NorthFarm`, `Quarry`).
   * Choose a zone type: `COMPOUND`, `FARM`, `RANCH`, `SAFE_ZONE`, or `RESTRICTED`.
   * Click **Save Area**.

---

## 🧊 3D Cuboid Volume Extrusion (`/bot area height` / `/bot area cube`)
You can transform flat boundary perimeters into 3D holographic volumes:

### 1. Positive Upward Extrusion
* `/bot area height Compound 15` — Projects a 15-block tall 3D wireframe volume with vertical corner beacons and upper/lower particle boundaries.

### 2. Negative Subterranean Pits (Quarries & Mines)
* `/bot area height Quarry -30` — Projects a 30-block deep subterranean volume beneath the surface.

### 3. Explicit World Y Ranges
* `/bot area height DeepMine -64 12` — Sets explicit bounding volume from Y = -64 to Y = 12.

---

## 🛡️ Perimeter Confinement & AI Telemetry
* **Boundary Confinement**: Assign a bot to an area with `/bot area assign <botName> <areaName>` or natural chat (*"guard Compound"*). The companion's movement vectors reflect off boundaries back toward the zone center.
* **Contextual AI Telemetry**: Companions pass their current area location to Gemini AI prompts, allowing the AI to understand room-by-room context.
