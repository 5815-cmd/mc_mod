# 第六轮 · Boss 模型重制契约（更帅 / 更有压迫感）

> 2026-11-07 ｜ 用户要求：「BOSS 的模型不够帅，不够有压迫感，参考冰与火之歌的龙类 Boss 和灾变的 Boss」。
> 目标：把现有效果最弱的 5 只 Boss 重做成高细节、强剪影的巨物，并把「恶·三阶段上半身」「仙庭之主」强化；全部满足零模型错误门禁。

---

## 0. 现状（实读渲染，见 `_cache/boss_sheet/00_全Boss总览.png`）

| Boss | 模型 | cube | 高 | 问题 |
|---|---|---|---|---|
| 傀儡祖师 | PuppetMasterModel | 170 | 2.4 格 | 尚可（本轮不动） |
| 万兽之主 | BeastLordModel | 29 | 1.6 格 | 像趴着的棕色箱子，无兽威 |
| 血魔老祖 | BloodDemonModel | 32 | 3.3 格 | 像红色乐高人，手臂小、无头饰 |
| 虚空道主 | VoidLordModel | 29 | 3.4 格 | 尚可但翼/独眼弱 |
| 焚天炎帝 | BlazeEmperorModel | 34 | 3.7 格 | 矮胖方块人，无熔岩巨神感 |
| 虚空龙帝 | VoidDragonEmperorModel | 28 | 3.1 格 | **看起来像紫色小飞机**，完全不是龙 |
| 恶（三阶段上身） | EvilUpperModel | 72 | 10.8 格 | 像紫色方块墙，缺血肉/骨架剪影 |
| 仙庭之主 | HeavenLordModel | 67 | 6.9 格 | 又细又高像塔，肩太窄无帝王威 |

**参考要点（本轮设计语言）**
- **《冰与火之歌》龙**（Drogon 等）：长而 S 形的颈、箭头状带后掠角的头骨、上下颚利齿、**巨大的蝙蝠状膜翼**（肩-肱-前臂-4 根长指+膜）、数字行走的后腿+爪、**长尾+尾鳍骨刺**、鳞甲深色+翼膜半透+眼与喉部发光；远看先读"龙"，再看细节。
- **灾变（L_Ender's Cataclysm）Boss**：极大体量、**层叠装甲板**、发光裂缝/核心/眼睛、夸张肩甲与巨臂、不规则骨刺/触须、悬浮碎片、慢而重的待机动作；剪影第一、细节第二；每只 Boss 有一个"必须一眼记住"的符号（龙=翼+颈、炎帝=熔炉胸核、万兽=骨板驼峰、血魔=三头六臂、虚空=六片浮翼独眼）。

**统一升级手法**：剪影优先（巨翼/巨肩/巨角拉开外轮廓）、层次甲片（2~3 层叠加、不对称）、发光弱点（眼/胸核/裂缝，配 `_glow.png` + RankGlowLayer）、负空间（翼指间的膜、肋骨、悬空碎片）、动画（呼吸/摆翼/转头/尾摆，绝对赋值）。

---

## 1. 通用工程约束（所有模型 agent 必守）

- 工具链：`工程/角色设计/tools/studio.py` + `build_entity.py`（唯一真源：Spec → UV 装箱 → Java + 贴图 + 发光层 + 审计）。每个 Boss 在 `工程/角色设计/round6/<boss>/spec.py` 写 Spec，跑：
  `python -X utf8 工程/角色设计/tools/build_entity.py 工程/角色设计/round6/<boss>/spec.py`
