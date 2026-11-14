# 交接文档 · 鸿蒙修仙 mod 剩余工作

> 写给接手的新会话。**先读这份，再读 `docs/modpack-roadmap.md`。**
> 项目根：`C:\Users\duyun\Documents\deepseek-harness\default-workspace\xiuxian-mod`
> 部署目标：`D:\MC\.minecraft\versions\1.20.1-Forge_47.4.0\mods\hongmeng-forge-0.1.0.jar`

---

## 一、当前状态（截至 2026-10-10 16:36）

| 项目 | 值 |
|---|---|
| 已部署 jar | **1 802 479 B** · 16:36:30 · 哈希一致 |
| 编译 | **339 类 0 error** |
| 离线断言 | **587 项 0 失败** |
| 客户端 | **玩家已跑通**（能进游戏、能打怪、能开界面） |

### 最近 5 次修复（玩家已确认前几项有效）

| # | 问题 | 根因 | 状态 |
|---|---|---|---|
| 1 | 满屏黑块 | 25 个作物模型缺 `"render_type": "minecraft:cutout"` | **玩家确认已好** |
| 2 | 杀怪刷屏「破境」 | `CultivationSource.grant()` 里误发公告 | **玩家确认已好** |
| 3 | 三个新群系找不到 | `ObfuscationReflectionHelper` 传官方名 → 生产环境失效 | 待验证 |
| 4 | 命中心形粒子 | `ParticleTypes.DAMAGE_INDICATOR` **贴图就是心形** | 待验证 |
| 5 | 战斗屏幕震动 | `Shake` 振幅过大且连续攻击叠加 | 待验证 |

---

## 更新（接手会话已完成，2026-10-10 晚）

**上面「二、剩余 11 项」里的 11 项已经全部落地并部署**（逐项证据见 docs/TEST-CHECKLIST-round9.md）：

| # | 内容 | 备注 |
|---|---|---|
| ① | 六种灵草 24 个阶段模型 + 32×32 贴图 | 严格模型校验 0 问题 |
| ② | 领域数字 0 开关 | 上一轮已完成 |
| ③ | 法术栏拖拽 | 状态机 6 个清理点，编译 0 error |
| ④ | 双击空格起飞 | 含飞行中不计数护栏 |
| ⑤ | 雷领域真落雷 | setVisualOnly(true)，不点火不参与伤害 |
| ⑥ | 特效浓度 | 部分（沿用上一轮） |
| ⑦ | 突破/提升文字黄色 | 含伤害数字与动作栏 |
| ⑧ | 法宝栏挪到背包上方 | 横排，窗口小则退到右侧 |
| ⑨ | 挖矿无掉落 | **根因：灵兽袋 canAttackBlock 返回 false** → 已修；另加三环黑匣子 /hongmeng breakdiag |
| ⑩ | /hongmeng learnall | 顶层可用（并清掉一条重复定义） |
| ⑪ | 宫殿宝箱+怪物 | 22 宝箱 + 2 刷怪笼 + 屋面瓦质感 |

**另外新开了「维度内容扩展」线**（玩家新要求，方案见 docs/dimension-content-plan.md）：
B1 地形差异化 ✅ / B2 12 个自建群系 ✅ / B3 采集资源 ✅ / B4 9 座维度地标 ✅ / B5 进度锁+指引 ✅（首版）
→ **剩余 B6（小遗迹 + 收集点）**。

**新会话接手时先看**：docs/TEST-CHECKLIST-round9.md（当前待玩家实测项）+ docs/dimension-content-plan.md（维度线）。

**方法论教训（两次都栽在验证工具上，别再犯）**：
execute … run say HIT 在**无玩家在线时不产生任何 RCON 输出** → 探针会报假阴性；
locate structure <id> **只在执行者所在维度里找**，在别的维度查必然 MISS。
正确写法：判定用 execute store result score …，跨维度查用 execute in <维度> run locate …。

---

## 二、剩余 11 项（玩家明确要求）

| # | 玩家原话 | 目标文件（起手） | 备注 |
|---|---|---|---|
| ① | 「我需要他们好看的实际模型」「你知道神秘模组的花吗」 | `assets/hongmeng/models/block/{6种}_stage{0..3}.json` + `textures/block/` | **最大一块**。24 个模型重做成多层几何（神秘时代风格）。**必须带 `render_type: cutout`** |
| ② | 「领域按数字0可以打开或者是关闭」 | — | **已完成**（`key.hongmeng.domain_toggle`） |
| ③ | 「左边的法术可以拖到右边…放到zxcv…被动拖到被动栏」 | `client/SpellBookScreen.java` | 改成拖拽 |
| ④ | 「御剑飞行为何双击空格飞不起来」 | `core/SwordFlight.java` + `ClientSetup` | 双击空格；别破坏 H 键与创造飞行 |
| ⑤ | 「雷的领域没有真实的落雷劈下来」 | `domain/DomainField.java` | **必须 `setVisualOnly(true)`**（真闪电带 `BYPASSES_ARMOR`，曾致"雷劫必死"） |
| ⑥ | 「特效要浓郁一点要看得出来」 | — | **部分完成**（`DENSE_PER_TICK=8`、`AURA_PERIOD=1`、水雨幕 6 滴/tick）；可继续加 |
| ⑦ | 「提升的显示字体为黄色，在血条上方白色看不清」 | `CultivationHud`、`MobHealthBarRenderer` | 未做 |
| ⑧ | 「法宝放上方」 | `client/EquipmentPanel.java` | 现在在背包**右侧**，挡 tooltip 与合成输出格 |
| ⑨ | 「挖东西掉落有延迟」 | `core/RealmMining.java` | 先查根因，不要瞎改 |
| ⑩ | `/hongmeng learnall` 报错 | `command/HongmengCommand.java` | 现只在 `/hongmeng technique learnall` 下，加顶层别名 |
| ⑪ | 「宫殿不是太难是太难看」「可以设置怪物和宝箱奖励」 | `data/hongmeng/structures/xianmen_main_hall.nbt` | 外观精修 + 加怪 + 宝箱 |

