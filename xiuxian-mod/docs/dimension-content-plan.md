# 鸿蒙修仙 · 维度内容扩展方案（四个自建维度：能探索、能采集）

> 对应玩家原话：「宫殿建筑群落群系什么的……在我们设立的维度中也要添加新的群系方块建筑等等，
> 不仅仅是副本群系还要能探索采集，可以参考暮色模组」
> 上位文档：`docs/modpack-roadmap.md:91`（四、世界生成）、`:161`（七、维度战斗场景）、
> `docs/grand-structures-spec.md`（骨架阶段已完成）。
>
> 本文只是**方案 + 施工顺序**，本批次**不改任何 Java、不动 `src/main/resources`**。
> 标注约定：`文件:行号` = 现状出处（本机实读）；`[S1]`…`[S6]` = §七 的网络来源。

---

## 〇、现状基线（先对齐事实，后面每一步都由这里推出）

### 0.1 四个维度现在到底是什么

| 维度 | 注册方式 | `settings`（实读于 2026-10-10 17:23） | `biome_source` | 天光 / 效果 |
|---|---|---|---|---|
| `hongmeng:epoch_realm` 纪元秘境 | 数据文件 | `minecraft:overworld` | `multi_noise` `preset: overworld` | `has_skylight:false`、`effects: the_end`、环境光 0.12（`data/hongmeng/dimension_type/epoch_realm.json:8-16`） |
| `hongmeng:beast_realm` 妖兽秘境 | 数据文件 | `minecraft:large_biomes` | 同上 | overworld 天空、有天光（`README.md:1431`） |
| `hongmeng:demon_realm` 魔道巢穴 | 数据文件 | `minecraft:caves` | 同上 | 下界天空、`fixed_time:18000`、环境光 0.22（`README.md:1432`） |
| `hongmeng:void_realm` 虚空裂隙 | 数据文件 | `minecraft:overworld` | 同上 | 末地天空、`fixed_time:6000`、环境光 0.08（`README.md:1433`） |

> ⚠️ **基线会漂移**：四份 JSON 的 `biome_source` 目前逐字相同（`multi_noise` + `preset: overworld`）；
> `settings` 已经被**并行的另一个批次**改掉了两份（妖兽 → `large_biomes`、魔道 → `caves`），
> 纪元与虚空仍是 `overworld`。**因此 §三 B1 的剩余范围收窄为 `epoch_realm` 与 `void_realm` 两份**，
> 且 B1 的验收项（`[arena] floorY=` 日志）必须重新在 `caves` 那份上跑一次。
> 动手前请重读这 4 个文件——本文按"实读时刻"记录，不假设它们是静止的。
> 境界门槛与波次见 `dungeon/Dungeon.java:28-41` 与 `README.md:1344-1351`。

### 0.2 「维度里只有一块战斗场地」的准确范围

`dungeon/DungeonArena.java` 建的是：半径 **19** 的干净平台（`:46 PLAZA_R`）、外沿到半径 **24**
（`:49 BUILD_R`）、floorY 用半径 **28–32** 的探测环取（`:52-53`）、台基往下最多补 **8** 格
（`:59 FOUNDATION_MAX`）。进场流程是 `forceLoad 5×5 区块 → DungeonArena.ensureBuilt → findLanding`
（`dungeon/DungeonRealm.java:180-184`）。而 `README.md:1986-1987` 已经把结论写死了：
**「副本没有预制建筑：纪元秘境用的是常规地形生成 + 动态刷怪，没有手搭的祭坛/遗迹结构」**。
这就是本轮要补的东西。

### 0.3 三个把方案限死在"可施工"范围内的硬事实

1. **新群系现在只进主世界，进不去四个维度。** `worldgen/OverworldBiomeInjector.java:83` 的注释是
   「只在主世界注入（四个秘境保持原版群系分布，不掺新群系）」，实现上 `:95` 直接
   `if (!Level.OVERWORLD.equals(level.dimension())) return;`。
   所以四个维度里跑的是**纯原版 61 群系**，一个 `hongmeng:` 群系都没有。
2. **1.20.1 的 `multi_noise` 点位表不能由数据包自定义。** 我在
   `forge-1.20.1-47.4.0_mapped_official_1.20.1.jar` 里查了 `MultiNoiseBiomeSource.class`：
   只有 `PRESET_CODEC` 与 `preset` 字段，`MultiNoiseBiomeSourceParameterList$Preset` 只有两个内部类；
   原版自己的 `multi_noise_biome_source_parameter_list/overworld.json` 内容就是
   `{"preset": "minecraft:overworld"}`（本机从 `client-extra.jar` 实读）。这与
   `docs/quality-and-biomes-spec.md:177-181`「Forge 1.20.1 没有 TerraBlender 那种接口」一致。
   → **要"维度自己的群系集合"，只有两条路：换非 multi_noise 的群系源（纯数据），或者照抄注入器写代码。**
3. **可用的数据驱动群系源是存在的**（同一 jar 内实测）：`FixedBiomeSource`（字段 `biome`，单群系）、
   `CheckerboardColumnBiomeSource`（字段 `biomes` + `scale`；反编译确认取群系用
   `Math.floorMod(index, allowedBiomes.size())`，**列表长度任意、自动取模**）、`TheEndBiomeSource`；
   而 `NoiseBasedChunkGenerator` 接受任意 `BiomeSource` → 「每个维度 2~3 个自己的群系」**B1/B2 可零 Java 完成**。

### 0.4 结构性副作用（是好事，也要写清楚）

* 三座宏伟建筑的群系白名单是**纯原版标签**（`tools/gen_grand_structures.py:1264-1273` 的
  `BIOMES_COMMON` / `BIOMES_MOUNTAIN`，`:1275-1280` 的 `PLAN`）。一旦维度换成 `hongmeng:` 群系，
  `#minecraft:is_forest` 之类**不再匹配**，`docs/grand-structures-spec.md:182-184` 记的那条
  「四座副本维度里也会生成这三座结构」**自动消失**（不用写维度白名单——原版 1.20.1 结构 JSON 没这字段）。
* 反过来，维度自己的地标只要把 `has_structure/<id>.json` 里只写 `hongmeng:` 群系 id，
  它**物理上不可能**出现在主世界。这是"维度专属建筑"最干净的做法。
* 代价：维度里**连原版村庄/要塞/废弃传送门也不会生成了**。「空得只剩我们放的东西」——
  所以 B4 的建筑密度必须够，否则维度会显得比现在还空。

---

## 一、设计原则（从暮色森林提炼，8 条；每条给出"它在暮色里对应什么"）

