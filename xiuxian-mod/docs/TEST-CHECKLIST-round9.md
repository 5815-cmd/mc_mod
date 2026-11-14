# 本轮测试清单（接手会话 · 第一批）

> 这一批**全部是我改的**，起手前先看 `docs/HANDOFF.md`（上一轮的技术交接）。
> 部署：`D:\MC\.minecraft\versions\1.20.1-Forge_47.4.0\mods\hongmeng-forge-0.1.0.jar`
> **测试前完全退出游戏再启动**（属性上限、注册表、渲染器都在启动时确定）。

---

## 一、上一轮你反馈的 9 项，本批处理情况

| # | 你的原话 | 状态 | 怎么测 |
|---|---|---|---|
| ① | 灵草「要好看的实际模型」 | **做了**（等你看效果） | 别说，直接看地里的雪莲草 / 火焰花 / 寒冰藤 / 雷音草 / 空冥兰 / 血珞芝，四个生长阶段应该**越看越不一样** |
| ③ | 法术拖拽 | **做了** | `O` 开法术栏 → 左键按住左边功法**拖**到右边槽位松手；原来的"点一下→点槽位"还在 |
| ④ | 双击空格飞不起来 | **做了** | 金丹以上：**空中双击空格**（300ms 内两下）；`H` 键照旧可用 |
| ⑤ | 雷领域没有真实落雷 | **做了** | 开雷领域打怪，应该看到**真的雷柱劈下来**（不是只有火花） |
| ⑦ | 提升显示要黄色、白字看不清 | **做了** | 打怪 / 打坐时的"修为 +N"、破境提示、伤害数字，现在都是**黄色** |
| ⑧ | 法宝放上方 | **做了** | 开背包：法宝栏现在是**横排、在背包正上方**；窗口太小才会退到背包右边 |
| ⑨ | 挖东西掉落有延迟 | **已找到真凶并修掉** | 见下面第三节：手拿**灵兽袋**时任何方块都挖不掉 |
| ⑩ | `/hongmeng learnall` 报错 | **做了** | 直接输 `/hongmeng learnall`，应该一次学会全部功法 |
| ⑪ | 宫殿难看 + 要怪物宝箱 | **宝箱 + 刷怪笼都做了**（外观精修还没做） | 三座建筑里现在有 **22 个宝箱 + 2 个刷怪笼**（主殿 / 问道台 / 灵脉洞府）；外观那部分等你先说"哪里难看"我再改 |

---

## 二、重点验证项（按风险从高到低）

### 1. ⭐ 灵草新模型（①，最不确定的一项）
我**看不到画面**，模型是按"多层交叉平面 + 32×32 手绘贴图"做的，几何自检全是过。
要看的：
- 有没有**黑块 / 透明消失 / 贴图糊**？
- 四个阶段是不是**明显越来越大、越来越开花**？
- 站在旁边看，是不是"有厚度的一丛"，而不是一张纸片？

### 2. ⭐ 双击空格（④）
- 金丹以上、**站在地上跳一下 → 空中再按一次空格** → 应该起飞（脚下出现剑）。
- **空中连按两下**（不是先跳）→ 也应起飞。
- `H` 键仍然能起落。
- **创造模式飞着的时候**点空格，**不应该**把飞行关掉。
- 骑着灵兽时双击空格 → 应该只是"上升/二段跳"，不该切御剑。

### 3. ⭐ 雷领域落雷（⑤）
- 落雷是**纯视觉**闪电（`setVisualOnly`），伤害仍走领域数值表。
- 要确认：**没有点火**、**伤害没变**（正常 5 点 / 三连 15 点 + 10%）。

### 4. 法术栏拖拽（③）
- 拖到**主动槽**能装上；拖到**被动槽**能装上；
- 被动拖进主动槽 → 应该被拒绝并有提示；
- 拖到界面外面松手 → 取消，不报错；
- 拖到一半按 `Esc` / 按 `O` 关界面 / 改窗口大小 → 不能卡状态、不能崩。

### 5. 法宝栏位置（⑧）
- 开背包：法宝栏在**背包上方**，**不再压住合成输出格和 tooltip**；
- 5 个槽位横排，左键点空格位能自动装、点已装的能卸。

