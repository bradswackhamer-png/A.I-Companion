# Commands & Setup

## 🔑 Gemini API Key Configuration
Gemini AI Companions operate with a built-in offline tactical decision engine even without an API key. For full conversational banter, contextual planning, and complex instructions:

1. Obtain a free key with no billing required at [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).
2. Run `/bot apikey YOUR_API_KEY_HERE` in-game.

---

## 🎮 Slash Commands Reference

### Companion Management
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/bot spawn <name> [role]` | `/bot spawn gemini builder` | Spawn a companion with an optional starting role |
| `/bot remove <name>` | `/bot remove gemini` | Despawn a companion |
| `/bot list` | `/bot list` | List all active companions, their health, and current task |
| `/bot status <name>` | `/bot status gemini` | Detailed readout of companion HP, hunger, and zones |
| `/bot stop <name\|all>` | `/bot stop all` | Stop current bot tasks immediately |
| `/bot tp <name>` | `/bot tp gemini` | Teleport companion to your player coordinates |
| `/bot ask <name> <request>` | `/bot ask gemini mine diamond` | Issue a direct command or question to a companion |

### Custom Skins (`aicompanion/skins`)
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/bot skin list` | `/bot skin list` | List all detected skins in folder and model types (Alex/Steve) |
| `/bot skin reload` | `/bot skin reload` | Hot-reload all skin textures from disk |
| `/bot skin set <bot> <skin>` | `/bot skin set gemini guard_knight` | Manually assign a specific skin to a companion |

### Speedometer GUI & Dashboard
* Press **`B`** (configurable in Controls) or right-click any companion to open the dashboard.
* Alternatively, run `/bot gui [name]`.

### Surveyor Wand & Boundary Areas
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/bot wand` | `/bot wand` | Give yourself the Surveyor Wand tool |
| `/bot area list` | `/bot area list` | List all saved areas |
| `/bot area height <name> <height\|minY> [maxY]` | `/bot area height quarry -20` | Extrude 3D holographic cuboid volume or set explicit min/max Y |
| `/bot area cube <name> <height\|minY> [maxY]` | `/bot area cube quarry 60 90` | Alias for `/bot area height` |
| `/bot area show <name>` | `/bot area show Base` | Display glowing laser particle boundary |
| `/bot area assign <bot> <area>` | `/bot area assign gemini Base` | Confine companion movement within the area |
| `/bot area remove <name>` | `/bot area remove Base` | Delete a saved area |

### Blueprint Scanner & Construction
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/bot blueprint capture <area> <name>` | `/bot blueprint capture Tower1 watchtower` | Scan in-world structure into custom blueprint catalog |
| `/bot blueprint list` | `/bot blueprint list` | List all saved blueprints |
| `/bot blueprint info <name>` | `/bot blueprint info watchtower` | View dimensions, block count, and material bill |
| `/bot blueprint build <bot> <name>` | `/bot blueprint build bob watchtower` | Order a builder bot to construct the blueprint |