| # | 原则（一句话） | 暮色森林里对应什么 | 我们怎么落地 |
|---|---|---|---|
| 1 | **地标按网格布点，再用一张地图把它变成"可规划的目标"** | Major Landmark 之间恒为 **256 格**网格，Magic Map 一右键就把 Boss 结构画在图上 [S3]；Minor Landmark（废墟/井/石圈）随机撒、不上图 [S3] | 每个维度把 2~3 座地标设成 `spacing` 为 128/160/192 的 `random_spread`（互不重叠），配一个能定位它们的 `/hongmeng realm map` 或地图物品 |
| 2 | **每片区域必须有一件"只有这里有"的东西** | 独有物：火炬浆果/乌鸦羽毛（做地图的原料 [S1]）、钢叶 Steeleaf、铁木 Ironwood、骑士金属 Knightmetal、砷铅铁 Carminite、迷宫石 Mazestone、极光块 Aurora、巨魔石 Trollsteinn [S3][S5] | 每个维度绑定 1~2 种**只在它这里长/只在这里挖**的灵植与矿石（现有 6 灵植 + 4 矿石可以按维度重新配密度，见 §二） |
| 3 | **进度锁要做成两层：软惩罚 + 硬结界** | 软：没解锁的区域持续上负面效果——沼泽叠饥饿、黑暗森林给黑暗、雪原/冰川冰冻、火焰沼泽着火、高原与终焉城堡**酸雨真伤**；持有对应 Boss 战利品也算解锁 [S2]。硬：Boss 建筑自带**保护结界**——不能挖方块、不能对生物造成伤害、不能开箱子，只有完成成就才逐步解除 [S5] | B4 先上"软锁"（强怪 + 刷怪笼把守，零 Java）；B5 再上"硬锁"（`BreakEvent` / `PlayerInteractEvent` 拦截，见 §三 B5） |
| 4 | **钥匙是"可携带的战利品/凭证"，不是进度标记** | 掉落的 Boss 战利品拾取即完成成就 [S5]；把战利品放到**战利品宝座**上开门（地精骑士要塞的入口就是"放任意一个 Boss 战利品"）[S1][S5] | 复用**已有物品**当钥匙，不新增注册：纪元令 `hongmeng:jidan`（`DungeonRealm.java:549` 每次通关必给 1 个）、道印 `hongmeng:daoyin`、血珞精、空冥精 |
| 5 | **指引必须是一条线性成就链 + 一张能看的地图** | 18 条主线成就有明确前置（拾取战利品 → 开门 → 打下一个 Boss）；卡住时有 `/tfprogress <玩家> <BOSS>` 兜底 [S5]；Magic Map / Maze Map / Maze-Ore Map 三级地图 [S1][S5] | 沿用项目已有的 `/hongmeng dungeon`、`QuestChain` 思路，加一条「境界 → 维度 → 地标」的指引文本；地图降级为命令（零 Java 先行） |
| 6 | **大结构要"远看就有"，小遗迹负责"随手可捡"** | 空心山 Hollow Hill 分小/中/大三档（3/8、3/16、1/16 权重），内部有矿脉、幽灵、钟乳石 [S3]；次要地标必刷骷髅德鲁伊小屋、废墟（6×6~10×10，**约一半概率**地板下藏密室）[S3] | 每维度 2~3 座大建筑（`spacing` 大）+ 一批小遗迹（`spacing` 小、`separation` 接近 0），小遗迹里放 1 个小箱子 |
| 7 | **地标与群系强绑定，而不是全群系随机** | 附魔森林**必出** Quest Grove；火焰沼泽**必出** Hydra Lair；林间空地**必出** Labyrinth；暗黑塔**只在** Dark Forest 且每次 1/3 概率 [S3] | 每座地标的 `has_structure` 标签只列**它该出现的那个群系 id**，而不是模糊的 `#is_forest` |
| 8 | **放一个"收集型 NPC"给探索一个终点** | 彩虹公羊 Quest Ram：交给它 **16 种颜色的羊毛**，成就「做的不错」[S5]；任务型收集把"到处乱逛"变成"有清单地跑图" | 每个维度一个「收集点」（祭坛/图腾/道台），要求提交本维度各群系的采集物，齐了给一次奖励（可先用命令结算，后补方块交互） |

> 反向教训（同一条链上要避开的）：暮色**终焉城堡至今没有最终 Boss**，社区评价里最刺耳的一条就是
> 「它 unfinished 太久了，团队一直在优化别的而不是那个结局」[S6]。我们的对应策略是——
> **每个维度都必须有一个能立刻打、能结算、能拿奖励的收束点**，不允许出现"先放着，等以后做"的悬空结构。

---

## 二、四个维度各自的定位

### 2.0 总表

| 维度 | 主题定位 | 进入境界 | 新群系（中文 / 建议 id） | 可采集资源 | 标志性建筑（2~3 座） | 该维度的"进度锁" |
|---|---|---|---|---|---|---|
| `epoch_realm` 纪元秘境 | 入门采集场：灵田、遗迹、低危 | 练气（第 1 境，`Dungeon.java:28`） | 纪元草原 `epoch_meadow` / 灵稻田畴 `epoch_paddy` / 古坛废墟 `epoch_ruins` | 六种灵植（一~三阶为主）、灵铁矿石、灵晶矿石、**灵田土**（`hongmeng:lingtian_soil`）、纪元令 | 纪元祭坛 `epoch_altar`、望楼 `epoch_watchtower`、灵田小院 `epoch_farmstead` | 通关纪元秘境 1 次（现有 `data.addDungeonClear()`，`DungeonRealm.java:528`）→ 拿到纪元令当凭证 → 解除祭坛区域的刷怪笼封锁 |
| `beast_realm` 妖兽秘境 | 兽潮猎场：兽骨、妖丹、中危 | 金丹（第 3 境） | 兽骨荒原 `beast_bonefield` / 灵兽密林 `beast_grove` / 血食泥沼 `beast_mire` | 灵植（二~四阶）、灵兽掉落（妖丹）、灵铁/空冥矿石、骨块/深板岩（建材） | 驯兽坪 `beast_pen`、兽骨图腾柱 `beast_totem`、猎场哨塔 `beast_hunt_post` | 击杀首领波首领（`pickType` 的 `BEAST → PUPPET_CAPTAIN`，`DungeonRealm.java:476`）→ 掉「兽王骨符」类凭证 → 图腾柱深处的宝箱开箱权 |
| `demon_realm` 魔道巢穴 | 魔道战区：高阶灵植 + 最强杂兵 | 道印（第 6 境） | 焦土魔原 `demon_scorch` / 血珞林 `demon_grove` / 黑石魔窟 `demon_cavern` | 血珞精、空冥精、血珞芝（五~六阶灵植）、灵魂沙/岩浆块（装饰建材） | 魔宗山门 `demon_gate`、祭血坛 `demon_altar`、囚魔窟 `demon_cage` | 软锁：进维度 30 秒后开始叠「魔气侵蚀」（饥饿 + 虚弱，随时间加重）；持**道印**或击杀魔道统领解除 |
| `void_realm` 虚空裂隙 | 终局战场：浮空、星尘、最高危 | 仙人（第 8 境） | 虚空浮岛 `void_isles` / 星尘荒原 `void_stardust` / 归墟之眼 `void_abyss` | 空冥精、空冥兰（最高密度）、紫水晶/末地烛原料、灵晶 | 虚空道台 `void_dao_tai`、断界碑 `void_rift_stone`、虚空藏经阁 `void_pavilion` | 击杀首领波首领「虚空道主」（`DungeonRealm.java:484`）→ 得虚空道印 → 才能进「归墟之眼」（否则被「空间撕裂」弹回） |

**通用做法（四个维度一致，避免四套代码）**

* **群系源**：`checkerboard`，`biomes` 列本维度那 3 个 `hongmeng:` 群系，
  `scale` 取 8~24（大补丁，避免"棋盘花屏"观感）。长度不必是 4 的倍数——已反编译确认取模。
* **地形**：不改 `noise_settings`、不新建维度文件，只换 `settings` 引用（`README.md:1973` 已经指明这条路）。
  原版 jar 里现成的可选值：`minecraft:overworld` / `large_biomes` / `amplified` / `caves` / `floating_islands`
  （`data/minecraft/worldgen/noise_settings/` 实测存在；`nether` / `end` 因为 `min_y`/`height` 与本维度
  `dimension_type`（`min_y:-64, height:384`）不匹配，**不能用**）。
* **灵植/矿石怎么长**：新维度群系**不进** `#minecraft:is_overworld`（理由同
  `docs/quality-and-biomes-spec.md:203-207`：防止"基础 + 高密度"两份叠加），
  每个群系显式挂自己的 `forge:biome_modifier`——这正是
  `tools/gen_new_biomes.py:577-603 build_ores_and_spawns()` 已经在做的事，直接复用。
