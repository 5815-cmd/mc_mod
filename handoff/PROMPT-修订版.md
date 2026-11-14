# 鸿蒙修仙 · 8 BOSS GeckoLib 重制提示词（修订版）

> 本文件是可直接发给 3D 模型制作 AI 的完整提示词。  
> 原始提示词的视觉拆解保留；**渲染接入、动画触发、发光命名、禁改规则已按当前项目实际代码修正**。  
> 推荐先读 `handoff/README-交接说明.md`，再使用本提示词。

---

## 【角色设定】

你现在是顶级的 Minecraft 1.20.1 Forge 模组开发者、GeckoLib 4.8.4 专家兼 3D 建模师。你要独立完成“鸿蒙修仙”Mod 中 8 个阶梯 BOSS 的模型重制、动画绑定与代码接入。

你必须先通读交接包中的：
- `handoff/README-交接说明.md`
- `handoff/MODEL-RENDERER-INDEX.md`
- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/entity/`（实体）
- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/client/`（Model/Renderer）
- `handoff/docs/21-Boss模型重制契约.md`
- `handoff/docs/22-Boss模型重制交付.md`

---

## 【项目背景】

Mod 源码路径：

```text
xiuxian-mod
```

当前 8 个阶梯 BOSS：

| BOSS | id | 当前渲染类 |
|---|---|---|
| 傀儡祖师 | `epoch_boss` | `PuppetMasterRenderer` |
| 万兽之主 | `beast_boss` | `BeastLordRenderer` |
| 血魔老祖 | `demon_boss` | `BloodDemonRenderer` |
| 焚天炎帝 | `blaze_emperor` | `BlazeEmperorRenderer` |
| 虚空龙帝 | `void_dragon_emperor` | `VoidDragonEmperorRenderer` |
| 虚空道主 | `void_boss` | `VoidLordRenderer` |
| 仙庭之主 | `heaven_lord` | `HeavenLordRenderer` |
| 恶 | `evil_gazer` | `EvilGazerRenderer`（三套模型切换） |

现状：BOSS 模型主要是原版 `ModelPart` 拼搭（如 `VoidDragonEmperorModel`），视觉目标是《冰与火之歌》龙类 / 灾变 Boss 的压迫感。

---

## 【铁律：不可违背】

1. **绝对不许修改：**
   - `entity/BossAttackKit.java`
   - `difficulty/BossLadder.java`

   所有 BOSS 的血量、阶段划分、攻击池逻辑必须原封不动。

2. **渲染器注册不在 `ModEntities.java`。**  
   注册在 `ClientSetup.java` 的 `EntityRenderersEvent.RegisterRenderers`。  
   `ModEntities.java` 只允许读取；不要为了模型修改 EntityType、属性；判定箱仅在视觉尺寸超出时按 `handoff/BOSS-模型改造总表.md` 有条件同步这 8 只。

3. **当前 BOSS 实体没有实现 GeckoLib 动画接口。**  
   `GeoEntityRenderer<T extends Entity & GeoAnimatable>` 不能直接套在现有 `CultivationMonsterEntity` 上。  
   必须先做以下之一：
   - 新增 `GeoBossEntity extends CultivationMonsterEntity implements GeoEntity`，让 8 个 BOSS 继承它；
   - 或让 8 个 BOSS 各自 `implements GeoEntity`。

   必须实现：
   - `registerControllers(...)`
   - `getAnimatableInstanceCache()`
   - `getTick(...)`（或继承 `GeoEntity` 默认实现）

4. **不得修改交付包内的 `BossLadder.java` / `BossAttackKit.java`。**  
   交付前必须运行：
   ```powershell
   python verify_handoff.py
   ```
   两个文件 SHA256 必须与 `FORBIDDEN.sha256` 一致。若模型制作确实需要联动改动，停止并报告，不允许自动改写。

---

## 【模型改造权限：允许动骨骼、放大、特化】

