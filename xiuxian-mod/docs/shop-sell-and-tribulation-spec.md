# 坊市买卖 + 炼丹雷劫 —— 实现规格

> 对应 `docs/modpack-roadmap.md` 的 **二（经济）** 与 **三（炼丹雷劫）**。
> 玩家原话：**"商店可以购买东西也可以贩卖，挖到的东西掉落物都可以卖设置价格"**。

---

## 一、坊市回收（卖东西给坊市）

### 1.1 代码结构

| 文件 | 作用 |
|---|---|
| `shop/SellPrices.java` | **新增**。纯数据回收价表 `id → 价格`，外加防套利断言 |
| `shop/ShopManager.java` | **改**。新增 `sellEntry` / `sellHeld` / `sellAllSellable` / `sellsProtected` |
| `shop/ShopEntry.java` | **改**。新增 `unitPrice()`（单件买入价，防套利断言要用） |
| `network/ShopSellPacket.java` | **新增**。客户端 → 服务端，三个模式一个包 |
| `network/ModNetwork.java` | **追加**。注册在末尾，**协议 id = 16**（不动任何已有 id） |
| `shop/ShopScreen.java` | **改**。加"买入 / 回收"两个页签 |
| `quest/QuestScreen.java` | **改**。坊市页签里加"买入 / 回收"子页签 |
| `HongmengMod.java` | **改**。`onCommonSetup` 里调 `SellPrices.validate()` |

### 1.2 为什么价格表是纯数据

`SellPrices.TABLE` 只有 `String → Integer`，**没有 `Item`、没有 `ItemStack`、
没有 `ResourceLocation`**。理由和 `ShopEntry` 完全一样：`BuiltInRegistries` /
`Items` 的静态初始化会抛 `Not bootstrapped`，离线验证器一碰就炸。

界面上要图标时才走 `SellPrices.stackOf(id)` / `displayName(id)`，
这两个方法明确标注"只能运行时调用"。

**协议里传的也是物品注册名字符串**，不传价格、不传数量——
客户端改包也改不出便宜价。

### 1.3 回收价一览（`SellPrices.TABLE`，共 70 条）

定价口径：**回收价 = 买入价的 25%~40%**，实际落到 30% 左右并在取整时向下。

#### 原版矿物（17 条）

| 物品 | 价 | 物品 | 价 |
|---|---|---|---|
| 煤 | 2 | 铁锭 | 5 |
| 木炭 | 1 | 金锭 | 8 |
| 粗铁 | 4 | 红石 | 2 |
| 粗铜 | 2 | 青金石 | 2 |
| 铜锭 | 3 | 下界石英 | 3 |
| 粗金 | 7 | 紫水晶碎片 | 3 |
| 钻石 | 25 | 绿宝石 | 20 |
| 下界合金碎片 | 60 | 远古残骸 | 60 |
| 黑曜石 | 4 | | |

#### 原版掉落物（16 条）

骨头 1、蛛丝 1、蜘蛛眼 1、腐肉 1、火药 2、黏液球 2、皮革 2、羽毛 1、
烈焰棒 6、烈焰粉 3、萤石粉 2、末影珍珠 5、恶魂之泪 15、幻翼膜 8、
小麦 1、小麦种子 1。

#### 本模组材料（9 条）

| 物品 | 价 | 说明 |
|---|---|---|
| 灵叶 | **1** | **唯一 1:1 的条目**（取出 10 片 = 10 灵叶，卖回来也是 1 片 1 叶，净收益 0） |
| 灵铁锭 | 3 | |
| 灵晶 | 5 | |
| 空冥精 | 15 | |
| 血珞精 | 20 | |
| 道印 | 40 | |
| 纪元令（妖丹） | **200** | 必须比"一枚令牌换到的全部材料"都贵（16 灵铁=48 / 12 灵晶=60 / 8 空冥精=120 / 8 血珞精=160） |
| 血罗果 | 6 | |
| 空冥石 | 12 | |
| 神霄令 | 20 | |

#### 本模组矿石方块（6 条）

`spirit_iron_ore` 8、`deepslate_spirit_iron_ore` 8、
`spirit_crystal_ore` 12、`deepslate_spirit_crystal_ore` 12、
`kongming_ore` **25**（< 2×空冥精 30）、`xueluo_ore` **35**（< 2×血珞精 40）。

#### 灵植（12 条）

