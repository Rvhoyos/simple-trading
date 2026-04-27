# Simple Trading

A lightweight, server-side player-to-player trading mod for **Minecraft 1.20.1**.  
Supports both **Fabric** and **Forge**.

## Features

- **Chest-based trade GUI**: shared 6-row chest with one side per player and a center divider
- **Anti-scam protection**: ready states reset whenever items are added or removed
- **Dual confirmation**: trade only executes when both players click "Ready"
- **Item safety**: closing the GUI or disconnecting cancels the trade and returns all items
- **Clickable chat buttons**: trade recipients get `[ACCEPT]` / `[DENY]` buttons in chat
- **Request expiry**: pending requests expire after 60 seconds

## Commands

| Command | Description |
|---------|-------------|
| `/trade <player>` | Send a trade request to another player |
| `/trade accept` | Accept an incoming trade request |
| `/trade deny` | Deny an incoming trade request |

## Installation

Drop the mod JAR into your server's `mods/` folder:

- **Fabric**: requires [Fabric Loader](https://fabricmc.net/) >= 0.18.4 and [Fabric API](https://modrinth.com/mod/fabric-api)
- **Forge**: requires [Forge](https://files.minecraftforge.net/) >= 47.4.18

## Building from Source

```bash
./gradlew clean build
```

Built JARs are output to `fabric/build/libs/` and `forge/build/libs/`.

## License

[Apache 2.0](LICENSE)
