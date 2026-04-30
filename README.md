# MC-Plugins

All of my MC related plugins for Java (Spigot/Paper 1.20+).

## Project Structure

This is a Maven multi-module project. Each plugin lives under the `plugins/` directory and can be built independently or all at once from the root.

## Building

**Requirements:** Java 17+, Maven 3.6+

Build all plugins at once:

```bash
mvn clean package
```

Each plugin's JAR will be located at `plugins/<PluginName>/target/<PluginName>-1.0.0.jar`.

---

## Plugins

### 1. BasicUtilities

Provides essential player utility commands for everyday administration.

**Commands:**

| Command | Usage | Description | Permission |
|---------|-------|-------------|------------|
| `/heal` | `/heal [player]` | Heals a player to full health and removes fire | `basicutilities.heal` |
| `/feed` | `/feed [player]` | Restores a player's hunger to full | `basicutilities.feed` |
| `/fly` | `/fly [player]` | Toggles fly mode for a player | `basicutilities.fly` |
| `/god` | `/god [player]` | Toggles invincibility (god mode) | `basicutilities.god` |
| `/gamemode` | `/gamemode <mode> [player]` | Sets a player's gamemode (aliases: `/gm`) | `basicutilities.gamemode` |
| `/speed` | `/speed <1-10> [fly\|walk] [player]` | Sets walk or fly speed | `basicutilities.speed` |

**Permissions:**

| Permission | Default | Description |
|------------|---------|-------------|
| `basicutilities.*` | op | Grants all BasicUtilities permissions |
| `basicutilities.heal` | op | Use `/heal` on yourself |
| `basicutilities.heal.others` | op | Heal other players |
| `basicutilities.feed` | op | Use `/feed` on yourself |
| `basicutilities.feed.others` | op | Feed other players |
| `basicutilities.fly` | op | Toggle your own fly mode |
| `basicutilities.fly.others` | op | Toggle fly for other players |
| `basicutilities.god` | op | Toggle your own god mode |
| `basicutilities.god.others` | op | Toggle god mode for others |
| `basicutilities.gamemode` | op | Change your own gamemode |
| `basicutilities.gamemode.others` | op | Change gamemode for others |
| `basicutilities.speed` | op | Change your own speed |
| `basicutilities.speed.others` | op | Change speed for others |

---

### 2. HomeSystem

Allows players to save, manage, and teleport to personal home locations.

**Commands:**

| Command | Usage | Description | Permission |
|---------|-------|-------------|------------|
| `/sethome` | `/sethome [name]` | Sets a home at your current location (default name: `home`) | `homesystem.sethome` |
| `/home` | `/home [name]` | Teleports you to a saved home | `homesystem.home` |
| `/delhome` | `/delhome <name>` | Deletes a saved home | `homesystem.delhome` |
| `/homes` | `/homes` | Lists all your saved homes | `homesystem.homes` |

**Permissions:**

| Permission | Default | Description |
|------------|---------|-------------|
| `homesystem.*` | op | Grants all HomeSystem permissions |
| `homesystem.home` | true | Use `/home` |
| `homesystem.sethome` | true | Use `/sethome` |
| `homesystem.delhome` | true | Use `/delhome` |
| `homesystem.homes` | true | Use `/homes` |
| `homesystem.multiplehomes` | op | Bypass the max homes limit |

**Configuration (`config.yml`):**

```yaml
max-homes: 3          # Maximum homes per player (overridden by homesystem.multiplehomes)
teleport-delay: 3     # Seconds before teleporting (0 to disable)
cancel-on-move: true  # Cancel teleport if player moves during delay
```

---

### 3. TeleportSystem

Provides spawn management, `/back` to return to previous locations, and a TPA (teleport request) system.

**Commands:**

| Command | Usage | Description | Permission |
|---------|-------|-------------|------------|
| `/spawn` | `/spawn` | Teleports you to the world spawn | `teleportsystem.spawn` |
| `/setspawn` | `/setspawn` | Sets the world spawn at your location | `teleportsystem.setspawn` |
| `/back` | `/back` | Returns you to your last location (including after death) | `teleportsystem.back` |
| `/tpa` | `/tpa <player>` | Sends a teleport request to another player | `teleportsystem.tpa` |
| `/tpaccept` | `/tpaccept` | Accepts an incoming teleport request | `teleportsystem.tpaccept` |
| `/tpdeny` | `/tpdeny` | Denies an incoming teleport request | `teleportsystem.tpdeny` |

**Permissions:**

| Permission | Default | Description |
|------------|---------|-------------|
| `teleportsystem.*` | op | Grants all TeleportSystem permissions |
| `teleportsystem.spawn` | true | Use `/spawn` |
| `teleportsystem.setspawn` | op | Use `/setspawn` |
| `teleportsystem.back` | true | Use `/back` |
| `teleportsystem.tpa` | true | Use `/tpa` |
| `teleportsystem.tpaccept` | true | Use `/tpaccept` |
| `teleportsystem.tpdeny` | true | Use `/tpdeny` |

**Configuration (`config.yml`):**

```yaml
teleport-delay: 3     # Seconds before teleporting (0 to disable)
cancel-on-move: true  # Cancel teleport if player moves during delay
tpa-expire-time: 60   # Seconds before a TPA request expires
```

**Notes:**
- `/back` automatically saves your location on death and on non-plugin teleport events (e.g., entering a portal).
- Spawn locations are stored per-world in `spawns.yml`.
