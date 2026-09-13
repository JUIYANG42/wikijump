# WikiJump

[简体中文](README.md) | **English**

A Minecraft client-side mod for **NeoForge / Forge / Fabric**, shipped in two generations: **1.21.1** and **26.1**.

Press one key to open the Minecraft Wiki page of the block, entity, or item you are looking at — right in your browser.

| Minecraft | Loaders | jar |
|---|---|---|
| **1.21.1** | NeoForge / Forge / Fabric | `wikijump-<loader>-1.21.1-<version>.jar` |
| **26.1.x** | NeoForge / Fabric | `wikijump-<loader>-26.1.2-<version>.jar` |

Forge has no 26.x release (that loader stops at 1.21.x), so the 26.1 generation only builds for NeoForge and Fabric. Both generations share one copy of the core code and have identical features and config options.

## Features

- **Container screens** (inventory, chest, creative menu, …): hover over an item and press the key → opens that item's wiki page
- **In-world**: look at a block and press the key → opens that block's page
- **In-world**: look at a mob and press the key → opens that mob's page (by type, custom names ignored)
- **No target**: falls back to the main-hand item (configurable)
- **Modded content support**: the mod detects whether the target comes from vanilla or a mod — modded blocks/items/mobs with a Chinese display name (i.e. a Chinese translation is installed) are looked up on [mcmod.cn (MC百科)](https://www.mcmod.cn/); those without one use the foreign mod encyclopedia, the FTB Wiki
- **Forced English foreign search**: hold Shift while pressing the key (`Shift+K` by default) to look the target up using its **in-game English name** (en_us translation, modded content included) — modded content goes to `moddedForeignUrl` (FTB Wiki by default), vanilla content to the English counterpart of the configured wiki (Chinese sites switch to `minecraft.wiki` automatically). Great for Chinese-named items when you want the English wiki instead
- Page titles use the in-game localized name (Chinese game → Chinese page), URL-encoded automatically
- Vanilla content defaults to the wiki matching the game language: Chinese → `zh.minecraft.wiki`, everything else → `minecraft.wiki`
- **In-game settings screen**: run `/wikijump`, or press the **Config** button next to WikiJump in the mod list — every option is there, and changes apply immediately, no JSON editing required
- **Your own sites**: add "name + URL template" entries to the `sites` array in `config/wikijump.json` (MC百科, huijiwiki, the wiki of a modpack, …) and they appear in the settings screen's site dropdown, selectable like a built-in one
- **Command lookup**: `/wikijump <name>` looks up any name directly, without needing to hold an item or aim at a block
- **Key hint in tooltips**: the last line of every item tooltip names the key — and it follows the key you actually bound, so a rebind updates it automatically. Turn it off in the settings screen if you'd rather not have the extra line
- **JEI / EMI / REI compatible**: items in a viewer's item list or bookmark overlay can be looked up directly, without picking them up first. It takes **no dependency on any of the three APIs** — it rides the item-tooltip pipeline all of them share, so other overlays that draw item tooltips work as well

## Lookup Rules

| Target | Where it opens |
|---|---|
| Vanilla block / item / mob | The wiki configured via `wikiSite` (`auto`: Chinese game → zh.minecraft.wiki, else minecraft.wiki) |
| Modded content + Chinese display name (translated) | mcmod.cn search (`moddedChineseUrl`) |
| Modded content + non-Chinese display name (untranslated) | FTB Wiki search (`moddedForeignUrl`) |
| Items in a JEI / EMI / REI item list or bookmark overlay | Exactly the item rules above (it is simply "the item under the cursor") |
| When `wikiSite` is `custom:` | Everything uses `customUrl`, including modded content |
| When `wikiSite` is one of your own sites (a `name` from `sites`) | Everything uses that site's URL template, including modded content |
| Holding `Shift` while pressing the key (`Shift+K` by default) | Forced lookup with the **in-game English name**: modded content → `moddedForeignUrl`; vanilla → English counterpart of the configured wiki; `custom:` → `customUrl`; your own site → its `englishUrl`, or its `url` when that is empty |

## Default Keys

| Key | Action | Category |
|---|---|---|
| `K` | Open wiki page | Wiki Jump |
| `Shift + K` | Open the foreign wiki using the in-game English name | Wiki Jump |

Rebind it in `Options → Controls`.

## Commands

| Command | What it does |
|---|---|
| `/wikijump` | Open the settings screen |
| `/wikijump config` | Same as above |
| `/wikijump <name>` | Look up an arbitrary name on the configured wiki; spaces allowed (e.g. `/wikijump diamond sword`) |

Commands are client-side only, so they work in singleplayer and on any server without OP. Handy when writing docs — you don't have to find the thing in-game first.

## In-Game Settings Screen

Two ways in: run `/wikijump` in game, or select WikiJump in the mod list and press its **Config** button. NeoForge and Forge ship such a list and the button used to be greyed out; it now opens this screen and returns to the mod list when closed. Fabric has no built-in mod list, so install [Mod Menu](https://modrinth.com/mod/modmenu) to get the same button there (Mod Menu is optional — without it nothing changes).

Everything is adjustable:

- **Wiki site**: follow game language / minecraft.wiki / zh.minecraft.wiki / both Fandom wikis / **your own sites from the config file's `sites` array** / custom URL
- **Three URL templates**: custom, modded Chinese-name search, modded other-language search
- **Four switches**: fall back to the main-hand item, also work on JEI / EMI / REI item lists, show a message when opening, show the key hint in item tooltips

Changes take effect **immediately** and are written to `config/wikijump.json` when the screen closes. The custom-template box is greyed out unless the custom site is selected.

Opening the settings screen **re-reads the config file**, so sites you added to `config/wikijump.json` by hand show up in the dropdown as soon as you run `/wikijump` — no restart needed.

## Config File

Location: `config/wikijump.json` (auto-generated on first launch)

> Prefer the `/wikijump` settings screen; this section is for manual editing or version control.

```json
{
  "wikiSite": "auto",
  "customUrl": "https://minecraft.wiki/w/{name}",
  "moddedChineseUrl": "https://search.mcmod.cn/s?key={name}",
  "moddedForeignUrl": "https://ftb.fandom.com/wiki/Special:Search?query={name}",
  "sites": [
    { "name": "MC百科", "url": "https://search.mcmod.cn/s?key={name}" },
    { "name": "灰机wiki", "url": "https://mc.huijiwiki.com/wiki/{name}",
      "englishUrl": "https://ftb.fandom.com/wiki/Special:Search?query={name}" }
  ],
  "fallbackToMainHand": true,
  "showOpenMessage": true,
  "showTooltipHint": true,
  "overlayItemLookup": true
}
```

| Field | Description |
|---|---|
| `wikiSite` | `auto` (follow game language) / `minecraft.wiki` / `zh.minecraft.wiki` / `minecraft.fandom.com` / `minecraft.fandom.com/zh` / `custom:xxx` (with `customUrl`) / the `name` of one of your own `sites` |
| `customUrl` | Custom URL template, e.g. `https://wiki.biligame.com/mc/{name}` |
| `moddedChineseUrl` | Search URL template for modded content with a Chinese display name; defaults to mcmod.cn. Empty disables it (falls back to `wikiSite`) |
| `moddedForeignUrl` | Search URL template for modded content without a Chinese translation; defaults to the FTB Wiki. Empty disables it (falls back to `wikiSite`) |
| `sites` | Wiki sites you add yourself, see below |
| `sites[].name` | Site name, and the dropdown option label; must not clash with a built-in id (`auto`, `minecraft.wiki`, …) |
| `sites[].url` | That site's URL template, containing `{name}` |
| `sites[].englishUrl` | Optional. Used by the Shift (English) lookup; when empty, `url` is used with the English name |
| `fallbackToMainHand` | Whether to look up the main-hand item when the crosshair has no target |
| `showOpenMessage` | Whether to show an action-bar message when a page opens |
| `showTooltipHint` | Whether to append the key reminder to item tooltips (the text follows your actual keybinding) |
| `overlayItemLookup` | Whether the key also works on third-party screens such as the JEI / EMI / REI item lists (disable to use vanilla item slots only) |

About `sites`:

- Every entry needs a `name` and a `url` containing `{name}`; entries whose name is a duplicate or matches a built-in site are ignored, with a note in the log
- Selecting one of your sites is an **explicit choice**: vanilla and modded content alike go there, exactly like `custom:`
- `{name}` is URL-encoded (space → `%20`), so Chinese names and names with brackets open correctly
- An empty `sites` array behaves exactly as before

Tip: mcmod.cn also indexes English keywords — if Fandom/FTB Wiki is unreachable in your region, point `moddedForeignUrl` at `https://search.mcmod.cn/s?key={name}` too.

Changes made in the settings screen apply **immediately**; after editing this file by hand, opening the settings screen (`/wikijump`) once re-reads it, so no restart is needed. A file with broken JSON is renamed to `wikijump.json.broken` and the defaults are used, instead of being overwritten.

## Building from Source

Requires **JDK 21** for the 1.21.1 generation (**JDK 25** for the 26.1 one). The two generations are separate Gradle builds, each with its own wrapper — no local Gradle needed:

```bash
# 1.21.1: all three loaders
./gradlew build

# 26.1: NeoForge and Fabric only
cd modern && ./gradlew :fabric:build :neoforge:build
```

Artifacts:

| Generation | Loader | jar path |
|---|---|---|
| 1.21.1 | Fabric | `fabric/build/libs/wikijump-fabric-1.21.1-*.jar` |
| 1.21.1 | NeoForge | `neoforge/build/libs/wikijump-neoforge-1.21.1-*.jar` |
| 1.21.1 | Forge | `forge/build/libs/wikijump-forge-1.21.1-*.jar` |
| 26.1 | Fabric | `modern/fabric/build/libs/wikijump-fabric-26.1.2-*.jar` |
| 26.1 | NeoForge | `modern/neoforge/build/libs/wikijump-neoforge-26.1.2-*.jar` |

One gotcha on the 1.21.1 Fabric side: with the old Loom, `:fabric:jar` only produces the `-dev.jar` in `build/devlibs/`; the installable jar lands in `build/libs/`, so run `build`, not `jar`.

Try it in a dev environment: `./gradlew :fabric:runClient` (or `:neoforge:runClient` / `:forge:runClient`; for 26.1, run the same tasks from the `modern/` directory).

## Project Layout

```
wikijump/
├── common/    # Shared core (target resolution, URL building, config, settings screen, command tree), compiled into each loader
├── fabric/    # Fabric adapter (key registration, screen/tick event bridge) + access widener
├── neoforge/  # NeoForge adapter + access transformer
├── forge/     # Forge adapter + access transformer
├── buildSrc/  # Gradle convention plugins for the 1.21.1 generation
└── modern/    # The 26.1 generation: its own settings.gradle, wrapper and buildSrc
    ├── fabric/     # Files only this generation needs (access widener, …)
    ├── neoforge/   # Likewise
    └── overlay/    # The few replacements both loaders of this generation share
```

`modern/` builds from **the same `common/` sources** as the root build: it merges `common/` → the root `<loader>/` → `overlay/` → the project's own directory, in that order, with each later layer overriding same-named files from the earlier ones. Writing a file once is therefore the norm; only genuinely generation-specific code (the render entry points) goes into `overlay/`.

The 1.21.1 loaders all use official Mojang mappings; 26.x is unobfuscated and needs none. Shared code compiles identically across both generations.

## Known Limitations

- Only vanilla content (or content with wiki pages) resolves directly; modded targets open a **search** page rather than a direct article
- JEI / EMI / REI support works by following the item tooltip: only screens that **render item tooltips** can be queried, a fully hand-drawn list that draws none will not respond
- After clicking in a viewer, move the cursor back onto an item (a click clears the hover record — which is exactly what keeps typing in a viewer's search box from firing a lookup)
- A few wiki page titles differ from the in-game display name and land on a search/missing page
- The settings screen is a hand-rolled vanilla `Screen`; no config library such as Cloth Config is pulled in, keeping the mod dependency-free
- Your own sites (`sites`) are edited in the config file only: the settings screen lets you **select** one, it has no site-list editor
- No "mod id → Modrinth project page" shortcut: a registry namespace matches the Modrinth project slug only about 60% of the time (`twilightforest` vs `twilight-forest`, `cloth_config` vs `cloth-config`, `tconstruct` vs `tinkers-construct`), so building the URL directly would 404 constantly — modded content keeps using keyword search
- `minecraft.fandom.com` / `ftb.fandom.com` are unreachable in some regions (use `auto`, or point the modded URLs at mcmod.cn)
- No need to install on the server (client-side only)

## License

[MIT](LICENSE)
