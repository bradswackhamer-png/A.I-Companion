# Custom Skins System

## 📁 Drop-In Skins Folder
The mod features an automatic drop-in skin directory located at:
`<game_directory>/aicompanion/skins`

Any standard **64×64** or legacy **64×32** `.png` skin file placed in this folder is automatically detected and registered by the game!

---

## 🎭 Role-Themed Auto-Assignment
You can organize skins by roles to make colonies look like authentic bustling towns:

### 1. Dedicated Role Folders
Place skin files inside category subdirectories:
* `aicompanion/skins/builder/`
* `aicompanion/skins/guard/`
* `aicompanion/skins/lumberjack/`
* `aicompanion/skins/rancher/`
* `aicompanion/skins/miner/`
* `aicompanion/skins/farmer/`
* `aicompanion/skins/conductor/`

### 2. Role Filename Prefixing
Alternatively, skins in the root folder can be prefixed with the role name:
* `guard_templar.png`
* `farmer_overalls.png`
* `miner_hard_hat.png`

When companions spawn or are assigned a role, they select skins from that role's category, rotating across variations so guards and workers have distinct uniforms.

---

## 👤 Exact Companion Name Matching & General Fallback Pool
* **1:1 Exact Match**: Name a skin after a specific companion (e.g. `gemini.png` or `bob.png`). That companion will always wear that skin upon spawning.
* **Fallback Pool**: Any general skin placed in `aicompanion/skins` that is not role-specific or name-specific forms the general pool used for new companions.

---

## 👗 Dual-Layer & Model Auto-Detection (Alex 3px vs Steve 4px)
* **Full Dual-Layer Outer Geometry**: Outer 3D jacket and hat accessories (helmets, hoods, goggles, toolbelts, chaps, satchels) render with full depth.
* **Automatic Model Detection**:
  * The engine checks transparency at pixel `(55, 20)` (the standard Alex slim arm indicator). If transparent, it assigns the 3px Slim arm model.
  * Filenames containing `_slim` or `alex` automatically force the 3px Slim model.
  * Otherwise, standard Steve (4px Classic) is used.

---

## 🎮 In-Game Commands
* `/bot skin list` — Display all registered skins in chat, their detected role tags, and model types.
* `/bot skin reload` — Hot-reload skins from the folder without restarting the game.
* `/bot skin set <botName> <skinName>` — Assign a specific skin to a companion (persists across world saves).