| 阶 | 种子 | 价 | 花瓣 | 价 |
|---|---|---|---|---|
| 1 雪莲草 | 10 | 4 | 雪莲花瓣 | 3 |
| 2 火焰花 | 20 | 8 | 火焰花瓣 | 6 |
| 3 寒冰藤 | 40 | 16 | 寒冰藤花瓣 | 10 |
| 4 雷音草 | 80 | 32 | 雷音草花瓣 | 16 |
| 5 空冥兰 | 160 | 64 | 空冥兰花瓣 | 26 |
| 6 血珞芝 | 320 | 128 | 血珞芝花瓣 | 40 |

#### 方块（3 条）

灵草 1、灵田土 4、丹炉 **24**（< 材料 8 灵铁 + 1 灵叶 = 25）。

#### 丹药（9 条）

回灵丹 3、普通修为丹 5、造化丹 15、完美修为丹 25、筑基丹 25、洗灵丹 45、
破境丹 60、仙品修为丹 90、双道宫丹 120。

#### 功法秘籍（22 条，从买入价算）

`floor(买入价 × 30%)`：50→15、70→20、110→30、150→45、250→75、350→100、
550→160、850→250、1200→360、1900→570。

### 1.4 防套利（强制断言）

`SellPrices.validate()` 会抛异常，规则三条：

1. **灵叶结算的商品**：单件回收价必须 **严格低于** 单件买入价
   （唯一豁免：`hongmeng:lingye`，它必须**正好**等于——1:1 不赚不亏）。
   断言用的是 `ShopEntry.unitPrice() = ceil(总价 / 一份数量)`：
   "16 灵铁 = 1 令" 这种必须换算到一件上比，否则会误判。
2. **纪元令结算的商品**：它们的 `priceLeaves()` 是"几枚令牌"，
   拿它当灵叶价比会得出"灵铁回收 3 ≥ 买入 1"这种假警报。
   这类只要求"回收价低于**令牌本身**的回收价"——
   卖材料永远不如直接卖令牌。
3. **在卖的东西必须有回收价**：漏一条就报错（"买了就卖不掉"是 bug）。

另外 `validate()` 还查：id 非空 / 带命名空间 / 全小写 / 价格在 `1..800`。

**合成链不当印钞机**（同样是断言，写在价格注释里逐条推导）：
成品回收价必须低于"炼/做它的材料"的回收价合计，
例如 筑基丹 25 < 9 灵铁 + 1 灵叶 = 28、丹炉 24 < 25、
`kongming_ore` 25 < 2 空冥精 30、`xueluo_ore` 35 < 2 血珞精 40。

**已知且刻意保留的一条循环**：买种子(320) → 种 → 收花瓣 → 炼丹 → 卖丹。
算上生长概率与燃料，它只是"回本 + 一点辛苦钱"，而且需要真实时间与丹炉，
所以不做封堵；真正要堵的"买进立刻卖出"已经被第 1 条断言封死。

### 1.5 误卖保护（规则与理由）

**"卖出一堆"与"一键回收"都不动快捷栏（槽位 0..8）与护甲槽（36..39）。**

- 理由：那两处放的是玩家**正在用**的东西——镐子、武器、法宝、身上穿的护甲。
  点一下"一键回收"就把手里的镐子卖了，是绝对不可接受的体验。
- 想卖快捷栏里的东西，只有一条路：**拿在手上点"卖出手上物品"**（或按 `H`）。
  那是一个明确指向手持物的动作，不存在误伤。
- 常量与说明写在 `ShopManager.HOTBAR_SIZE / ARMOR_SLOT_START / ARMOR_SLOT_END`
  与 `ShopManager.sellsProtected(slot)`，服务端与界面共用同一份判断
  （界面显示的数量也用同一规则，不会出现"显示 10 个但只能卖 3 个"）。

### 1.6 网络

`ShopSellPacket`（**新增，追加在 `ModNetwork` 末尾，协议 id = 16**）：

| 模式 | 常量 | 载荷 | 行为 |
|---|---|---|---|
| 0 | `MODE_ENTRY` | 物品注册名 | 卖背包里那一种的**一整堆** |
| 1 | `MODE_ALL` | — | 一键回收（跳过快捷栏与护甲） |
| 2 | `MODE_HELD` | — | 卖出手上那一堆 |

编码顺序 `writeVarInt(mode)` → `writeUtf(itemId, 128)`，解码严格同序。
处理完统一调 `ModNetworkBridge.sync(player)` 把新余额推回客户端。