* **语言**：`assets/hongmeng/lang/zh_cn.json` 现在只有三条 `biome.hongmeng.*`，
  新群系要按同样格式补齐中英文（`docs/quality-and-biomes-spec.md:173`）。

### 2.1 纪元秘境 `epoch_realm`（练气期的新手采集场）

| 群系 | 主题 | 灵植密度（雪莲/火焰/寒冰/雷音/空冥/血珞） | 地表 | 独有物 |
|---|---|---|---|---|
| `epoch_meadow` 纪元草原 | 断裂的白玉台基散落在草甸上 | 34 / 20 / 14 / 8 / 4 / 2 | `grass_block` + `smooth_quartz` 残块 | **灵田土**成簇（可挖回家种灵植） |
| `epoch_paddy` 灵稻田畴 | 一格深的浅水田，田埂是深板岩砖 | 22 / 12 / 26 / 8 / 10 / 3 | `farmland` + 浅水 + `lingtian_soil` | 水生灵植（寒冰藤/空冥兰）密集 |
| `epoch_ruins` 古坛废墟 | 焦土与碎石路，铺着断柱 | 14 / 18 / 12 / 20 / 10 / 5 | `coarse_dirt` + `cobblestone` 残骸 | 灵铁矿石裸露（`spirit_iron_ore`） |

自检口径沿用现有群系表：**每一格都严格高于原版 `BiomePlan.OVERWORLD_HERB_COUNTS`
（`{8,5,6,5,3,2}`，`BiomePlan.java:103`），高阶占比随危险度严格上升**
（断言实现见 `BiomePlan.java:226-239`）。

### 2.2 妖兽秘境 `beast_realm`（金丹期的兽潮猎场）

| 群系 | 主题 | 灵植密度 | 刷怪 | 独有物 |
|---|---|---|---|---|
| `beast_bonefield` 兽骨荒原 | 巨型肋骨化石骨架横陈 | 10 / 16 / 10 / 18 / 8 / 4 | `spirit_wolf` 狼群为主 | 骨块 / 骨粉 / 妖丹（击杀掉落） |
| `beast_grove` 灵兽密林 | 巨木 + 悬挂藤蔓 | 26 / 18 / 20 / 12 / 10 / 4 | `spirit_steed` / `spirit_wolf` | 灵植成片（二~四阶） |
| `beast_mire` 血食泥沼 | 红褐色浅滩 | 12 / 10 / 24 / 10 / 14 / 6 | `venom_toad` / `burrow_marten` | 空冥矿石（`kongming_ore`） |

危险度排在纪元之后，所以 `dangerTier` 取 2，高阶占比必须高于 `epoch_*`。

### 2.3 魔道巢穴 `demon_realm`（道印期的魔道战区）

| 群系 | 主题 | 灵植密度 | 刷怪 | 独有物 |
|---|---|---|---|---|
| `demon_scorch` 焦土魔原 | 焦黑地表 + 岩浆缝 | 8 / 14 / 10 / 20 / 10 / 8 | `blood_demon_cultivator` / `puppet_captain` | 血珞精（`xueluo_essence`）、灵魂沙 |
| `demon_grove` 血珞林 | 暗红树叶，树身是绯红菌柄 | 6 / 8 / 8 / 16 / 18 / 14 | `demon_general` | **血珞芝**（六阶灵植）最高密度 |
| `demon_cavern` 黑石魔窟 | 黑石穹顶 + 萤石与灵魂灯 | 4 / 6 / 12 / 12 / 16 / 12 | `stone_golem` / `demon_general` | 空冥精、黑石建材 |

「魔气侵蚀」软锁建议用现有 `MobEffect`（`MobEffects.HUNGER` + `MobEffects.WEAKNESS`），
不新增效果注册——避免 `docs/modpack-roadmap.md:194-205` 那类"注册期踩坑"。

### 2.4 虚空裂隙 `void_realm`（仙人期的终局战场）

| 群系 | 主题 | 灵植密度 | 刷怪 | 独有物 |
|---|---|---|---|---|
| `void_isles` 虚空浮岛 | 悬空岛链（配 `settings: floating_islands`） | 6 / 6 / 10 / 12 / 26 / 12 | `void_overlord` | **空冥兰**（六阶灵植）最高密度 |
| `void_stardust` 星尘荒原 | 紫水晶簇 + 末地石地衣 | 4 / 8 / 8 / 16 / 20 / 10 | `void_overlord` / `demon_general` | 紫水晶块、末地烛原料 |
| `void_abyss` 归墟之眼 | 中心下沉的巨型环形坑 | 2 / 4 / 6 / 10 / 24 / 18 | 首领级 | 通关凭证掉落区 |

`void_abyss` 就是原则 8 的「收集点」：三个群系的采集物各交一份才开中心祭坛。

### 2.5 每个新群系 / 每座新建筑的产出文件清单（用哪个生成器、产出什么、怎么自检）

**群系（12 个，全部由 `tools/gen_realm_biomes.py` 产出，真源是新建的 `worldgen/RealmPlan.java`）**

| 产出 | 路径模板 | 数量 | 自检 |
|---|---|---|---|
| 群系本体 | `data/hongmeng/worldgen/biome/<realm>_<name>.json` | 12 | `gen_realm_biomes.py --check`（逐字节比对，仿 `tools/gen_new_biomes.py:311-313`） |
| 高密度灵植放置表 | `data/hongmeng/worldgen/placed_feature/<herb>_<biome>.json` | 72（12×6） | `count` 必须与 `RealmPlan.java` 逐字一致（仿 `:649-653`） |
| 灵植挂载 | `data/hongmeng/forge/biome_modifier/add_<herb>_<biome>.json` | 72 | `biomes` 必须精确等于 `["hongmeng:<biome>"]`（仿 `:657-660`） |
| 矿石 / 强怪挂载 | `forge/biome_modifier/add_<ore>_<biome>.json`、`add_spawn_<entity>_<biome>.json` | 48 + 按 `SPAWNS` 表 | 4 张矿石表各覆盖 12 群系（仿 `:663-668`）；实体必须在 `ModEntities.java` 注册（仿 `:669-679`） |
| 语言 | `assets/hongmeng/lang/{zh_cn,en_us}.json` 的 `biome.hongmeng.*` | 12×2 | 每条群系都有中英文（口径 `docs/quality-and-biomes-spec.md:173`） |
| 无环检查 | （不产出文件） | — | 生成器退出码 1 = 有环，**不许进下一批** |

**维度群系源（手改，无生成器）**：`data/hongmeng/dimension/{epoch,beast,demon,void}_realm.json`，
`settings` 换地形、`biome_source` 换成 `checkerboard` + 本维度 3 个群系 + `scale`。
自检：`check_new_structure_json.py`（全量 JSON 语法）+ 起服后 `/locate biome`。

**建筑（12 座，全部由 `tools/gen_realm_structures.py` 产出，复用 `Blueprint`）**

每座 5 份文件（格式照 `tools/gen_grand_structures.py:1334-1340`）：