本次任务**明确允许**：

- 新增/删除/重命名骨骼，改父子关系、pivot、骨骼层级；
- 改模型比例、视觉高度/宽度，把 BOSS 做得更大、更有压迫感；
- 重做剪影，强化专属特征（角、翼、尾、甲、胸核、日轮、操纵线等）；
- 改 `GeoModel` / `GeoRenderer` / 贴图 / UV / 动画；
- 新增 `GeoBossEntity`，让 8 只 BOSS 实现 `GeoEntity/GeoAnimatable`；
- 改 `ClientSetup` 中这 8 只 BOSS 的 Renderer / ModelLayer 注册；
- 在“视觉尺寸超出判定箱”时，**有条件**同步改这 8 只 BOSS 的 `sized()`。

**放大建议**：现有基础上 **+10%~40% 视觉尺寸**，以实机压迫感为准；  
具体每只的目标见 `handoff/BOSS-模型改造总表.md` 第二节。  
不要无限放大；放大后必须验证脚底、判定箱、镜头、阶段缩放、性能。

**仍然禁止**：

- 改 `BossLadder.java`、`BossAttackKit.java`；
- 改 BOSS 的 HP/ATK/品阶/境界/阶段阈值/攻击池；
- 改其他实体、其他装备/物品/方块/配方/维度/群系；
- 改 GeckoLib/Forge/Minecraft 版本或 `build.gradle` 依赖；
- 用 `_emissive`；只能 `_glowmask` 或 `_glow`；
- 不备份就删除旧 Renderer/Model；
- 未跑 `build` / `verify_handoff.py` 就交付。

> 完整权限矩阵、8 只 BOSS 放大目标、骨骼/特化重点、禁止清单：  
> 见 **`handoff/BOSS-模型改造总表.md`**；逐条中文解释见 **`handoff/禁止清单-中文解释.md`**。

---

## 【渲染迁移：必须先处理的现有系统】

### 1. `GeoEntityRenderer` 不会触发 `RenderLivingEvent`

`GeoEntityRenderer` 继承 `EntityRenderer`，不是 `LivingEntityRenderer`。因此：

- `BossPhaseRenderer` 的 `RenderLivingEvent.Pre/Post` 阶段缩放**不会自动生效**；
- `BossFormRenderer` 中依赖 `RenderLivingEvent` 的缩放/形态逻辑也**不会自动生效**。

必须：

- 在自定义 `GeoEntityRenderer.preRender` 里乘 `bossRenderScale × giantScale`；
- 保留 `BossFormRenderer` 中仍走 `RenderLevelStageEvent` 的冠/翼/场地层，但和 Geo 模型重复的装饰要报告后删除；
- 不要删除 `BossAuraRenderer`、`BossPresenceClient`、`MobHealthBarRenderer` 等独立系统。

### 2. `RankGlowLayer` 不能直接挂在 Geo 渲染器上

`RankGlowLayer` 继承原版 `RenderLayer`，需要 `RenderLayerParent`；`GeoEntityRenderer` 不是。必须二选一：

**方案 A：保留项目现有 `_glow.png` 命名**
- 写一个 `GeoRenderLayer` 复刻 `RankGlowLayer` 的品阶色发光逻辑；
- 贴图：`textures/entity/<id>_glow.png`；
- 保留品阶颜色（精英/头目/首领）。

**方案 B：改用 GeckoLib 自动发光**
- 使用 `AutoGlowingGeoLayer`；
- GeckoLib 4.8.4 读取的是 **`_glowmask.png`**，不是 `_emissive`；
- 贴图：`textures/entity/<id>_glowmask.png`；
- 会失去 `RankGlowLayer` 的品阶色 tint，需要自己补。

**禁止使用 `_emissive` 命名。**

### 3. 缩放只乘一次

现有规则：

```text
渲染最终倍率 = bossRenderScale × giantScale
命中箱倍率   = min(2.2, giantScale)
```

