# 血魔老祖 · Renderer 接线 Hunk（给 Lead 合）

> 写域：`client/BloodDemonRenderer.java`（本 agent 已直接改好，未动 ClientSetup / LAYER / 实体）。
> 契约：`工程/21-Boss模型重制契约.md` §1「Renderer：没有 RankGlowLayer 的加一层」。

## 1. 唯一改动：构造器里加 `RankGlowLayer`

`BloodDemonRenderer` 原本没有发光层，256² 新贴图 + `boss_demon_elder_glow.png`（血眼/胸核/刃口/冠宝石）
不会被绘制。改动如下（构造函数内，1 行）：

```java
public BloodDemonRenderer(EntityRendererProvider.Context ctx, String texName) {
    super(ctx, new BloodDemonModel(ctx.bakeLayer(LAYER)), 1.0F);
    this.texture = new ResourceLocation(HongmengMod.MOD_ID,
            "textures/entity/" + texName + ".png");
    // 第六轮重制：256² 主贴图 + `_glow.png`（血眼/胸口血核/刃口/冠冕宝石）
    this.addLayer(new RankGlowLayer<>(this));   // ← 唯一新增行
}
```

- `RankGlowLayer` 与 `BloodDemonRenderer` 同包（`com.hongmeng.xiuxian.client`），**无需 import**；
- 泛型匹配：`RankGlowLayer<T extends CultivationMonsterEntity, M extends EntityModel<T>>`，
  `BloodDemonRenderer extends MobRenderer<CultivationMonsterEntity, BloodDemonModel>` ✓；
- 层内按 `entity.effectiveRank()` 判断：DEMON_BOSS 是 BOSS 品阶（`glowingEyes()==true`），
  所以实机一定会画发光层；`_glow.png` 由 `getTextureLocation(entity)` 自动推导
  （`.../boss_demon_elder.png` → `.../boss_demon_elder_glow.png`），**不需要显式传路径**。

## 2. 没有动的东西（契约硬约束）

| 项 | 状态 |
|---|---|
| `BloodDemonRenderer.LAYER`（`blood_demon/main`） | **未改**（ClientSetup 用 `BloodDemonRenderer.LAYER` + `BloodDemonModel::createBodyLayer` 注册，保持不变） |
| 构造器签名 `(EntityRendererProvider.Context, String texName)` | **未改**（ClientSetup 的 `new BloodDemonRenderer(ctx, "boss_demon_elder")` 不变） |
| `getTextureLocation` / `GROUND_LIFT` 算法 | 未改（`GROUND_LIFT = (LOWEST_PIXEL_Y - 24) / 16 = 0`） |
| `scale()` 品阶倍率 ×1.10 | 未改（渲染高度 = 6.07 格 × BOSS `scaleY()1.12` × 1.10 ≈ 7.5 格） |
| `ClientSetup` / 实体 / hitbox / `ModEntities` | **未动** |

## 3. 模型侧配套（已由生成器写入 `BloodDemonModel.java`）

- `public static final double LOWEST_PIXEL_Y = 24.0;`（`spec.extra` 注入，重跑不丢）；
- `createBodyLayer()` = `LayerDefinition.create(mesh, 256, 256)`；
- 类名/`extends EntityModel<CultivationMonsterEntity>`/构造器 `(ModelPart root)` 全部保持。

## 4. 自查（可复跑）

```bash
python -X utf8 工程/角色设计/round6/demon/_selfcheck.py
```

`renderer 保留 LAYER / 构造器签名不变 / 已加 RankGlowLayer / 仍按 LOWEST_PIXEL_Y 算 GROUND_LIFT` 四项均 PASS。
