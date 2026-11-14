# 品质系统 + 新群系 规格（2026-10-10）

对应玩家原话：

> 「其他品质的丹药没有雷劈……然后其他的东西增加品质什么的东西多一些内容」
> 「丹药是品阶比如一种丹药有普通和完美这样」
> 「在地面生成灵植什么的」

路线图：`docs/modpack-roadmap.md` 二.2（品质系统）、四.1（新群系）。

---

## 一、品质阶梯（六档）

口径：与既有的**灵根 7 品**（`spirit/Rarity`）、**道宫 6 品**（`domain/DomainQuality`）
共用中文命名体系；「凡品」「仙品」两个词与它们完全一致。六档正好等于**六阶灵植**，
于是「一阶雪莲草花瓣 = 凡品 …… 六阶血珞芝花瓣 = 神品」是一条不需要额外表的对应关系。

| 档位 | 英文键 | 颜色 | 丹药/效果倍率 | 法宝倍率 | 回收价 | 抽取权重‰ |
|---|---|---|---|---|---|---|
| 凡品 | `mortal` | §7 灰 | ×1 | ×1.00 | 100% | 600 |
| 良品 | `fine` | §f 米白 | ×1.5 | ×1.06 | 130% | 220 |
| 上品 | `superior` | §a 绿 | ×3 | ×1.15 | 170% | 100 |
| 极品 | `exquisite` | §d 粉紫 | ×5 | ×1.28 | 230% | 50 |
| 仙品 | `immortal` | §5 金 | ×10 | ×1.45 | 320% | 25 |
| 神品 | `divine` | §6 品红 | ×20 | ×1.70 | 450% | 5 |

**为什么效果倍率必须是 1 / 3 / 10**：既有三档修为丹（普通 / 完美 / 仙品）的
效果是 ×1 / ×3 / ×10，tooltip、商店价、老存档全部按这个数字写。
六档阶梯把三档**嵌进去**（普通→凡品、完美→上品、仙品→仙品），
倍率一个数字都没改。`PillQuality.multiplier()` 现在直接委派给 `Quality`，
**倍率只有一份**（`quality/Quality.java`）。

**为什么法宝倍率比丹药保守**（神品只 +70%）：法宝是五件套叠加的，
按 ×20 放大等于 100 倍护甲，`ArmorMitigation` 的软上限都救不回来；丹药是一次性的。

代码：`quality/Quality.java`（纯数据 + `validate()`）。

---

## 二、品质是怎么来的（获取表）

`quality/QualitySource.java`（纯数据，权重之和必须 = 1000‰）：

| 途径 | 凡品 | 良品 | 上品 | 极品 | 仙品 | 神品 | 难度偏移来源 |
|---|---|---|---|---|---|---|---|
| 合成 | 1000 | 0 | 0 | 0 | 0 | 0 | 无（固定凡品） |
| 炼丹 | 400 | 300 | 180 | 80 | 35 | 5 | 最优灵植品阶（一阶→0 … 六阶→5） |
| 妖兽掉落 | 520 | 260 | 130 | 60 | 25 | 5 | 怪物生命上限（四道阈值 → 0..4） |
| 副本掉落 | 300 | 280 | 200 | 140 | 65 | 15 | 同上，起点更高 |
| 任务奖励 | 260 | 300 | 230 | 130 | 65 | 15 | 同上 |

**难度偏移（shift，0..5）**：掷出基础档位后，每级偏移有 **25%** 概率把结果抬高**一档**
（最多抬到神品）。shift = 0 就是基础表本身；偏移每加一级，**期望档位严格上升**——
这条用**解析解**（`expectedRankExact`）断言，不用抽样，避免"20 万次抽样在 shift 4/5
之间抖动"导致的偶发自检失败。

**四条落地规则**：

1. **合成 = 固定凡品，但不许把好东西拉低**：`craftedQuality(id) = max(凡品, 天生品质)`。
   合成一颗完美修为丹仍然是上品（×3），不会被打成凡品（×1）。
2. **天生品质**（没有 NBT 时的默认档）只有两类不是凡品：
   三档修为丹（×1/×3/×10 锚点）与六阶灵植花瓣（= 灵植品阶）。
3. **掷骰子只掷一次**：已经有品质 NBT 的物品不会被重新掷
   （否则"丢出去再捡回来"能刷品质）。
4. **玩家死亡掉落不掷**（`LivingDropsEvent` 跳过玩家）。

