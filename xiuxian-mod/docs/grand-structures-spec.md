# 宏伟建筑 + 维度战斗场地（骨架阶段 · 路线 c）

> 对应 `docs/modpack-roadmap.md` 的 **四.2（建筑要"大气宏伟仙气"）** 与 **七（维度战斗场景）**。
> 玩家选了 **(c)：先程序化铺量做出体量与骨架，之后再挑一两座精修**。
> 所以这一轮的目标不是"每个斗拱都到位"，而是**把骨架做对**：
> 台基、中轴、尺度、飞檐的渐变、斗拱的节制、留白。

设计原则来自玩家指定要搜的两篇：
[中式玄幻建筑避坑](https://post.smzdm.com/p/a03026w0/)（标题本身就是原则：
**飞檐不是房角，斗拱不是越多越好**）与
[Minecraft 东亚建筑参考手册](https://kaleidscoper.github.io/2025/10/15/2025-10-15-mc-cn-building/)。

---

## 一、三座新结构（结构 NBT + worldgen JSON）

生成器：`tools/gen_grand_structures.py`（可重复执行，结果确定，无随机/无时间戳）。
它复用 `tools/fix_structure_nbt.py` 的 NBT 读写，**不重复造轮子**。

| 结构 id | 中文 | 占地 (X×Y×Z) | 脊高 | 台基 | "宏伟"靠什么 |
|---|---|---|---|---|---|
| `hongmeng:xianmen_main_hall` | 仙门主殿 | 49 × 47 × 49 | **47 格** | 4 级收分石台基（49→47→45→广场 41） | **重檐庑殿顶**（下檐 + 上檐两重）、八角柱网 34 根檐柱/金柱、41×41 白玉广场 + 汉白玉栏杆、7 格宽御道与三级踏道、南面三间牌楼、**东西一对镜像配殿** |
| `hongmeng:wendao_tai` | 问道台 | 41 × 23 × 41 | 22 格 | 4 级收分台地，每级收 4 抬 3 | **圜丘式通天台**：四向踏道、顶台八柱 + 额枋（无顶，通天问道）、四角 19 格表木（华表）、中央太极石坛、四层栏杆 |
| `hongmeng:lingmai_dongfu` | 灵脉洞府 | 45 × 26 × 45 | 24 格 | 4 级台地山体（每级 4 格高、收 4 格） | 台地山体 + 中轴山门（牌楼式，带小屋顶）+ **掏空的内殿**（21×18×8：二级神台、下沉灵泉、两畦灵田、六根金柱）+ 东西对称登山踏道 + 顶台聚灵台 |

三座都通过脚本自检：**关于 X 中轴完全镜像**（`mirror_offenders()` 逐格对比，
一座 17542 格、一座 7406 格、一座 12741 格，0 个非镜像格）。

### 几个具体的"做法"（对应那篇文章的六条）

* **台基**：不是贴一圈装饰，而是**骨架**——主殿是 49→47→45→41 四级，
  每级一皮深色勒脚 + 一皮玉石压边；洞府山体每级 3 皮深色 + 玉石台面。
* **飞檐（起翘是渐变的）**：屋顶每上一层四周各收 1 格（庑殿顶），
  **四角按梯度剖面抬起**：最外一层沿边 4 格内抬 2/1 格，第二层抬 1 格，
  之后不再抬——所以是"檐口向角部连续翘起"，不是"四角各放一个翘角"。
* **斗拱（有节制）**：只有**一圈额枋**（`chiseled_quartz_block`）+ 柱头各出一块，
  没有任何密堆。
* **中轴**：主殿居中，配殿成对；广场栏杆、御道、香炉、灵田、灯柱全部成对镜像。
* **尺度**：主殿脊高 47 格、殿身 35 格宽、二层可见 10 格（> 要求的 15 格下限）。
* **留白**：41×41 的广场只有御道、一对香炉、两处灵田、四角灯柱；
  问道台顶台 13×13 只有中央石坛——空的地方就是设计。

### worldgen JSON（每座 4 份）

```
data/hongmeng/worldgen/structure/<id>.json                 jigsaw + beard_thin
data/hongmeng/worldgen/template_pool/<id>/start_pool.json   single_pool_element
data/hongmeng/worldgen/structure_set/<id>.json              random_spread
data/hongmeng/tags/worldgen/biome/has_structure/<id>.json   群系白名单
```

* `terrain_adaptation: beard_thin`：让地形在结构范围内被削平/回填，
  y=0 就是原地表，所以**台基第一级一定落在地上**。
* spacing/separation：主殿 112/64、问道台 72/44、洞府 88/52（都满足 spacing > separation，
  而且 separation 大于结构尺寸，避免两座互相咬在一起）。

---

## 二、四座副本的战斗场地

代码：`dungeon/DungeonArena.java`（新），接入点：
* `dungeon/DungeonRealm.java`：`enter()` 里"先立场地再找落点"；`ringPosition()` 改成优先落场地地面；
* `dungeon/DungeonRealmEvents.java`：玩家 tick 兜底 + **维度 tick 兜底**（每 20 tick 建一座）。

### 场地长什么样（四座同一套语法，材料按秘境换）

| 部位 | 做法 | 战斗意义 |
|---|---|---|
| 战斗地面 | 半径 19 的干净平台（纹样：同心环 + 中轴十字 + 斜线），**没有任何凸起** | 够跑、够放风筝；老的"按高度图刷怪"照样落在上面 |
| 台基 | 往下补到原地形（最多 8 格），往上垫到场地层 | 不会悬空、不会掉进沟里 |
| 中央祭坛 | 7×7→5×5→3×3 三级，顶上 3×3 平地 + 阵眼 + 四角灯 | **进场落点就是祭坛顶**，一眼看到全场 |
| 擂台 ×4 | 斜角 12 格处，5×5、2 格高，带朝内踏步 | 垂直性：抢高地、远程站位 |
| 角塔 ×4 | 3×3 基座 6 格 + 方柱到 14 格 + 顶灯 | 垂直性 + 地标，一眼判断方位 |
| 看台 | 外圈 5 层，逐层 +1，顶层女儿墙，四向 5 格宽踏道 | 层次；也把台基的断面藏起来 |
| 灯柱 ×4 | 中轴 17 格处，8 格高 + 顶灯 | 照明（副本维度天光很暗） |
| 材料 | 纪元=白玉/金；妖兽=深板岩/骨；魔道=黑石/岩浆块/灵魂灯；虚空=末地石/紫水晶/末地烛 | 四座气质不同 |

刷怪逻辑**没有被破坏**：`DungeonRealm.ringPosition()` 现在先问
`DungeonArena.spawnSpot()`（在场地地面上找落脚点，取不到才回落到高度图），
所以怪不会再刷到石柱顶或看台上；`EpochRealm`（别人负责、不能改）走的是高度图，
而半径 15.5 以内的刷怪环里唯一的凸起只有斜角 12 格处 2 格高的擂台——落上去也能自己跳下来。

建完之后还有一步 `rescue()`：把被台基埋住的玩家/怪挪到场地地面上
（第一次进副本时场地是"先有地形后有场地"，不救就会把刚刷出来的怪埋掉）。

---

## 三、怎么保证 NBT 真的能加载

三层检查，全在 `tools/`：

1. **`gen_grand_structures.py` 的镜像/调色板自检**——生成时就报"非镜像格"和"Name 带 `[`"。
2. **`validate_structure_palette.py`**——把每个 palette 条目拿去和**真实的 blockstate 定义**对：
   方块名存不存在、属性名对不对、属性值合不合法。
   判据与游戏一致：`NbtUtils.readBlockState` 走
   `BlockStateParser.parseForBlock(..., allowPartial=true)`，
   **缺属性没事（补默认值），多一个不存在的属性/非法值直接抛异常 → 整份结构加载失败**。
   这一轮就是靠它抓到两个真 bug：
   * 我自己写的栏杆用了 **`minecraft:quartz_wall`——原版根本没有这个方块**（石英没有墙），
     改成 `end_stone_brick_wall`；
   * 老结构 `xiuxian_yard.nbt` 里有一张讲台写了 `has_book=true`，
     而 1.20.1 的 lectern blockstate 只有 `facing`——同样是"整份结构静默不生成"级别的错误，
     已用 `fix_xiuxian_yard_lectern.py` 去掉那个属性。
3. **`check_new_structure_json.py`**——JSON 语法 + 交叉引用
   （structure→template_pool→structures/*.nbt→biome tag）+ spacing/separation +
   群系 id/标签是否存在（`data/**` 写错会让服务端起不来）。

---

## 五、验证记录（本轮实跑）

| 项目 | 结果 |
|---|---|
| 编译 | `javac` **0 error**（291 个 class；只有 1 条 `ResourceLocation(String,String)` 的 deprecation 提示，是老代码） |
| 离线验证器 | `通过 587 项，失败 0 项`（改完 Java 之后再跑一次，仍是 0 失败） |
| `tools/check_flying_speed.py` | 通过（本轮没加任何实体） |
| 生成器确定性 | 重跑一遍，5 份 nbt **逐字节一致**（gzip mtime=0、palette 排序） |
| 调色板严格校验 | 5 份 nbt（含既有 2 份）**0 问题** |
| JSON 语法 + 交叉引用 | `data/hongmeng` 下 276 份 JSON 全部可解析，引用全部有效 |

服务端实跑（RCON，服务器 `127.0.0.1:25598`）：

```
place template hongmeng:xianmen_main_hall 400 140 400   → Loaded template
place template hongmeng:wendao_tai        400  80 300   → Loaded template
place template hongmeng:lingmai_dongfu    300 140 400   → Loaded template
place template hongmeng:xiuxian_yard      350 120 350   → Loaded template（顺手修掉的老结构）

if block 410 144 410 minecraft:smooth_quartz   → Test passed   # 主殿 41x41 广场白玉地面
if block 424 186 417 minecraft:end_rod         → Test passed   # 主殿 47 格高的宝顶（end_rod 在 y=186）
if block 424 150 409 hongmeng:root_altar       → Test passed   # 主殿殿内神台
if block 411 100 311 minecraft:gold_block      → Test passed   # 问道台 19 格表木的金顶
if block 322 158 422 hongmeng:root_altar       → Test passed   # 洞府顶台聚灵台
if block 322 145 420 hongmeng:root_altar       → Test passed   # 洞府内殿神台（真的是掏空的）

locate structure hongmeng:xianmen_main_hall → [-3312, ~, 368]
locate structure hongmeng:wendao_tai        → [ -864, ~, -2240]
locate structure hongmeng:lingmai_dongfu    → [-1040, ~, -2688]
locate structure hongmeng:xiuxian_yard      → [ -800, ~, -1040]   # 老结构也没坏

# 战斗场地（四座维度，floorY=63，阵眼在 y=66）
if block 0 66 0 minecraft:gold_block        → Test passed   # 纪元
if block 0 66 0 minecraft:bone_block        → Test passed   # 妖兽
if block 0 66 0 minecraft:magma_block       → Test passed   # 魔道
if block 0 66 0 minecraft:amethyst_block    → Test passed   # 虚空
if block 3 63 3 minecraft:smooth_quartz     → Test passed   # 纪元地面
if block 8 64 0 minecraft:air               → Test passed   # 刷怪环是干净的（刷怪逻辑没被破坏）
if block 21 69 21 minecraft:quartz_pillar   → Test passed   # 角塔
if block 5 68 24 minecraft:chiseled_stone_bricks → Test passed # 看台
```

日志关键字（今天全部服务端日志，含 run/logs 轮转的 `.log.gz`）：

| 关键字 | 计数 | 说明 |
|---|---|---|
| `Failed to parse` | 1 | **11:42 的老记录**，是当时坏掉的 `xiuxian_yard.json`，与本轮无关 |
| `Unbound values` | 1 | 同上（同一次崩溃的两行） |
| `Unreachable` | **0** | |
| `Couldn't load structure` | 3 | 全部是 **13:15 / 13:07 / 11:43 的 `hongmeng:xiuxian_yard`**（老结构，就是上面那条 lectern 属性坑）；**本轮 14:12/14:17/14:20 三次启动 + 我的两次 runServer 日志里 0 条** |
| `Encountered an unexpected exception` | 5 | 3 次 `Exception generating new chunk`（并行那条线的 biome feature order cycle）、2 次 `Ticking entity`（11:43 / 13:05 的更早记录） |
| `crash-report` | 7 | 同上那些崩溃的附带产物 |
| `Parsing error` | **0** | |
| `Missing texture` | **0** | 本轮没加任何贴图 |
| `Feature order cycle` | 55 | **并行那条线的 `OverworldBiomeInjector`**（`yunmeng_marsh` / `huanggu_battlefield`），不是本轮改动 |
| `[arena]` | 8 | 4 条 `floorY=63`（修好之后）、4 条 `floorY=-56`（修之前的第一次运行，已被我清掉并重生成） |

## 六、没做完 / 没验到的（明确写清楚）

1. **"玩家进副本打一波怪"这条端到端没跑**——RCON 起不了玩家（服务端没有客户端连接），
   所以 `DungeonRealm.ringPosition()` 改造后的刷怪效果只做到了"逻辑 + 静态检查"：
   刷怪环半径 15.5 以内是干净地面、`spawnSpot()` 的取点逻辑只依赖场地地面 y。
   要真正确认，需要一次带客户端的实机测试。
2. **精修没做**（这是路线 c 的原计划）：斗拱只做到"一圈额枋 + 柱头各出一块"，
   没有做真正的层叠斗拱；屋顶没有做瓦垄/脊兽；栏杆是最简的望柱+栏板；
   门窗全是空洞没有隔扇。这些留给"挑一两座精修"的阶段。
3. **三座结构没有做旋转/镜像变体**（jigsaw 起始件默认不旋转），
   所以主殿永远朝北、山门永远在南——这是有意的（中轴朝向固定），但主世界里会看出重复感。
4. **没有做地形适配的额外兜底**：靠的是原版 `beard_thin`；
   如果生成在山坡上，结构范围内的地形会被削平，范围外会留一圈"胡子"。
   实际观感需要在主世界里找一座真生成的看看（本轮因为并行线的 biome 崩服没能去看）。
5. **四座副本维度里也会生成这三座结构**（structure_set 是按群系匹配的，
   而副本维度用的是主世界噪声 + 主世界群系），所以副本里可能撞见仙门主殿。
   这是沿用老 `xiuxian_yard` 的既有行为，不算回归，但如果玩家不想这样，
   得给结构 JSON 加维度白名单（原版没有这个字段，需要自己写 placement）。

## 七、复现命令

```powershell
$py = "C:\Users\duyun\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py tools\gen_grand_structures.py            # 生成 NBT + JSON + 预览图
& $py tools\validate_structure_palette.py      # 调色板 vs blockstate 定义
& $py tools\check_new_structure_json.py        # JSON 语法与交叉引用
& $py tools\grep_server_logs.py                # 服务端日志关键字计数表
& $py tools\fix_structure_nbt.py               # 修 palette 里的 Name 状态串（历史工具）
& $py tools\fix_xiuxian_yard_lectern.py        # 去掉 lectern 上不存在的 has_book
```

预览图在 `tools/build/structure_preview/<id>.png`：
正面 / 侧面 / **中轴剖面** / 平面图四联，用来目视检查体量与比例
（剖面能直接看出内殿、甬道、灵泉是不是真的掏空了）。
