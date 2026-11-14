# 第六轮 Boss 重制 · 门禁与证据

> 由 `tools/check_boss_remake.py` 生成（只读脚本，不写任何 Java/贴图）。
> 契约：`工程/21-Boss模型重制契约.md` §1/§3。

## 复跑

```
python -X utf8 tools/check_boss_remake.py                 # 门禁+证据+全量审计
python -X utf8 tools/check_boss_remake.py --wait 2400     # 等 7 个模型落盘
python -X utf8 tools/check_boss_remake.py --no-render     # 只出 md/json
python -X utf8 tools/check_boss_remake.py --only dragon   # 单只调试
```

全量审计（脚本内部会调用，也可单独跑）：

```
python -X utf8 工程/角色设计/tools/audit_all.py --no-render --tag "13-Boss重制"
```

## 产物

| 路径 | 内容 |
|---|---|
| `boss-remake.md` / `.json` | 逐只审计数字、契约目标达标表、renderer/API 兼容、问题清单、全量审计结论、剪影 IoU |
| `bosses/<key>/g1_四视图.png` | front/right/back/hero 四视图 |
| `bosses/<key>/g1_剪影64.png` / `g1_缩略32.png` / `g2_hero.png` | 剪影 / 缩略 / hero |
| `bosses/<key>/g3_新旧对比.png` | 重制前 vs 重制后 |
| `剪影对比.png` | 7 模型 64px 剪影并排 + 两两 IoU + 与未重制 Boss 池最近邻 |
| `baseline/old/` | 重制前的 7 模型 + 贴图 + renderer 快照（只读对照） |
| `../00-Boss重制总览.png` | 7 模型同比例并排（体量对比）+ 数字 + 判定 |

## 口径

* 完全复用 `charlib` + `renderlib`（`uv_audit/texture_audit/classify_holes/ground_audit/gap_audit`）
  与 `audit_all` 同判定；另加契约 §1 的 renderer API 检查（类名 / `createBodyLayer` /
  `LOWEST_PIXEL_Y` / `RankGlowLayer` / LAYER id / ClientSetup 注册 / 贴图路径 / glow 尺寸）。
* 挂点：与 `audit_all` 同阈值（>10px 未登记豁免才算硬问题；3~10px 只记警告）；
  悬浮/装饰件与"pivot 口径、网格实际相接"按设计豁免并在报告注明。
* 契约目标：cube 下限、模型高范围、翼展/肩宽/长度范围均在 `check_boss_remake.py` 的
  `BOSSES` 配置里逐只可查。
* 剪影：`renderlib.silhouette(px=64, view=hero)`；与未重制 Boss 池（`audit_all.ENTITIES`
  的 boss/boss_tier 组去掉 7 个重制项）取最近邻 IoU，>0.70 标红。

## 交付边界

* 只读 Java / 贴图 / 其他工具；发现模型问题**只报告不修**（见 `boss-remake.md` §3）；
* 写盘仅限 `工程/角色设计/round6/evidence/**`、`工程/角色设计/round6/00-Boss重制总览.png`；
  全量审计由任务书指定命令自写 `工程/角色设计/审计/`。
