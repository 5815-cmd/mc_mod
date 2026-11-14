# 鸿蒙修仙 · BOSS 模型改造总表（授权版）

> 用途：给 3D 模型制作 AI 看的“能改什么 / 不能改什么”一页总表。  
> 结论先说：**允许动骨骼、允许改比例、允许把 BOSS 做得更大、允许做专属特征；禁止动战斗核心、数值、其他实体和依赖。**

---

## 一、权限矩阵

| 项目 | 是否允许 | 说明 |
|---|---|---|
| 骨骼树：新增/删除/重命名骨骼、改父子关系、改 pivot | ✅ **允许** | 8 只 BOSS 的骨架可以按需要重排；不必保留旧 `ModelPart` 层级 |
| 模型比例、体型、视觉尺寸 | ✅ **允许** | 允许在现有基础上放大 10%~40%（具体见第二节）；以“有压迫感”为目标，不是越大越好 |
| BOSS 专属特征（角、翼、尾、甲、胸核、日轮、操纵线等） | ✅ **允许** | 允许新增/强化记忆点，允许改轮廓和剪影 |
| root 骨骼 | ✅ 允许调整 | 但 root 本身不要有位移/旋转；垂直动态放 `body/hip/chest` |
| GeoModel / GeoRenderer / 动画文件 | ✅ **允许** | 可新建、替换旧 Model/Renderer；旧文件先备份 |
| 贴图、UV、发光层 | ✅ **允许** | 允许重排 UV、重画贴图、重做发光；发光命名见第三节 |
| `ClientSetup` 的 8 只 BOSS Renderer/ModelLayer 注册 | ✅ **允许** | 只动这 8 只，不要碰其他实体 |
| `entity/GeoBossEntity` / 8 只 BOSS 实现 `GeoEntity` | ✅ **允许** | 推荐新增 `GeoBossEntity` 基类；不改属性/攻击 |
| `ModEntities` 中 8 只 BOSS 的 `sized()` 判定箱 | ⚠ **有条件允许** | 仅当视觉尺寸超出判定箱时才改；必须与模型同步、逐只测试；不得改其他实体 |
| BOSS 基础属性、HP/ATK、品阶、境界缩放 | ❌ **禁止** | 属于战斗核心，模型任务不碰 |
| `BossLadder.java` | ❌ **绝对禁止** | 8 只 BOSS 的阶梯/目标 HP/ATK 唯一真源 |
| `BossAttackKit.java` | ❌ **绝对禁止** | 攻击池、前摇/判定/后摇、召唤表、客户端同步 |
| `BossPhase.java` 的阶段阈值/倍率/回血/转场时序 | ❌ **禁止** | 只允许读阶段状态；不允许改 66%/33% 或阶段属性 |
| `CultivationMonsterEntity` 的伤害软上限/属性/境界逻辑 | ❌ **禁止** | 模型任务不碰战斗结算 |
| 其他实体、其他 Model/Renderer、其他装备/物品/方块/配方/维度/群系 | ❌ **禁止** | 本次范围仅 8 只 BOSS 的视觉表现 |
| GeckoLib / Forge / Minecraft 版本 | ❌ **禁止** | 固定 GeckoLib 4.8.4、Forge 47.4.0、MC 1.20.1 |
| `build.gradle` 的依赖/构建逻辑 | ❌ **禁止** | 除明确允许的本地 GeckoLib 依赖外不改 |
| 网络包、服务端逻辑、AI 行为、掉落表 | ❌ **禁止** | 模型 AI 只做客户端表现 |
| 删除/覆盖旧 Renderer/Model 且不留备份 | ❌ **禁止** | 必须先备份、保留 diff；验收后再清理 |
| 引入外部模型/贴图/动画包 | ❌ **禁止** | 版权与 UV/坐标不可控；只能自制 |
| 使用 `_emissive` 命名 | ❌ **禁止** | GeckoLib 4.8.4 自动发光用 `_glowmask`；项目旧层用 `_glow`，二选一 |
| 未跑 build / 未跑 verify_handoff 就交付 | ❌ **禁止** | 必须 PASS |
| 为了编译去改 `BossLadder`/`BossAttackKit` | ❌ **绝对禁止** | 遇到阻塞应停止并报告 |

---

## 二、8 只 BOSS 的放大与特化目标

> 下面的“视觉尺寸”是**建议范围**，不是硬编码数值；以实机压迫感、判定箱对齐和性能为准。  
> 现有模型高度/判定箱来自当前代码与第六轮交付记录。

