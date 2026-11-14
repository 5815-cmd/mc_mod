# 灵植 / 炼丹 系统 —— 设计规格

> 依据 `docs/content-expansion-research.md` 的内容缺口审计：本 mod 原来**只有一株灵草**
> （`spirit_grass`），丹药也**只能在工作台直接合成**，于是"种 → 采 → 炼 → 用"
> 这条修仙游戏最核心的生产闭环是断的。本文档记录补完后的完整数值与口径。
>
> 所有内容**自研**：灵感来自通用修仙游戏的"灵田 / 丹炉 / 主辅料"套路，
> 不复制任何 mod 的素材或代码。贴图全部由 `xiuxian-mod/tools/gen_herb_alchemy_textures.py`
> 程序化生成（ASCII 剪影 + 自动描边 + 三级明暗，无渐变、无抗锯齿）。

## 一、灵植六阶

六株灵植都是 `SpiritHerbBlock`（原版 `CropBlock` 的灵植版，`age 0..7`，八阶生长、
可骨粉催熟、能被踩坏）。每株产出两件物品：`<herb>_seed`（种子）与 `<herb>_petal`（花瓣，炼丹材料）。

| id | 中文 | 品阶 | 用途方向 | 基础生长概率（每次随机刻） | 野生生成 |
|---|---|---|---|---|---|
| `xuelian_grass` | 雪莲草 | 1 | 回灵 / 疗伤 | 0.30 | 主世界，常见（count 8） |
| `huoyan_hua` | 火焰花 | 2 | 攻击 / 火系 | 0.27 | 主世界，常见（count 5） |
| `hanbing_teng` | 寒冰藤 | 3 | 控制 / 冰系 | 0.24 | 针叶林 `#minecraft:is_taiga`（count 6） |
| `leiyin_cao` | 雷音草 | 4 | 雷系 / 突破 | 0.21 | 山地 `#minecraft:is_mountain`（count 5） |
| `kongming_lan` | 空冥兰 | 5 | 高阶 / 空间 | 0.18 | 主世界，稀有（count 3 + 1/4 稀释） |
| `xueluo_zhi` | 血珞芝 | 6 | 顶级 / 血道 | 0.15 | 主世界，极稀有（count 2 + 1/12 稀释） |

**品阶越低越好种**：高阶灵植的基础概率更低（0.30 → 0.15），
同时野生成簇次数与稀有度也更苛刻，所以"高阶丹药"的瓶颈是**灵植**，不是别的。

### 1.1 生长速度与灵田土

`SpiritHerbBlock` **没有**照抄 `CropBlock#getGrowthSpeed`（那是个 static 方法，
子类没法按"脚下是什么方块"改倍率）。它直接覆盖 `randomTick`，只保留两条规则：

1. 光照 ≥ 9 才会生长（和原版作物一致）；
2. 每次随机刻以"土质概率"进阶一阶。

| 脚下方块 | 倍率 | 一阶雪莲草的每次随机刻概率 | 大致成熟时间* |
|---|---|---|---|
| **灵田土** `lingtian_soil` | **×3.0** | 0.90 | 约 12 分钟 |
| 耕地 `minecraft:farmland` | ×1.0 | 0.30 | 约 36 分钟 |
| 泥土 / 草方块 / 苔藓 / 黏土 / 沙子 | ×0.5 | 0.15 | 约 70 分钟 |

\* 按"每 1365 tick 轮到一次随机刻"估算，只用于说明量级；实际受区块加载与随机波动影响。

**灵田土（`lingtian_soil`）**
- 合成：9 个泥土（3×3）围 1 片灵叶 → **3 个灵田土**；
- 也可直接在创造标签页 / 坊市（见 §五 的缺口说明）获得；
- 每 12 tick 有 1/12 概率冒一颗绿色微光粒子（`HAPPY_VILLAGER`），一眼能看出哪块是灵田；
- 是整方块（不是 15/16 高的耕地），不需要锄头，也不会被踩回泥土。

### 1.2 掉落

掉落表在 `data/hongmeng/loot_tables/blocks/<herb>.json`：

| 池 | 条件 | 内容 |
|---|---|---|
| 1 | 无 | `<herb>_seed` ×1（**没长熟也掉**，种下去不会亏） |
| 2 | `age=7`（长熟） | `<herb>_petal` ×1 ~ N，N = 1 + ⌈品阶/2⌉（一阶 2 片，六阶 4 片），受时运加成 |

另外 `SpiritHerbBlock#playerDestroy` 还会：给"奇遇"修为（6 + 品阶×4），
并按 20% + 品阶×5% 的概率**额外**爆 1 片花瓣——鼓励"等到熟再采"。

### 1.3 保底：不用去野外也能开工

