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

## 查询规则

| 目标类型 | 打开位置 |
|---|---|
| 原版方块 / 物品 / 生物 | `wikiSite` 配置的 Wiki（`auto`：中文 → zh.minecraft.wiki，其他 → minecraft.wiki） |
| 模组内容 + 中文显示名（有中文翻译） | mcmod.cn 搜索（`moddedChineseUrl`） |
| 模组内容 + 非中文显示名（无中文翻译） | FTB Wiki 搜索（`moddedForeignUrl`） |
| `wikiSite` 设为 `custom:` 时 | 全部使用 `customUrl`（包括模组内容） |
| 按住 `Shift` 按键（默认 `Shift+K`） | 强制用**游戏内英文名**搜索外网：模组内容 → `moddedForeignUrl`；原版 → 所配置 Wiki 的英文对应站；`custom:` → `customUrl` |

## 默认按键

| 按键 | 功能 | 分类 |
|---|---|---|
| `K` | 打开 Wiki 页面 | Wiki 跳转 |
| `Shift + K` | 用游戏内英文名搜索外网 Wiki | Wiki 跳转 |

可在游戏内 `选项 → 控件设置` 中修改。

## 配置文件

位置：`config/wikijump.json`（首次启动自动生成）

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

| 字段 | 说明 |
|---|---|
| `wikiSite` | `auto`（跟随游戏语言）/ `minecraft.wiki` / `zh.minecraft.wiki` / `minecraft.fandom.com` / `minecraft.fandom.com/zh` / `custom:xxx`（配合 `customUrl`） |
| `customUrl` | 自定义 URL 模板，`{name}` 会被替换为页面标题，例如 `https://wiki.biligame.com/mc/{name}` |
| `moddedChineseUrl` | 模组内容（显示名为中文）的搜索 URL 模板，默认 mcmod.cn；留空禁用（改用 `wikiSite`） |
| `moddedForeignUrl` | 模组内容（无中文翻译）的搜索 URL 模板，默认 FTB Wiki；留空禁用（改用 `wikiSite`） |
| `fallbackToMainHand` | 准星无目标时是否查询主手物品 |
| `showOpenMessage` | 打开页面时是否在动作栏显示提示 |

提示：mcmod.cn 也支持英文关键词搜索，若你所在地区无法访问 Fandom/FTB Wiki，可把 `moddedForeignUrl` 也改为 `https://search.mcmod.cn/s?key={name}`。

修改后重启游戏生效。

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
├── common/    # 共享核心逻辑（目标解析、URL 构建、配置），被三端分别编译
├── fabric/    # Fabric 适配器（按键注册、屏幕/刻事件桥接）+ access widener
├── neoforge/  # NeoForge 适配器 + access transformer
├── forge/     # Forge 适配器 + access transformer
└── buildSrc/  # Gradle 约定插件（源码级共享 common 模块）
```

三端统一使用 Mojang 官方映射，共享代码零差异编译。

## 已知限制

- 原版内容直达词条页面；**模组内容打开的是搜索页**而非直接词条
- 不拦截 JEI/EMI 等第三方界面的物品悬停区域
- 极少数页面标题与游戏内显示名不一致时会落到搜索/不存在页面
- `minecraft.fandom.com` / `ftb.fandom.com` 在部分地区无法直连（可改用 `auto`，或把模组查询 URL 都指向 mcmod.cn）
- 服务端不需要安装本模组（纯客户端功能）

## 许可证

[MIT](LICENSE)
