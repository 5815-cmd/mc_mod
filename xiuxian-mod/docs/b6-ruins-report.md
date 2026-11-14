# B6 交付报告 · 四个自建维度的「小遗迹 + 采集点」

> 批次：`docs/dimension-content-plan.md` §三 B6（小遗迹批次）
> 施工范围：**只动 `tools/` 与 `data/hongmeng/` 下的数据**；未改任何 Java、未跑 gradle。
> 生成器：`tools/gen_realm_ruins_b6.py`（新增）
> 独立校验器：`tools/check_realm_ruins_b6.py`（新增）
> 日期：2026-10-10

---

## 一、新增小遗迹：13 座（纪元 3 / 妖兽 4 / 魔道 3 / 虚空 3）

每座产 5 份文件（nbt + structure + template_pool + structure_set + biome tag），
全部用 `tools/gen_grand_structures.py` 的 `Blueprint` 生成（可重复执行、结果确定）。

| id | 中文 | 维度 | 尺寸(X×Y×Z) | 方块 | 箱子内容 | 刷怪笼 | spacing/sep |
|---|---|---|---|---|---|---|---|
| `epoch_broken_steles` | 残破石碑阵 | 纪元秘境 | 11×7×11 | 184 | 灵叶×3、雪莲花瓣×2、纪元令×1 | 傀儒队长 | 32/8 |
| `epoch_collapsed_observatory` | 坍塌观星台 | 纪元秘境 | 11×9×11 | 165 | 灵晶×2、回灵丹×2、雪莲花瓣×3 | — | 36/9 |
| `epoch_portal_ruins` | 废弃传送门残骸 | 纪元秘境 | 9×8×9 | 79 | 灵叶×3、雪莲花瓣×2、灵晶×1 | — | 28/7 |
| `beast_bone_pile` | 兽骨堆 | 妖兽秘境 | 9×6×9 | 111 | 雪莲花瓣×3、火焰花瓣×2、灵叶×3 | 灵狼 | 30/8 |
| `beast_den_remains` | 巢穴残骸 | 妖兽秘境 | 11×5×11 | 164 | 寒冰藤花瓣×2、灵叶×3、雷音草花瓣×1 | 土行貂 | 34/9 |
| `beast_hunt_fence` | 猎场围栏 | 妖兽秘境 | 11×6×11 | 72 | 雷音草花瓣×2、灵叶×3、灵晶×1 | 灵狼 | 30/8 |
| `beast_hunting_camp` | 废弃猎营 | 妖兽秘境 | 11×5×11 | 121 | 火焰花瓣×2、灵叶×3、雪莲花瓣×2、回灵丹×1 | 土行貂 | 38/10 |
| `demon_ritual_circle` | 邪祭石圈 | 魔道巢穴 | 11×5×11 | 172 | 血珞芝花瓣×2、道印×1、灵晶×2 | 血魔修士 | 32/8 |
| `demon_broken_pillar` | 断裂的魔柱 | 魔道巢穴 | 7×9×7 | 91 | 血珞芝花瓣×3、空冥兰花瓣×1、灵叶×2 | 魔道统领 | 26/7 |
| `demon_dry_well` | 枯骨枯井 | 魔道巢穴 | 7×7×7 | 92 | 血珞芝花瓣×2、道印×1、血珞精×1 | 血魔修士 | 24/6 |
| `void_shattered_dais` | 碎裂的道台 | 虚空裂隙 | 11×6×11 | 145 | 空冥兰花瓣×3、空冥石×2、灵晶×2 | 虚空蛇 | 34/9 |
| `void_grounded_stele` | 贴地残碑 | 虚空裂隙 | 7×7×7 | 57 | 空冥兰花瓣×2、雷音草花瓣×2、灵叶×3 | — | 26/7 |
| `void_rift_trace` | 虚空裂隙残迹 | 虚空裂隙 | 9×7×9 | 84 | 空冥兰花瓣×3、空冥石×2、纪元令×1 | 虚空道主 | 30/8 |

* 尺寸全部落在要求的 **5×5×5 ~ 11×11×9** 内（脚本里 `SIZE_MIN/SIZE_MAX` 逐座断言）。
* 13 座 NBT 合计 **11.5 KB**（最大 1.2 KB），对 jar 体积无压力。
* 每座 **恰好 1 个箱子**（3~6 格、`Slot` 不重复且在 0~26、`Count` 1~3），10 座带 1 个刷怪笼 ——
  这就是 B6 的"软锁"：没到境界打不过看守，到了才拿得走。