- 种子：`小麦种子 + 1 灵叶`（无序合成）→ **2 颗对应灵植种子**；
- 种子：`1 片对应花瓣`（无序合成）→ 1 颗种子（**农场自续种**，不必反复找野生株）；
- 灵田土：9 泥土 + 1 灵叶 → 3 灵田土；
- 因此"第一次种田"的成本只有 1 片灵叶 + 1 颗小麦种子。

## 二、野生成簇（世界生成）

每株灵植三个 JSON（生成器：`xiuxian-mod/tools/gen_herb_alchemy_data.py`）：

```
data/hongmeng/forge/biome_modifier/add_<herb>.json   → forge:add_features, step=vegetal_decoration
data/hongmeng/worldgen/placed_feature/<herb>.json    → rarity_filter? + count + in_square + heightmap + biome
data/hongmeng/worldgen/configured_feature/<herb>.json → random_patch → simple_block（air 过滤，age=0）
```

要点：
- 只长在空气里（`matching_blocks: minecraft:air`），且 `SimpleBlockFeature` 会再问一次
  `canSurvive`，所以**不会**硬塞进石头或水里；
- 生成的灵植是 `age=0`（幼苗），玩家采到的是"野生的幼苗/成株"，不是现成的药材——
  想要花瓣还是得等它长熟或者自己种；
- 生物群系标签只用原版确实存在的三个：`#minecraft:is_overworld`、`#minecraft:is_taiga`、
  `#minecraft:is_mountain`（标签名写错不会崩，但会一株都不长，所以自检里断言了前缀）。

## 三、丹炉与炼丹

### 3.1 方块与界面

| 项目 | 决定 |
|---|---|
| 方块 | `hongmeng:alchemy_furnace`（丹炉）。青铜色整方块，`facing` + `lit` 两个状态；点着时炉口冒烟冒火星并发出炉火噼啪声，模型换成 `alchemy_furnace_lit` |
| 界面 | 标准 `AbstractContainerMenu`（`AlchemyFurnaceMenu` + `AlchemyFurnaceScreen`），**不需要任何自定义网络包**：槽位由菜单同步，进度/燃料由 `ContainerData` 同步 |
| 布局 | 176×166；主料 (44,15) / 辅料 (44,39) / 燃料 (44,63) 竖排在左，产物 (116,35) 在右，中间一根 36px 的进度条，燃料槽左侧一根竖向燃料条 |
| 界面贴图 | **不画贴图**，全部用 `GuiGraphics#fill` 色块拼（GUI 贴图尺寸/缺失会直接崩客户端渲染，而这个界面只有四个槽和两根条） |
| 打开方式 | 右键丹炉（**没有新按键**，所以不存在 `key.hongmeng.*` 漏配的问题） |
| 拆炉子 | 内容物全部掉落（`Containers.dropContents`），不会吞材料 |

### 3.2 炼制规则

| 规则 | 值 |
|---|---|
| 炼制时间 | **200 tick / 10 秒**（所有丹方统一，`AlchemyRecipes.BREW_TICKS`） |
| 主料 / 辅料顺序 | **无关**：谁放主料槽、谁放辅料槽都能炼（`AlchemyRecipes.find` 会做双向匹配） |
| 燃料 | 见 §3.4 |
| 产物槽 | 只出不进；产物槽满（或堆不下）时丹炉**停下**，不浪费材料 |
| 出丹反馈 | `minecraft:block.brewing_stand.brew` 音效 + 10 颗 `HAPPY_VILLAGER` 粒子 |

方块实体每 tick 的逻辑（`AlchemyFurnaceBlockEntity#serverTick`）：

```
有燃料 → 燃料-1
主料+辅料命中丹方 且 产物槽放得下：
    没火了就吃一份燃料；有火 → 进度+1；进度≥200 → 扣 1 主料 + 1 辅料，产物入槽，进度归零
否则 → 进度归零（已经烧掉的燃料不退，与熔炉一致）
```

### 3.3 丹方总表（主料 + 辅料 → 丹药）

**`item/` 下的九颗丹药全部可炼**（自检会按"实现类是丹药类"扫描 `ModItems`，
以后新增丹药没配方会直接失败）：