### 6. 黄色文字（⑦）
- "修为 +N"、"修为已满 · 按 B 破境"、破境提示、伤害数字 → **黄色**；
- 如果还有别的地方是白字看不清，**截个图**告诉我大概位置。

### 7. 宝箱（⑪）
- 找建筑：`/locate structure hongmeng:xianmen_main_hall`（还有 `wendao_tai`、`lingmai_dongfu`），`/tp` 过去。
- 看箱子里有没有东西（灵叶 / 灵晶 / 丹药 / 纪元令等）。
- ⚠️ **已有世界的旧区块不会补生成**，要看新建筑得**开新世界**或去没加载过的区域。

### 8. 宫殿外观（⑪ 的一部分）
这一轮我只做了一处**有把握**的外观改动：**屋面的瓦**。
原来整个屋顶是同一种 `waxed_oxidized_copper`，一大片纯色，远看像塑料板；
现在按坐标错开三种铜（氧化铜 / 打蜡氧化铜 / 打蜡氧化切制铜），
做出"瓦垄 + 铜锈深浅"的质感（`tools\gen_grand_structures.py` 的 `_roof_tile`）。

**平面/立面预览**（改前 → 改后都在这个目录，`front/side/section/plan` 四视图）：
- `tools\build\structure_preview\xianmen_main_hall.png`
- `tools\build\structure_preview\wendao_tai.png`
- `tools\build\structure_preview\lingmai_dongfu.png`

> ⚠️ 这是**正交立面图，不是游戏内观感**。它只能看比例、对称、配色搭配，
> 看不出材质细节与光影。所以"还难不难看"必须你在游戏里定。

**"外观精修"我只做到这一步，剩下的需要你指方向** —— 说清是下面哪一类我就照那类改：
1. **屋面**（颜色太单一 / 檐口不够翘 / 正脊太素）；
2. **柱子与墙体**（柱子太细太密 / 墙面太空白 / 门窗比例）；
3. **台基与广场**（台基太矮 / 广场太空 / 栏杆样式）；
4. **尺度与比例**（整体太矮胖 / 屋顶过大压住殿身）；
5. **配色**（青绿+朱红+白玉这套搭配本身不喜欢）。

---

## 三、⑨ 挖矿那个问题：真凶已找到并修掉

你说的是：**"挖掉之后方块消失，但踩上去还有方块（只是看不见），而且没有掉落物"**。
这在 MC 里叫**幽灵方块**：客户端以为挖掉了，服务端其实**没挖掉**。

### 根因：手拿「灵兽袋」时，任何方块都挖不掉

`item/beast/BeastPouchItem.java` 里有一句

```java
@Override
public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
    return false;      // ← 这句是 bug
}
```

**`canAttackBlock` 与"右键放置方块"毫无关系**（原来那句注释就是这么以为的）。
Forge 只在**左键破坏方块**时问它一次：

```java
// ForgeHooks.onBlockBreakEvent
if (!stack.isEmpty() && !stack.getItem().canAttackBlock(state, level, pos, player)) {
    cancel = true;          // → 返回 -1 → 方块不删、不掉落
}
```

所以：**手里拿着灵兽袋挖方块 = 服务端每次都取消**，客户端却已经本地预测"方块没了"。
表现精确对应你描述的三件事：看不见 / 踩上去还在 / 没有掉落物。

**已改成 `return true`。** 袋子真正要拦的是右键，而那走的是 `use()` 与
`interactLivingEntity()`，跟这个方法无关——所以修完**右键收灵兽的行为一点没变**。

> 排查路径（留个记录）：先确认这不是挖矿速度问题（速度表没动），
> 再把 Forge 的 `ServerPlayerGameMode.destroyBlock` 字节码翻出来核对，
> 确认"事件被取消 → `return false`"是**唯一**会造成幽灵方块的路径，
> 最后在 mod 里全文搜 `canAttackBlock` / `onBlockStartBreak` —— 只有这一处。

### 请这样验证

1. **手里拿着灵兽袋**去挖石头 / 泥土 / 木头 → 现在应该正常破、正常掉；
2. 顺手确认**右键灵兽仍能收进袋子**、**潜行右键仍开名册**（这两个不该被改坏）；
3. 如果还有别的物品拿在手里挖不掉，**告诉我是什么物品**（同样的机制，我再查一遍）。