| BOSS | id | 现有模型高度 / 判定箱 | 建议视觉放大 | 骨骼/特化重点 | 注意事项 |
|---|---|---|---|---|---|
| 傀儡祖师 | `epoch_boss` | 约 2.6 高 / 0.8×2.6 | +10%~30% | 菱形切角木块、冠冕、背后操纵线、木偶关节 | 人形骨架别做得太宽；操纵线要有“提线”感 |
| 万兽之主 | `beast_boss` | 4.21 高 / 4.6×4.95 | +10%~25%，体长 9→10~11 | 驼峰骨板、巨角兽首、外露肋骨、四粗腿、长尾 | 体长变大后注意转向/贴脸判定；腿别穿地 |
| 血魔老祖 | `demon_boss` | 6.07 高 / 5.2×7.4 | +10%~25% | 三头、六臂、胸前血核、4 层破袍 | 六臂要有主次，不要六条一样长；三头剪影要能一眼看出 |
| 虚空道主 | `void_boss` | 6.50 高 / 4.5×8.9 | +10%~25% | 独眼、6 片黑紫晶翼、4 臂方刃、虚空环、悬浮 | 翼展变大后别让镜头穿模；浮空高度要稳 |
| 焚天炎帝 | `blaze_emperor` | 6.00 高 / 5.4×6.7 | +10%~25% | 巨肩、胸口熔炉、巨拳、牛角冠、火焰鬃毛 | 熔炉是发光核心；肩宽不要压过模型比例 |
| 虚空龙帝 | `void_dragon_emperor` | 7.62 高 / 3.5×9.1 | +10%~20%，翼展 14.9→16~18 | S 长颈、带角头骨、双膜翼、8 节尾、尾刃 | 龙最容易穿模；先做骨架验证再贴膜 |
| 仙庭之主 | `heaven_lord` | 6.6 高 / 4.0×7.9 | +10%~25% | 宽袍、冕旒、日轮、玉圭、帝王肩甲 | 日轮/披风别和旧 `BossFormRenderer` 重复 |
| 恶 | `evil_gazer` | 15.88 高 / 8.0×21.0 | +0%~15%，不要超出天穹场地 | 三阶段：巨臂 / 巨颅 / 上半身；眼、触须、巨掌 | 三套模型切换，最后做；必须测试竞技场边界 |

### 判定箱规则

1. 视觉放大**不强制**改判定箱；先保持原 `sized()`，看是否够用。
2. 如果新模型明显超出判定箱：
   - 只允许改这 8 只 BOSS 的 `sized()`；
   - 改完必须同步验证：近战距离、Boss 攻击命中、阶段缩放、 giant 倍率；
   - 一次只改一只，写进 `NOTES.md`。
3. 不要让“视觉放大”和 `bossRenderScale × giantScale` 重复乘；阶段缩放只在自定义 GeoRenderer 里乘一次。
4. 恶的判定箱是“下压 9 格”特殊方案，改之前必须单独确认。

---

## 三、发光贴图规则

| 方案 | 贴图命名 | 需要的层 |
|---|---|---|
| 保留项目旧品阶发光 | `<boss_id>_glow.png` | 自定义 `GeoRenderLayer`（复刻 `RankGlowLayer` 的品阶色） |
| 使用 GeckoLib 自动发光 | `<boss_id>_glowmask.png` | `AutoGlowingGeoLayer` |

**禁止**：

- 使用 `_emissive`；
- 两套命名混用；
- 只改 PNG 尺寸不重排 UV；
- 把旧原版模型的 `_glow.png` 直接套到 Geo UV 上。

---

## 四、骨骼/动画硬约束

1. **root 骨骼**：不允许位移/旋转；垂直运动放在 `body/hip/chest`。
2. **脚底**：模型脚底/最低点必须落在实体脚底；用试点 BOSS 验证，不要默认 root 0 就对。
3. **朝向**：正面必须与实体朝向一致，不能前后颠倒。
4. **idle / attack_* / phase_transition** 必须存在，并与 `bossPhase()`、`bossState()`、`bossAttack()` 绑定。
5. `attack_*` 按 `bossAttack()` 1~9 映射：
   1 SLAM / 2 SHOCKWAVE / 3 SUMMON / 4 SPELL / 5 BREATH / 6 AMBUSH / 7 CATACLYSM / 8 CHARGE / 9 ENRAGE。
6. **恶**用 `EvilGazerEntity` 自己的阶段/技能状态，不套普通 `BossAttackKit` 表。
7. **旧层迁移**：
   - `BossPhaseRenderer` 的阶段缩放在 GeoRenderer.preRender 重做；
   - `BossFormRenderer` 的冠/翼/场地层与 Geo 模型重复时，报告后再删除；
   - `RankGlowLayer` 不能直接挂 GeoRenderer，必须改 `GeoRenderLayer`。

---

## 五、交付前必须通过

```powershell
cd 鸿蒙修仙-3D模型交接包-2026-11-11
python verify_handoff.py
cd xiuxian-mod
.\gradlew.bat build --offline -x test
```

- `verify_handoff.py` 必须 **PASS**；
- `BossLadder.java`、`BossAttackKit.java` 哈希必须不变；
- 交付说明必须列出：
  - 改了哪些骨骼/比例/特化；
  - 是否改了判定箱；
  - 发光方案（`_glow` 还是 `_glowmask`）；
  - 旧 Renderer/Model 备份路径；
  - 实机截图与验证结果。

---

## 六、一句话总结

**骨骼、比例、特化、贴图、动画都可以大胆改；  
8 只 BOSS 可以做得更大更有压迫感；  
但 HP/ATK、阶段、攻击池、其他实体和 GeckoLib 版本一律不要碰。**