- **写死路径**：spec 的 `TARGET["java"]` 必须指向现有模型文件（整文件重写，类名不变）；`TARGET["texture"]` 指向现有贴图路径（保持文件名/新尺寸）；`TARGET["glow"]` 指向同目录 `<tex>_glow.png`（新文件）；`outdir` = `工程/角色设计/round6/<boss>`。
- **类 API 必须保持**：`public class <XxxModel> extends EntityModel<CultivationMonsterEntity>`、`public static LayerDefinition createBodyLayer()`、构造器 `(<XxxModel>ModelPart root)`。生成后**手动补一行**（studio 不产出）：
  `public static final double LOWEST_PIXEL_Y = 24.0;`（脚底在 y=24 → renderer GROUND_LIFT=0；若模型最低点不是 24 必须写实际值并在报告中说明）。
- **坐标约定**：+y 向下、-z 前、脚底/最低点 y=24；1 格=16px。
- **硬门禁**：`build_entity.py` 打印 0 problems；UV 0 越界/0 跨足迹重叠/贴图 100% 不透明（真洞 0）/脚底 0/挂点 0；`_glow.png` 尺寸与主贴图一致。
- **Renderer**：若原 renderer 没有 `RankGlowLayer`（beast/demon/void/blaze/dragon 都没有），在 renderer 的 `createBodyLayer` 附近 `addLayer(new RankGlowLayer<>(this, <glow 路径>))`（照抄 `PuppetMasterRenderer` 的写法）；不得改 LAYER id / ClientSetup / 实体 / hitbox / 审计工具。
- **体量变大是目的**：可以超出旧 hitbox（audit 只记警告）；但模型必须站得住（脚 y=24）、锚点/renderer 兼容，不能浮空/穿地。
- 禁止跑 gradle/javac；Lead 统一编译构建。

---

## 2. 各 Boss 设计任务书

### A. 虚空龙帝 `void_dragon_emperor`（旗舰，整条龙重做）
文件：`client/VoidDragonEmperorModel.java`、`textures/entity/void_dragon_emperor.png`（新 256×256）+ `_glow.png`。
- **目标尺寸**：站高 **7.0 格（112px）**、翼展 **14~16 格（224~256px）**、鼻尖到尾尖 **12 格**；cube **≥90**。
- **结构**：躯干（胸/腹/背脊）→ 长 S 颈 3~4 节（每节 12~18px）→ 头骨（38×30×50：吻部、眉骨、上下颚+12 牙、双眼发光、2 根后掠大角各 3 节 + 4 根颊刺）→ **双巨翼**（肩甲/肱/前臂/各 4 根指骨分节 + 指间膜=1px 薄板 4~6 片，半透或深色；翼展铺开、左右轻微不对称）→ 2 后腿（大腿/小腿/足+3 爪，数字行走站姿）→ 2 小前爪 → 长尾 4~5 节逐渐收细 + 6~8 根背鳍刺 + 尾部虚空晶刃。
- **配色**：虚空紫黑鳞（暗紫/黑/冷灰三阶）+ 骨白角/牙 + 青色发光裂纹（脊椎/翼骨/眼/喉）+ 翼膜暗红半透。
- **动画**：慢速翼展呼吸（xRot/zRot 小幅度）、颈部 S 摆、头部随目标转（`netHeadYaw * 0.35`）、尾部左右摆、待机低吼（身体上下 0.5~1px 呼吸）。
- **叙事符号**：远看=张开的巨翼 + 长颈俯视，近看=裂纹/骨角。

### B. 焚天炎帝 `blaze_emperor`（灾变风熔岩泰坦）
文件：`client/BlazeEmperorModel.java`、`textures/entity/blaze_emperor.png`（新 256×256）+ `_glow.png`。
- **目标尺寸**：站高 **6.0 格（96px）**、肩宽 **4.5 格（72px）**、cube **≥80**。
- **结构**：粗腿+蹄足（脚 y=24）→ 腰甲 → **巨胸（熔炉）**：胸口 28×28 发光熔炉格栅 + 两侧散热口 → **超大层叠肩甲（2~3 层+骨刺）** → 双臂各 3 节（上臂/前臂/巨拳，拳=24×24×28 + 4 指）→ 头（牛角冠/火焰鬃毛/发光眼/獠牙面甲）→ 背部排气管/熔岩喷口 + 背部尖刺 → 腰链/挂炉。
- **配色**：黑曜石黑/暗红/熔岩橙三阶 + 金饰边 + 发光熔岩裂缝（胸核/眼/关节/背管）。
- **动画**：胸口熔炉呼吸（xScale/zScale 脉动）、双臂展开-握拳、头部缓慢环视、肩甲轻微上下。