| 产出 | 路径 | 自检 |
|---|---|---|
| 结构本体 | `data/hongmeng/structures/<id>.nbt` | `gen_realm_structures.py --check`（镜像 / 调色板 / 越界）+ `validate_structure_palette.py` |
| 结构定义 | `data/hongmeng/worldgen/structure/<id>.json` | `check_new_structure_json.py`：`biomes` 必须是 `#hongmeng:has_structure/<id>`，`type: minecraft:jigsaw` |
| 模板池 | `data/hongmeng/worldgen/template_pool/<id>/start_pool.json` | `location` 必须等于 `hongmeng:<id>`（`:47-49`） |
| 布点 | `data/hongmeng/worldgen/structure_set/<id>.json` | **`separation` < `spacing`**，否则原版抛异常（`:57-58`） |
| 群系白名单 | `data/hongmeng/tags/worldgen/biome/has_structure/<id>.json` | 值里只允许出现本维度的 `hongmeng:` 群系 id；**引用了不存在的 id 会让服务端起不来** |
| （宝箱/刷怪笼） | 打包在 `.nbt` 里 | `check_grand_structure_loot.py <id>.nbt`（**必须显式传文件名**，它内置只有三座，`:49`） |

**建议的 12 座地标定位表（便于 B4 起手）**

| 维度 | 三座地标：id / 中文 / 占地 X×Y×Z / spacing-separation / 内配 |
|---|---|
| 纪元 | `epoch_altar` 纪元祭坛 35×22×35、160/96、中央祭坛 + 4 角灯柱 + 2 箱 + 1 刷怪笼；`epoch_watchtower` 望楼 13×28×13、96/56、可登顶的塔 + 顶层 1 箱；`epoch_farmstead` 灵田小院 25×12×25、80/40、两畦灵田（`lingtian_soil`）+ 1 箱 |
| 妖兽 | `beast_pen` 驯兽坪 41×16×41、176/104、围栏 + 看台 + 刷怪笼×2 + 2 箱；`beast_totem` 兽骨图腾柱 9×40×9、128/72、垂直地标 + 顶灯 + 碑底 1 箱；`beast_hunt_post` 猎场哨塔 15×24×15、88/48、火把 + 1 箱 |
| 魔道 | `demon_gate` 魔宗山门 33×26×19、160/96、中轴山门 + 两侧配殿 + 2 箱；`demon_altar` 祭血坛 31×18×31、128/72、三级祭坛 + 灵魂灯 + 1 箱；`demon_cage` 囚魔窟 23×20×23、96/56、掏空岩窟 + 刷怪笼×2 + 2 箱 |
| 虚空 | `void_dao_tai` 虚空道台 45×30×45、192/112、圜丘式通天台（复用 `build_wendao_tai` 语法）+ 2 箱；`void_rift_stone` 断界碑 7×33×7、144/80、碑底 1 箱；`void_pavilion` 虚空藏经阁 29×28×29、176/104、两层楼阁 + 掏空内殿 + 刷怪笼×2 + 2 箱 |

> `spacing` 两条硬规则都满足：`spacing > separation`，且 `separation` 大于结构最大边长
> （`docs/grand-structures-spec.md:54-55`）。12 座里只有 `epoch_altar` / `demon_altar` / `void_dao_tai`
> 适合走镜像自检；其余**故意不对称**，见 §5.3 第 9 条。

### 2.6 进度锁机制对照表（暮色 → 我们，便于逐条对齐）

| 暮色的机制 | 出处 | 我们的对应实现 | 需要 Java？ |
|---|---|---|---|
| 未解锁区域持续负面效果（饥饿/黑暗/冰冻/着火/酸雨），**持 Boss 战利品也算解锁** | [S2] | 未持凭证时叠 `HUNGER` + `WEAKNESS` + `MOVEMENT_SLOWDOWN`；凭证复用现有掉落——纪元令 / 道印 / 空冥精（`DungeonRealm.java:549,577,591`） | 是（B5 第 2 层） |
| Boss 建筑保护结界（不能挖 / 不能打 / 不能开箱），靠完成成就逐步解除 | [S5] | `BlockEvent.BreakEvent` + `PlayerInteractEvent.RightClickBlock` 按结构包围盒拦截 | 是（B5 第 3 层） |
| 把战利品放到**战利品宝座**上开门 | [S1][S5] | 地标里的「阵眼」方块（可先用现有 `hongmeng:root_altar`）承载同样的交互语义 | 是（若要做交互） |
| 魔法地图画出 Boss 结构；地标恒在 256 格网格上 | [S1][S2][S3] | `/hongmeng realm map` 打印最近 3 座地标坐标与距离；布点靠 `random_spread` 的 `spacing`/`separation` | 是（一条命令） |
| 地标与群系强绑定（附魔森林必出 Quest Grove 等） | [S3] | `has_structure` 标签只列本维度群系 id | 否 |
| Minor 地标随机撒、不上图；空心山 = 矿 + 怪 + 宝藏 | [S3][S4] | B6 的小遗迹批次；地标内配 = 刷怪笼 + 箱子 + 裸露矿石 | 否 |
| 彩虹公羊收集任务（交 16 色羊毛） | [S2][S5] | 每维度一个「收集点」，交齐本维度采集物 | 是（B6） |
| `/tfprogress` 兜底解锁（防卡死） | [S5] | `/hongmeng realm unlock <维度>`（管理员用） | 是（B5 必备） |
| 终焉城堡长期未完成 → 最主要的差评 | [S1][S6] | **反模式**：每维度必须有可打的收束点，不留悬空结构 | — |

---

## 三、分阶段实施顺序（6 批，每批只动一两个子系统）

> 每批的收尾动作统一是：`编译 0 error` + `通过 587 项，失败 0 项`（口径见
> `docs/grand-structures-spec.md:115`）+ `runServer` 日志扫描（明细见 §六）。

### B1 —— 维度地形差异化（**纯数据，零 Java**，一次只改 4 个文件）

* **起手文件**：`data/hongmeng/dimension/{epoch,beast,demon,void}_realm.json`（各 12 行）。
* **改动（剩余范围，见 §0.1 的漂移提示）**：`settings` 目标值是
  `overworld` / `large_biomes` / `caves` / `floating_islands`——其中妖兽、魔道**已被并行批次改完**，
  本批只需补 `epoch_realm`（维持 `overworld` 或换 `large_biomes`）与
  `void_realm`（换 **`floating_islands`**）。本批**先不动 `biome_source`**，
  以便把"地形变了但群系还是原版"这一步单独验掉。
* **自检**：`python tools/check_new_structure_json.py`（顺带把 `data/hongmeng` 下 276 份 JSON
  全部解析一遍，`:84-94`）；`/execute in hongmeng:void_realm run setblock 0 100 0 minecraft:glass`
  （口径同 `README.md:35`）。
* **风险前置**：`DungeonArena.floorY()` 靠半径 28–32 的探测环（`DungeonArena.java:52-53`），
  `floating_islands` 下这个环可能落在虚空里 → **必须先确认场地还能正常立起来**，
  否则整个维度副本会崩在 `ensureBuilt` 上。**已经改过的 `caves` 那份也要补跑一次这条验收。**

### B2 —— 维度自己的群系集合（纯数据，零 Java）

* **起手文件**：
  1. 新建 `worldgen/RealmPlan.java`（**纯数据表**，仿 `BiomePlan.java` 的写法，
     record 见 `worldgen/BiomeSpec.java:23`）；
  2. 新建 `tools/gen_realm_biomes.py`，正则读 `RealmPlan.java` 当真源
     （仿 `tools/gen_new_biomes.py:108-135`），产出
     `worldgen/biome/<realm>_*.json`（12 个）+ 每群系的 `placed_feature` / `forge/biome_modifier`；
  3. 4 份 `dimension/*.json` 的 `biome_source` 换成
     `{"type":"minecraft:checkerboard","biomes":[...],"scale":12}`。
