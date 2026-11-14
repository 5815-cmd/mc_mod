# AGENTS.md — 鸿蒙修仙仓库 Agent 规则

> 本文件面向所有在本仓库工作的 AI / 开发 Agent。  
> 开始修改前必须先读：`handoff/README-交接说明.md`、`handoff/BOSS-模型改造总表.md`、`handoff/禁止清单-中文解释.md`。

## 1. 项目信息

- 项目：鸿蒙修仙 / Hongmeng Cultivation
- 类型：Minecraft 1.20.1 Forge 模组
- Forge：47.4.0（ForgeGradle）
- Java：17
- Gradle：8.8
- 动画：GeckoLib 4.8.4（本地 `xiuxian-mod/libs/`）
- 主源码：`xiuxian-mod/src/main/java/com/hongmeng/xiuxian/`
- 资源：`xiuxian-mod/src/main/resources/assets/hongmeng/`
- 数据：`xiuxian-mod/src/main/resources/data/hongmeng/`

## 2. 绝对禁止

以下两个文件**任何情况下都不能修改、删除、重命名**：

- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/difficulty/BossLadder.java`
- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/entity/BossAttackKit.java`

原因：

- `BossLadder.java` 是 8 只阶梯 BOSS 的 HP/ATK/阶位唯一真源；
- `BossAttackKit.java` 是 BOSS 攻击池、阶段攻击、召唤、客户端同步的核心。

提交前必须在仓库根目录运行：

```powershell
python verify_handoff.py
```

两个文件的 SHA256 必须与 `FORBIDDEN.sha256` 一致。  
如果任务需要改这两个文件，**立即停止并报告**，不允许为了“让代码编译”硬改。

## 3. 允许的改动范围

### 通用开发

- Java 源码、资源、数据、构建脚本（`build.gradle` 仅限确有必要且不影响 GeckoLib 版本）；
- 文档、测试、验证脚本；
- 修复与本仓库代码直接相关的 bug。

### 3D 模型任务

允许：

- 新增/删除/重命名骨骼、改父子关系/pivot；
- 调整模型比例、视觉尺寸、剪影；
- 做 BOSS 专属特化特征（角/翼/尾/甲/胸核/日轮/操纵线等）；
- 新增/替换 `GeoModel`、`GeoRenderer`、`.geo.json`、`.animation.json`、贴图/UV；
- 新增 `GeoBossEntity`，让 8 只 BOSS 实现 `GeoEntity/GeoAnimatable`；
- 修改 `ClientSetup` 中 **8 只 BOSS** 的 Renderer/ModelLayer 注册；
- 视觉尺寸超出判定箱时，有条件同步这 8 只 BOSS 的 `sized()`。

限制：

- BOSS 视觉放大建议 **+10%~40%**，最终以实机、判定箱和性能为准；
- 不要改其他实体、其他装备/物品/方块/配方/维度/群系；
- 不要改 `BossPhase` 的阶段阈值/回血/倍率/时序；
- 不要改 `CultivationMonsterEntity` 的伤害、属性、境界结算逻辑；
- 不要改 GeckoLib/Forge/Minecraft 版本，不要升级/降级依赖；
- 不要用 `_emissive` 命名；只能用 `_glow.png` 或 `_glowmask.png`（每只 BOSS 二选一）；
- 不要不备份就删除旧 Renderer/Model；
- 不要引入外部模型/贴图/动画包。

详细权限矩阵：`handoff/BOSS-模型改造总表.md`。  
逐条中文禁止解释：`handoff/禁止清单-中文解释.md`。

## 4. 渲染迁移注意

- Renderer 注册在 `ClientSetup.java` 的 `EntityRenderersEvent.RegisterRenderers`，**不是** `ModEntities.java`；
- `GeoEntityRenderer` 要求实体实现 `GeoEntity/GeoAnimatable`；当前 `CultivationMonsterEntity` 没有实现，不能直接替换；
- `GeoEntityRenderer` 继承 `EntityRenderer`，不会触发 `BossPhaseRenderer`/`BossFormRenderer` 的 `RenderLivingEvent`；阶段缩放在 GeoRenderer 的 `preRender` 重做；
- `RankGlowLayer` 不能直接挂 GeoRenderer；改用 `GeoRenderLayer` 或 `AutoGlowingGeoLayer`；
- 模型缩放只乘一次，禁止和 `bossRenderScale × giantScale` 重复。

## 5. 构建与验证

```powershell
cd xiuxian-mod
.\gradlew.bat build --offline -x test
```

构建前确认：

- `xiuxian-mod/libs/geckolib-forge-1.20.1-4.8.4.jar` 存在；
- `build.gradle` 含 `geckolib-forge-1.20.1-4.8.4.jar`；
- 不提交 `build/`、`run/`、`.gradle/`、日志、临时文件。

模型任务额外验证：

```powershell
cd ..
python verify_handoff.py
python handoff/reference-tools/check_boss_remake.py
python handoff/reference-tools/audit_entity_models.py
python handoff/reference-tools/check_model_ground.py
```

## 6. 工作流程

1. 先读相关文档和现有代码；
2. 涉及模型/渲染时，先确认是否触碰禁改区；
3. 改动保持小步、可回滚，不删除旧文件而不备份；
4. 修改后编译；
5. 运行 `verify_handoff.py`；
6. 写清楚改动文件、验证结果、未解决项；
7. 不提交生成物、日志、绝对路径本地文件。

## 7. Git 规则

- 不修改历史、不 force push；
- 提交信息用简短中文或英文，例如：
  - `feat: rework boss geckolib models`
  - `fix: migrate boss phase scaling to geo renderer`
  - `docs: update handoff docs`
- 不在仓库中提交 `*.jar`（`xiuxian-mod/libs/geckolib...` 除外）、`build/`、`run/`、日志、临时截图。

## 8. 快速索引

| 内容 | 路径 |
|---|---|
| 交接说明 | `handoff/README-交接说明.md` |
| 模型 AI 提示词 | `handoff/PROMPT-修订版.md` |
| BOSS 权限/放大总表 | `handoff/BOSS-模型改造总表.md` |
| 禁止清单中文解释 | `handoff/禁止清单-中文解释.md` |
| 模型/渲染器索引 | `handoff/MODEL-RENDERER-INDEX.md` |
| 禁改哈希 | `FORBIDDEN.sha256` |
| 交接自检 | `verify_handoff.py` |
| 模型参考 | `handoff/references/round6/` |

---

**一句话：可以大胆改模型和骨骼，不能改战斗核心；提交前必须让 `verify_handoff.py` PASS。**