---

## 三、验证流程（原样复制就能跑）

### 编译
```powershell
$ws = "C:\Users\duyun\Documents\deepseek-harness\default-workspace"
$javaHome = "$env:APPDATA\.minecraft\runtime\java-runtime-gamma-snapshot"
$cpj = "$ws\_cache\cpjar.jar".Replace('\','/')
$outDir = "$ws\_cache\modout"
$env:JAVA_TOOL_OPTIONS = "-Duser.language=en -Duser.country=US"
$srcs = Get-ChildItem "$ws\xiuxian-mod\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName -replace '\\','/' }
$a1 = @('-encoding','UTF-8','-nowarn','-cp',$cpj,'-d',($outDir -replace '\\','/')) + $srcs
[System.IO.File]::WriteAllLines("$ws\_cache\f1.args", $a1, (New-Object System.Text.UTF8Encoding($false)))
Remove-Item -Recurse -Force $outDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
& "$javaHome\bin\javac.exe" "@$ws\_cache\f1.args" 2>&1 | Select-String 'error' -Context 0,3
```
**加了/删了源文件必须重新生成 `f1.args` / `f2.args`。**

### 离线断言（必须 0 失败）
```powershell
$ws = "C:\Users\duyun\Documents\deepseek-harness\default-workspace"
$javaHome = "$env:APPDATA\.minecraft\runtime\java-runtime-gamma-snapshot"
$cpj = "$ws\_cache\cpjar.jar".Replace('\','/')
$outDir = "$ws\_cache\modout"; $vout = "$ws\_cache\verifyout"
$vsrcs = Get-ChildItem "$ws\xiuxian-mod\src\verify\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName -replace '\\','/' }
$a2 = @('-encoding','UTF-8','-nowarn','-cp',(($outDir -replace '\\','/') + ";$cpj"),'-d',($vout -replace '\\','/')) + $vsrcs
[System.IO.File]::WriteAllLines("$ws\_cache\f2.args", $a2, (New-Object System.Text.UTF8Encoding($false)))
Remove-Item -Recurse -Force $vout -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $vout | Out-Null
& "$javaHome\bin\javac.exe" "@$ws\_cache\f2.args" 2>&1 | Select-String 'error'
& "$javaHome\bin\java.exe" -Dfile.encoding=UTF-8 -cp "$vout;$outDir;$cpj" com.hongmeng.xiuxian.verify.LogicVerifier
```

### 服务端实测（一次只能跑一个）
```powershell
$ws = "C:\Users\duyun\Documents\deepseek-harness\default-workspace"
$env:GRADLE_USER_HOME = "$ws\.gradle-home"
$env:JAVA_HOME = "$env:APPDATA\.minecraft\runtime\java-runtime-gamma-snapshot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location "$ws\xiuxian-mod"
& "$ws\.gradle-home\wrapper\dists\gradle-8.8-bin\6p2afd4p34fvhcdfh8vlmki0fr\gradle-8.8\bin\gradle.bat" runServer --no-daemon --console=plain --no-watch-fs
```
RCON：`127.0.0.1:25598`，密码 `hongmengtest`（`node tools\rcon.js`）。

### 日志必须扫全（**少扫一项就漏过过 bug**）
`Failed to parse` / `Unbound values` / `Unreachable` / `Couldn't load structure` /
`Encountered an unexpected exception` / `crash-report` / `Parsing error` /
`Missing texture` / `Feature order cycle`

### 构建部署
```powershell
& $gradle build --no-daemon --console=plain --no-watch-fs
Copy-Item build\libs\hongmeng-forge-0.1.0.jar "D:\MC\.minecraft\versions\1.20.1-Forge_47.4.0\mods\hongmeng-forge-0.1.0.jar" -Force
```

---

## 四、踩过的坑（**每一条都是真事故，别再犯**）