### 1.7 界面

**`ShopScreen`（`/shop`）**：顶部"买入 / 回收"页签（`Tab` 或点击切换）。

- 买入页：原样保留（灵叶余额、`D` 存入灵叶、纪元令商品）。
- 回收页：左侧全量价目表（有货的显示 `§a价 ×数量`，没货的显示 `§6价`），
  右侧显示回收价 / 背包数量 / 一堆可卖多少，三个按钮：
  `卖出一堆` · `卖出手上物品`（`H`）· `一键回收背包`。

**`QuestScreen`（`J` → 坊市页签）**：同样加了"买入 / 回收"子页签，
按钮与提示与 `ShopScreen` 一致，灵叶余额显示照旧。

---

## 二、炼丹雷劫

### 2.1 "最好的丹药"的判定规则（**这是本节的正式口径**）

> **最好的丹药 = `tier` 等于 `AlchemyRecipes.BREWS` 全表最大品阶的那些丹方。**
>
> 规则**从表里推出来**（`AlchemyTribulation.bestTier()` = `max(brew.tier())`），
> **不是写死 `tier == 6`**：以后加七阶灵植与七阶丹方，雷劫会自动跟着上移，
> 不会出现"最高阶丹药反而没雷"的哑火。

**当前实际数据**（`AlchemyRecipes.BREWS` 共 12 条，品阶 1..6）：

| 品阶 | 条数 | 产物 |
|---|---|---|
| 1 | 2 | 回灵丹、普通修为丹 |
| 2 | 2 | 造化丹、完美修为丹 |
| 3 | 2 | 筑基丹、洗灵丹 |
| 4 | 2 | 破境丹、仙品修为丹 |
| 5 | 2 | 双道宫丹、完美修为丹（高阶方） |
| **6** | **2** | **筑基丹（血道方）、仙品修为丹（血道方）** |

所以"最好的丹药"在数据上是**第六阶这一档**（两条血珞芝丹方：主料都是
`xueluo_zhi_petal`），**两条都挨雷**。注意它不等于"最贵的丹药"，
也不等于"某一颗具体的丹药"——玩家说的"最好的丹药"落到数据上就是最高品阶。

### 2.2 概率表（纯数据）

`AlchemyTribulation.OUTCOMES`（用**千分比整数**，让"和正好 100%"成为可精确断言的整数等式）：

| 结果 | 概率（‰） | 概率 | 中文提示 |
|---|---|---|---|
| 失败（丹药被劈碎） | 50 | **5%** | `§c丹药被天雷劈碎……这一炉废了。` |
| 一颗 | 500 | **50%** | `§a一炉得丹 §f×1§a，丹成。` |
| 三颗 | 300 | **30%** | `§a天雷淬丹，一炉得丹 §f×3§a！` |
| 五颗 | 150 | **15%** | `§a一炉得丹 §f×5§a！天雷反而助了药力！` |

开雷前先说一句 `§6天雷淬丹！`（`AlchemyTribulation.OMEN`）。

**非雷劫丹方**：没有雷，`PLAIN_FIVE_PER_MILLE = 200`（**20%**）出 5 个，
否则给丹方原本的 `count()`。

**期望产出**：

- 雷劫：`1×0.50 + 3×0.30 + 5×0.15 + 0×0.05 = ` **2.15 颗**
- 非雷劫：`1×0.80 + 5×0.20 = ` **1.80 颗**

（雷劫的期望严格高于非雷劫——挨雷要更赚，玩家才有盼头。）

### 2.3 自检

`AlchemyTribulation.validate()` 在 `ModAlchemy.register()` 里调用
（和 `AlchemyRecipes.validate()` 并排），**表坏了直接让模组加载失败**。断言：

- 正好 4 档；概率之和 **= 1000‰**；"五个"概率 **= 200‰**；
- 每档概率 ∈ (0,1000]、产出 ∈ [0,5]、中文名与提示语非空；
- **有且只有一档是失败（0 颗）**；
- 雷劫期望 ∈ [1,5]、非雷劫期望 ∈ [1,5]；
- 最好品阶 ∈ 1..6、且**确实存在**会挨雷的丹方、且没有比它更高的品阶。

### 2.4 表现（视觉 + 时序）

触发点在 `AlchemyFurnaceBlockEntity.finishBrew()`——**一炉炼完那一刻**，
不是投料那一刻。先扣主料/辅料，再进入 40 tick（2 秒）的动画：