* 破坏感来自确定性哈希撒块：地面只剩零星石板/碎石、断柱顶端随机缺格、围栏留断口。
* `epoch_portal_ruins` 用本模组的 `hongmeng:epoch_portal_frame` 搭**故意残缺**的门框
  （门洞里有塌落的碎石 + 右柱只有 4 格 + 顶梁只搭半边），所以它在世界里只是残骸，
  **点不着火、不会把玩家传进副本**（`EpochPortalFrameBlock#use` 的门洞校验过不去）。
* 悬空自检：除白名单里的悬挑（星盘架在立柱顶、晾骨架横在两柱之间、虚空刻意漂浮的碎块）外，
  任何 y>0 的方块下方都必须是实心；白名单外的悬空会直接报错。
* 预览图：`tools/preview/b6_ruins_sheet.png`（13 座的正俯视 + 南向立面，一眼看形）。

## 二、新增采集点：24 个 placed_feature + 24 个 biome_modifier + 5 个 configured_feature

四类资源簇 × 四个维度，全部照抄 B3 的写法（`forge:add_features` + `step`），
**没有改任何群系 JSON 的 `features` 数组**（避开"顺序/自环"那类崩服坑）。

| 类别 | 资源 | 生成的 placed_feature | 覆盖群系 | 参数 |
|---|---|---|---|---|
| 花草簇 | 雪莲草 / 寒冰藤 | `xuelian_grass_cluster_epoch`、`hanbing_teng_cluster_epoch` | 纪元 3 群系 | 1/4~1/5 概率、每簇 2 丛、地表 |
| 花草簇 | 雷音草 / 火焰花 | `leiyin_cao_cluster_beast`、`huoyan_hua_cluster_beast` | 妖兽 3 群系 | 同上 |
| 花草簇 | 血珞芝 / 空冥兰 | `xueluo_zhi_cluster_demon`、`kongming_lan_cluster_demon` | 魔道 3 群系 | 同上 |
| 花草簇 | 空冥兰 / 雷音草 | `kongming_lan_cluster_void`、`leiyin_cao_cluster_void` | 虚空 3 群系 | 1/3~1/5 概率 |
| **灵液簇** | `hongmeng:spirit_grass`（采下掉**灵叶**） | `spirit_grass_cluster_{epoch,beast,demon,void}` | 每维度 3 群系 | 1/3~1/4 概率、每簇 2~3 丛 |
| **矿脉** | 灵铁/灵晶/空冥/血珞矿 | 8 个 `<ore>_vein_<realm>` | 每维度 2 种矿 | 窄高度带 + `count` 2~4 |
| **灵木** | `lingmu_{epoch,beast,demon,void}`（树） | `lingmu_grove_{epoch,beast,demon,void}` | 每维度 3 群系 | 1/10~1/12 概率、2~3 棵 |

* **灵草（灵液）是全新资源**：此前 `hongmeng:spirit_grass` 在 worldgen 里**一处都没生成**
  （只有掉落表用到它）。现在四个维度×各 3 群系都会长，采下 = 灵叶 1~3 + 25 修为 + 概率丹药。
  密度刻意压在 B3 灵植之下（≈1 丛/区块 vs B3 的 6~30 丛/区块）。
* **灵木也是新内容**：`minecraft:tree` 配置逐字段照抄原版 `oak.json`，只换树干/树叶，
  四维度四种观感（橡木 / 深色橡木 / 绯红菌柄+下界疣块 / 诡异菌柄+诡异疣块）。
  注意魔道 3 个群系此前**一棵树都没有**，灵木补上了这一点。
* 矿脉是"窄高度带 + 高 count"（例：`xueluo_ore_vein_demon` y=-60~-8、count 3），
  在 B3 的全高度矿之上再叠一层"能撞见的矿脉带"。

## 三、落盘清单（共 118 份新文件）

| 目录 | 新增 |
|---|---|
| `data/hongmeng/structures/*.nbt` | 13 |
| `data/hongmeng/worldgen/structure/*.json` | 13 |
| `data/hongmeng/worldgen/template_pool/<id>/start_pool.json` | 13 |
| `data/hongmeng/worldgen/structure_set/*.json` | 13 |
| `data/hongmeng/tags/worldgen/biome/has_structure/*.json` | 13 |
| `data/hongmeng/worldgen/placed_feature/*.json` | 24 |
| `data/hongmeng/worldgen/configured_feature/*.json` | 5 |
| `data/hongmeng/forge/biome_modifier/add_*.json` | 24 |

`structure_set` 的群系白名单（`has_structure` tag）**精确等于本维度的那 3 个群系**，
逐座与 `dimension/<realm>_realm.json` 的 `biome_source.biomes` 对比过 —— 主世界不可能出现这些遗迹。