* **为什么新开一个脚本而不改 `gen_new_biomes.py`**：那个脚本里写死了
  「从 `BiomePlan.java` 里读到 %d 个群系，应当是 3 个」（`:133-134`）与
  「应当是 6 株」（`:104`），且 `BiomePlan.validate()` 硬断言 `ALL.size() != 3`（`BiomePlan.java:161-163`）、
  危险度 `1..3`（`:183-185`）、接管来源必须是 `minecraft:`（`:193-195`）。
  往里面塞 12 个维度群系会把 587 项断言和现有 30 份 modifier 一起改坏。
* **自检**：
  * 新脚本自带 `--check`（不写盘只对比，仿 `:311-313`）；
  * `check_feature_order_acyclic()` 的**等价实现必须搬过来**（`:728-785`）：
    换成 checkerboard 之后一个维度只有 3 个群系参与 `FeatureSorter`，
    环的概率大幅降低，但**自环（同一 step 里同一特征出现两次）依然会崩服**
    （成因见 `docs/quality-and-biomes-spec.md:248-250`）；
  * `forceload add` 一片全新区块 + `/locate biome hongmeng:epoch_meadow`，**必须找得到**。

### B3 —— 采集资源挂载（纯数据）

* **起手文件**：`tools/gen_realm_biomes.py` 的 `build_ores_and_spawns()` 等价段
  （原型见 `tools/gen_new_biomes.py:577-603`），以及 `SPAWNS` / `STRUCTURE_TAGS` 两张表
  （原型 `:68-82`）。
* **产出**：每维度群系 × 每灵植一份 `placed_feature` + `forge/biome_modifier/add_<herb>_<biome>.json`；
  4 张矿石表各挂 4 个新群系；强怪表按 `SPAWNS` 的格式挂本模组已注册实体。
* **前置校验（从 `tools/gen_new_biomes.py` 抄）**：
  * 每个引用的原版 `placed_feature`/`configured_feature`/群系/标签**在 jar 里真实存在**
    （`:690-710`，`Vanilla` 类直接读 `client-extra.jar`）；
  * `placed_feature` 的 `count` 与 Java 表**逐字一致**（`:649-653`）——防"Java 说 32、JSON 写 12"；
  * 引用的实体 id 在 `ModEntities.java` 里注册过（`:675-676`）。
* **⚠️ 地表材质决定灵植能不能长**：灵植是作物类方块，
  `mayPlaceOn` 只接受耕地/灵田土/`#minecraft:dirt`/草方块/苔藓/黏土，
  **砸在石头/深板岩/水底就一株都没有**（`docs/quality-and-biomes-spec.md:215-232` 有实测组数：
  云梦泽第一版接管河流点位 → 灵植 **0 株**）。
  所以 §二 里每个群系都写了"地表"一列——`beast_bonefield` 与 `demon_cavern` 这两行
  必须要么换接管地表的做法，要么显式铺一层土/苔藓（`tools/gen_grand_structures.py:375-377`
  已经在用 `hongmeng:lingtian_soil` + `hongmeng:xuelian_grass[age=4]` 的组合）。

### B4 —— 维度标志性建筑（NBT + 结构 JSON，零 Java）

* **起手文件**：新建 `tools/gen_realm_structures.py`，
  **直接 `importlib` 复用** `tools/gen_grand_structures.py` 的 `Blueprint` 类与
  `tools/fix_structure_nbt.py` 的 NBT 读写——注意 `gen_grand_structures.py:60-63` 本来就是这么引的，
  照抄这个模式即可（不要重写 NBT 层）。
* **可用能力（已被三座主建筑验证过）**：`set/fill/hrect/hring`（`:96-120`）；
  `place_chest`/`place_spawner`（`:158-171`，自带"自身必须是空气 + 下方必须实心"的硬断言）；
  `roof()` 逐层收分 + 四角梯度抬升的飞檐（`:190-244`）；`railing()` 望柱 + 栏板（`:247-265`）；
  `mirror_offenders()` 关于 X 中轴镜像自检（`:309-316`）；`palette_offenders()` 查 `Name` 带 `[`（`:318-323`）。
* **产出（每座 5 份文件，格式照 `:1334-1340`）**：`data/hongmeng/structures/<id>.nbt`；
  `worldgen/structure/<id>.json`（`jigsaw` + `beard_thin`，模板 `:1219-1232`）；
  `worldgen/template_pool/<id>/start_pool.json`（`:1235-1249`）；
  `worldgen/structure_set/<id>.json`（`random_spread`，`:1252-1261`）；
  `tags/worldgen/biome/has_structure/<id>.json`。
* **群系白名单只写本维度群系**（原则 7）：例如 `epoch_altar` 的 tag 只列 `hongmeng:epoch_ruins`，
  于是它**只可能**出现在纪元秘境，主世界一次都不会有。
* **本批的"进度锁"是软锁（零 Java）**：每座地标里放 1~2 个刷怪笼（`place_spawner`，实体 id 用
  `hongmeng:blood_demon_cultivator` / `hongmeng:stone_golem` 这类已在 `gen_grand_structures.py:413-414`
  核对过的注册名）+ 1 个 3~6 格的箱子。没到境界的玩家打不过看守，到了才拿得走——
  这就是暮色"结界"的最糙但可用的版本。
* **自检（四件套，全在 `tools/`）**：
  1. `validate_structure_palette.py` —— 每个 palette 条目拿去和**真实 blockstate** 对（判据与游戏一致：
     缺属性补默认值没事，**多一个不存在的属性/非法值直接整份结构加载失败**，`:6-11`）。
     这一条抓到过 `minecraft:quartz_wall`（原版没有石英墙）和 lectern 的 `has_book`
     （`docs/grand-structures-spec.md:96-103`）。
  2. `check_grand_structure_loot.py <新 nbt...>` —— 箱子/刷怪笼起飞前检查（`:6-19`）：`Name` 带 `[` 的
     条目数为 0、每箱 3~6 格、`Count` 1~3、`Slot` 不重复且在 0~26、物品/实体 id 现场扫
     `ModItems.java`/`ModEntities.java`、刷怪笼 `id` 必须是 `minecraft:mob_spawner`（**不是方块名
     `minecraft:spawner`**）。它内置 `DEFAULT_IDS` 只有三座（`:49`），新结构要**显式传文件名**。
  3. `check_new_structure_json.py` —— JSON 语法 + 交叉引用（structure → template_pool →
     `structures/*.nbt` → biome tag）+ `spacing > separation` + 群系 id/标签存在性（`:1-9`）；
     它的 `IDS` 同样写死三座（`:22`），需同步扩。
  4. `gen_realm_structures.py --check` —— 生成器自身的镜像/调色板/越界自检（仿 `:1289-1332`）。
* **确定性要求**：重跑两遍，NBT **逐字节一致**（验收口径见 `docs/grand-structures-spec.md:117`）。

### B5 —— 进度锁与指引（本项目第一批"必须写 Java"的改动，仍只碰 1~2 个子系统）

* **起手文件**（新建，不改既有类）：`dungeon/RealmProgression.java` + `dungeon/RealmGateEvents.java`。
* **三层锁，按代价从低到高，允许只做第一层就收工**：
  1. **软锁（数据 + 现有实体）**：靠 B4 的刷怪笼守卫。零新增代码。
  2. **区域 debuff**：每 20 tick 检查玩家所在群系是否为"未解锁维度群系"，是则施加 `HUNGER` +
     `WEAKNESS` + `MOVEMENT_SLOWDOWN`，持凭证或达成条件则不再施加。接入点与 `DungeonRealmEvents`
     的"玩家 tick 兜底"完全同构（`docs/grand-structures-spec.md:63`）。
  3. **硬结界**：`BlockEvent.BreakEvent` + `PlayerInteractEvent.RightClickBlock` 拦截"未解锁玩家在
     受保护结构范围内挖方块/开箱子"。范围不必自己算——把"受保护区域"定义成地标中心的固定半径
     （B4 生成时坐标已知），或结构生成后在 `StructureStart` 上打标记。