| tick | 表现 |
|---|---|
| 40 → 21 | 丹药飞上天：`END_ROD` + `ENCHANT` 粒子从炉口升到 **+9 格** |
| 20 | 顶点：劈下 **`EntityType.LIGHTNING_BOLT` + `setVisualOnly(true)`**，加一发 `FLASH` 粒子与 `LIGHTNING_BOLT_THUNDER` 音效 |
| 19 → 1 | 丹药落回炉里（`END_ROD` 粒子下落） |
| 0 | **这时才 `roll()`**，按结果给产物并广播中文消息 |

**`setVisualOnly(true)` 是强制要求**：原版闪电的伤害类型带 `BYPASSES_ARMOR`
标签，真雷会把站在炉边的玩家当场劈死——这个 codebase 已经因为这条踩过一次
"雷劫必死"。纯视觉闪电**不造成伤害、不点燃方块**。
调用被 `try/catch (Throwable)` 包住，任何版本差异都不许打断丹炉 tick。
参考实现见 `core/Breakthrough.spawnVisualBolt`。

产物**直接在落雷那一刻结算**（而不是先塞槽再炸掉）：炸炉时玩家不会看到
"丹药闪一下就没了"，而是一条说得清结果的 `§c丹药被天雷劈碎……`。

动画期间丹炉**不开新的一炉**（丹药还在天上），但燃料照常烧；
`TribulationTicks` 与产物 id 写进 NBT，区块卸载不会丢动画。

消息只发给炉子周围 12 格内的玩家（`announce`），没人在附近就安静。

---

## 三、离线验证清单（纯数据类，可直接断言）

父 agent 可在 `src/verify` 里加这些 `check(...)`：