覆盖范围（`quality/QualityTable.java`，纯数据、离线可断言）：
九颗丹药（`AlchemyRecipes.PILL_IDS` 全覆盖）、5 部位 × 10 境 = 50 件法宝、
10 把法宝剑、6 种灵植花瓣、妖丹（`jidan`）——共 **76 件**。

---

## 三、品质实际做了什么

| 系统 | 落点 | 效果 |
|---|---|---|
| **丹药·修为丹** | `item/ExperiencePillItem.java` | 给的修为 = 当前小境界需求 × 25% × **品质倍率**。NBT 品质优先，其次物品自带三档。于是可能存在「神品普通修为丹」（×20）。 |
| **丹药·回灵丹** | `item/ManaPillItem.java` | 回复量 = `ManaTable.pillRestore(tier)` × **品质倍率**（凡品 ×1 → 神品 ×20，上限仍是灵力上限）。 |
| **法宝** | `item/gear/ArtifactItem.java` + `GearManager` | 护甲 / 韧性 / 额外生命 × **品质倍率**（×1.00 → ×1.70）。`GearManager.refresh()` 走的是带 stack 的取值。 |
| **法宝剑** | `item/gear/RealmWeaponItem.java` + `WeaponAttributeHandler` | 附加攻击与增幅 × **品质倍率**（鸿蒙道剑神品：增幅 45% → 76.5%）。 |
| **灵植花瓣** | `quality/AlchemyQuality.java` | 花瓣品质 = 品阶；炼丹出锅品质按最优灵植品阶掷（见下"需要别人配合的两处"）。 |
| **卖出价格** | `quality/QualityTable.sellPrice(base, q)` | `floor(基础价 × 品质% / 100)`，下限 1。凡品 = 原价（商店永远只产出凡品，防套利断言不受影响）。 |
| **tooltip** | `quality/QualityEvents.onTooltip` | 统一的彩色一行「品质：上品 效果 ×3」。走 `ItemTooltipEvent`，新增有品质的东西不用再改每个物品类。 |

**持久化（NBT）**：`quality/ItemQuality.java`

```
hongmeng_quality        字符串   mortal/fine/superior/exquisite/immortal/divine
hongmeng_quality_rank   整数     0..5（字符串丢了也能兜住）
hongmeng_quality_rolled 布尔     true = 掷过骰子（合成章不带这个标记）
```

读取优先级：NBT 键 → NBT 序号 → 物品自带档位（丹药的 `PillQuality`）→ 天生品质 → 凡品。
**老存档（完全没有这些 NBT）读出来一个数字都不变。**

---

## 四、需要别人配合的两处（本次**没有**改，因为文件不在我的辖区）

### 1. 炼丹出锅品质 → `alchemy/AlchemyFurnaceBlockEntity.java`

品质规则已经全部实现且离线可断言，只差丹炉调用一行。
`applyOutcomePills(Level, BlockPos, AlchemyRecipes.Brew, int)` 里：

```java
ItemStack out = new ItemStack(result, Math.min(count, getMaxStackSize()));
// ↓ 新增这一行（主料/辅料品阶 → 掷品质 → 写 NBT + tooltip）
com.hongmeng.xiuxian.quality.ItemQuality.applyAlchemy(
        out,
        com.hongmeng.xiuxian.quality.AlchemyQuality.herbTierOf(itemId(items.get(SLOT_MAIN))),
        com.hongmeng.xiuxian.quality.AlchemyQuality.herbTierOf(itemId(items.get(SLOT_AUX))),
        java.util.concurrent.ThreadLocalRandom.current());
items.set(SLOT_RESULT, out);
```

（`AlchemyQuality.rollByItems(mainId, auxId, rng)` 是等价的字符串版本。
`items` 是丹炉的槽位表，`SLOT_MAIN` / `SLOT_AUX` 都在同一个类里。）

规则：`shift = clamp(max(主料品阶, 辅料品阶) - 1, 0, 5)`，
一阶雪莲草 → shift 0（大多凡品），六阶血珞芝 → shift 5（神品概率显著，**但仍可能出锅凡品**）。
期望品质随品阶严格上升；一阶出货神品 ≤ 5‰。

### 2. 卖出价格 → `shop/SellPrices.java`（+ 卖出的调用点）

`SellPrices.unitPrice(String)` 是纯字符串入参，拿不到物品品质。建议加一个重载：