---

## 四、服务端实测结果（本轮已跑，不用你验）

在**干净工作目录 + 独立端口**起了一次专用服务端（把 `run/` 临时挪走、跑完原样还回去），
用 RCON 做了功能检查：

| 检查 | 结果 |
|---|---|
| 服务端启动（含新 NBT / 新 JSON / 新配方） | **无异常、无崩溃**（只有两条 Forge 首次生成配置文件的 "corrected from null to default" 提示） |
| `place template hongmeng:xianmen_main_hall` | **Loaded template** ✓ |
| `place template hongmeng:wendao_tai` | **Loaded template** ✓（先 `forceload` 区块） |
| `place template hongmeng:lingmai_dongfu` | **Loaded template** ✓ |
| 六种灵植方块 `setblock ... [age=N]` | 6/6 **Changed the block** ✓ |
| 丹炉 / 灵田土 / 道宫台 `setblock` | 3/3 ✓ |
| 六种实体召唤（血魔修士/石傀/雷鹰/灵驹/雷鹰坐骑/蛟龙） | 6/6 **Summoned**（还带品阶与境界前缀）✓ |
| 日志扫描（`Couldn't load structure` / `Failed to parse` / `Missing texture` / `Feature order cycle` …） | **0 命中** |

> **这只证明"数据合法、服务端认得"**。模型长什么样、界面手感、粒子和雷柱的效果，
> 服务端一概不加载 —— 仍然是只能你来跑的那部分。

---

## 五、维度探索：第一批已落地（B1 + B2 样板）

### B1 地形差异化（四个副本维度不再一模一样）

| 维度 | `settings` | 效果 |
|---|---|---|
| 纪元秘境 | `minecraft:overworld` | 不变 |
| 妖兽秘境 | **`minecraft:large_biomes`** | 群系补丁更大 → 辽阔荒原/密林 |
| 魔道巢穴 | **`minecraft:caves`** | 整层洞窟地形 → 天然"巢穴" |
| 虚空裂隙 | `minecraft:overworld` | 刻意不变（浮空岛会让 `DungeonArena` 的探测环落进虚空） |

### B2 十二个自建群系（**四个维度各 3 个，服务端已验证**）

已产出并部署：**18 个 `placed_feature`**（6 灵植 × 3 档密度）+ **16 个灵植 `biome_modifier`**
+ **4 个矿石 `biome_modifier`**，按维度危险度与灵植阶数分配密度（低阶灵植在前段维度密、
高阶在后段维度密），矿石按维度给（纪元=灵铁+灵晶、妖兽=灵铁+空冥、魔道=血珞+灵晶、虚空=空冥+灵晶）。
自检脚本会核对：引用的 `configured_feature` / `placed_feature` 全部真实存在、
`biomes` 只允许出现 12 个维度群系、每个群系至少 6 株灵植 + 1 种矿石 → **问题合计 0**。

⚠️ **诚实说明**：我用来验证"世界里真的长出来了"的探针**是坏的**——
`execute in <dim> if block ... run say HIT` 在无玩家在线时**不产生任何输出**，
所以探针报的"命中 0"是**假阴性，不能作为证据**。已经做到的是：
服务端带这批 modifier **正常启动、无异常无崩溃**（说明数据本身合法）。
**"灵植/矿石真的在维度里生成"这一条，下一轮要用可靠探针重验**
（改用 `execute store result score ...` 或 `fill` 计数，不要用 `say`）。

**你可以先自己看一眼（最快）**：进纪元秘境走一圈，看地上有没有雪莲草/火焰花。


| 维度 | 群系（中文） | `scale` |
|---|---|---|
| 纪元秘境 | 纪元草原 / 灵稻田畴 / 古坛废墟 | 40 |
| 妖兽秘境 | 兽骨荒原 / 灵兽密林 / 血食泥沼 | 32 |
| 魔道巢穴 | 焦土魔原 / 血珞林 / 黑石魔窟 | 28 |
| 虚空裂隙 | 虚空浮岛 / 星尘荒原 / 归墟之眼 | 36 |

**服务端实测（12/12 全部能定位，这就是"真的生成了"的证据）**：

