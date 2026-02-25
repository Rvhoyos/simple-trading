# Simple Trading

A lightweight, server-side player-to-player trading mod for **Minecraft 1.21.8**.  
Supports both **Fabric** and **NeoForge**.

## Features

- **Secure chest-based trade GUI** — a shared 6-row chest with one side per player and a centre divider
- **Anti-scam protection** — ready states reset whenever items are added or removed
- **Dual confirmation** — trade only executes when both players click "Ready"
- **Item safety** — closing the GUI or disconnecting cancels the trade and returns all items
- **Clickable chat buttons** — trade recipients get chat-based `[ACCEPT]` / `[DENY]` buttons
- **Request expiry** — pending requests expire after 60 seconds

## Commands

| Command | Description |
|---------|-------------|
| `/trade <player>` | Send a trade request to another player |
| `/trade accept` | Accept an incoming trade request |
| `/trade deny` | Deny an incoming trade request |

## Installation

Drop the mod JAR into your server's `mods/` folder:

- **Fabric** — requires [Fabric Loader](https://fabricmc.net/) ≥ 0.17.2 and [Fabric API](https://modrinth.com/mod/fabric-api)
- **NeoForge** — requires [NeoForge](https://neoforged.net/) ≥ 21.8

## Building from Source

```bash
./gradlew clean build
```

Built JARs are output to `fabric/build/libs/` and `neoforge/build/libs/`.

## License

[Apache 2.0](LICENSE)
