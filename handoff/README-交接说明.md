# 鸿蒙修仙 · 3D 模型制作 AI 交接包

> 版本：2026-11-11  
> 适用：Forge 1.20.1 / Java 17 / Gradle 8.8 / GeckoLib 4.8.4  
> 交接目标：让接手方能在**不改坏战斗核心**的前提下，继续制作、重制、打磨 3D 模型与渲染表现。  
> **直接发给模型 AI 的任务提示词见：`PROMPT-修订版.md`**

---

## 0. 交接方给的硬性要求（原文三条）

1. **完整的源码目录**（特别是 `com.hongmeng.xiuxian.entity` 和 `client` 下的 Model/Renderer 类）。
2. **GeckoLib 依赖**（确保 `build.gradle` 里有 `geckolib-forge-1.20.1-4.8.4`）。
3. **核心规则：绝对不能动 `BossLadder.java` 和 `BossAttackKit.java`。**

本包已按这三条准备：

- `xiuxian-mod/src/`：完整源码与资源（含 `entity/` 全量、`client/` 全量 Model/Renderer）；
- `xiuxian-mod/libs/geckolib-forge-1.20.1-4.8.4.jar`：本地 GeckoLib 依赖；
- `xiuxian-mod/build.gradle` 第 137 行：
  ```gradle
  implementation fg.deobf(files("libs/geckolib-forge-1.20.1-4.8.4.jar"))
  ```
- `BossLadder.java`、`BossAttackKit.java`：已设为只读，并在 `FORBIDDEN.sha256` 记录哈希；
- `verify_handoff.py`：一键检查禁改文件、GeckoLib、目录完整性。

---

## 1. 包内结构

```text
鸿蒙修仙-3D模型交接包-2026-11-11/
├─ README-交接说明.md              ← 本文件
├─ PROMPT-修订版.md                ← 可直接发给模型 AI 的修订版任务提示词
├─ BOSS-模型改造总表.md            ← 骨骼/放大/特化权限 + 8 BOSS 目标 + 禁止清单
├─ 禁止清单-中文解释.md            ← 单项禁止的详细中文解释
├─ verify_handoff.py              ← 交接前/交付前自动检查
├─ FORBIDDEN.sha256               ← 两个禁改文件的 SHA256
├─ MODEL-RENDERER-INDEX.md        ← 当前模型/渲染器/实体索引
├─ xiuxian-mod/
│  ├─ build.gradle                 ← 已含 GeckoLib 本地依赖
│  ├─ settings.gradle
│  ├─ gradle.properties
│  ├─ gradlew / gradlew.bat / gradle/
│  ├─ libs/geckolib-forge-1.20.1-4.8.4.jar
│  └─ src/
│     ├─ main/java/com/hongmeng/xiuxian/
│     │  ├─ entity/                ← 全部实体注册/属性/战斗/怪物类
│     │  ├─ client/                ← 全部 Model / Renderer / 特效类
│     │  ├─ difficulty/BossLadder.java       ← ⛔ 只读
│     │  └─ entity/BossAttackKit.java        ← ⛔ 只读
│     └─ main/resources/assets/hongmeng/
│        ├─ geo/                   ← 151 个 geo 模型
│        ├─ textures/entity/       ← 106 个实体贴图
│        ├─ animations/
│        └─ models/
├─ docs/                           ← 设计合同/交付说明/模型参考
│  ├─ 21-Boss模型重制契约.md
│  ├─ 22-Boss模型重制交付.md
│  ├─ 23-龙与灾变Boss设计参考.md
│  ├─ 19-Boss强度阶梯表.md
│  └─ 06-武器盔甲总表-职业版.md
├─ references/round6/              ← 第六轮 Boss 模型规格、预览、证据图
└─ reference-tools/                ← 模型审计/预览/生成工具
   ├─ model_parse_lib.py
   ├─ check_boss_remake.py
   ├─ audit_entity_models.py
   ├─ check_model_ground.py
   ├─ audit_all.py
   └─ art-tools/                   ← 工程/art/tools 原样参考
```

> `references/round6` 是**参考素材**，不是运行时资源；不要直接塞进 `src/main/resources`。

---

## 2. 绝对禁改区

### 2.1 两个文件

| 文件 | 原因 | 处理 |
|---|---|---|
| `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/difficulty/BossLadder.java` | BOSS 强度阶梯的**唯一真源**；改它会让 8 只 Boss 的 HP/ATK/阶位全部漂移 | 只读，禁改、禁删、禁重命名 |
| `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/entity/BossAttackKit.java` | BOSS 攻击池、阶段攻击状态机、召唤表、客户端同步字段 | 只读，禁改、禁删、禁重命名 |

两文件的 SHA256 见 `FORBIDDEN.sha256`。交付前必须运行：

```powershell
python verify_handoff.py
```

只要其中一个哈希变了，脚本会 **FAIL**。如果模型制作确实需要联动改动，**不要直接改**：在交付说明里提“需要改哪个方法、为什么”，由 Lead 决定。