* **凭证从哪来（不新增注册物品）**：纪元 = `hongmeng:jidan` 纪元令（`DungeonRealm.java:549`
  每次通关**必给 1 个**）；魔道 = `hongmeng:daoyin` 道印（`:577`，35%）；虚空 =
  `hongmeng:kongming_essence` 空冥精（`:591`，45%）。
* **⚠️ 现有数据的缺口（必须先决定）**：`CultivationData.dungeonClears` 是**一个全局 int**
  （`core/CultivationData.java:37`，自增 `:510-511`，存档键 `DungeonClears` `:572`，同步 `:646`/`:678`），
  **分不出"通关的是哪一座副本"**。要么加一个 `EnumSet<Dungeon>` 字段，要么就用"凭证物品在背包里"
  当唯一判据（**推荐后者**：零存档迁移、老存档无损）。
* **指引（原则 5 的低成本版）**：加一条 `/hongmeng realm map [维度]`，打印"最近 3 座地标的坐标与距离
  + 当前维度的解锁状态 + 你缺什么凭证"。比做地图物品便宜得多，且与现有 `/hongmeng dungeon`
  命令族同构（`README.md:1416-1421`）。

### B6 —— 打磨（可选，按玩家反馈插队）

* **小遗迹批次**：每维度 3~4 种小结构（井、断碑、石圈、废弃灵田），`spacing` 24~40 / `separation`
  6~10，各自 1 个小箱子（3~6 格）。这是原则 6 的"Minor Landmark"位，也是让维度**不空**的关键。
* **收集点**：每维度一个"上交采集物"的结算点（先用命令，后补方块交互），对应原则 8。
* **精修**：挑一座地标补真正的层叠斗拱、瓦垄、脊兽、隔扇——这正是
  `docs/grand-structures-spec.md:172-175` 明确留下的"路线 c 第二阶段"。

## 四、可复用的既有工具（现状能力 + 下一步要扩什么）

| 工具 | 现在能干什么（出处） | 下一步要扩什么 |
|---|---|---|
| `tools/gen_new_biomes.py` | 正则读 `BiomePlan.java` 当真源（`:108-135`）；从 `client-extra.jar` 核对每个原版引用（`:690-710`）；**特征顺序无环检查**（`canonical_feature_order()` `:202-251`、`sort_by_canonical()` 去重修自环 `:253-277`、`check_feature_order_acyclic()` 离线复现 `:728-785`）；`--check` 只校验不写盘（`:311-313`） | **不要改它**（写死 3 群系 `:133-134`）。把其中 4 个函数（`canonical_feature_order` / `sort_by_canonical` / `check_feature_order_acyclic` / `Vanilla`）**抽成共享模块**给 B2 的新脚本用 |
| `tools/gen_grand_structures.py` | `Blueprint` 体量搭建 + 庑殿顶收分 + 飞檐渐变（`:190-244`）；箱/刷怪笼带硬断言（`:140-171`）；镜像自检（`:309-316`）；调色板 `[` 自检（`:318-323`）；NBT/4 份 JSON/预览图一次产出（`:1334-1343`）；重跑逐字节一致 | 抽成 `tools/structure_kit.py`（Blueprint + JSON 三个 writer + 预览渲染），B4 的新生成器只写"每座建筑长什么样" |
| `tools/fix_structure_nbt.py` | 最小 NBT 读写库（`read_nbt`/`write_nbt`），被上面两个脚本 `importlib` 复用 | 无需扩。**注意它是被 import 的库，不是 CLI 修复工具**（虽然也能直接跑） |
| `tools/validate_structure_palette.py` | 每个 palette 条目 vs 真实 blockstate 定义（原版 + `assets/hongmeng/blockstates`）：方块名、属性名、属性值、变体合法性；判据与 `BlockStateParser.parseForBlock(allowPartial=true)` 一致（`:6-11`） | 无需扩。**每批 B4 必跑** |
| `tools/check_grand_structure_loot.py` | 宝箱/刷怪笼起飞前检查（`:6-19`）；物品 id 现场扫 `ModItems.java`、实体 id 扫 `ModEntities.java` | `DEFAULT_IDS` 只有 3 座（`:49`）→ 加新结构默认值或改成"扫 `structures/` 目录" |
| `tools/check_new_structure_json.py` | JSON 语法 + 交叉引用 + `spacing > separation` + 群系/标签存在性；顺带把 `data/hongmeng` 下全部 JSON 过一遍（`:84-94`） | `IDS` 写死 3 座（`:22`）→ 同上改成目录扫描 |
| `tools/grep_server_logs.py` | 服务端日志关键字计数表（含轮转的 `.log.gz`） | 增加 `Feature order cycle` / `Couldn't load structure` / `[arena]` 以外的关键字：`biome`、`checkerboard` |
| `tools/run_verify.ps1` | 编译 mod + 编译 `src/verify` + 跑 `LogicVerifier`（`:20-63`），不启动游戏 | 新增 `RealmPlan.validate()` 的断言行 |

---

## 五、风险与已知坑（含本项目已踩过的全部 5 条硬约束）

### 5.1 项目硬约束（原文照抄，每条都有出处）

1. **结构 NBT 的 palette `Name` 绝不能带 `[`**——要拆成 `Name` + `Properties`。
   否则 `NbtUtils.readBlockState` 把 `Name` 当 `ResourceLocation` 解析时抛
   `ResourceLocationException`，**结构静默永不生成，而服务端照常启动**
   （`tools/fix_structure_nbt.py:8-12`、`docs/modpack-roadmap.md:199-200`、
   `tools/gen_grand_structures.py:26-31`）。
2. **群系 features 数组不能有环**。`FeatureSorter` 把一个 step 内所有群系的特征做拓扑排序，
   有环就在**生成新区块时**抛 `IllegalStateException: Feature order cycle found` 并停服。
   两种成因都踩过：同 step 内顺序与原版冲突、同 step 内同一特征出现两次（自环）
   （`docs/quality-and-biomes-spec.md:234-250`）。修法全在生成器里：
   `canonical_feature_order()` / `sort_by_canonical()`（含去重）/ `check_feature_order_acyclic()`
   （`tools/gen_new_biomes.py:202-277,728-785`）。**手写 JSON 不许再改顺序。**
3. **用了 `FlyingMoveControl` 就必须注册 `FLYING_SPEED`**，否则区块里有该实体就崩
   （`docs/modpack-roadmap.md:201`）；已有检查脚本 `tools/check_flying_speed.py`。
4. **不下载网上素材**（授权不明）：一切用原版方块/粒子组合。
   原型证据：`docs/player-requirements.md:29-34`（没抓网上皮肤，改为自制 64×64 贴图）、
   `docs/modpack-roadmap.md:108-111`（建筑只读参考文章，不拷素材）。
5. **结构用生成器脚本产 NBT（可重复执行、结果确定），不用手工搭**
   （`docs/grand-structures-spec.md:17`；确定性验收口径 `:117`）。

### 5.2 docs 里读到的、与本方案直接相关的坑

