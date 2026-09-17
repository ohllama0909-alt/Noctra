<p align="center">
  <img src="icon.png" alt="Noctra" width="200"/>
</p>

<p align="center">
  <a href="https://github.com/ohllama0909-alt/Noctra/releases/latest"><img src="https://img.shields.io/github/v/release/ohllama0909-alt/Noctra?style=flat-square&label=release" alt="Latest Release"/></a>
  <a href="https://github.com/ohllama0909-alt/Noctra/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/ohllama0909-alt/Noctra/ci.yml?style=flat-square&label=build" alt="Build Status"/></a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=flat-square" alt="Minecraft 1.21.11"/>
  <img src="https://img.shields.io/badge/Fabric-0.19.3-blue?style=flat-square" alt="Fabric"/>
  <img src="https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square" alt="MIT License"/>
</p>

---

**Noctra** is a client-side Fabric mod for Minecraft 1.21.11. It follows a vanilla+ philosophy — no new items, no new blocks, no server-side changes. Every feature is optional and toggled through an in-game menu.

---

## Features

### Utility
| Module | Description |
|---|---|
| Auto Totem | Automatically moves a totem of undying to your offhand when your health is low |
| Stack Refill | Refills a depleted stack from your inventory when it empties |
| Auto Eat | Automatically consumes food when hunger drops below a configurable threshold |
| Tool Selector | Switches to the optimal tool for the block you are breaking |
| Smart Replace | Replaces a block in your hand with the same type when it runs out |
| Anti AFK | Prevents AFK kicks by performing subtle movements at a configurable interval |
| Middle Click Pick | Pick-blocks item from your inventory with middle-click, like in Creative |
| Death Coordinates | Displays your coordinates at the moment of death in chat |
| Food Tooltip | Shows hunger, saturation, and eat-time values in item tooltips |
| Inventory Lock | Locks selected hotbar/inventory slots to prevent accidental drops or moves |
| Sprint Toggle | Toggles sprint with a key instead of double-tap |
| Sneak Toggle | Toggles sneak with a key instead of holding |

### Visual / HUD
| Module | Description |
|---|---|
| Coordinates HUD | Shows X/Y/Z coordinates as a movable overlay |
| Direction HUD | Displays cardinal direction and yaw angle |
| FPS & Ping Display | Shows frames per second and server ping |
| Armor Status HUD | Displays durability for all four armor pieces |
| Potion Effects HUD | Lists active potion effects with remaining time |
| Fullbright | Removes darkness underground (gamma override) |
| Durability HUD | Shows durability of the currently held item |
| Target HP | Displays the health of the entity you are looking at |
| Biome Display | Shows the biome name at your current position |
| Speed Display | Shows your current movement speed in blocks/second |
| Real-Time Clock | Overlay showing your system time |
| Saturation Bar | Renders a saturation indicator next to the hunger bar |
| Zoom | Smooth scroll-to-zoom keybind, no Optifine required |
| Crosshair Customizer | Change crosshair color and style |
| Hit Color | Changes the flash color when you deal damage to an entity |
| Item Age Timer | Displays a countdown above dropped items showing when they despawn |
| Rain Disable | Hides rain/snow visuals without changing weather |
| Scoreboard HUD | Movable, always-visible scoreboard overlay |
| MC Time Display | Shows in-game Minecraft time (ticks / hour) |
| Anti Fog | Removes depth fog for improved visibility |
| Anti Vignette | Removes the vignette effect at the screen edges |
| Item Info HUD | Shows item NBT / component data for the held item |
| Boss Bar Customizer | Toggles boss bar visibility and adjusts its position |
| Damage Indicator | Floating numbers above entities when they take damage |
| Held Item Info | Shows the name and durability of the held item below the hotbar |
| TPS Display | Displays server ticks per second |
| Memory Usage HUD | Shows JVM memory usage |
| Chunk Extension | Adds chunk coordinates to the Coordinates HUD |
| Chunk Render Stats | Displays rendered chunk count and render distance |
| Day Counter | Shows how many in-game days have passed |
| XP & Level HUD | Displays current XP level and progress to next level |
| Item Counter HUD | Counts a specific item across your whole inventory |
| Stack Counter HUD | Shows how many full stacks of the held item you are carrying |
| Altitude HUD | Displays your Y coordinate relative to sea level |
| Redstone Signal HUD | Shows the redstone signal strength of the block you look at |
| Speedometer HUD | Bar-style speedometer showing movement speed |
| Server Address HUD | Displays the current server IP/address |

### Combat
| Module | Description |
|---|---|
| Attack Cooldown Indicator | Visual indicator for the weapon attack cooldown |
| Hit Indicator | Screen flash or overlay when you deal or receive a hit |
| Kill Counter | Tracks how many entities you have killed this session |
| CPS Counter | Displays clicks per second |
| Auto Shield | Automatically raises your shield when taking damage |
| Reach Display | Shows the distance to the entity or block you are targeting |
| Arrow Counter | Displays how many arrows you are carrying |
| Damage Dealt HUD | Tracks and displays total damage dealt this session |
| Combo Counter | Counts consecutive hits without being hit back |

### Elytra
| Module | Description |
|---|---|
| Elytra Swap | Automatically swaps between elytra and chestplate on landing/takeoff |
| Pitch Lock | Locks your pitch angle while gliding for stable flight |
| Glide Stats HUD | Real-time altitude, speed, and glide ratio while flying |
| Firework Boost | Automatically uses a firework rocket when elytra speed drops |
| Elytra Landing Swap | Swaps back to chestplate automatically on landing |

### Chat
| Module | Description |
|---|---|
| Mention Highlight | Highlights messages that contain your username |
| Message Filter | Blocks or mutes chat messages matching configurable patterns |
| Quick Messages | Send preconfigured messages with a single keybind |
| Copy Coords | Copies your current coordinates to the clipboard with a command |

### World
| Module | Description |
|---|---|
| Waypoints | Create, label, and render named waypoints in the world |
| Light Level Overlay | Overlay showing light levels on blocks to locate hostile mob spawns |
| Chest Highlight | Highlights nearby chest blocks through walls |
| Cave Finder | Highlights nearby cave openings and air pockets |
| Slime Chunks | Highlights slime chunks based on the world seed |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11.
3. Download the latest `mandatory-<version>.jar` from [Releases](https://github.com/Snenjih/Mandatory/releases/latest).
4. Drop the JAR into your `.minecraft/mods/` folder.
5. Launch Minecraft. Press **Escape → Mandatory** to open the module carousel.

---

## Usage

- Open the **Noctra** menu from the pause screen (or press Right-Shift).
- Browse and configure features across all categories.
- Click the toggle buttons to enable or disable modules.
- Click the settings icon to configure module-specific options.
- Enabled/disabled states and settings are saved automatically to `.minecraft/config/noctra.json`.

---

## Building from Source

**Requirements:** JDK 21, Git

```bash
git clone https://github.com/ohllama0909-alt/Noctra.git
cd Noctra
./gradlew build
```

The release JAR is produced at `build/libs/noctra-<version>.jar`. The `-dev` and `-sources` variants are build artifacts only.

```bash
# Launch Minecraft with the mod loaded for testing
./gradlew runClient
```

---

## Tech Stack

| Component | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Yarn Mappings | 1.21.11+build.6 |
| Java | 21 |

---

## Configuration

Settings are stored in `.minecraft/config/noctra.json`. The file is created automatically on first launch and is backwards-compatible — legacy configs (`mandatory.json`) are migrated automatically.

---

## License

[MIT](LICENSE) — © 2025 Snenjih