### 2.2 建议不要动的共享接线

以下文件不是“绝对禁改”，但会牵一发动全身；模型 AI 不要顺手改：

- `entity/ModEntities*.java`：实体注册、`sized()` 判定箱、属性入口；
- `ClientSetup.java`：ModelLayer / Renderer 注册；
- `entity/CultivationMonsterEntity.java`：境界/品阶/阶段/伤害软上限；
- `difficulty/BossScaling.java`、`MobScaling.java`：数值缩放；
- `entity/BossPhase.java`：三阶段规则；
- `data/.../dimension*`、`worldgen`：维度/群系数据。

如果模型尺寸与判定箱不匹配，**先提交尺寸说明**，不要直接改 `sized()`。

---

## 3. 建议允许的修改范围（3D 模型 AI）

### 3.1 可直接改

- `client/**Model.java`：模型几何、部件层级、动画骨骼；
- **骨骼树：新增/删除/重命名骨骼、改父子关系/pivot/比例/剪影**；
- **BOSS 视觉尺寸：允许在现有基础上放大，做特化特征（角/翼/尾/甲/胸核/日轮/操纵线等）**；
- `client/**Renderer.java`：渲染层、贴图、发光层、缩放/挂点；
- `src/main/resources/assets/hongmeng/geo/`：GeckoLib `.geo.json`；
- `src/main/resources/assets/hongmeng/textures/entity/`：实体主贴图 / `_glow.png`；
- `src/main/resources/assets/hongmeng/animations/`：动画 JSON；
- `src/main/resources/assets/hongmeng/models/`：物品/方块模型（如果本次涉及）。

### 3.2 需先报备

- 新增 `ModelLayerLocation` 或改注册名 → 必须同步 `ClientSetup.java`；
- 改实体 `sized()` / 判定箱 → **仅当视觉尺寸超出时**，按 `BOSS-模型改造总表.md` 有条件同步这 8 只；不得改其他实体；
- 改 `RenderType`、阴影、发光层顺序 → 注意第一人称/第三人称和光影兼容；
- 改贴图尺寸 → 必须保持 UV 全量重排，不能只改 PNG 尺寸。

### 3.3 禁止

完整中文禁止清单见 **`BOSS-模型改造总表.md` 第一、三、四节**。速查：

- 修改/删除 `BossLadder.java`、`BossAttackKit.java`；
- 改实体注册 id、Boss 阶位、Boss 基础 HP/ATK；
- 改阶段阈值/回血/攻击池/其他实体；
- 改 GeckoLib/Forge/MC 版本与 `build.gradle` 依赖；
- 用 `_emissive`（只能 `_glow` 或 `_glowmask`）；
- 不备份就删除旧 Renderer/Model；
- 未跑 `build` / `verify_handoff.py` 就交付。- 把 `references/round6` 的预览图当运行时贴图直接放进 assets；
- 引入未声明的外部模型/贴图包。

---

## 4. 当前模型与渲染架构速查

| 层 | 位置 | 说明 |
|---|---|---|
| 实体注册/判定 | `entity/ModEntities*.java` | 55 个 EntityType；Boss 判定箱已按模型放大 |
| 实体基类 | `entity/CultivationMonsterEntity.java` | 品阶、境界、阶段、Boss 条、伤害上限 |
| 模型类 | `client/*Model.java` | 37 个模型类；原生 `ModelPart` 与 GeckoLib 混合 |
| 渲染器 | `client/*Renderer.java` | 58 个渲染器；贴图、发光层、Boss 形态层 |
| 注册接线 | `ClientSetup.java` | `registerEntityRenderer` / `ModelLayerLocation` |
| 模型资源 | `assets/hongmeng/geo/` | 151 个 geo 文件 |
| 实体贴图 | `assets/hongmeng/textures/entity/` | 106 个主贴图/发光贴图 |
| Boss 模型参考 | `references/round6/<boss>/` | spec、预览、证据；不是运行时资源 |

### 4.1 Boss 与模型/判定对照

| Boss | id | 模型 | 判定箱（宽×高） | 备注 |
|---|---|---|---|---|
| 傀儡祖师 | `epoch_boss` | `PuppetMasterModel` | 0.8×2.6 | 独立旧模型，未参加第六轮重制 |
| 万兽之主 | `beast_boss` | `BeastLordModel` | 4.6×4.95 | 高 4.21 / 长 9.0 |
| 血魔老祖 | `demon_boss` | `BloodDemonModel` | 5.2×7.4 | 高 6.07 / 肩宽 4.0 |
| 虚空道主 | `void_boss` | `VoidLordModel` | 4.5×8.9 | 高 6.50 / 翼展 8.9 |
| 焚天炎帝 | `blaze_emperor` | `BlazeEmperorModel` | 5.4×6.7 | 高 6.00 / 肩宽 4.5 |
| 虚空龙帝 | `void_dragon_emperor` | `VoidDragonEmperorModel` | 3.5×9.1 | 高 7.62 / 翼展 14.9 |
| 仙庭之主 | `heaven_lord` | `HeavenLordModel` | 4.0×7.9 | 高 6.6 / 宽 6.5 |
| 恶 | `evil_gazer` | `EvilArmModel` / `EvilHeadModel` / `EvilUpperModel` | 8.0×21.0，下压 9 | 三阶段三套骨架 |

