# Roles & Tactical AI

## 🧠 Intelligent Stuck Detection & Dynamic Obstacle Bypass Navigation
Companions are equipped with real-time environment analysis and dynamic unstuck pathfinding:

* **Distance Stagnation Detection**:
  * Instead of simple jitter tracking, companions monitor their actual progress toward the target (`moveNoProgressTicks`).
  * If distance cannot be decreased over 16 ticks (~0.8s), they recognize that they are blocked and evaluate their surroundings.
* **Hole & Trench Trap Analysis**:
  * Evaluates solid walls on 3+ sides at chest/head level.
  * Scans a $5 \times 5$ perimeter for natural steps or ramps within 2 blocks (`findNearbyExitStep`).
  * If trapped in a vertical pit ($\ge 2$ blocks deep) with no ramp, clears overhead obstructions with proper tools and executes a pillar jump (`executeHoleEscape()`).
* **Lateral Ray Probing for Walls & Buildings**:
  * Probes perpendicular left and right rays up to 8 blocks away to find the nearest corner or opening around obstacles.
  * Sets an intermediate **`activeBypassWaypoint`** around the corner so the companion maneuvers naturally around buildings and walls.
  * If facing a low 1–2 block fence or barrier with no opening within 8 blocks, the bot cleanly pillars over the barrier.
* **Deep Pit & Chasm Rim Search**:
  * When approaching a wide drop-off ahead with no bridging blocks, companions probe left and right along the rim for continuous terrain or natural crossings.

---

## ⚔️ Tactical Combat & Guarding (`guard`)
* **Sprint-Jump Critical Strikes**: Companions jump into melee range and strike at downward velocity peak for guaranteed critical damage.
* **Dynamic Shield Blocking**: Off-hand shields are raised when Creepers swell or projectile mobs (Skeletons/Pillagers) draw bows.
* **Worker Distress Network**: When non-combat workers take damage, they broadcast a distress call in chat; nearby Guards within 48 blocks charge to defend them.
* **Reverse Mob A\* Pathfinding & Anti-Spin**: Reverse paths to navigate around cover toward mobs; unreachable mobs time out after 4 seconds and enter a temporary 10-second blacklist.

---

## 🌾 Autonomous Farming (`farmer`)
* **Maturity-Only Harvesting**: Only harvests 100% fully grown crops, leaving developing plants intact.
* **Farmland Tilling & Replanting**: Equips hoes to till dirt/grass into farmland inside the farm zone and immediately replants seeds.
* **Smart Storage**: Stores harvested produce in nearby chests while retaining seed reserves.

---

## 🐄 Animal Husbandry & Ranching (`husbandry` / `rancher`)
* **Autonomous Selective Breeding**: Feeds breeding crops to mature livestock ready to mate.
* **Controlled Population Management**: Automatically culls adult animals when herd size exceeds your limit, never harming babies or dropping below your minimum herd threshold.
* **Drop Collection**: Gathers meat, leather, wool, and feathers into storage chests.

---

## 🚂 Train Conductor (`conductor` / `train`) (`miningtools` Integration)
* **Locomotive Operation**: Mounts locomotives, manages fuel and throttle, sounds horns.
* **Precision Distance Commands**: *"move ahead 4 tiles"*, *"forward 10 blocks"*, *"reverse 5 blocks"*.
* **Rail Pathfinding**: *"come here"* or *"drive to me"* rolls the locomotive down the tracks to your position.

---

## 🚜 Tractor Automation (`tractor`) (`janemoose` Integration)
* Mounts rideable tractors with attached plows, seeders, and harvesters to automate farm field work.