### C. 万兽之主 `beast_boss`（灾变风骨甲巨兽）
文件：`client/BeastLordModel.java`、`textures/entity/boss_beast_lord.png`（新 256×256，旧 192×128）+ `_glow.png`。
- **目标尺寸**：四足站立高 **4.2 格（67px）**、鼻到尾长 **9 格（144px）**、肩宽 **3.2 格**、cube **≥70**。
- **结构**：低伏长躯干 → **驼峰骨板 3 排（每排 2~3 块大骨板+棘刺）** → 兽首（头骨+巨角 2 支+獠牙 8 颗+发光眼）→ 上下颚可开合（下颌部件）→ 前肢（肩甲+爪 3 指）→ 后肢（大腿/小腿/爪）→ 长尾 4 节+尾刺/尾锤 → 肋侧外露骨架 + 胸口发光核心。
- **配色**：骨白/土褐/暗红三阶 + 发绿金光的眼/肋骨/核心。
- **动画**：呼吸起伏、头部低伏再抬、尾巴横扫、下颚开合（`jaw.xRot`）。

### D. 血魔老祖 `demon_boss`（三头六臂血魔）
文件：`client/BloodDemonModel.java`、`textures/entity/boss_demon_elder.png`（新 256×256，旧 128×128）+ `_glow.png`。
- **目标尺寸**：站高 **6.0 格（96px）**、肩宽 **4 格**、cube **≥85**。
- **结构**：**三颗头**（中央戴冠主头 + 左右两小头，各有角/眼/獠牙，中央头最大）→ 宽肩胸甲 + 外露肋骨/血核 → **六条手臂**（左右各 3：上臂/前臂/爪或血刃，姿态错落）→ 下半身血袍/裙甲（3~4 层下摆，脚 y=24 藏于袍下）→ 背部骨刺脊/蝠翼残片 + 血雾环（薄板）。 
- **配色**：暗血红/黑/骨白 + 发光血眼/胸口血核/刃口。
- **动画**：三头各自微转（错相）、六臂缓慢张合、袍摆/血雾浮动。

### E. 虚空道主 `void_boss`（浮空独眼虚无法相）
文件：`client/VoidLordModel.java`、`textures/entity/boss_void_lord.png`（新 256×256）+ `_glow.png`。
- **目标尺寸**：浮空高 **6.5 格（104px）**、翼展 **9 格**、cube **≥85**。
- **结构**：无腿（下摆收束到 y=24 的虚空触须/布幔）→ 长袍/法相躯干（层叠布幔 3 层）→ **独眼头**（巨眼+眼睑+瞳孔+额头冠/裂纹）→ **6 片悬浮翼片（每侧 3，分节、离体 1~3px 的悬浮感）** → 4 条手臂（2 长 2 短，持虚空晶刃）→ 环绕的悬浮符文石 3~5 块 + 背后虚空环。
- **配色**：深紫/暗蓝黑 + 冷白/青色发光眼/符文/裂纹 + 金边。
- **动画**：悬浮上下 ±2px、翼片错相浮动、独眼瞳孔移动、法袍摆动。

