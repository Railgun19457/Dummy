# Dummy

Language / 语言: [简体中文](README.md) | English

![:Dummy](https://count.getloli.com/@railgun19457_Dummy?name=railgun19457_Dummy&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

Dummy is a fake player plugin for modern Paper servers. It creates real server-side fake player entities and provides inventory, equipment, experience, actions, skins, persistence, and chunk loading.

## Features

- Creates real `ServerPlayer` dummy players using Paper + NMS
- Supports spawn, remove, list, reload, and teleport management
- Opens a GUI by right-clicking a dummy, with inventory, armor, and offhand management
- Supports per-dummy settings: invulnerable, collision, physics, spawning, chunk loader, chunk radius, Tab visibility, and name format
- Uses the creator skin by default and supports fetching skins by premium player name
- Caches fetched skins to reduce repeated Mojang API requests
- Persists dummy inventory, armor, offhand, experience, skin, settings, and location
- Can keep removed dummy data and restore it when the same name is spawned again
- Loads chunks around a dummy using the dummy's own view distance (configurable radius `0`-`32`) for redstone and entity ticking; when disabled only the dummy's own chunk is kept
- Includes `zh_CN` and `en_US` language files
- Supports `/dummy` and `/dm` command aliases

## Support Matrix

| Item | Supported Range |
| --- | --- |
| Java | 21+ |
| Server | Paper |
| Verified Paper versions | 1.21.11, 26.1.2 |

## Installation

1. Download the plugin Jar from Releases, or build it locally.
2. Put the Jar into the Paper server `plugins` directory.
3. Start the server. The plugin will generate these files on first startup:
   - `plugins/Dummy/config.yml`
   - `plugins/Dummy/lang/zh_CN.yml`
   - `plugins/Dummy/lang/en_US.yml`
   - `plugins/Dummy/dummies.db`
   - `plugins/Dummy/skins.yml`

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/dummy` | Shows command help | `dummy.command` |
| `/dm` | Alias of `/dummy` | `dummy.command` |
| `/dummy spawn <name>` | Spawns a dummy at your current location; from the console it spawns at the main world spawn point | `dummy.command.spawn` |
| `/dummy remove <name>` | Removes one dummy | `dummy.command.remove` |
| `/dummy remove all` | Removes all manageable dummies | `dummy.command.remove` |
| `/dummy delete <name>` | Removes a dummy and deletes its saved data | `dummy.command.delete` |
| `/dummy list` | Lists active dummies | `dummy.command.list` |
| `/dummy reload` | Reloads config and language files | `dummy.command.reload` |
| `/dummy config <name> [key] [value]` | Opens the config GUI or updates dummy settings | `dummy.command.config` |
| `/dummy skin <name> set <playerName>` | Uses the skin of a premium player name | `dummy.command.skin` |
| `/dummy skin <name> clear` | Clears the dummy skin | `dummy.command.skin` |
| `/dummy exp <name> <amount|all>` | Transfers dummy experience to the command player | `dummy.command.exp` |
| `/dummy inv <name>` | Opens dummy inventory, armor, and offhand slots | `dummy.command.inv` |
| `/dummy tpto <name>` | Teleports you to a dummy | `dummy.command.tpto` |
| `/dummy tphere <name>` | Teleports a dummy to your current location | `dummy.command.tphere` |
| `/dummy tps <name>` | Swaps positions with a dummy | `dummy.command.tps` |
| `/dummy actions <name>` | Lists currently running repeated actions | `dummy.command.actions` |
| `/dummy actions <name> <action> ...` | Controls dummy actions | `dummy.command.actions` |

## Actions

Available actions:

- `attack` Attacks the current locked target; if none is locked, it attacks the looked-at visible entity and respects range, cooldown, and line-of-sight checks; enable `actions.attack.auto-target-nearest-visible` for the nearest-visible-entity fallback
- `chat <message>` Sends a chat message
- `command <command>` Runs a command as the dummy
- `drop` Drops the held item
- `hold <0-8>` Switches the selected hotbar slot
- `jump` Jumps
- `look direction <east|west|north|south>` Looks in a fixed direction
- `look entity [player [name]|monster]` Looks at an entity target; omitted type means the nearest visible entity, `player` without a name means the nearest visible player, and `monster` means the nearest visible hostile mob
- `look angle <yaw|~> <pitch|~>` Looks in a specific rotation; `~` keeps the current yaw or pitch, and repeated entity tracking turns smoothly while ignoring targets blocked by blocks
- `lookat <x> <y> <z>` Looks at a block position, supports `~`, `~1`, `~-1`
- `mine` Mines the looked-at block
- `mount` Mounts or dismounts a nearby mountable entity
- `move [speed|slow|walk|sprint]` Moves toward the current look direction; speed must be between `0` and normal sprint speed, defaults to normal walk speed, and speed/slowness potions stack on top
- `place` Places the main-hand block against the looked-at block
- `sneak [toggle|on|off]` Toggles or sets sneaking
- `swap` Swaps main-hand and offhand items
- `use` Uses the main-hand item
- `stop [action]` Stops all actions or one specific action

### Action Examples

```text
/dummy actions bot attack
/dummy actions bot attack repeat interval:20
/dummy actions bot attack repeat interval:20 duration:1200
/dummy actions bot look entity player repeat interval:1
/dummy actions bot lookat ~ ~ ~5
/dummy actions bot move repeat
/dummy actions bot move sprint repeat duration:100
/dummy actions bot jump repeat
/dummy actions bot
/dummy actions bot stop
/dummy actions bot stop attack
```

Action mode notes:

- If no mode is specified, the action runs once.
- `repeat interval:<ticks> duration:<ticks>` repeats the action; both `interval:` and `duration:` are optional.
- If `interval:` is omitted, the action default is used, for example `move repeat` is continuous walking and `jump repeat` is normal continuous jumping.
- `jump` repeat intervals are never shorter than one complete normal jump cycle.
- Repeated actions pause by default when a dummy dies, is removed, or quits, and continue when the same UUID dummy appears again. Disable this with `actions.preserve-on-lifecycle`.

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `dummy.command` | Allows using the base command | All players |
| `dummy.command.manage-all` | Allows managing dummies spawned by other players | OP |
| `dummy.command.spawn` | Allows spawning dummies | All players |
| `dummy.command.remove` | Allows removing one dummy | All players |
| `dummy.command.delete` | Allows removing dummies and deleting saved data | All players |
| `dummy.command.list` | Allows listing dummies | All players |
| `dummy.command.reload` | Allows reloading config | OP |
| `dummy.command.config` | Allows changing dummy settings | All players |
| `dummy.command.skin` | Allows changing dummy skins | All players |
| `dummy.command.exp` | Allows transferring dummy experience | All players |
| `dummy.command.inv` | Allows opening dummy inventories | All players |
| `dummy.command.tpto` | Allows teleporting to dummies | All players |
| `dummy.command.tphere` | Allows teleporting dummies to you | All players |
| `dummy.command.tps` | Allows swapping positions | All players |
| `dummy.command.actions` | Allows controlling dummy actions | All players |

Regular players can only list, complete, configure, inventory, teleport, transfer experience, run actions, remove, and delete dummies they spawned. Players with `dummy.command.manage-all` can operate on all dummies; `/dummy remove all` removes every dummy the sender can manage.

## Configuration Overview

Main `config.yml` sections:

- `language`: Language file, built-in values are `zh_CN` and `en_US`
- `defaults`: Default settings for newly created dummies
- `limits`: Server-wide and per-player dummy limits, use `-1` for unlimited (`max-chunk-radius` uses `-1` to follow the world `simulation-distance`)
- `storage`: Startup restoration and removed-data retention
- `inventory`: Whether inventory, armor, and offhand items drop when a dummy is removed or the plugin shuts down
- `commands`: Console commands executed before and after dummy creation
- `death`: Whether a dead dummy is automatically re-summoned
- `actions`: Action system settings, such as preserving lifecycle actions, attack fallback, and mount search range

Per-dummy settings configurable by command (defaults apply to newly created dummies):

| Setting | Default | Description |
| --- | --- | --- |
| `invulnerable` | `false` | Whether the dummy takes damage |
| `collision` | `true` | Whether the dummy collides with and pushes other entities |
| `physics` | `true` | Whether gravity and block collisions apply; disable it for a floating dummy that passes through blocks |
| `spawning` | `false` | Whether the dummy counts as a player for natural mob spawning; when disabled, the dummy does not make nearby chunks spawn mobs |
| `chunk-loader` | `true` | Whether chunks around the dummy are loaded and ticked using `chunk-radius` |
| `chunk-radius` | `0` | Chunk radius (`0`-`32`); the covered area is `(2r+1)×(2r+1)` chunks, e.g. `4` covers `9×9`. `0` loads only the dummy's own chunk, and random ticks (crops, fire spread) require a radius of at least 1. The upper bound comes from `limits.max-chunk-radius` |
| `show-in-tab` | `false` | Whether the dummy appears in the Tab list |
| `name-format` | `%name%` | Display name format, supports `%name%` |

Command usage: `/dummy config <name> <key> [value]`, where `chunk-radius` accepts `0`-limit and every other setting accepts `true`/`false`. In the GUI, "Chunk Range" cycles through `0`-limit with left click `+1` and right click `-1`, and shows the covered chunk area (for example `9x9 chunks`).

`limits.max-chunk-radius`: upper bound for a dummy's chunk radius. The default `-1` follows each world's `simulation-distance` (the server's own chunk loading radius); a concrete value fixes the bound but is still clamped to the API hard limit of `32`. Values above the bound are rejected by the command with the current limit shown, instead of being silently clamped.

Settings are independent: when `chunk-loader` is disabled, `chunk-radius` has no effect (it behaves like `0`, keeping only the dummy's own chunk).

### Spawning semantics

A dummy is a real server-side `ServerPlayer`, so by default the server treats it as a spawn-context player and makes chunks within 128 blocks spawn candidates. With `spawning: false` (the default) the plugin cancels the dummy's `PlayerNaturallySpawnCreaturesEvent`, which removes it from that decision:

- The dummy no longer makes nearby chunks spawn mobs, and no longer counts towards the local mob cap;
- When a real player walks nearby, their own spawning behaviour is unchanged;
- Mob spawners, breeding, spawn eggs, and structure spawning are not affected (with `spawning: true` the dummy keeps the vanilla behaviour);
- The implementation is one hash lookup per player per tick — no entity scans and no intercepting of spawn results.

### Chunk loading semantics

A dummy is a real server-side `ServerPlayer`, so the server's player chunk loader already loads and ticks chunks around it based on its own view distance. Dummy no longer registers per-chunk plugin tickets; it sets the dummy's own view distances instead:

| Setting | Effect |
| --- | --- |
| `chunk-loader: true` + `chunk-radius: n` | Tick area = a square of `(2n+1)` chunks centred on the dummy's chunk (`9x9` for `n=4`); the load area extends one more ring (the API view distance floor is 2) |
| `chunk-loader: false` | Treated as `n = 0`: only the dummy's own chunk is ticked |
| Send distance | Fixed at 2: the dummy never receives usable packets, so keeping it minimal avoids building chunk packets |

Keep in mind:

- With `chunk-radius: 0` only the dummy's own chunk is ticked and **no** random ticks happen (crops, fire spread, ice/snow); use a radius of at least 1 if you need them.
- Disabling `chunk-loader` does not make a dummy "load no chunks at all" — a dummy must live in a loaded chunk, so that chunk and the minimal load range stay loaded.
- The radius only affects that single dummy; it never changes the world's `simulation-distance` or the view distance of real players.

Command placeholders for `commands.before-spawn` and `commands.after-spawn`:

- `%dummy%`: Dummy name
- `%uuid%`: Dummy UUID
- `%creator%`: Creator name
- `%creator_uuid%`: Creator UUID
- `%world%`: Spawn world
- `%x%`: Spawn X coordinate
- `%y%`: Spawn Y coordinate
- `%z%`: Spawn Z coordinate

## Data Files

- `dummies.db`: SQLite database storing active and removed dummy data; WAL mode is enabled and items are stored as Bukkit-serialized BLOBs
- `skins.yml`: Stores skin cache fetched by player name
- `lang/zh_CN.yml`: Chinese language file
- `lang/en_US.yml`: English language file

Migration note: when upgrading from 0.3.x, the legacy `dummies.yml` is migrated into `dummies.db` on first startup (only when the database is empty) and the original file is renamed to `dummies.yml.bak`. Set `storage.migrate-from-yml` to `false` to skip the migration.

## Local Build

JDK 21 or newer is required. Use the bundled Gradle Wrapper (pinned to 9.8.1; paperweight 2.0.0-SNAPSHOT requires Gradle 9.7.1 or newer):

```bash
./gradlew clean build
```

On Windows:

```bat
gradlew.bat clean build
```

Build output:

- `build/libs/Dummy-<version>.jar`