> 完整索引见 `MODEL-RENDERER-INDEX.md`。

### 4.2 模型制作硬约束

1. **脚底落点**：模型最低点要对齐原版 `y=24` 脚底约定（现有模型大多如此）；新增模型必须用现有 `check_model_ground.py` 类工具验证。
2. **UV 不越界**：1024/512/256 贴图布局必须完全对应，不留跨足迹重叠、真洞。
3. **判定箱不擅自改**：模型可以比判定箱大，但判定箱改动必须报备；Boss 已有 giant 倍率，别重复放大。
4. **不要改 ModelLayer 名**：已有 `ModelLayerLocation` 被 `ClientSetup` 引用；新增要同步注册。
5. **第一人称**：避免模型/拖尾糊住第一人称镜头；已有镜头/拖尾规则不要回退。
6. **发光层**：Boss 的 `_glow.png` 与 `RankGlowLayer` 已有约定；新增发光要遵守现有命名与层顺序。
7. **动画**：GeckoLib 动画名/控制器名保持兼容；不要删除已有动画。

---

## 5. 构建与验证

### 5.1 环境

- Java 17；
- Gradle 8.8（包内 `gradlew` 可用，但第一次可能需联网解析 Forge；离线构建需 Gradle 缓存）；
- Forge 47.4.0；
- GeckoLib 4.8.4 已放 `libs/`，`build.gradle` 已声明。

### 5.2 构建

```powershell
cd 鸿蒙修仙-3D模型交接包-2026-11-11\xiuxian-mod
.\gradlew.bat build --offline -x test
```

如果接手环境没有 Gradle 缓存，可联网构建：

```powershell
.\gradlew.bat build -x test
```

> 生成 jar：`build/libs/hongmeng-forge-0.1.0.jar`。  
> 不要将 `build/`、`run/`、`.gradle/` 交回；只交源码/资源变更。

### 5.3 模型审计（建议）

```powershell
python reference-tools\check_boss_remake.py
python reference-tools\audit_entity_models.py
python reference-tools\check_model_ground.py
python verify_handoff.py
```

> 工具内若有绝对路径，请改成你本机的包路径；不要修改游戏源码来“适配”工具。

### 5.4 交付前自检

- `python verify_handoff.py` → PASS；
- 两个禁改文件哈希与 `FORBIDDEN.sha256` 一致；
- `build.gradle` 仍包含 `geckolib-forge-1.20.1-4.8.4.jar`；
- 变更文件列表、diff/patch、预览截图、审计输出齐全；
- 不提交 `build/`、`run/`、`.gradle/`、日志、临时贴图。

---

## 6. 交付格式（建议）

请按以下结构回交：

```text
交付/
├─ changed-files.txt          ← 改动文件列表
├─ patch.diff                 ← 源码/资源 diff
├─ screenshots/               ← 模型正面/侧面/背面/游戏内预览
├─ verify-output.txt          ← python verify_handoff.py 输出
├─ audit-output.txt           ← 模型审计输出
└─ NOTES.md                   ← 设计说明、未解决项、需要报备的改动
```

**NOTES.md 至少写：**

- 本次改了哪些模型/渲染器；
- 是否改动了判定箱/实体注册/ModelLayer；
- 是否触碰两个禁改文件（必须是“否”）；
- 模型高度/脚底/UV 校验结果；
- 实机截图或预览图路径；
- 剩余风险（光影、第一人称、动画穿模等）。

---

## 7. 参考文件说明

| 文件 | 用途 |
|---|---|
| `docs/21-Boss模型重制契约.md` | 第六轮 Boss 模型目标、分工、验收门禁 |
| `docs/22-Boss模型重制交付.md` | 实际交付结果、模型尺寸、判定箱对齐记录 |
| `docs/23-龙与灾变Boss设计参考.md` | 龙类/灾变 Boss 的设计参考 |
| `docs/19-Boss强度阶梯表.md` | 8 只 Boss 的 HP/ATK/阶位；只读参考，不要改 |
| `docs/06-武器盔甲总表-职业版.md` | 装备系列历史设计表（注意：实际已扩展到 200 甲/60 武器） |
| `references/round6/` | 每只 Boss 的 spec、预览图、审计证据 |
| `reference-tools/` | 模型解析、审计、预览工具 |

---

## 8. 一句话交接

**你只做模型/渲染表现；`BossLadder.java` 和 `BossAttackKit.java` 一个字都不要动。交付前必须让 `verify_handoff.py` 打印 PASS。**