## 四、校验命令与真实输出

Python：`C:\Users\duyun\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe`
（完整原始输出另存 `_cache/b6_verify_output.txt`）

### 1) 生成 + 生成器自带自检（重跑生成器、与磁盘逐条比对）

```powershell
& $py tools\gen_realm_ruins_b6.py
```
```
== B6 small ruins: 13 ==
id                             realm  sizeXYZ     blocks  chest   spawn spacing/sep     salt
epoch_broken_steles            epoch  11x7x11        184      1       1      32/8 20261221
epoch_collapsed_observatory    epoch  11x9x11        165      1       0      36/9 20261222   cantilever=8(ok)
epoch_portal_ruins             epoch  9x8x9           79      1       0      28/7 20261223   cantilever=1(ok)
beast_bone_pile                beast  9x6x9          111      1       1      30/8 20261224
beast_den_remains              beast  11x5x11        164      1       1      34/9 20261225
beast_hunt_fence               beast  11x6x11         72      1       1      30/8 20261226   cantilever=3(ok)
beast_hunting_camp             beast  11x5x11        121      1       1     38/10 20261227
demon_ritual_circle            demon  11x5x11        172      1       1      32/8 20261228
demon_broken_pillar            demon  7x9x7           91      1       1      26/7 20261229
demon_dry_well                 demon  7x7x7           92      1       1      24/6 20261230
void_shattered_dais            void   11x6x11        145      1       1      34/9 20261231   cantilever=4(ok)
void_grounded_stele            void   7x7x7           57      1       0      26/7 20261232
void_rift_trace                void   9x7x9           84      1       1      30/8 20261233   cantilever=4(ok)

== B6 harvest clusters ==
configured_feature 5 / placed_feature 24 / biome_modifier 24

wrote 118 files (13 ruins x 5 + 53 harvest files)

== disk re-check (NBT read back and diffed against the blueprint) ==
OK epoch_broken_steles            blocks=184   palette=13   chest=1 spawner=1
OK epoch_collapsed_observatory    blocks=165   palette=11   chest=1 spawner=0
OK epoch_portal_ruins             blocks=79    palette=11   chest=1 spawner=0
OK beast_bone_pile                blocks=111   palette=10   chest=1 spawner=1
OK beast_den_remains              blocks=164   palette=12   chest=1 spawner=1
OK beast_hunt_fence               blocks=72    palette=10   chest=1 spawner=1
OK beast_hunting_camp             blocks=121   palette=11   chest=1 spawner=1
OK demon_ritual_circle            blocks=172   palette=13   chest=1 spawner=1
OK demon_broken_pillar            blocks=91    palette=14   chest=1 spawner=1
OK demon_dry_well                 blocks=92    palette=15   chest=1 spawner=1
OK void_shattered_dais            blocks=145   palette=11   chest=1 spawner=1
OK void_grounded_stele            blocks=57    palette=9    chest=1 spawner=0
OK void_rift_trace                blocks=84    palette=12   chest=1 spawner=1

problems = 0
```

### 2) `--check`（不写盘，纯内存自检）

```powershell
& $py tools\gen_realm_ruins_b6.py --check
```
```
(--check: nothing written) in-memory problems = 0
```

### 3) 独立硬约束检查（新增脚本）

```powershell
& $py tools\check_realm_ruins_b6.py
```
```
vanilla reference: 231 placed_feature (15 placement types), 194 configured_feature (53 types)
biome features layer: 15 biomes / 621 entries, 0 not-a-placed_feature-id
biome_modifier: 97 checked (76 add_features), 0 bad features field
placed_feature: 72 checked / 314 placement steps (0 bad ref, 0 unknown type)
configured_feature: 17 checked (0 unknown type)
B6 ruins: 13 checked, 0 with a wrong biome whitelist
configured_feature field-path check: 17 checked against vanilla files of the same type, 0 with invented paths
NBT tag-type check: 13 ruin nbt scanned (Slot/Count=BYTE, spawner shorts=SHORT, id=STRING), 0 violations

problems = 0
```
> 这条覆盖了三条硬约束：`features` 层全是 placed_feature id（**621 条引用零方块 id**）、
> 用到的放置修饰符/特征类型都在原版注册表里出现过（**封闭注册表，写错单词=整包加载失败**）、
> 新 configured_feature 的字段路径都能在原版同类文件里找到（**没有自造字段**）。