Geo 模型**不要烘焙** `bossRenderScale` / `giantScale`；  
阶段缩放只在自定义 `GeoEntityRenderer.preRender` 里乘一次；  
不要改 `ModEntities.sized()` 来“适配”新模型，除非单独报备。

---

## 【动画规范】

每个 BOSS 必须至少有：

### 1. `idle`
- 常态待机循环；
- 呼吸、摆尾、翼膜微动等按 BOSS 类型设计。

### 2. `attack_*`
`BossAttackKit` 同步到客户端的 `bossAttack()` 取值：

| ID | 招式 |
|---|---|
| 1 | `SLAM` |
| 2 | `SHOCKWAVE` |
| 3 | `SUMMON` |
| 4 | `SPELL` |
| 5 | `BREATH` |
| 6 | `AMBUSH` |
| 7 | `CATACLYSM` |
| 8 | `CHARGE` |
| 9 | `ENRAGE` |

- 至少为每只 BOSS 实际会用的招式各做一个 `attack_<id>`；
- 可保留 `attack_default` 兜底；
- 攻击必须有**高度动态变化**（躯干/髋部/胸部位移），不能只转手脚；
- root 骨骼不位移、不旋转，垂直动态放在 `body/hip/chest`。

### 3. `phase_transition`
- 读 `CultivationMonsterEntity.bossPhase()`；
- 在 `1→2`、`2→3` 时各触发一次；
- 66% / 33% 对应 `BossPhase` 的转阶段，演出要有仰天长啸/爆气；
- 恶使用自己的 `EvilGazerEntity` 阶段状态，不要套普通 `BossAttackKit` 动画表；
- 转阶段时 `BossPhase` 会定格 20 tick、回血、播放特效；动画不能与攻击前摇打架。

### 4. 动画控制器位置
- `AnimationController` 写在实体的 `registerControllers()` 中；
- `GeoModel` 只负责 `getModelResource / getTextureResource / getAnimationResource`；
- 不要在 Renderer 里“绑定动画”。

---

## 【8 个 BOSS 的视觉拆解与骨骼树】

> 骨骼树保留设计意图；实际命名可微调，但必须与 `.geo.json`、`.animation.json` 完全一致。

### 1. 傀儡祖师 `epoch_boss`
木偶皮影风，菱形切角木块，背后有操纵线。

```text
root -> torso -> head_crown, arm_l/r, puppet_string_1~4
```

### 2. 万兽之主 `beast_boss`
白骨巨兽，外露胸腔肋骨，四条粗壮兽腿。

```text
root -> body, spine_plate_1~3, head_horns,
        leg_front/back_l/r, tail_1~4
```

### 3. 血魔老祖 `demon_boss`
三头六臂血佛，胸前血核，4 层破烂血袍。

```text
root -> head_center/l/r, arm_1~6, blood_core, robe_layer_1~4
```

### 4. 虚空道主 `void_boss`
独眼法相，悬浮，6 片黑紫晶翼，4 臂虚空方刃。

```text
root -> eye_main (可缩放), wing_1~6, arm_1~4, void_ring
```

### 5. 焚天炎帝 `blaze_emperor`
熔岩泰坦，宽阔肩膀，胸口巨大发光熔炉，牛角冠。

```text
root -> chest_furnace (发光), shoulder_l/r, fist_l/r,
        horn_crown, flame_mane_1~3
```

### 6. 虚空龙帝 `void_dragon_emperor`
冰火式虚空巨龙，S 型长颈，双膜翼，8 节带晶刃尾巴。

```text
root -> neck_1~5, head_jaw, wing_bone_l/r,
        wing_membrane_l/r, tail_1~8, tail_blade
```

### 7. 仙庭之主 `heaven_lord`
天帝法相，宽袍，背后金色日轮，头戴冕旒。

```text
root -> sun_disc, crown_mianliu, cloak_back, arm_hold_jade
```

