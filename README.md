# 鸿蒙修仙 · Hongmeng Cultivation

基于《谁把我仙侠游戏退出键扣了？》的 Minecraft 1.20.1 Forge 修仙模组。

- Minecraft：1.20.1
- Forge：47.4.0
- Java：17
- Gradle：8.8
- GeckoLib：4.8.4

## 仓库结构

```text
mc_mod/
├─ xiuxian-mod/                 ← 模组本体：源码、资源、构建脚本、本地 GeckoLib
├─ handoff/                     ← 交接文档、3D 模型任务、禁止清单、模型参考
│  ├─ README-交接说明.md
│  ├─ PROMPT-修订版.md
│  ├─ BOSS-模型改造总表.md
│  ├─ 禁止清单-中文解释.md
│  ├─ MODEL-RENDERER-INDEX.md
│  ├─ docs/                     ← Boss 模型契约/交付/设计参考、系统总览
│  ├─ references/round6/        ← 第六轮 Boss 模型规格与预览（参考素材）
│  └─ reference-tools/          ← 模型审计/解析工具
├─ AGENTS.md                    ← AI/开发 Agent 工作规则
├─ agent.md                     ← 同 AGENTS.md（兼容小写文件名）
├─ verify_handoff.py            ← 检查禁改文件 / GeckoLib / 源码完整性
└─ FORBIDDEN.sha256             ← 两个禁改文件的 SHA256
```

## 构建

```powershell
cd xiuxian-mod
.\gradlew.bat build --offline -x test
```

首次构建如果没有 Gradle/Forge 缓存，可联网构建：

```powershell
.\gradlew.bat build -x test
```

产物：

```text
xiuxian-mod/build/libs/hongmeng-forge-0.1.0.jar
```

## 交接前检查

在仓库根目录执行：

```powershell
python verify_handoff.py
```

必须看到 **PASS**。它会检查：

- `BossLadder.java`、`BossAttackKit.java` 未被修改；
- `build.gradle` 已声明 GeckoLib 4.8.4；
- `libs/geckolib-forge-1.20.1-4.8.4.jar` 哈希正确；
- `entity` / `client` / `geo` / `textures` 数量完整。

## 重要规则

**绝对不允许修改：**

- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/difficulty/BossLadder.java`
- `xiuxian-mod/src/main/java/com/hongmeng/xiuxian/entity/BossAttackKit.java`

模型 AI 可以改骨骼、比例、视觉尺寸、特化特征、GeoModel/GeoRenderer、贴图、UV、动画；但不能改 HP/ATK、阶段、攻击池、其他实体和 GeckoLib 版本。

详细权限与禁止清单：

- [BOSS-模型改造总表](handoff/BOSS-模型改造总表.md)
- [禁止清单-中文解释](handoff/禁止清单-中文解释.md)
- [3D 模型 AI 提示词](handoff/PROMPT-修订版.md)

## 说明

- `handoff/references/round6/` 只作模型设计参考，不是运行时资源；
- 不要提交 `build/`、`run/`、`.gradle/`、日志和临时文件；
- 本仓库只提交源码、资源、文档和必要工具。
