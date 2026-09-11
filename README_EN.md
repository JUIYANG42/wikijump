# WikiJump

[简体中文](README.md) | **English**

A Minecraft **1.21.1** client-side mod, available for all three major loaders: **NeoForge / Forge / Fabric**.

Press one key to open the Minecraft Wiki page of the block, entity, or item you are looking at — right in your browser.

## Features

- **Container screens** (inventory, chest, creative menu, …): hover over an item and press the key → opens that item's wiki page
- **In-world**: look at a block and press the key → opens that block's page
- **In-world**: look at a mob and press the key → opens that mob's page (by type, custom names ignored)
- **No target**: falls back to the main-hand item (configurable)
- **Modded content support**: the mod detects whether the target comes from vanilla or a mod — modded blocks/items/mobs with a Chinese display name (i.e. a Chinese translation is installed) are looked up on [mcmod.cn (MC百科)](https://www.mcmod.cn/); those without one use the foreign mod encyclopedia, the FTB Wiki
- **Forced English foreign search**: hold Shift while pressing the key (`Shift+K` by default) to look the target up using its **in-game English name** (en_us translation, modded content included) — modded content goes to `moddedForeignUrl` (FTB Wiki by default), vanilla content to the English counterpart of the configured wiki (Chinese sites switch to `minecraft.wiki` automatically). Great for Chinese-named items when you want the English wiki instead
- Page titles use the in-game localized name (Chinese game → Chinese page), URL-encoded automatically
- Vanilla content defaults to the wiki matching the game language: Chinese → `zh.minecraft.wiki`, everything else → `minecraft.wiki`

## Lookup Rules

| Target | Where it opens |
|---|---|
| Vanilla block / item / mob | The wiki configured via `wikiSite` (`auto`: Chinese game → zh.minecraft.wiki, else minecraft.wiki) |
| Modded content + Chinese display name (translated) | mcmod.cn search (`moddedChineseUrl`) |
| Modded content + non-Chinese display name (untranslated) | FTB Wiki search (`moddedForeignUrl`) |
| When `wikiSite` is `custom:` | Everything uses `customUrl`, including modded content |
| Holding `Shift` while pressing the key (`Shift+K` by default) | Forced lookup with the **in-game English name**: modded content → `moddedForeignUrl`; vanilla → English counterpart of the configured wiki; `custom:` → `customUrl` |

## Default Keys

| Key | Action | Category |
|---|---|---|
| `K` | Open wiki page | Wiki Jump |
| `Shift + K` | Open the foreign wiki using the in-game English name | Wiki Jump |

Rebind it in `Options → Controls`.

## Config File

Location: `config/wikijump.json` (auto-generated on first launch)

```json
{
  "wikiSite": "auto",
  "customUrl": "https://minecraft.wiki/w/{name}",
  "moddedChineseUrl": "https://search.mcmod.cn/s?key={name}",
  "moddedForeignUrl": "https://ftb.fandom.com/wiki/Special:Search?query={name}",
  "fallbackToMainHand": true,
  "showOpenMessage": true
}
```

| Field | Description |
|---|---|
| `wikiSite` | `auto` (follow game language) / `minecraft.wiki` / `zh.minecraft.wiki` / `minecraft.fandom.com` / `minecraft.fandom.com/zh` / `custom:xxx` (with `customUrl`) |
| `customUrl` | Custom URL template; `{name}` is replaced with the page title, e.g. `https://wiki.biligame.com/mc/{name}` |
| `moddedChineseUrl` | Search URL template for modded content with a Chinese display name; defaults to mcmod.cn. Empty disables it (falls back to `wikiSite`) |
| `moddedForeignUrl` | Search URL template for modded content without a Chinese translation; defaults to the FTB Wiki. Empty disables it (falls back to `wikiSite`) |
| `fallbackToMainHand` | Whether to look up the main-hand item when the crosshair has no target |
| `showOpenMessage` | Whether to show an action-bar message when a page opens |

Tip: mcmod.cn also indexes English keywords — if Fandom/FTB Wiki is unreachable in your region, point `moddedForeignUrl` at `https://search.mcmod.cn/s?key={name}` too.

Restart the game after editing.

## Building from Source

Requires **JDK 21**. The project uses the Gradle Wrapper, no local Gradle needed:

```bash
# Build all three loaders
./gradlew build

# Or build a single loader
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :forge:build
```

Artifacts:

| Loader | jar path |
|---|---|
| Fabric | `fabric/build/libs/wikijump-fabric-1.21.1-*.jar` |
| NeoForge | `neoforge/build/libs/wikijump-neoforge-1.21.1-*.jar` |
| Forge | `forge/build/libs/wikijump-forge-1.21.1-*.jar` |

Try it in a dev environment: `./gradlew :fabric:runClient` (or `:neoforge:runClient` / `:forge:runClient`).

## Project Layout

```
wikijump/
├── common/    # Shared core (target resolution, URL building, config), compiled into each loader
├── fabric/    # Fabric adapter (key registration, screen/tick event bridge) + access widener
├── neoforge/  # NeoForge adapter + access transformer
├── forge/     # Forge adapter + access transformer
└── buildSrc/  # Gradle convention plugins (source-level sharing of the common module)
```

All three loaders use official Mojang mappings, so the shared code compiles identically everywhere.

## Known Limitations

- Only vanilla content (or content with wiki pages) resolves directly; modded targets open a **search** page rather than a direct article
- Does not intercept hovered items in JEI/EMI or other third-party screens
- A few wiki page titles differ from the in-game display name and land on a search/missing page
- `minecraft.fandom.com` / `ftb.fandom.com` are unreachable in some regions (use `auto`, or point the modded URLs at mcmod.cn)
- No need to install on the server (client-side only)

## License

[MIT](LICENSE)