```java
/** 带品质的单件回收价：凡品 = 原价（防套利断言不变），高品质按百分比加价。 */
public static int unitPrice(String itemId, com.hongmeng.xiuxian.quality.Quality quality) {
    return com.hongmeng.xiuxian.quality.QualityTable.sellPrice(unitPrice(itemId), quality);
}
```

卖出路径（`shop/ShopManager` 之类拿得到 `ItemStack` 的地方）改成：

```java
int unit = SellPrices.unitPrice(id, com.hongmeng.xiuxian.quality.ItemQuality.of(stack));
```

**防套利不受影响**：商店买入 / 灵叶取出永远只给「凡品」（×100%），
而 `SellPrices.validate()` 比的是基础价；品质加价只作用于"掷过骰子的东西"。
唯一要注意的是 `SellPrices.MAX_PRICE = 800`：神品是 450%，
基础价高于 178 的条目（例如纪元令 200 → 神品 900）会越过上限——
如果坊市那边有上限裁剪，请一并决定是"裁剪到 800"还是"提高上限"。
（`QualityTable.validate()` 里没有对 800 的硬断言，就是为了把选择权留给你。）

---

## 五、新群系（三个）

数据：`data/hongmeng/worldgen/biome/*.json`（3 个）、
`placed_feature/<herb>_<biome>.json`（18 个高密度灵植）、
`forge/biome_modifier/add_<herb>_<biome>.json`（18 个）、
`forge/biome_modifier/add_<ore>_spirit_biomes.json`（4 个矿石）、
`forge/biome_modifier/add_spawn_*.json`（8 个强怪）、
`configured_feature/huanggu_scorch.json` / `huanggu_rubble.json` + 对应的 placed_feature（荒古战场的焦土与碎石）、
`data/minecraft/tags/worldgen/biome/has_structure/*.json`（原版遗迹：废弃传送门 / 沙漠神殿）。

| 群系 | 危险度 | 主题 | 灵植密度（雪莲/火焰/寒冰/雷音/空冥/血珞） | 高阶占比 |
|---|---|---|---|---|
| `lingmai_valley` 灵脉山谷 | 1 | 地脉灵气外溢的谷地，六阶灵植成片 | 32 / 24 / 18 / 12 / 6 / 3 | 0.28 |
| `yunmeng_marsh` 云梦泽 | 2 | 水泽湿地，水属 / 空间属灵植 | 20 / 10 / 26 / 10 / 16 / 8 | 0.61 |
| `huanggu_battlefield` 荒古战场 | 3 | 焦土废墟 + 强怪，高阶灵植 | 12 / 16 / 18 / 22 / 12 / 6 | 0.87 |
| （对照）原版 overworld | — | — | 8 / 5 / 6 / 5 / 3 / 2 | 0.53 |

- **每一格都严格高于原版对应值**（离线断言在 `BiomePlan.validate()`，
  磁盘 JSON 与 Java 表的一致性由 `tools/gen_new_biomes.py` 对撞）。
- **品阶门控**：高阶灵植占比随危险度严格上升（0.28 → 0.61 → 0.87），
  低危群系刷低阶灵植、高危群系才刷得出高阶灵植。
- **强怪**：荒古战场挂血魔修士 / 傀儡队长 / 石傀 / 魔道统领；
  云梦泽挂毒涎蟾 / 土行貂 / 灵狼；灵脉山谷挂灵狼 / 灵驹。
- **语言**：`biome.hongmeng.<id>` 中英文都补了。

### 群系怎么进主世界（为什么需要代码）

Forge 1.20.1 没有 TerraBlender 那种"往已有维度追加群系"的接口，
`forge:biome_modifier` 只能往**已有群系**里加特征，不能新增群系。
纯数据包的做法是覆盖 `data/minecraft/dimension/overworld.json`，
把原版 61 条气候点位表整张抄一遍——那既碰别人的维度文件，
又必须逐字抄对（抄错一位，全世界群系分布悄悄变），还会和其他群系模组打架。

`worldgen/OverworldBiomeInjector.java` 换成"**接管**"：

1. 主世界加载时（`LevelEvent.Load`）读出**现成的**原版点位表
   （`MultiNoiseBiomeSource.parameters`，约 61 条，由原版 `OverworldBiomeBuilder` 生成）；
2. 按 `BiomeSpec.donors()` 的优先级挑一个原版群系，
   要求它**至少还有 2 个点位**（这样它不会消失），把它**体积最大**的陆地点位换成新群系；
3. 重建 `Climate.ParameterList` 写回，并刷新两个惰性缓存
   （`BiomeSource.possibleBiomes`、`ChunkGenerator.featuresPerStep`）——
   不刷新的话新群系的特征（灵植）不会生成。

