# BugReport Plugin
Minimal Paper plugin for in-game bug reporting via DiscordSRV.

## Features

- `/bug <message>` command for players
- Sends report to a Discord channel through DiscordSRV
- Includes timestamp, player name, world, XYZ, and message
- Optionally includes the latest captured server log line
- Uses a configurable external embed template (`discord-embed.yml`)
- Supports live reload: `/bugreport reload` (in-game) and `bugreport reload` (console)

## Config
Edit `plugins/BugReport/config.yml` after first run:

Missing keys from newer plugin versions are auto-added on startup and `/bugreport reload`.
- `discordChannel`: DiscordSRV game channel mapping key (default: `admin`)
- `embedTemplateFile`: embed template file name in plugin folder (default: `discord-embed.yml`)
- `includeRecentConsoleLine`: include last captured server log line (`true`/`false`)
- `license.licenseKey`: server license key (`""` is allowed and maps to unlicensed mode)
- `cooldown.enabled`: enable/disable report cooldown (`true`/`false`)
- `cooldown.seconds`: cooldown duration per player
- `cooldown.message`: message shown when still on cooldown (`{seconds_left}` supported)
- `linkedDiscord.enabled`: enable/disable DiscordSRV account-link lookup
- `linkedDiscord.includeLinkedMention`: include `<@...>` mention when linked
- `linkedDiscord.unlinkedPlaceholder`: placeholder text when not linked (used by `{linked_discord_mention}`)
- `playerMessages.initializing`: shown immediately when `/bug` starts (default: `Initializing report`)
- `playerMessages.success`: shown when report is sent (default: `bug reported`)
- `playerMessages.failure`: shown when send fails

## License Validation on Startup

- On startup the plugin reads `license.licenseKey` from `config.yml`.
- The AuthAPI endpoint URL and `currentVersion` value are internal (not user-configurable in `config.yml`).
- It sends `POST /api/license/validate` with the required AuthAPI validation payload.
- Request timeout is 10 seconds.
- If mode is `blocked` (or `isValid: false`), startup is blocked and the plugin disables itself.
- If mode is `unlicensed`, startup continues and a non-intrusive console notice is logged every 24h.
- If the API is unreachable/times out, startup continues (fail-open) and a warning is logged.
- If `isUpdateAvailable: true`, a non-blocking update notification is shown (console + online OPs), including version and URL.

## Embed Template

Edit `plugins/BugReport/discord-embed.yml` to customize Discord output without changing Java code.

### Mention examples

- User mention: `<@123456789012345678>`
- Role mention: `<@&123456789012345678>`
- Everyone/here: `@everyone`, `@here`

Set these in `embed.mention` (or in any field/title/description using placeholders).

## Reload Commands

- In-game: `/bugreport reload` (requires `bugreport.reload`)
- Console: `bugreport reload`

### Available placeholders

- `{timestamp}`: formatted local timestamp (for example `2026-04-29 22:40:00 EDT`)
- `{timestamp_epoch}`: Unix seconds timestamp
- `{username}`: in-game player username
- `{world}`: world name
- `{x}`: X coordinate with 2 decimals
- `{y}`: Y coordinate with 2 decimals
- `{z}`: Z coordinate with 2 decimals
- `{xyz}`: combined coordinates (`x, y, z`)
- `{message}`: player report message (Minecraft color codes stripped)
- `{console_line}`: latest captured server log line (or `(none)`)
- `{chat_history}`: last N public chat lines, newline-separated (N set by `chatHistorySize` in config)
- `{mention}`: value of `embed.mention`
- `{linked_discord_mention}`: `<@discordId>` when linked, else configured placeholder
- `{linked_discord_id}`: raw linked Discord user id (empty if not linked)

You can use these placeholders in:

- `embed.title`
- `embed.description`
- `embed.footer`
- `embed.thumbnailUrl`
- `embed.fields[].name`
- `embed.fields[].value`
- `embed.mention`

## Requirements
- Paper 1.21+
- DiscordSRV installed and configured

## Build

```powershell
mvn clean package
```

The plugin jar will be in `target/`.