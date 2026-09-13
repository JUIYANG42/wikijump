# WikiJump

**简体中文** | [English](README_EN.md)

一个 Minecraft **1.21.1** 客户端模组，同时支持 **NeoForge / Forge / Fabric** 三大加载器。

按一下按键，就能在浏览器中打开你正在查看的方块、生物或物品的 Minecraft Wiki 百科页面。

## 功能

- **容器界面**（物品栏、箱子、创造模式物品栏等）：鼠标悬停在一个物品上按键 → 打开该物品的 Wiki 页面
- **游戏世界**：准星指向方块时按键 → 打开该方块的页面
- **游戏世界**：准星指向生物时按键 → 打开该生物的页面（按类型，忽略自定义名字）
- **无目标时**：自动回退到主手物品（可在配置中关闭）
- **模组内容支持**：自动识别目标来自原版还是模组——模组物品/方块/生物且显示名为中文（即有中文翻译）时，优先在 [mcmod.cn（MC百科）](https://www.mcmod.cn/) 搜索；无中文翻译时使用国外的模组百科 FTB Wiki 搜索
- **强制英文外网搜索**：按住 Shift 再按键（默认 `Shift+K`），用目标**游戏内的英文名称**（en_us 翻译，模组内容同样支持）搜索外网 Wiki——模组内容走 `moddedForeignUrl`（默认 FTB Wiki），原版内容走所配置 Wiki 的英文对应站（中文站自动切换为 `minecraft.wiki`）。对中文名物品想查英文资料时特别有用
- 页面标题使用游戏内本地化名称（中文游戏 → 中文页面），URL 自动编码
- 原版内容默认 Wiki 站点自动跟随游戏语言：中文 → `zh.minecraft.wiki`，其他语言 → `minecraft.wiki`
- **游戏内设置界面**：输入 `/wikijump` 即可调整全部选项，改完即时生效，不必手改 JSON
- **自定义站点**：在 `config/wikijump.json` 的 `sites` 数组里写「站点名 + URL 模板」（比如 MC百科、灰机wiki、B站 wiki、某个整合包自建的百科），设置界面的站点下拉里就能直接选中，和内置站点一样用
- **命令查询**：`/wikijump <名称>` 直接查任意词条，不需要先拿着物品或对着方块
- **按键提示**：每个物品的提示（tooltip）最后一行会显示按键提醒，而且**跟着你实际绑定的按键走**（改键后自动更新），不必去翻文档才知道有这个功能；觉得占地方可在设置里关掉
- **兼容 JEI / EMI / REI**：查看器物品列表、书签栏里的物品也能直接按键查询，不用先把它拿到手里。实现上**不依赖任何一家的 API**，而是复用三家共用的物品提示渲染管线，因此连其他会画物品提示的界面（各类背包/查看器模组）也一并支持

## 查询规则

| 目标类型 | 打开位置 |
|---|---|
| 原版方块 / 物品 / 生物 | `wikiSite` 配置的 Wiki（`auto`：中文 → zh.minecraft.wiki，其他 → minecraft.wiki） |
| 模组内容 + 中文显示名（有中文翻译） | mcmod.cn 搜索（`moddedChineseUrl`） |
| 模组内容 + 非中文显示名（无中文翻译） | FTB Wiki 搜索（`moddedForeignUrl`） |
| JEI / EMI / REI 物品列表、书签栏中的物品 | 与上面的物品规则完全一致（等于「鼠标悬停的物品」） |
| `wikiSite` 设为 `custom:` 时 | 全部使用 `customUrl`（包括模组内容） |
| `wikiSite` 设为自己加的站点（`sites` 里某个 `name`） | 全部使用该站点的 URL 模板，同样包括模组内容 |
| 按住 `Shift` 按键（默认 `Shift+K`） | 强制用**游戏内英文名**搜索外网：模组内容 → `moddedForeignUrl`；原版 → 所配置 Wiki 的英文对应站；`custom:` → `customUrl`；自建站点 → 该站点的 `englishUrl`（留空则仍用 `url` 配英文名） |

## 默认按键

| 按键 | 功能 | 分类 |
|---|---|---|
| `K` | 打开 Wiki 页面 | Wiki 跳转 |
| `Shift + K` | 用游戏内英文名搜索外网 Wiki | Wiki 跳转 |

可在游戏内 `选项 → 控件设置` 中修改。

## 命令

| 命令 | 功能 |
|---|---|
| `/wikijump` | 打开设置界面 |
| `/wikijump config` | 同上 |
| `/wikijump <名称>` | 用当前配置的 Wiki 站查询任意名称，支持空格（如 `/wikijump 钻石剑`） |

命令是纯客户端的，单人游戏和多人服务器都能用，不需要 OP 权限。适合写文档、查资料时使用——不必先在游戏里找到那个东西。

## 游戏内设置界面

输入 `/wikijump` 打开，可调整全部选项：

- **Wiki 站点**：跟随游戏语言 / minecraft.wiki / zh.minecraft.wiki / 两个 Fandom 站 / **你在配置文件 `sites` 里加的站点** / 自定义 URL
- **三个 URL 模板**：自定义、模组·中文名搜索、模组·其他语言搜索
- **四个开关**：无目标时回退主手物品、兼容 JEI / EMI / REI 物品列表、打开页面时显示提示、物品提示中显示按键提醒

界面里的改动**立即生效**，关闭时自动写入 `config/wikijump.json`。未选中「自定义 URL」站点时，自定义模板输入框会置灰。

打开设置界面时会**重新读一次配置文件**，所以手动在 `config/wikijump.json` 里加好站点后，打开一次 `/wikijump` 就能在下拉里看到，不必重启游戏。

## 配置文件

位置：`config/wikijump.json`（首次启动自动生成）

> 推荐用 `/wikijump` 设置界面修改；这份说明供手动编辑或版本管理时参考。

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

| 字段 | 说明 |
|---|---|
| `wikiSite` | `auto`（跟随游戏语言）/ `minecraft.wiki` / `zh.minecraft.wiki` / `minecraft.fandom.com` / `minecraft.fandom.com/zh` / `custom:xxx`（配合 `customUrl`）/ `sites` 里某个站点的 `name` |
| `customUrl` | 自定义 URL 模板，例如 `https://wiki.biligame.com/mc/{name}` |
| `moddedChineseUrl` | 模组内容（显示名为中文）的搜索 URL 模板，默认 mcmod.cn；留空禁用（改用 `wikiSite`） |
| `moddedForeignUrl` | 模组内容（无中文翻译）的搜索 URL 模板，默认 FTB Wiki；留空禁用（改用 `wikiSite`） |
| `sites` | 你自己加的站点列表，见下 |
| `sites[].name` | 站点名，同时是设置界面下拉里的选项名；不能与内置站点 id（`auto`、`minecraft.wiki`…）重复 |
| `sites[].url` | 该站点的 URL 模板，含 `{name}` |
| `sites[].englishUrl` | 可选。按 `Shift` 查英文时用；留空则仍用 `url` 配英文名 |
| `fallbackToMainHand` | 准星无目标时是否查询主手物品 |
| `showOpenMessage` | 打开页面时是否在动作栏显示提示 |
| `showTooltipHint` | 是否在物品提示的最后一行显示按键提醒（内容跟随你的实际按键绑定） |
| `overlayItemLookup` | 是否让按键作用在 JEI / EMI / REI 的物品列表等第三方界面上（关闭后只在原版界面的物品槽上生效） |

关于 `sites`：

- 每项必须有 `name` 和带 `{name}` 的 `url`，缺一不可；名字重复、或与内置站点同名（`auto`、`minecraft.wiki`…）的条目会被忽略，日志里有提示
- 选中 `sites` 里的站点属于**显式选择**：不论原版还是模组内容、名字是不是中文，一律走该站点（和 `custom:` 一样）
- `{name}` 会被 URL 编码（空格 → `%20`），所以中文名、带括号的名字都能正常打开
- `sites` 为空数组时行为与之前完全一致

提示：mcmod.cn 也支持英文关键词搜索，若你所在地区无法访问 Fandom/FTB Wiki，可把 `moddedForeignUrl` 也改为 `https://search.mcmod.cn/s?key={name}`。

通过设置界面修改**即时生效**；手动编辑本文件后，打开一次设置界面（`/wikijump`）就会被重新读取，不必重启游戏。文件写坏（JSON 语法错误）时会被重命名为 `wikijump.json.broken` 并回退到默认值，不会直接覆盖掉你写的内容。

## 从源码构建

需要 **JDK 21**。项目使用 Gradle Wrapper，无需本地安装 Gradle：

```bash
# 构建全部三端
./gradlew build

# 或单独构建某一端
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :forge:build
```

产物位置：

| 加载器 | jar 路径 |
|---|---|
| Fabric | `fabric/build/libs/wikijump-fabric-1.21.1-*.jar` |
| NeoForge | `neoforge/build/libs/wikijump-neoforge-1.21.1-*.jar` |
| Forge | `forge/build/libs/wikijump-forge-1.21.1-*.jar` |

在开发环境中试运行：`./gradlew :fabric:runClient`（或 `:neoforge:runClient` / `:forge:runClient`）。

## 项目结构

```
wikijump/
├── common/    # 共享核心逻辑（目标解析、URL 构建、配置、设置界面、命令树），被三端分别编译
├── fabric/    # Fabric 适配器（按键注册、屏幕/刻事件桥接）+ access widener
├── neoforge/  # NeoForge 适配器 + access transformer
├── forge/     # Forge 适配器 + access transformer
└── buildSrc/  # Gradle 约定插件（源码级共享 common 模块）
```

三端统一使用 Mojang 官方映射，共享代码零差异编译。

## 已知限制

- 原版内容直达词条页面；**模组内容打开的是搜索页**而非直接词条
- JEI / EMI / REI 的兼容是"跟随物品提示"实现的：只有**会绘制物品提示**的界面才能查询，完全不画提示的自绘列表不会生效
- 在查看器里点击鼠标后，需要把鼠标重新移到物品上（点击会清空悬停记录——这正是为了避免你在查看器搜索框里打字时误触发查询）
- 极少数页面标题与游戏内显示名不一致时会落到搜索/不存在页面
- 设置界面是自绘的 vanilla `Screen`，故意不引入 Cloth Config 等第三方配置库，以保持零依赖
- 自建站点（`sites`）只能在配置文件里增删改，设置界面里只能**选择**站点，没有编辑站点列表的界面
- 暂不提供「mod id → Modrinth 项目页」的精确跳转：注册表 namespace 与 Modrinth 的项目 slug 只有约六成一致（`twilightforest` 对应 `twilight-forest`、`cloth_config` 对应 `cloth-config`、`tconstruct` 对应 `tinkers-construct`），直接拼 URL 会大量 404，因此模组内容仍走关键词搜索
- `minecraft.fandom.com` / `ftb.fandom.com` 在部分地区无法直连（可改用 `auto`，或把模组查询 URL 都指向 mcmod.cn）
- 服务端不需要安装本模组（纯客户端功能）

## 许可证

[MIT](LICENSE)