| 坑 | 表现 | 出处 |
|---|---|---|
| `worldgen/structure` 的 `biomes` **不能写裸数组** | **服务端直接起不来**：`Failed to parse ... Not a string: [...]`。必须引用群系标签 `#hongmeng:has_structure/<id>` | `docs/player-requirements.md:94-111` |
| `data/**` 的 JSON 写错不是"少个功能"，是**整个服务器起不来** | 所以任何 worldgen 改动都必须真跑一次 `runServer` | `docs/player-requirements.md:110`、`docs/modpack-roadmap.md:198` |
| **日志要 grep 全** | `Couldn't load structure` 这类"不致命但功能失效"的错误漏过一次 | `docs/modpack-roadmap.md:204-205` |
| **接管点位的"地表材质"决定灵植能不能长** | 云梦泽第一版接管 `minecraft:river` 点位 → 地表是被切下去的河道 → 灵植 **0 株**；换 `mangrove_swamp` 才有 | `docs/quality-and-biomes-spec.md:215-232` |
| **新群系刻意不进 `#minecraft:is_overworld`** | 进去会让灵植密度翻倍（基础 + 高密度两份叠加），文档数字与区块生成负担都翻倍 | `docs/quality-and-biomes-spec.md:203-207` |
| **注入过程必须包 try/catch，绝不能不让服务器起来** | 反射失败只打警告 | `OverworldBiomeInjector.java:105-108`、`:68-72` |
| **反射必须用 `ObfuscationReflectionHelper`** | 生产环境字段名是 SRG，`getDeclaredField("parameters")` 线上失效 | `OverworldBiomeInjector.java:70-72`、`worldgen/ReflectField.java` |
| **刷新两个惰性缓存是必须的** | 不刷 `possibleBiomes` 与 `featuresPerStep`，新群系的灵植根本不生成 | `OverworldBiomeInjector.java:74-79,189-206` |
| **`floorY` 的坑** | 不先 forceLoad 就把 floorY 算成基岩层，整座场地建到地底下去——踩过一次 | `dungeon/DungeonArena.java:206` |
| **结构会生成在副本维度** | 因为副本用的是主世界噪声 + 主世界群系；1.20.1 的结构 JSON 没有维度白名单字段 | `docs/grand-structures-spec.md:182-184` |
| **`RegistryObject.get()` 不能在 mod 构造阶段调用** | 会让游戏起不来 | `docs/modpack-roadmap.md:197`、`docs/content-expansion-research.md:81` |
| **离线验证器不能碰注册表** | `Blocks`/`Attributes`/`ItemStack` 静态初始化会炸 | `docs/modpack-roadmap.md:203` |
| **`spacing` 必须大于 `separation`** | 不满足原版会抛异常；且 `separation` 要大于结构尺寸，否则两座互相咬 | `docs/grand-structures-spec.md:54-55`、`tools/check_new_structure_json.py:57-58` |

### 5.3 本方案**新引入**的风险（B1~B6 各自的前置检查）

1. **B1：`floating_islands` 会让 `DungeonArena.floorY()` 的探测环落空。** 探测环在半径 28–32
   （`DungeonArena.java:52-53`），浮空岛地形下可能全是空气 → `floorY` 取到极低值 → 整个平台建到虚空里。
   **B1 必须先验证四座场地的 `[arena] floorY=` 日志。**
2. **B1：`noise_settings` 与 `dimension_type` 的 `min_y`/`height` 必须匹配。** 当前是
   `min_y:-64, height:384, logical_height:384`（`data/hongmeng/dimension_type/epoch_realm.json:12,15-16`）；
   `overworld`/`large_biomes`/`amplified`/`caves`/`floating_islands` 都是 -64/384，安全；
   **`minecraft:nether`（0/256）绝对不能用**。
3. **B1/B2：维度会同时失去原版结构与原版群系。** 村庄、要塞、废弃传送门全没了（白名单是原版群系）。
   若不做 B4/B6，维度会比现在**更空**。
4. **B2：`checkerboard` 是纯二值棋盘，没有气候过渡。** 观感上是"三大块硬边"，`scale` 调大能缓解但消不掉。
   退路是照抄 `OverworldBiomeInjector` 写一个 `RealmBiomeInjector`（每维度一份接管表），
   但那是 **Java + 反射**，代价高一个数量级。**建议先跑通数据版再看要不要升级。**
5. **B2：`checkerboard` 的 `biomes` 里若混进 `minecraft:` 群系**，原版结构就会回来一部分，
   "维度专属"的干净性也没了。**约束：只写 `hongmeng:` 群系。**
6. **B2：新维度群系不进 `#minecraft:is_overworld`**，所以"全主世界"的矿石/灵植/刷怪 modifier
   一个都不会自动生效——必须像 `gen_new_biomes.py:577-603` 那样逐条显式挂载，漏一条就是"什么都不长"。
7. **B3：温度/降水必须相容。** 现有生成器断言 `temperature <= -0.5 或 > 2.0` 就报错
   （`gen_new_biomes.py:631-632`）；温度还会影响积雪/结冰，`demon_scorch` 这类"焦土"要配高温。
8. **B4：jar 体积。** 现有三座 NBT 共 ~105 KB（`xianmen_main_hall.nbt` 48941 +
   `lingmai_dongfu.nbt` 34933 + `wendao_tai.nbt` 21137）；12 座按同量级估再加 ~400 KB。
   建议每座 **≤ 40×40×40**，并复用调色板（`Blueprint.to_nbt()` 按 `(name, props)` 去重，`:281-288`）。
9. **B4：镜像自检只认 X 中轴。** `mirror_offenders()` 假定关于 `X=(sx-1)/2` 对称（`:309-316`）；
   **故意不对称的地标会报"非镜像格"**——它是提示不是错误，B4 要给不对称结构一个显式开关，
   别让 `--check` 变成噪声。
10. **B5：`dungeonClears` 是全局计数，分不出维度**（`CultivationData.java:37,506-511`）。
    用"凭证物品在背包"当判据可完全绕开，**强烈建议走这条**。
11. **B5：硬结界会挡住玩家正常玩法。** 范围判定必须**精确到结构包围盒**，
    且必须给 `/hongmeng` 一条管理员绕过命令。
12. **B6：不要留下"以后再做"的悬空结构。** 这是暮色最被诟病的点 [S6]。

---

## 六、每批的验收清单（谁能验、怎么验）

**四道通用闸门（每批都要过）**

| 闸门 | 判定 | 谁能验 |
|---|---|---|
| ① 编译 | `javac` **0 error**（口径 `docs/grand-structures-spec.md:114`） | AI / 开发者 |
| ② 离线断言 | `pwsh -File tools/run_verify.ps1` → 输出「**通过 587 项，失败 0 项**」（口径 `:115`） | AI |
| ③ `runServer` 日志扫描 | `python tools/grep_server_logs.py`：`Failed to parse` / `Unbound values` / `Unreachable` / `Couldn't load structure` / `Encountered an unexpected exception` / `crash-report` / `Parsing error` / `Feature order cycle` **全部为 0**（口径 `docs/quality-and-biomes-spec.md:267-273`、`docs/grand-structures-spec.md:152-165`） | AI（RCON `127.0.0.1:25598`） |
| ④ 玩家实测 | 见下表逐批"只能玩家验"列 | **玩家**（RCON 起不了玩家，见 `docs/grand-structures-spec.md:169-171`） |

**逐批附加项**