**为什么是"换"不是"加"**：multi-noise 查询是"找包含该气候坐标的点位，
并列时由 R 树遍历顺序决定"。新加一个和原版重叠的点位会得到**斑块状**花屏群系；
替换点位保持点位集合与顺序不变，R 树结构与原版一致，
新群系出现的位置就是被接管点位原本出现的位置——一整片、连贯、`/locate biome` 必找得到。

**代价（写清楚）**：被接管的原版群系各少一个气候点位
（森林 / 河流 / 热带草原各少一片），三个新群系共占 61 条里的 3 条。
反射用 `ObfuscationReflectionHelper`（生产环境字段名是 SRG，直接 `getDeclaredField("parameters")` 会在线上失效）；
整个过程包在 try/catch 里，任何一步失败只打警告日志，**绝不让服务器起不来**。

新群系**刻意没有加进 `#minecraft:is_overworld`**：
该标签下挂着本模组所有"全主世界"的 biome_modifier（矿石 / 灵植 / 刷怪），
加进去会让新群系的灵植**密度翻倍**（基础 + 高密度两份叠加），
既让文档数字对不上，也让区块生成的负担翻倍。
所以新群系用**自己的** biome_modifier 显式挂载需要的矿石与强怪。
如果以后想让"所有主世界模组"也覆盖这三个群系，把它们的 id 加进
`#minecraft:is_overworld` 即可，但那时要把本文档的密度表改成"基础 + 高密度"的合计值。

### 地形怎么来

不新造噪声设置、不覆盖维度文件：地形 / 高度 / 地表演变全部沿用被接管点位的原样。

⚠️ **实测教训**：接管点位的**地表材质**决定了灵植能不能长出来——
灵植是作物类方块（`SpiritHerbBlock#mayPlaceOn` 接受耕地/灵田土/`#minecraft:dirt`/草方块/苔藓/黏土），
**没有可落的土就一株都没有**。实测（每次都在**全新区块**里用 `fill ... air replace` 数）：

| 群系 | 接管来源（实测点位余量） | 取样范围 | 地面灵植 | 地表 |
|---|---|---|---|---|
| 灵脉山谷 | `minecraft:forest`（588→587） | 21×21 | **35 株** | 草地 ✓ |
| 荒古战场 | `minecraft:savanna`（356→355） | 21×21 | **28 株** | 草地 ✓ |
| 云梦泽 | `minecraft:mangrove_swamp`（14→13） | 17×17 | **5 株** | 草地 151 / 泥 619 / 土 674 |
| （对照）原版森林 | —（基础 modifier count=8） | 17×17 | **1 株** | 草地 ✓ |

- 三个新群系都**显著高于原版**（云梦泽 5 倍，另两个 20 倍量级）——数据上也是
  （密度表每一格都 > overworld 对应值）。
- 云梦泽的第一版接管来源是 `minecraft:river`，实测地表是**被切下去的河道**
  （grass_block 0、water 3036、dirt 280、sand 115），灵植 0 株；
  改成红树林沼泽后落到"有地有水"的水泽上，灵植能落地。
  想让它更密可以把首选换成 `minecraft:swamp`（草地更多），代价是经典沼泽会被接管掉
  （红树林沼泽有 14 个点位、留 13 个；经典沼泽点位很可能只有 1 个）。

### ⚠️ 特征顺序必须无环（这个坑崩了两次服）

Minecraft 1.18+ 的 `FeatureSorter` 会把**同一 step 内所有群系**的特征合成一张
"谁必须在谁前面"的图做拓扑排序，**一旦有环就在生成新区块时抛异常、服务端自己停**：

```
IllegalStateException: Feature order cycle found, involved sources: [...]
  at FeatureSorter.buildFeaturesPerStep
  at OverworldBiomeInjector.lambda$inject$4   ← 我重建 featuresPerStep 的那一行
```

两种成因，都踩过：

1. **同一 step 内的相对顺序与原版约束冲突**：手写植被表时把沼泽模板的顺序改了
   （`patch_dead_bush` 挪到最后、追加 `seagrass_normal`），与原版另一个群系形成 A→B→A；
2. **同一 step 里同一个特征出现两次 = 自环**：荒古战场想补 `minecraft:monster_room`，
   而热带草原模板里本来就有它 → `involved sources` 只列**一个**群系，极难从日志看懂。