| # | 主料 | 辅料 | 产物 | 数量 | 品阶 |
|---|---|---|---|---|---|
| 1 | `xuelian_grass_petal` 雪莲花瓣 | `lingye` 灵叶 | `pill_huiling` 回灵丹 | 2 | 1 |
| 2 | `xuelian_grass_petal` 雪莲花瓣 | `minecraft:wheat` 小麦 | `pill_exp_common` 普通修为丹 | 2 | 1 |
| 3 | `huoyan_hua_petal` 火焰花瓣 | `xuelian_grass_petal` 雪莲花瓣 | `zhaohua_dan` 造化丹 | 1 | 2 |
| 4 | `huoyan_hua_petal` 火焰花瓣 | `minecraft:glowstone_dust` 萤石粉 | `pill_exp_perfect` 完美修为丹 | 1 | 2 |
| 5 | `hanbing_teng_petal` 寒冰藤花瓣 | `spirit_crystal` 灵晶 | `pill_zhuji` 筑基丹 | 1 | 3 |
| 6 | `hanbing_teng_petal` 寒冰藤花瓣 | `minecraft:amethyst_shard` 紫水晶碎片 | `pill_xiling` 洗灵丹 | 1 | 3 |
| 7 | `leiyin_cao_petal` 雷音草花瓣 | `minecraft:blaze_powder` 烈焰粉 | `pill_pojing` 破境丹 | 1 | 4 |
| 8 | `leiyin_cao_petal` 雷音草花瓣 | `kongming_essence` 空冥精 | `pill_exp_immortal` 仙品修为丹 | 1 | 4 |
| 9 | `kongming_lan_petal` 空冥兰花瓣 | `daoyin` 道印 | `pill_shuangdao` 双道宫丹 | 1 | 5 |
| 10 | `kongming_lan_petal` 空冥兰花瓣 | `lingye` 灵叶 | `pill_exp_perfect` 完美修为丹（高阶方） | 1 | 5 |
| 11 | `xueluo_zhi_petal` 血珞芝花瓣 | `spirit_crystal` 灵晶 | `pill_zhuji` 筑基丹（血道方） | 1 | 6 |
| 12 | `xueluo_zhi_petal` 血珞芝花瓣 | `kongming_essence` 空冥精 | `pill_exp_immortal` 仙品修为丹（血道方） | 1 | 6 |

设计口径：**每一阶灵植都当过主料**（否则高阶灵植没有存在意义）、
**每颗丹药至少一条方子**、低阶丹药只用低阶灵植 + 常见材料，
高阶丹药才吃副本产物（灵晶 / 空冥精 / 道印）。

配方表在 `alchemy/AlchemyRecipes.java`，**纯字符串、零 Minecraft 类型**——
这样才能离线断言（`Blocks` / `ItemStack` 的静态初始化在没起游戏时会抛
`Not bootstrapped`）。表写坏的表现是**模组加载失败 + 一条说明原因的异常**，
而不是"玩家炼不出丹"。

灵植的品阶 / 用途 / 生长概率同样是一份**纯数据表** `alchemy/HerbCatalog.java`
（六株各一个 `Herb` 记录，`ModBlocks` 注册方块时把记录交给 `SpiritHerbBlock`，
数值只有这一份）。它也能离线断言——实测见 `tools/HerbOfflineProbe.java`：
`HerbCatalog` 能在没有游戏的 JVM 里完成静态初始化，而
`SpiritHerbBlock` / `ModBlocks` / `ModItems` 会抛 `Not bootstrapped`，
所以验证器<b>只能</b>断言前者，方块类一律不碰。

### 3.4 燃料：灵叶 + 烈焰粉（两者的取舍）

| 燃料 | 一格能烧 | 等于几炉 | 为什么 |
|---|---|---|---|
| **灵叶** `hongmeng:lingye` | 200 tick | **1 炉** | 本 mod 的硬通货，采灵草就有；"一片灵叶烧一炉"是最直观的换算 |
| **烈焰粉** `minecraft:blaze_powder` | 2400 tick | 12 炉 | 原版酿造台的老燃料，给"不想频繁添柴"的玩家；也和丹炉的合成配方呼应 |

**为什么主燃料选灵叶**：它把"采灵草 → 炼丹药"两件事绑在同一条资源轴上，
练气期玩家不必去下界就能开工；烈焰粉是效率选项，不是必需品。

## 四、完整玩家流程（可直接抄进测试清单）

```
1. 拿到第一颗种子：采一株野生雪莲草（或用 1 小麦种子 + 1 灵叶 合成 2 颗雪莲草种子）
2. 整地：9 个泥土围 1 片灵叶 → 3 个灵田土，摆在地上
3. 种植：手持「雪莲草种子」右键灵田土 → 出现幼苗（age=0）
4. 等待：灵田土上约 12 分钟长熟（骨粉可催熟，一等 2~5 阶）；成熟时贴图变成开花的大株
5. 采药：左键打掉成株 → 掉 1 颗种子 + 1~2 片雪莲花瓣（另有概率多爆一片）
6. 造丹炉：3×3 配方 灵铁锭/灵叶/炼药锅/烈焰粉（见 recipes/alchemy_furnace.json）
7. 炼丹：右键丹炉打开界面 → 主料槽放「雪莲花瓣」，辅料槽放「灵叶」，
   燃料槽放「灵叶」→ 进度条走满 10 秒 → 产物槽出现 2 颗回灵丹
8. 取丹：从产物槽拖出来（或 Shift 点击直接进背包）；燃料耗尽就再放一片灵叶
```