### 反射与生产环境
1. **绝不按名字反射查字段**。`ObfuscationReflectionHelper.findField(C, "officialName")`
   在开发环境能过、**生产环境必炸**（重映射只改字节码引用，不改字符串常量）。
   已知两次事故：
   - `RangedAttribute.minValue/maxValue`（真名是 `f_22307_` / `f_22308_`）
   - `MultiNoiseBiomeSource.parameters` / `BiomeSource.possibleBiomes` / `ChunkGenerator.featuresPerStep`
     → **三个新群系在整合包里从没生效过**
   **正确做法**：`worldgen/ReflectField.java`（按类型找）或 `compat/AttributeCaps`（按值找）。

### 客户端模型
2. **`HumanoidModel` 的 `head` 与 `hat` 都是从【根】取的**（`pRoot.getChild("hat")`），
   不是 `head` 的子部件。挂错层 → `NoSuchElementException: Can't find part hat`
   → 模型建不出来 → **资源重载失败 → 所有资源包被卸载 → 核心着色器消失
   → `renderChunkLayer` 读 `RenderSystem.getShader()` NPE → 崩客户端**。
   一个错误引发三次崩溃。
3. **作物/十字花模型必须写 `"render_type": "minecraft:cutout"`**，否则透明像素变**纯黑方块**。
4. **自定义 `RenderType` 要缓存**（原版 `EyesLayer` 就是 `static final`）。
   每帧新建会让 `BufferSource` 反复分配 `ByteBufferBuilder`。
5. **别在共享的关卡 `BufferSource` 上调无参 `endBatch()`** —— 会提前刷掉整个关卡的缓冲、
   把 `RenderSystem.getShader()` 留在 null。要用自己的 `MultiBufferSource.immediate(...)`。

### 粒子
6. **`ParticleTypes.DAMAGE_INDICATOR` 的贴图就是心形**（名字里没 HEART，实际是心）。
   想表达"命中"用 `CRIT`。
7. `ParticleTypes.HEART` 才是名字带 HEART 的那个（治疗/繁殖用）。

### 注册与数据
8. **服务端不加载客户端渲染器** —— `runServer` 永远测不出模型/贴图/渲染问题。
   **客户端 bug 只能靠玩家跑**。
9. **`RegistryObject.get()` 绝不能在 mod 构造期调用**。
10. **`data/**` 的 JSON 写错 → 服务端起不来**。
11. **结构 NBT 的 palette `Name` 不能带 `[...]`**，状态要放独立的 `Properties`；
    否则结构**静默永不生成**。
12. **群系 `features` 数组的排列有环 → 新区块 100% 崩**。
    用 `tools/gen_new_biomes.py` 的 `canonical_feature_order()` + `check_feature_order_acyclic()`。
13. **用 `FlyingMoveControl` 的实体必须注册 `FLYING_SPEED`**，否则实体进区块后 ~2 秒崩服。
    体检：`python tools/check_flying_speed.py`。
14. **`ItemStack` 最大安全堆叠是 127**（`save()` 走 `putByte`）。1024 会读回 0 = 静默丢物品。
15. **伤害用 `magic()` / `indirectMagic()`，绝不用 `lightningBolt()`**（带 `BYPASSES_ARMOR`，
    曾致"雷劫必死"）。雷劫闪电一律 `setVisualOnly(true)`。
16. **新按键必须同时加 `key.hongmeng.*` 和 `key.categories.hongmeng` 两份 lang**（中英都要），
    否则设置里看不见。已有键：`R B J K O U Z X C V T Y H 0`。

### 玩家已明确不喜欢的（别改回去）
17. **不要冻结玩家**（顿帧只冻受击方）。曾致"战斗时走路一顿一顿"。
18. **别延长玩家的 `hurtTime`/`hurtDuration`**（原版受伤红屏由它驱动，延长 = 红屏更大更久挡视野）。
19. **压迫感 vignette 边带保持 `/12`、透明度上限 `0.30`**（原来 `/5` + `0.62` 被投诉"红框太大"）。
20. **`Shake` 振幅保持低压**（LIGHT 0.22 度）。玩家投诉过两次"打怪屏幕震动"。

---

## 五、平衡不变量（改动时别破坏）

- 单次怪伤 ≤ 目标最大生命的 **35%**
- 护甲软上限 **92%**
- 首领的百分比类效果一律 `min(p, 0.02)`
- **水领域纯视觉，绝不碰方块/火把/作物**
- 雷劫闪电 `setVisualOnly(true)`

---

## 六、重要提醒

- **开发环境跑得通 ≠ 整合包跑得通**。反射、模型、贴图这三类问题**只有玩家那边会暴露**。
- **本项目从未由 AI 跑过客户端**。所有观感/交互声明都是编译级推断，
  **必须让玩家实测确认**，不要写成"已完成"。
- **改动要小而频**：一次只动一两个子系统，改完立刻构建部署让玩家测。
  大跨度改动（尤其让子代理并行改多个子系统）在这个项目里**失败率很高**：
  曾 3 个子代理同时开工全部中途失败、留下编译错误。
- 玩家偏好「你先大体制作，不满意我再修改」——**先交出能跑的东西，再迭代**。