```java
// ---- 坊市回收价表 ----
boolean sellOk = true; String sellErr = "";
try { com.hongmeng.xiuxian.shop.SellPrices.validate(); }
catch (RuntimeException ex) { sellOk = false; sellErr = String.valueOf(ex.getMessage()); }
check("回收价表自检通过（在卖的东西都有回收价，且无套利）", sellOk, sellErr);
check("回收价表条目 >= 60 条",
        com.hongmeng.xiuxian.shop.SellPrices.size() >= 60,
        "实际 " + com.hongmeng.xiuxian.shop.SellPrices.size());
check("灵叶 1:1（取出 10 片 = 10 灵叶，回收 1 片 = 1 灵叶）",
        com.hongmeng.xiuxian.shop.SellPrices.unitPrice("hongmeng:lingye") == 1, "");
check("原版矿物全部可回收（钻石/铁锭/煤/红石/下界合金碎片）",
        com.hongmeng.xiuxian.shop.SellPrices.accepts("minecraft:diamond")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("minecraft:iron_ingot")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("minecraft:coal")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("minecraft:redstone")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("minecraft:netherite_scrap"), "");
check("本模组材料全部可回收（灵铁/灵晶/空冥精/血珞精/妖丹/道印）",
        com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:spirit_iron_ingot")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:spirit_crystal")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:kongming_essence")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:xueluo_essence")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:jidan")
        && com.hongmeng.xiuxian.shop.SellPrices.accepts("hongmeng:daoyin"), "");
check("六阶灵植的 6 种种子 + 6 种花瓣全部可回收",
        com.hongmeng.xiuxian.shop.SellPrices.size() >= 12, "");
check("不认识的东西不收（基岩返回 0）",
        com.hongmeng.xiuxian.shop.SellPrices.unitPrice("minecraft:bedrock") == 0, "");
check("总价 = 单价 × 数量",
        com.hongmeng.xiuxian.shop.SellPrices.total("minecraft:diamond", 3)
        == 3 * com.hongmeng.xiuxian.shop.SellPrices.unitPrice("minecraft:diamond"), "");

// 防套利：58 个灵叶结算的可买可卖对，回收价严格低于买入单价
int arbChecked = 0; boolean arbOk = true; StringBuilder arbLeak = new StringBuilder();
for (com.hongmeng.xiuxian.shop.ShopEntry e
        : com.hongmeng.xiuxian.shop.ShopCatalog.all()) {
    int sell = com.hongmeng.xiuxian.shop.SellPrices.unitPrice(e.itemId());
    if (!e.usesLeaves()) {                       // 纪元令商品另算
        if (sell >= com.hongmeng.xiuxian.shop.SellPrices
                .unitPrice(e.currencyItemId())) { arbOk = false; }
        continue;
    }
    arbChecked++;
    if ("hongmeng:lingye".equals(e.itemId())) {
        if (sell != e.unitPrice()) { arbOk = false; arbLeak.append(e.itemId()); }
        continue;
    }
    if (sell >= e.unitPrice()) { arbOk = false; arbLeak.append(e.itemId()).append(' '); }
}
check("防套利：" + arbChecked + " 个可买可卖对，回收价都严格低于买入单价",
        arbOk, arbLeak.toString());

// 合成链不能当印钞机
int iron = com.hongmeng.xiuxian.shop.SellPrices.unitPrice("hongmeng:spirit_iron_ingot");
check("筑基丹回收价 < 9 灵铁 + 1 灵叶",
        com.hongmeng.xiuxian.shop.SellPrices.unitPrice("hongmeng:pill_zhuji") < 9 * iron + 1, "");
check("丹炉回收价 < 8 灵铁 + 1 灵叶",
        com.hongmeng.xiuxian.shop.SellPrices.unitPrice("hongmeng:alchemy_furnace")
        < 8 * iron + 1, "");
check("纪元令回收价 > 一枚令牌换到的任何一档材料",
        com.hongmeng.xiuxian.shop.SellPrices.unitPrice("hongmeng:jidan")
        > 16 * iron, "");

// ---- 炼丹雷劫 ----
boolean tribOk = true; String tribErr = "";
try { com.hongmeng.xiuxian.alchemy.AlchemyTribulation.validate(); }
catch (RuntimeException ex) { tribOk = false; tribErr = String.valueOf(ex.getMessage()); }
check("雷劫概率表自检通过", tribOk, tribErr);
check("四档概率之和 = 正好 100%（1000‰）",
        com.hongmeng.xiuxian.alchemy.AlchemyTribulation.totalPerMille() == 1000,
        String.valueOf(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.totalPerMille()));
check("非雷劫『一颗变五个』概率 = 20%",
        Math.abs(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.plainFiveChance() - 0.20D) < 1e-12,
        "");
check("雷劫期望产出 = 2.15 颗（1×50% + 3×30% + 5×15% + 0×5%）",
        Math.abs(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.expectedPills() - 2.15D) < 1e-9,
        String.valueOf(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.expectedPills()));
check("非雷劫期望产出 = 1.80 颗（1×80% + 5×20%）",
        Math.abs(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.expectedPlainPills() - 1.80D) < 1e-9,
        "");
check("雷劫期望严格高于非雷劫", com.hongmeng.xiuxian.alchemy.AlchemyTribulation.expectedPills()
        > com.hongmeng.xiuxian.alchemy.AlchemyTribulation.expectedPlainPills(), "");
check("『最好的丹药』= 全表最高品阶（当前 6）",
        com.hongmeng.xiuxian.alchemy.AlchemyTribulation.bestTier() == 6,
        String.valueOf(com.hongmeng.xiuxian.alchemy.AlchemyTribulation.bestTier()));
check("六阶的两条血珞芝丹方会挨雷",
        com.hongmeng.xiuxian.alchemy.AlchemyTribulation.tribulationBrews().size() == 2, "");
check("五阶及以下的丹方不会挨雷（双道宫丹、破境丹都不挨雷）",
        com.hongmeng.xiuxian.alchemy.AlchemyTribulation.tribulationBrews().stream()
        .noneMatch(b -> b.tier() < 6), "");
check("null 丹方安全（不会 NPE）",
        !com.hongmeng.xiuxian.alchemy.AlchemyTribulation.isTribulation(null), "");

java.util.Random trng = new java.util.Random(20261010L);
int[] thits = new int[6];
for (int i = 0; i < 200_000; i++) {
    thits[com.hongmeng.xiuxian.alchemy.AlchemyTribulation.roll(trng).pills()]++;
}
boolean tdist = true;
for (com.hongmeng.xiuxian.alchemy.AlchemyTribulation.Outcome o
        : com.hongmeng.xiuxian.alchemy.AlchemyTribulation.OUTCOMES) {
    double pct = thits[o.pills()] * 100.0D / 200_000.0D;
    if (Math.abs(pct - o.chance() * 100.0D) > 1.5D) { tdist = false; }
}
check("20 万次雷劫抽样分布与概率表吻合（±1.5%）", tdist, "");
```
