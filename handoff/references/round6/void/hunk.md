# 合并 hunk（虚空道主 · 第六轮重制 E）

> 写域内改动共 4 处：模型 Java（整文件重写）、两张贴图（256² + glow 新增）、`VoidLordRenderer` 一行、本证据目录。
> **未改**：`ClientSetup`、`LAYER` id、实体、hitbox、审计工具、任何其它 Java。

## 1. `client/VoidLordRenderer.java`

```diff
     public VoidLordRenderer(EntityRendererProvider.Context ctx, String texName) {
         super(ctx, new VoidLordModel(ctx.bakeLayer(LAYER)), 1.0F);
         this.texture = new ResourceLocation(HongmengMod.MOD_ID,
                 "textures/entity/" + texName + ".png");
+        // 第六轮：品阶发光层（独眼 / 胸核 / 晶刃 / 符文 / 裂纹发光；贴图按 <tex>_glow.png 自动派生）
+        this.addLayer(new RankGlowLayer<>(this));
     }
```

- `RankGlowLayer` 与本类同包，无需 import；构造签名 `RankGlowLayer(RenderLayerParent<T,M>)`（照抄 `PuppetMasterRenderer`）。
- 发光贴图派生规则：`boss_void_lord.png` → `boss_void_lord_glow.png`（本任务已新增该文件，尺寸 256×256）。
- 渲染器原有 `GROUND_LIFT = (VoidLordModel.LOWEST_PIXEL_Y - 24) / 16.0D` 保持不变；新模型最低点 24 → `GROUND_LIFT = 0`。

## 2. `client/VoidLordModel.java`（整文件重写，studio 生成）

- 类签名不变：`public class VoidLordModel extends EntityModel<CultivationMonsterEntity>`；
- `public static LayerDefinition createBodyLayer()`、构造器 `public VoidLordModel(ModelPart root)` 不变；
- 新增 **63 个部件字段 / 119 cube**（旧 26 / 26）；
- 由 `SPEC.extra` 生成：

```java
    /** 模型最低点像素 y（袍摆触须收束处，浮空基准）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;
```

## 3. 贴图

| 文件 | 旧 | 新 |
|---|---|---|
| `textures/entity/boss_void_lord.png` | 128×128，7237 B | **256×256，约 27.5 KB** |
| `textures/entity/boss_void_lord_glow.png` | 不存在 | **新增 256×256**（约 1.5 KB，独眼/瞳孔/晶刃/符文/金饰发光） |

## 4. 给 Lead 的编译/验收提示

1. `VoidLordModel.java` 由 `studio.py` 生成，**不要手改生成段**（改 `round6/void/spec.py` 后重跑 build）；
2. `LOWEST_PIXEL_Y` 已在生成的 Java 里（`SPEC.extra`），类型 `double`，`VoidLordRenderer` 的算术兼容；
3. 发光层依赖 `boss_void_lord_glow.png` 与主贴图**同目录同名 + `_glow` 后缀**；缺文件会在游戏里显示紫黑方块；
4. `audit_all --no-render --tag "13-Boss重制"` 由 Lead 统一跑（本任务只保证单模型 `build_entity.py` 0 problems）；
5. 阴影半径仍为渲染器原有 `1.0F`（契约未要求改）；品阶放大 `scale()` 未改。