修法（都在 `tools/gen_new_biomes.py`，生成器负责，手写 JSON 不许再改顺序）：

- `canonical_feature_order()`：读**全部原版主世界群系**，每个 step 做拓扑排序，
  得到一份原版合法顺序；
- `sort_by_canonical()`：新群系每个 step 的特征表按这份顺序重排（新群系引入的边必然朝前 → **必然无环**），
  并**去重**（去重就是修自环）；
- `check_feature_order_acyclic()`：离线复现同一个环检测（含自环），有环生成器直接失败退出——
  不必再靠"起服 + forceload"才发现。

自测：`_cache/check_cycle_probe.py`（把崩服那版顺序塞回去，检测器必须报环）。

**重新生成后实测**（`forceload add` 三片全新 2×2 区块）：
三条全部成功生成、服务器不崩、日志里
`Feature order cycle` / `Exception generating new chunk` / `FATAL` 均为 0。

**服务端实测汇总（最终一次干净的 run，14:27–14:30）**：
服务端 `Done (…)` 正常；`品质系统就绪：6 档（凡品 / 良品 / 上品 / 极品 / 仙品 / 神品），
覆盖 76 件物品，合成固定 凡品、炼丹/掉落掷骰，回收价神品 450%`；
三个群系 `/locate biome` 全部找得到；
日志 grep：`Failed to parse` / `Unbound values` / `Unreachable` / `Couldn't load structure` /
`Encountered an unexpected exception` / `crash-report` / `Parsing error` / `Missing texture`
**全部为 0**。

---

## 六、自检与验证

- 纯数据自检（模组加载期 + 服务器启动期各跑一次）：
  `Quality.validate()` / `QualitySource.validate()` / `QualityTable.validate()` /
  `AlchemyQuality.validate()`（入口 `QualityEvents.validateAll()`）与 `BiomePlan.validate()`。
  表写错的表现是"加载失败 + 一条说得清原因的异常"，而不是"某件东西悄悄变了"。
- 生成器：`tools/gen_new_biomes.py`（正则读 `BiomePlan.java` 当唯一真源，
  从原版 jar 里核对每一个被引用的原版 id，`--check` 只校验不写盘，
  并离线做**特征顺序无环**检查）。
- 离线验证器建议新增的 `check(...)` 行：共 **56 条**（已在 `_cache/QualityProbe.java`
  里逐条跑通，输出「通过 56 项，失败 0 项」），分组如下。

### 附：建议加进 `src/verify/**` 的断言（56 条）

**品质阶梯（Quality）**：六档名称 `凡品 / 良品 / 上品 / 极品 / 仙品 / 神品`；
档位数 = 6；权重和 = 1000‰；权重严格递减；凡品/上品/仙品倍率 = ×1/×3/×10；
效果倍率、法宝倍率、回收价百分比三者都严格递增；神品法宝倍率 ≤ ×2；
凡品回收价 = 100%；灵植品阶 1..6 → 凡品..神品；六阶 = 神品；
`byKey` 认英文键/中文名/未知回落；与 `DomainQuality` 共用「凡品」「仙品」锚点词；
与 `Rarity` 的「凡品灵根」同体系；`PillQuality` 三档映射 + 倍率仍是 1/3/10。

**获取表（QualitySource / QualityTable / AlchemyQuality）**：五条途径权重和都是 1000‰；
合成固定凡品；难度偏移期望严格上升（解析解）；shift=0 分布 = 基础表（抽样 ±8‰）；
副本/任务期望高于野怪；九颗丹药全覆盖；三档修为丹天生品质；
六阶花瓣品质 = 品阶且逐阶上升；一阶=凡品、六阶=神品；法宝+剑 60 件全覆盖；
妖丹有条目；合成不拉低天生品质；`hongmeng:` 前缀也认；掉落偏移随生命上限单调且封顶 4；
凡品回收价 = 原价；神品 = 450%；回收价随品质单调；基础价 0 → 0；
炼丹偏移一阶 0 / 六阶满档；炼丹期望随品阶严格上升；六阶仍可能出凡品；一阶出神品 ≤ 5‰；
神品概率随品阶上升。

**新群系（BiomePlan）**：3 个群系 id/名；每群系 × 每株灵植密度 > 原版；
高阶占比随危险度严格上升；灵脉山谷低阶最多；荒古战场高阶最多；云梦泽寒冰藤最多；
每群系 6 株灵植齐全；首选接管来源互不相同；**磁盘 JSON 的 count 与 Java 表一致（18 个文件）**。