### 8. 恶 `evil_gazer`
三阶段不可名状巨神，必须建立三套独立 GeckoLib 模型/骨架：

- `EvilArmModel` — 一阶段：天空裂口垂下的巨臂；
- `EvilHeadModel` — 二阶段：巨颅 + 再一条臂；
- `EvilUpperModel` — 三阶段：挤满天空的整个上半身。

```text
root -> eye_1~N, tentacle_1~N, hand_massive
```

> 恶的现有 `EvilGazerRenderer` 是按阶段切换三套模型的；  
> 换成 Geo 时不能当普通单模型，必须写自定义 `GeoEntityRenderer` 切换逻辑，或单模型内做三阶段显隐。  
> 恶最后做。

---

## 【文件输出位置】

```text
xiuxian-mod/src/main/resources/assets/hongmeng/
├─ geo/<boss_id>.geo.json
├─ animations/<boss_id>.animation.json
└─ textures/entity/<boss_id>.png
   + <boss_id>_glowmask.png（若用 AutoGlowing）
   或 <boss_id>_glow.png（若保留 RankGlowLayer 风格）
```

Java：

- `client/<Boss>GeoModel.java`
- `client/<Boss>GeoRenderer.java`（或统一 `GeoBossRenderer`）
- `entity/GeoBossEntity.java`（推荐）
- `ClientSetup.java` 注册新 Renderer
- 旧 Renderer/Model 保留备份或标记 deprecated，不要直接删除到无法回滚

---

## 【交付标准】

1. 直接生成 `.geo.json`、`.png`、`.animation.json`，写入上述目录；
2. 编写/修改 Model、Renderer、`GeoBossEntity`；
3. 修改 `ClientSetup.java` 的 Renderer 注册；
4. 保留 `BossPhaseRenderer` 的阶段缩放语义（迁到 Geo 渲染器）；
5. 运行：
   ```powershell
   .\gradlew.bat build --offline -x test
   ```
6. 运行：
   ```powershell
   python verify_handoff.py
   ```
   必须 PASS；
7. 交付：
   - changed-files 列表
   - patch/diff
   - 正面/侧面/背面/游戏内截图
   - build 输出
   - `verify_handoff.py` 输出
   - `NOTES.md`（说明旧层迁移、判定箱、未解决项）

---

## 【执行顺序：不要一次做完 8 只】

1. **`epoch_boss` 试点**：验证 GeoEntityRenderer + GeoBossEntity + 阶段缩放 + 判定箱 + 动画触发。
2. **`void_dragon_emperor`**：验证大模型、翼展、长尾、多人视角。
3. **`beast_boss` + `demon_boss`**：验证多腿、三头六臂。
4. **`void_boss` + `blaze_emperor` + `heaven_lord`**：验证浮空、熔炉、日轮。
5. **`evil_gazer` 最后**：三套骨架切换。

每只完成后必须：
- 编译 PASS；
- `verify_handoff.py` PASS；
- 实机截图；
- 阶段缩放/攻击动画/转阶段验证；
- 未修改 `BossLadder.java`、`BossAttackKit.java`。

---

## 【停止条件】

出现以下任一情况，立即停止并报告，不允许硬改核心：

- 需要修改 `BossLadder.java` 或 `BossAttackKit.java` 才能编译；
- `GeckoLib` API 与 4.8.4 不匹配；
- 模型缩放/判定箱无法对齐，需要改 `ModEntities.sized()`；
- `BossPhaseRenderer` / `BossFormRenderer` 旧逻辑与 Geo 渲染冲突；
- 模型贴图 UV 无法在现有尺寸内完整排布。

---

## 【最终回复格式】

完成一只后回复：

```text
BOSS：<中文名> (<id>)
状态：PASS / FAIL
改动文件：
- ...
新增资源：
- ...
编译：PASS
verify_handoff：PASS
实机截图：<路径>
未解决项：
- ...
```

完成 8 只后，再给一份总交付说明。