### F. 恶（三阶段上半身）+ 仙庭之主 强化
1. **EvilUpperModel**：改 `tools/gen_evil_phases.py` 的 `UPPER` 部件表后重跑（会重生成三套模型+1024²图集，phase1/2 保持原样）。目标：**挤满天空的巨神上半身**——胸宽 100px、巨肩甲+骨刺、双巨臂各 3 节+5 爪、巨颅（主眼+嘴/牙+8 小眼+双角）、外露肋骨 8 根、全身 ≥20 眼、背部裂口触须/骨刺；cube **≥120**、高度 15~17 格（仍 ≤+15% 上限则按 250px 上限调整，最多 265px）。保留 1024² 共享图集 + 现有 `evil_phases.png` 路径。
2. **仙庭之主**：`client/HeavenLordModel.java`（studio 重写，generic=`HeavenLordEntity`、entity_import）、`textures/entity/heaven_lord.png`（256×256）+ `_glow.png`。目标：**帝王级宽度**——身高 6.9 格不变、肩宽/袍摆从 2.8 格拉到 **6+ 格**；加宽肩甲+垂袖+披风/袍摆 3 层、冕旒冠+背后日轮/光轮、悬空玉圭/宝珠；cube ≥100；保留 Renderer 需要的 `LOWEST_PIXEL_Y` 与 RankGlowLayer（已存在）。⚠ 该模型 generic 是 `HeavenLordEntity`，spec 需写 `entity_import="com.hongmeng.xiuxian.entity.HeavenLordEntity"` 且 `generic="HeavenLordEntity"`。

---

## 3. 验收门禁（Lead 统一跑）

1. 每只：`build_entity.py` 0 problems、UV 0/0、真洞 0、脚底 0、挂点 0、贴图/发光尺寸一致；renderer 能编译（类 API/LOWEST/glow 层齐全）。
2. `audit_all.py --no-render --tag "13-Boss重制"`：全量 **55 实体硬问题 0**（允许 hitbox 高度警告增加，硬问题必须 0）。
3. 证据：每只 before/after 三视图+剪影 64px+hero+新旧对比 + `round6/00-Boss重制总览.png`；**剪影必须一眼可辨**（与其它 Boss 64px IoU 尽量 <0.70）。
4. Lead：javac 0 error → build_deploy.ps1 构建部署（三段哈希一致）。
5. 实机（用户）：重生 Boss / 打旧 Boss 看体型、翼展、发光。

## 4. 分工与写域（互不重叠）

| agent | Boss | 写域 |
|---|---|---|
| boss-puppet | A 虚空龙帝 | `VoidDragonEmperorModel.java`、`void_dragon_emperor.png/_glow.png`、`VoidDragonEmperorRenderer.java`、`round6/dragon/**` |
| elite-flameape | B 焚天炎帝 | `BlazeEmperorModel.java`、`blaze_emperor.png/_glow.png`、`BlazeEmperorRenderer.java`、`round6/blaze/**` |
| npc-merchant | C 万兽之主 | `BeastLordModel.java`、`boss_beast_lord.png/_glow.png`、`BeastLordRenderer.java`、`round6/beast/**` |
| texture-repair | D 血魔老祖 | `BloodDemonModel.java`、`boss_demon_elder.png/_glow.png`、`BloodDemonRenderer.java`、`round6/demon/**` |
| boss-uv-a | E 虚空道主 | `VoidLordModel.java`、`boss_void_lord.png/_glow.png`、`VoidLordRenderer.java`、`round6/void/**` |
| boss-uv-b | F 恶上身 + 仙庭之主 | `tools/gen_evil_phases.py`、`EvilUpperModel.java`（重生成）、`evil_phases.png`、`HeavenLordModel.java`、`heaven_lord.png/_glow.png`、`round6/evil-heaven/**` |
| npc-id-a | 门禁/证据 | `tools/check_boss_remake.py`、`round6/evidence/**`、`round6/00-Boss重制总览.png`、`audit_new_mobs.py`（如需要） |
| design-batch | 文档 | `工程/22-Boss模型重制交付.md`、`工程/14/15/README` 追索引、`工程/23-龙与灾变Boss设计参考.md` |

**不要改**：实体/ModEntities/ClientSetup/任何 task 外的 Java；`_glow` 层加在各自 renderer；模型生成前先看 `工程/角色设计/01-傀儡祖师/spec.py` 与 `round3` 下的 spec 例子。