| 批次 | AI / 脚本能验的 | **必须玩家实测的** |
|---|---|---|
| B1 | 4 份 JSON 可解析（`check_new_structure_json.py`）；`/execute in <维度> run setblock` 返回成功；`[arena] fieldY=` 日志四条都在合理高度 | **肉眼看地形**：虚空裂隙是不是真的浮空岛、魔道巢穴是不是洞穴感；走一圈会不会掉出世界 |
| B2 | 12 份群系 JSON 生成且 `--check` 通过；无环检查 0 问题；`forceload add` + `/locate biome hongmeng:<id>` 12 条全部命中；`biome.hongmeng.*` 语言键补齐 | 三个群系的**边界观感**（checkerboard 硬边能不能接受）、F3 里群系名正确 |
| B3 | 每群系×每灵植 `placed_feature` 的 `count` 与 Java 表逐字一致；矿石/刷怪 modifier 指向正确；引用存在性 0 问题 | **在全新区块里用 `fill ... air replace` 数灵植株数**（这是唯一可信的密度验证法，口径 `docs/quality-and-biomes-spec.md:215-232`）；资源"够不够采" |
| B4 | `validate_structure_palette.py` 0 问题；`check_grand_structure_loot.py` 0 问题；`check_new_structure_json.py` 0 问题；生成器重跑**逐字节一致**；`place template <id> x y z` → `Loaded template`；`locate structure <id>` 能定位 | **进建筑里走一圈**：有没有卡住的位置、箱子够不够得着、刷怪笼强度是否合理、飞檐比例好不好看 |
| B5 | 施加/解除 debuff 的逻辑断言；结界范围判定的离线断言；`/hongmeng realm map` 输出正确 | **真的被锁一次、真的被解锁一次**：没凭证时挖不动/开不了箱子，拿到凭证后正常；debuff 的强度能不能忍 |
| B6 | 同 B4 + 小遗迹的 `spacing > separation` | 维度"空不空"的总体感受；收集点交东西是否顺畅 |

---

## 七、参考资料

> 说明：标注了 **[全文]** 的是用 `web_fetch` 读了正文的来源；标注 **[摘要]** 的只用了搜索摘要。
> 下文每条结论后面的 `[S#]` 指向本节。

| 编号 | 来源 | 用途 |
|---|---|---|
| S1 | [暮色森林进度顺序 — mcmod.cn](https://www.mcmod.cn/post/3266.html) **[全文]** | 完整流程走查：传送门做法；魔法地图三次合成链（渡鸦羽毛 + 火炬浆果 + 萤石粉 → 魔法地图核心 → 空白魔法地图）；娜迦 → 巫妖 → **三条并行支线**（米诺菇/九头蛇、幻影骑士/暮色恶魂、雪怪首领/冰雪女王）；「**暮色的 Boss 要按顺序打，如果不按顺序就会受到结界的惩罚**」；地精骑士要塞入口要在**战利品宝座**上放任意 Boss 战利品才开门；牛头人迷宫里的**迷宫地图核心**与隐藏的**迷宫破坏者**；巨魔洞窟的魔豆 → 云上小屋 → 巨人镐 → 巨型黑曜石 → 余烬之灯 → 烧荆棘 → 终焉城堡（**最终 Boss 没做**） |
| S2 | [Twilight Forest Mod Guide: Portal, Boss Order & Every Boss — minecraft-guides.com](https://www.minecraft-guides.com/mod/the-twilight-forest/) **[全文]** | Boss 顺序/血量/解锁表：娜迦(120)→巫妖(100)→{米诺菇(120)→九头蛇(360) \| 幻影骑士(35×6)→暮色恶魂(250) \| 雪怪首领(200)→冰雪女王(200)}→高原（需三线全通）；**跳过区域的具体惩罚**：沼泽叠饥饿、黑暗森林给黑暗、雪原/冰川冰冻、火焰沼泽着火、高原与终焉城堡**酸雨真伤**；**持有 Boss 战利品或其掉落物也算解锁**；魔法地图的合成与用法 |
| S3 | [Twilight Forest Landmarks — ftbwiki.org](https://ftbwiki.org/Twilight_Forest_Landmarks) **[全文]** | 地标体系：**Major Landmark 恒在 256 格网格上且上魔法地图**，Minor 随机撒且不上图；Major 概率表（小空心山 3/8、中 3/16、树篱迷宫 1/8、巫妖塔 1/8、大空心山 1/16、九头蛇巢 1/16）；**群系强绑定**（附魔森林必出 Quest Grove、火焰沼泽必出 Hydra Lair、林间空地必出 Labyrinth、暗黑塔只在 Dark Forest 且 1/3）；Minor 地标清单：骷髅德鲁伊小屋、废墟（6×6~10×10，约一半概率地下密室）、水井（井壁箱子）、石柱（黑曜石柱顶青金石 / 石柱顶铁块）、石笋、石圈（纯装饰） |
| S4 | [Twilight Forest — ftbwiki.org](https://ftbwiki.org/Twilight_Forest) **[全文]** | 维度总览：永久黄昏、无昼夜；地形比主世界平坦但仍有**中空的山丘**（内含矿脉、宝藏与危险怪物）；「主世界地牢/石油/矿物也会生成，**但不会以矿簇形式出现在空心山里**」；传送门可能把玩家直接送进空心山甚至 Boss 巢穴 |
| S5 | [1.7.X 暮色森林成就攻略 — mcmod.cn](https://www.mcmod.cn/post/106.html) **[全文]** | **进度锁的机制细节**：「新增的地形结界会造成一些负面的 BUFF，而 BOSS 的建筑也附带了一层保护结界（**无法挖掘破坏方块、无法对生物造成伤害、无法开启宝箱**）。只有完成成就，逐步解除结界，才能正常进行游戏」；18 条主线成就的**严格前置链**（拾取战利品即完成）；卡住时的 `/tfprogress <玩家> <BOSS>` 兜底；支线成就与三级地图（魔法地图 / 迷宫地图 / 迷宫-矿石地图）；**彩虹公羊**任务（交 16 色羊毛）；空心山三档对应红帽地精/红帽工兵/幽灵 |
| S6 | [Want to love it, but... — ModDex 玩家评测](https://moddex.gg/mod/the-twilight-forest/reviews/460) **[全文]** | 设计批评：「它 unfinished 太久了……团队似乎一直定不下来最终 Boss 要做什么，于是陷入不停地修别的、打磨别的，而不是做那个结局」；建议「先定一个 MC 版本做完再谈移植」 |

**每条"网上结论"的归属速查（便于回溯）**

> 进度锁 = 软 debuff + 硬保护结界 → [S2][S5] ｜ 解锁凭据是"可携带的战利品"而非进度标记 →
> [S1][S2][S5] ｜ 地标网格 + 地图指引 → [S1][S3] ｜ 群系强绑定到特定地标 → [S3] ｜
> Major/Minor 地标分层与权重 → [S3] ｜ 收集型 NPC（彩虹公羊）→ [S2][S5] ｜
> "空心山 = 矿 + 怪 + 宝藏"的地标内配比 → [S3][S4] ｜ 三条并行支线 + 最终高原汇聚 → [S1][S2] ｜
> 终焉城堡未完成是最主要的负面评价 → [S1][S6]

## 附录：复现命令（当前工具链）

```powershell
$py = "C:\Users\duyun\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py tools\gen_new_biomes.py --check            # 三个主世界群系：只校验（含无环检查）
& $py tools\gen_grand_structures.py --check      # 三座宏伟建筑：镜像 / 调色板 / 越界自检
& $py tools\validate_structure_palette.py        # 全部结构 NBT 的 palette vs 真实 blockstate
& $py tools\check_grand_structure_loot.py        # 三座建筑的箱子与刷怪笼
& $py tools\check_new_structure_json.py          # data/hongmeng 下全部 JSON 语法 + 交叉引用
& $py tools\grep_server_logs.py                  # 服务端日志关键字计数表
pwsh -File tools\run_verify.ps1                  # 编译 + 跑 LogicVerifier（587 项断言）
```

> B1/B2 起手时（在 `gen_realm_structures.py` / `gen_realm_biomes.py` 落地之前），
> 先跑 `check_new_structure_json.py` 确认 4 份 `dimension/*.json` 改完仍然可解析。