```powershell
& $py tools\check_realm_ruins_b6.py --b6-only
```
```
biome_modifier: 24 checked (24 add_features), 0 bad features field
placed_feature: 24 checked / 120 placement steps (0 bad ref, 0 unknown type)
configured_feature: 5 checked (0 unknown type)
B6 ruins: 13 checked, 0 with a wrong biome whitelist
problems = 0
```

### 4) 项目既有工具：结构 JSON 交叉引用

```powershell
& $py tools\check_new_structure_json.py
```
```
OK beast_bone_pile      spacing=30 sep=8 salt=20261224 群系 3 条 引用全部有效
...
OK void_rift_trace      spacing=30 sep=8 salt=20261233 群系 3 条 引用全部有效
（26 座结构全部 OK，含 B4 的 9 座地标与 3 座宏伟建筑）

data/hongmeng 下 467 份 JSON 语法全部可解析
问题合计: 0
```

> ⚠️ 这个脚本原来把 `hongmeng:` 群系也拿到原版 `client-extra` 缓存里找，
> 于是 **B4 的 9 座维度地标一直被误报"群系不存在"**（基线：`epoch_altar` 报 3 条错、退出码 1），
> 而且 `IDS` 只写死了 4 座。本次一并修掉：本模组群系查本模组数据目录、`IDS` 改成扫
> `worldgen/structure/` 目录 —— 现在 26 座结构一起过，问题合计 0。

### 5) 项目既有工具：调色板 vs 真实 blockstate

```powershell
& $py tools\validate_structure_palette.py
```
```
OK beast_bone_pile.nbt          palette 10 条全部对得上 blockstate 定义
OK beast_den_remains.nbt        palette 12 条全部对得上 blockstate 定义
...
OK void_shattered_dais.nbt      palette 11 条全部对得上 blockstate 定义
（27 份 NBT 全部 OK）

问题条目合计: 0
```

### 6) 项目既有工具：palette 的 `[` 复查

```powershell
& $py tools\fix_structure_nbt.py
```
```
共修复 0 条

=== 复查：还有没有带 [ 的 Name ===
  剩余问题条目: 0
```

### 7) 确定性：重跑两遍，118 份产物逐字节一致

```powershell
# 取 118 份产物的 sha256 清单再整体 sha256 → 跑一遍生成器 → 再取一次
combined sha256 BEFORE = 6f9544102e03a6688b7a8eef721695fb6393a863e55e40adc30cfa73ce676674
wrote 118 files (13 ruins x 5 + 53 harvest files)
problems = 0
combined sha256 AFTER  = 6f9544102e03a6688b7a8eef721695fb6393a863e55e40adc30cfa73ce676674
DETERMINISTIC = True
```

## 五、没做到 / 需要主代理接手的事

1. **没有跑 `runServer`，也没有跑 gradle**（任务明确要求不做）。所以 B6 的数据
   还没经过"起服加载"这一关：`Failed to parse` / `Unbound values` / `Couldn't load structure`
   这些日志关键字需要在父代理的下一轮 `runServer` 里扫一遍
   （`python tools/grep_server_logs.py`）。
2. **就地生成验证**（`/locate structure hongmeng:epoch_broken_steles`、`place template`）没做，
   同样需要起服。
3. **B6 的"收集点"（上交采集物的结算点）没做** —— 那部分按分工属于 Java 侧（命令/交互）。
   本批已经把"可上交的采集物"备齐：六阶灵植花瓣、灵叶（灵草）、四种矿石。
4. **灵木/灵草的实际生成率**只能在游戏里用 `fill ... air replace` 数（口径见
   `docs/quality-and-biomes-spec.md:215-232`）；离线只能保证配置与引用正确。
   预估：灵草 ≈1 丛/区块（纪元/妖兽）、0.5 丛/区块（魔道/虚空）；灵木 ≈0.3 棵/区块（纪元/妖兽）。
5. **经济影响提示**：灵草会给灵叶（坊市货币）与修为，密度虽低于 B3 灵植，
   但四个维度同时开长，建议起服后实测一轮产出，必要时只改
   `tools/gen_realm_ruins_b6.py` 里 `collect_content()` 的 `(chance, count)` 两个数即可。

## 六、本次顺带修掉的既有问题

* `tools/check_new_structure_json.py`：
  ① 群系解析对 `hongmeng:` 走本模组数据目录（原来拿原版缓存找，B4 的 9 座地标全被误报）；
  ② 结构 id 从写死的 4 座改成扫 `worldgen/structure/`（现在 26 座一起查）；
  ③ 群系 id 缺命名空间时报错而不是崩。
* 没有改 `tools/gen_grand_structures.py`（`Blueprint` 原样复用；它同时被并行的
  "宫殿精修"批次在改，本批只读它）。