```
execute in hongmeng:epoch_realm  positioned 0 100 0 run locate biome hongmeng:epoch_meadow
  → The nearest hongmeng:epoch_meadow is at [0, 100, 0] (0 blocks away)
execute in hongmeng:beast_realm  positioned 0 100 0 run locate biome hongmeng:beast_grove
  → …at [32, 100, 32] (45 blocks away)
execute in hongmeng:demon_realm  positioned 0 100 0 run locate biome hongmeng:demon_cavern
  → …at [-32, 100, 32] (45 blocks away)
execute in hongmeng:void_realm   positioned 0 100 0 run locate biome hongmeng:void_stardust
  → …at [-32, 100, -32] (45 blocks away)
（其余 8 个同样全部命中）
```
服务端启动**无异常无崩溃**，日志扫描 0 命中。

**怎么验（你）**：
1. 四个副本各进去走一圈（或 `/execute in hongmeng:epoch_realm run tp @s 0 100 0`）；
2. 看**三块群系补丁的边界会不会太生硬**（每块 28~40 格见方，像三色棋盘就是太生硬）；
3. 看配色氛围对不对（每个群系都给了独立的天空/雾/水色/草色）。

**要告诉我的**：哪一块的**边界**或**配色**不对、往哪个方向调。

⚠️ **这两批只做了"地形 + 群系"，里面还是空的**：新群系里暂时**没有灵植、没有矿、没有建筑** ——
那是 B3（采集资源挂载）与 B4（12 座地标建筑）的内容，等你确认群系观感没问题我再往上铺。

### 这一批踩到、并已写进自检的坑

**群系 `features` 的每一层都只接受 placed_feature 的引用，写方块 id 会让整个数据包加载失败。**
我第一版把 `minecraft:grass_block` 填进了 step 5（以为那是"地表材质"层），服务端直接拒绝启动：

```
Registry loading errors:
  Unbound values in registry ... worldgen/placed_feature: [minecraft:coarse_dirt, minecraft:grass_block]
  Failed to load datapacks, can't proceed with server load.
```

正解：地表材质由地形的 `surface_rule` 决定，**不是群系能指定的**（原版 plains 的 step 5 本来就是空数组）。
另外还有两条同类问题被自检抓出来：`minecraft:patch_glow_lichen` / `minecraft:ore_diamond_small` / `minecraft:patch_sugar_cane_swamp` 这几个 id **原版并不存在**（我凭印象写的），
现已全部换成原版真实存在的 id —— 现在自检会拿 `client-extra.jar` 里的 placed_feature 清单**逐条核对全部 619 处引用**。


---

### B3 采集资源（已挂载，**"世界里真的长出来"尚未证实**）

已产出并部署：**18 个 `placed_feature`**（6 灵植 × 3 档密度）+ **16 个灵植 `biome_modifier`** + **4 个矿石 `biome_modifier`**。
密度按"维度危险度 + 灵植阶数"配（低阶灵植在前段维度密、高阶在后段维度密）；
矿石按维度给（纪元=灵铁+灵晶、妖兽=灵铁+空冥、魔道=血珞+灵晶、虚空=空冥+灵晶）。
自检会核对：引用的 `configured_feature`/`placed_feature` 全部真实存在、`biomes` 只允许出现
12 个维度群系、每个群系至少 6 株灵植 + 1 种矿石 → **问题合计 0**。

⚠️ **诚实说明（本轮的方法论失误）**：我用来验证"世界里真的长出来了"的探针**是坏的** ——
`execute in <dim> if block ... run say HIT` 在**无玩家在线时不产生任何输出**，
所以探针报的"命中 0"是**假阴性，不能当证据**。目前能确认的只有：
服务端带这批 modifier **正常启动、无异常无崩溃**（说明数据合法）。
**"灵植/矿石真的在维度里生成"要下一轮用可靠探针重验**（改用 `execute store result` 计数，不用 `say`）。

**你可以先自己看一眼（最快）**：进纪元秘境走一圈，看地上有没有雪莲草/火焰花。

---

## 六、本轮已知的、不用报的问题

- **老世界的旧区块不会有新群系**：三个新群系要新世界才生成。
- **创造模式挖方块不掉落**（有意为之）。
- **法宝只有装在法宝栏里才生效**，放背包里不算。
- **腐肉/灵兽不能繁殖**、**NPC 对话是硬编码中文**：都是已知限制。