产物槽满了（或堆不下）丹炉会自动停下，不会白烧燃料。

## 五、已知缺口与刻意的取舍

1. **坊市（商店）里没有卖这些种子**：`shop/**` 不在本次改动范围内
   （`ShopCatalog.all()` 是静态列表，没有注册钩子，要加就得改那个文件）。
   替代路径已经够用：任意一株野生灵植 → 花瓣 → 种子 → 自续种，
   外加"小麦种子 + 灵叶 → 2 颗种子"的保底配方。补商店条目所需的
   `ShopEntry` 行写在交付报告里，交给坊市的负责人直接粘。
2. **工作台的旧丹药配方**（`recipes/pill_*.json`、`zhaohua_dan.json`）**保留**：
   删除属于"改别人的文件"，且会让已有存档的合成表突变。丹炉的优势是
   **产量/成本**（回灵丹 1 花瓣 + 1 灵叶 = 2 颗；普通修为丹同理），
   而不是"唯一途径"。若将来要彻底改成"只能炼丹"，只需删掉
   `data/hongmeng/recipes/pill_exp_common.json`、`pill_exp_perfect.json`、
   `pill_exp_immortal.json`、`pill_huiling.json`、`pill_pojing.json`、
   `pill_shuangdao.json`、`pill_xiling.json`、`pill_zhuji.json`、`zhaohua_dan.json`
   这九个文件，丹炉立刻成为唯一来源。
3. **没有做自动化接入**（漏斗/管道）：丹炉没有暴露 `IItemHandler` capability，
   自动化请等后续版本；`canPlaceItem` 已经按槽位约束好（产物槽只出不进、燃料槽只收燃料），
   未来接上去不需要改逻辑。
4. **远程/离线进度**：区块卸载时方块实体随区块保存（`Progress` / `Fuel` 都写进 NBT），
   重新加载后从原进度继续；但没有做"离线补算"。

## 六、验证方式

```powershell
# 1. 资源与模型
python tools/check_item_assets.py                 # 期望：无真实缺口（0 缺失）
python xiuxian-mod/tools/check_herb_alchemy.py    # 期望：通过 34 项，失败 0 项
#    （后者会真的跑编译产物里的 AlchemyRecipes.validate() 与 HerbCatalog.validate()，
#      再和 ModItems.java / ModBlocks.java / data/**.json / 双语 lang 对撞）
# 2. 重生成资源（可重复执行）
python xiuxian-mod/tools/gen_herb_alchemy_textures.py --preview
python xiuxian-mod/tools/gen_herb_alchemy_data.py
# 3. 进游戏
#    /setblock ~ ~ ~ hongmeng:alchemy_furnace
#    /setblock ~ ~ ~ hongmeng:xuelian_grass
#    /give @s hongmeng:xuelian_grass_petal 8
#    /give @s hongmeng:lingye 8
# 4. 服务端联机自检（需要服务端已在跑，RCON 已开）
powershell -ExecutionPolicy Bypass -File xiuxian-mod/tools/check_herb_alchemy_ingame.ps1
```

### 6.1 服务端实测记录（本机跑过，可复查）

```
[13:16:08] [Server thread/INFO] [minecraft/DedicatedServer]: Done (28.646s)! For help, type "help"
  （datapack 解析错误 0 条；8 张新方块全部 setblock 成功；新物品 id 全部可解析）

setblock 8 100 2 hongmeng:alchemy_furnace            -> Changed the block at 8, 100, 2
data merge block 8 100 2 {Items:[主料=雪莲花瓣×1, 辅料=灵叶×1, 燃料=灵叶×2]}
  ... 等 14 秒 ...
data get block 8 100 2
  -> {Progress: 0, Fuel: 0, Items: [{Slot: 3b, id: "hongmeng:pill_huiling", Count: 2b},
                                    {Slot: 2b, id: "hongmeng:lingye", Count: 1b}]}
  （主料与辅料各扣 1、一片灵叶烧掉 200 tick、产物槽出现 2 颗回灵丹 —— 与丹方表第 1 行完全一致）

data merge ... {主料=雪莲花瓣, 辅料=泥土, 燃料=灵叶}   （没有方子的组合）
  -> {Progress: 0, Fuel: 0, ...}   （不烧燃料、不出丹）

place feature hongmeng:xuelian_grass 8 100 24       -> Placed "hongmeng:xuelian_grass" at 8, 100, 24
fill ... minecraft:air replace hongmeng:xuelian_grass -> Successfully filled 5 block(s)
  （随机成簇真的在世界里长出了 5~6 株灵植）
```

