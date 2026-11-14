package com.hongmeng.xiuxian.command;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.Awakening;
import com.hongmeng.xiuxian.core.Breakthrough;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.core.Karma;
import com.hongmeng.xiuxian.core.ModEvents;
import com.hongmeng.xiuxian.core.QuestChain;
import com.hongmeng.xiuxian.core.Realm;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /hongmeng —— 调试与自用命令。
 * 正式玩的时候基本不会碰，但开发与救档都靠它。
 */
public final class HongmengCommand {

    private HongmengCommand() {
    }

    /**
     * 自动装配全部法术：前 5 门被动进被动槽、前 4 门主动进主动槽。
     *
     * @return {装上的被动数, 装上的主动数}
     */
    private static int[] autoEquipAll(CultivationData data) {
        var tech = data.getTechniques();
        int passives = 0;
        int actives = 0;
        try {
            for (var t : tech.knownList()) {
                if (t.isPassive() && passives < com.hongmeng.xiuxian.technique.TechniqueData.PASSIVE_SLOTS) {
                    if (tech.equipPassive(t, passives)) {
                        passives++;
                    }
                } else if (!t.isPassive()
                        && actives < com.hongmeng.xiuxian.technique.TechniqueData.ACTIVE_SLOTS) {
                    if (tech.equipActive(t, actives)) {
                        actives++;
                    }
                }
            }
        } catch (Throwable t) {
            // 装配失败不影响"已经学会了"这件事
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 自动装配法术失败：{}", t.toString());
        }
        return new int[]{passives, actives};
    }


    // ------------------------------------------------------------------
    // B6：收集点结算（采集物 → 灵叶 + 修为）
    // ------------------------------------------------------------------

    /** 花瓣 → 每片多少灵叶 / 多少修为。 */
    private static final int[] PETAL_REWARD = {8, 30};

    /** 灵草 → 每株多少灵叶 / 多少修为。 */
    private static final int[] GRASS_REWARD = {12, 45};

    /** 灵矿 → 每块多少灵叶 / 多少修为。 */
    private static final int[] ORE_REWARD = {15, 60};

    /** 哪些物品算"花瓣"，哪些算"灵矿"。用注册名后缀判断，新增物品自动纳入。 */
    private static int[] rewardOf(net.minecraft.world.item.Item item) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (path.endsWith("_petal")) {
            return PETAL_REWARD;
        }
        if (path.equals("spirit_grass")) {
            return GRASS_REWARD;
        }
        if (path.endsWith("_ore") || path.contains("_ore_")) {
            return null;      // 矿石方块本身不可上交（要挖成掉落物）
        }
        if (path.contains("spirit_iron") || path.contains("spirit_crystal")
                || path.contains("kongming") || path.contains("xueluo")) {
            return ORE_REWARD;
        }
        return null;
    }

    /**
     * 把背包里所有可上交的采集物结算掉。
     *
     * @return {上交件数, 得到灵叶, 得到修为}
     */
    private static int[] collectAll(net.minecraft.server.level.ServerPlayer player,
                                    CultivationData data) {
        int items = 0;
        int leaves = 0;
        double cultivation = 0.0D;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            int[] reward = rewardOf(stack.getItem());
            if (reward == null) {
                continue;
            }
            int n = stack.getCount();
            items += n;
            leaves += reward[0] * n;
            cultivation += reward[1] * (double) n;
            player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (items > 0) {
            // 灵叶走坊市余额（ShopManager 里加不了，直接记到玩家数据上）
            data.addLeaves(leaves);
            com.hongmeng.xiuxian.core.CultivationSource.ADVENTURE
                    .grant(player, cultivation, false);
        }
        return new int[]{items, leaves, (int) Math.round(cultivation)};
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hongmeng")
                // ---- 职业与天赋点（剑/血/魔/鬼/体）----
                .then(Commands.literal("path")
                        .executes(ctx -> {
                            com.hongmeng.xiuxian.talent.Talents.show(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.argument("name", com.mojang.brigadier.arguments
                                        .StringArgumentType.word())
                                .executes(ctx -> {
                                    var sp = ctx.getSource().getPlayerOrException();
                                    String name = com.mojang.brigadier.arguments
                                            .StringArgumentType.getString(ctx, "name");
                                    var p = com.hongmeng.xiuxian.talent.Career.byName(name);
                                    if (p == null) {
                                        ctx.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal(
                                                        "\u00a7c没有这个职业。可选："
                                                                + com.hongmeng.xiuxian.talent
                                                                        .Career.allNames()));
                                        return 0;
                                    }
                                    com.hongmeng.xiuxian.talent.Talents.choose(sp, p);
                                    return 1;
                                })))
                .then(Commands.literal("talent")
                        .executes(ctx -> {
                            com.hongmeng.xiuxian.talent.Talents.show(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", com.mojang.brigadier.arguments
                                                .StringArgumentType.word())
                                        .executes(ctx -> {
                                            var sp = ctx.getSource().getPlayerOrException();
                                            String id = com.mojang.brigadier.arguments
                                                    .StringArgumentType.getString(ctx, "id");
                                            com.hongmeng.xiuxian.talent.Talents
                                                    .rankUp(sp, id);
                                            return 1;
                                        }))))
                // ---- 打开界面（服务端发 S2C 包，客户端 setScreen）----
                .then(Commands.literal("ui")
                        .then(Commands.literal("profession").executes(ctx -> {
                            var sp = ctx.getSource().getPlayerOrException();
                            com.hongmeng.xiuxian.network.OpenUiPacket.open(sp, "profession");
                            return 1;
                        })))
                // ---- 洗点（把天赋点全部退回，职业不变）----
                .then(Commands.literal("respec").executes(ctx -> {
                    var sp = ctx.getSource().getPlayerOrException();
                    var path = com.hongmeng.xiuxian.talent.Talents.pathOf(sp);
                    if (path == null) {
                        ctx.getSource().sendFailure(
                                net.minecraft.network.chat.Component.literal(
                                        "\u00a77尚未择业，无忆可洗。"));
                        return 0;
                    }
                    int cleared = com.hongmeng.xiuxian.talent.Talents.clearRanks(sp);
                    if (cleared <= 0) {
                        ctx.getSource().sendFailure(
                                net.minecraft.network.chat.Component.literal(
                                        "\u00a77你还没点过天赋。"));
                        return 0;
                    }
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a75灵台涤尘 \u00a77· 退回 \u00a7e" + cleared
                                    + " \u00a77点天赋点（职业未变）"));
                    com.hongmeng.xiuxian.network.SyncCultivationPacket.send(sp,
                            com.hongmeng.xiuxian.core.CultivationCapability.get(sp));
                    return 1;
                }))
                // ---- 副业制作（炼丹 / 炼器 / 阵法真能产出东西）----
                .then(Commands.literal("craft")
                        .executes(ctx -> {
                            com.hongmeng.xiuxian.talent.ProfessionCrafting.showAll(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.argument("profession", com.mojang.brigadier.arguments
                                        .StringArgumentType.word())
                                .executes(ctx -> {
                                    var sp = ctx.getSource().getPlayerOrException();
                                    var pr = com.hongmeng.xiuxian.talent.Profession.byName(
                                            com.mojang.brigadier.arguments
                                                    .StringArgumentType.getString(ctx,
                                                            "profession"));
                                    if (pr == null) {
                                        ctx.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal(
                                                        "\u00a7c没有这门副业。可选："
                                                                + com.hongmeng.xiuxian.talent
                                                                        .Profession.allNames()));
                                        return 0;
                                    }
                                    com.hongmeng.xiuxian.talent.ProfessionCrafting
                                            .showRecipes(sp, pr);
                                    return 1;
                                })
                                .then(Commands.argument("id", com.mojang.brigadier.arguments
                                                .StringArgumentType.word())
                                        .executes(ctx -> {
                                            var sp = ctx.getSource().getPlayerOrException();
                                            String id = com.mojang.brigadier.arguments
                                                    .StringArgumentType.getString(ctx, "id");
                                            return com.hongmeng.xiuxian.talent
                                                    .ProfessionCrafting.craft(sp, id) ? 1 : 0;
                                        }))))
                // ---- 道途机制（阵域 / 血池 / 状态查看）----
                .then(Commands.literal("mechanic")
                        .executes(ctx -> {
                            com.hongmeng.xiuxian.talent.PathRuntime.show(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("domain").executes(ctx -> {
                            com.hongmeng.xiuxian.talent.PathRuntime.toggleFormation(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("blood").executes(ctx -> {
                            com.hongmeng.xiuxian.talent.PathRuntime.bloodHeal(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                // ---- 副业（阵法 / 炼丹 / 炼器）：可兼修，用哪门哪门涨 ----
                .then(Commands.literal("profession")
                        .executes(ctx -> {
                            com.hongmeng.xiuxian.talent.Talents.showProfessions(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.argument("name", com.mojang.brigadier.arguments
                                        .StringArgumentType.word())
                                .executes(ctx -> {
                                    var sp = ctx.getSource().getPlayerOrException();
                                    var pr = com.hongmeng.xiuxian.talent.Profession.byName(
                                            com.mojang.brigadier.arguments
                                                    .StringArgumentType.getString(ctx, "name"));
                                    if (pr == null) {
                                        ctx.getSource().sendFailure(
                                                net.minecraft.network.chat.Component.literal(
                                                        "\u00a7c没有这门副业。可选："
                                                                + com.hongmeng.xiuxian.talent
                                                                        .Profession.allNames()));
                                        return 0;
                                    }
                                    com.hongmeng.xiuxian.talent.Talents.recordWork(sp, pr);
                                    com.hongmeng.xiuxian.talent.Talents.showProfessions(sp);
                                    return 1;
                                })))
                // ---- 飞剑剑阵（学御剑工艺：V 展开 / Ctrl+R 换阵 / Ctrl+H 锁定）----
                .then(Commands.literal("sword")
                        .then(Commands.literal("toggle").executes(ctx -> {
                            com.hongmeng.xiuxian.sword.SwordFormationRuntime.toggle(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("pattern").executes(ctx -> {
                            com.hongmeng.xiuxian.sword.SwordFormationRuntime.cyclePattern(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("lock").executes(ctx -> {
                            com.hongmeng.xiuxian.sword.SwordFormationRuntime.lockAhead(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                // ---- 「真实世界」：玩家在聊天里点的链接走这三个子命令 ----
                .then(Commands.literal("realworld")
                        .then(Commands.literal("on").executes(ctx -> {
                            var sp = ctx.getSource().getPlayerOrException();
                            var server = sp.level().getServer();
                            var stage = com.hongmeng.xiuxian.realworld.RealWorld.stage(server);
                            if (stage == com.hongmeng.xiuxian.realworld.RealWorld.Stage.LOCKED) {
                                ctx.getSource().sendFailure(net.minecraft.network.chat.Component
                                        .literal("\u00a7c你还没有窥见世界的背面 —— "
                                                + "先通关一次虚空裂隙。"));
                                return 0;
                            }
                            if (com.hongmeng.xiuxian.realworld.RealWorld.activate(sp)) {
                                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat
                                        .Component.literal("\u00a75\u00a7l真实世界已开启。"), true);
                            } else {
                                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat
                                        .Component.literal("\u00a78真实世界早已开启。"), false);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            var sp = ctx.getSource().getPlayerOrException();
                            com.hongmeng.xiuxian.realworld.RealWorld.decline(sp);
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var stage = com.hongmeng.xiuxian.realworld.RealWorld.stage(server);
                            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat
                                    .Component.literal("\u00a77真实世界：\u00a7f" + switch (stage) {
                                        case LOCKED -> "未解锁（先通关虚空裂隙）";
                                        case UNLOCKED -> "已解锁，尚未开启";
                                        case ACTIVE -> "\u00a7c已开启";
                                    }), false);
                            return 1;
                        })))
                .requires(src -> src.hasPermission(2))

                // ---- 状态 ----
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    CultivationData data = CultivationCapability.get(player);
                    Realm realm = data.realm();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7b=== 鸿蒙修仙 · 状态 ==="), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f灵根：" + (data.getRoot() == null ? "未觉醒"
                                    : data.getRoot().rarity().colored() + " " + data.getRoot().cn()
                                    )), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f境界：" + realm.cn() + "  \u00a77(" + data.progress() + "/"
                                    + data.needed() + ")"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f累计修为：" + data.getTotalCultivation()
                                    + "  业力：" + data.getKarma()
                                    + "  突破成功率：" + String.format("%.1f%%", data.successRate() * 100.0D)), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f任务：" + data.getQuestStage() + " — " + QuestChain.questText(data)), false);
                    return 1;
                }))

                // ---- 修为 ----
                .then(Commands.literal("cultivation")
                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    CultivationSource.SPECIAL.grant(player, amount, false);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "\u00a7a修为 +" + amount), false);
                                    return 1;
                                })))

                // ---- 觉醒 / 重抽 ----
                .then(Commands.literal("awaken").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ModEvents.forceAwaken(player);
                    return 1;
                }))
                .then(Commands.literal("reroll").executes(ctx -> {
                    if (!HongmengConfig.SERVER.allowRerollByCommand.get()) {
                        ctx.getSource().sendFailure(Component.literal(
                                "该存档已关闭命令重抽（allowRerollByCommand=false）"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Awakening.reroll(player, CultivationCapability.get(player), true);
                    return 1;
                }))

                // ---- 业力 ----
                .then(Commands.literal("karma")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7c业力：" + data.getKarma() + " 层，突破失败率 "
                                            + Karma.failurePercentText(data)), false);
                            return 1;
                        }))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            CultivationData data = CultivationCapability.get(player);
                                            Karma.add(data, IntegerArgumentType.getInteger(ctx, "amount"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "\u00a7c业力 → " + data.getKarma()), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("clear").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(player);
                            int cleared = Karma.clear(data);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7a业力已清空（- " + cleared + "）"), false);
                            QuestChain.onKarmaCleared(player);
                            return 1;
                        })))

                // ---- 突破 ----
                .then(Commands.literal("breakthrough").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean ok = Breakthrough.attempt(player, CultivationCapability.get(player));
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            ok ? "\u00a7a突破成功" : "\u00a7c突破失败"), false);
                    return 1;
                }))

                // ---- 任务 ----
                .then(Commands.literal("quest")
                        .then(Commands.literal("stage")
                                .then(Commands.argument("stage", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String stage = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "stage");
                                            CultivationCapability.get(player).setQuestStage(stage);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "\u00a7a任务阶段 → " + stage), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("questtext").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a77" + QuestChain.questText(CultivationCapability.get(player))), false);
                    return 1;
                }))

                // ---- 副本总入口（列出 / 进入 / 退出） ----
                //
                // 有了命令就可以不搭门直接测副本；传送门那条路照常可用。
                .then(Commands.literal("dungeon")
                        .requires(s -> s.hasPermission(0))
                        // 不带参数：列出全部副本与自己的可进状态
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(p);
                            int tier = data.realm().tier();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a76\u00a7l═══ 副本一览 ═══"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a77当前境界：\u00a7e" + data.realm().cn()
                                            + " \u00a78(第 " + tier + " 境)"), false);
                            for (var d : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
                                boolean can = d.canEnter(tier);
                                String line = (can ? "\u00a7a✔ " : "\u00a7c✘ ")
                                        + d.colored()
                                        + " \u00a78门槛第" + d.minTier() + "境"
                                        + " \u00a78" + d.waves() + "波"
                                        + " \u00a78奖励" + d.rewardCultivation()
                                        + (can ? " \u00a77怪强度 \u00a7c×"
                                        + String.format("%.2f", d.mobScaleFor(tier)) : "");
                                ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a78用 \u00a7f/hongmeng dungeon enter <名字>\u00a78 进入，"
                                            + "\u00a7f/hongmeng dungeon leave\u00a78 退出。"), false);
                            return 1;
                        })
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(p);
                            // 先按当前维度判断在哪座副本里，再走对应的退出逻辑
                            var here = com.hongmeng.xiuxian.dungeon.Dungeon.byDimension(
                                    p.level().dimension().location().toString());
                            if (here == null) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "\u00a7c你不在任何副本里。"));
                                return 0;
                            }
                            if (here == com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH) {
                                com.hongmeng.xiuxian.epoch.EpochRealm.leave(p, data);
                            } else {
                                com.hongmeng.xiuxian.dungeon.DungeonRealm.leave(p, here);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("enter")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> {
                                            for (var d
                                                    : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
                                                b.suggest(d.cn());
                                                b.suggest(d.dimensionId());
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource()
                                                    .getPlayerOrException();
                                            CultivationData data = CultivationCapability.get(p);
                                            String name = StringArgumentType.getString(ctx, "name");
                                            var d = com.hongmeng.xiuxian.dungeon.Dungeon
                                                    .byName(name);
                                            if (d == null) {
                                                d = com.hongmeng.xiuxian.dungeon.Dungeon
                                                        .byDimension(name);
                                            }
                                            if (d == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c没有叫「" + name + "」的副本。"));
                                                return 0;
                                            }
                                            if (!d.canEnter(data.realm().tier())) {
                                                var dd = d;
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c境界不足：" + dd.colored()
                                                                + "\u00a7c 需要第 " + dd.minTier()
                                                                + " 大境（当前 "
                                                                + data.realm().cn() + "\u00a7c）"));
                                                return 0;
                                            }
                                            if (d == com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH) {
                                                com.hongmeng.xiuxian.epoch.EpochRealm.enter(p, data);
                                            } else {
                                                com.hongmeng.xiuxian.dungeon.DungeonRealm
                                                        .enter(p, d, data);
                                            }
                                            return 1;
                                        })))
                        // 清空当前波：测试波次推进用
                        .then(Commands.literal("skipwave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            var here = com.hongmeng.xiuxian.dungeon.Dungeon.byDimension(
                                    p.level().dimension().location().toString());
                            if (here == null || here == com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH) {
                                int killed = com.hongmeng.xiuxian.epoch.EpochRealm
                                        .debugKillWave(p);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "\u00a7a已清空当前波（清除 " + killed + " 个敌人）"), false);
                                return killed;
                            }
                            int killed = com.hongmeng.xiuxian.dungeon.DungeonRealm
                                    .debugKillWave(p, here);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7a已清空当前波（清除 " + killed + " 个敌人）"), false);
                            return killed;
                        })))

                // ---- 场景施工（地形地貌批次：无玩家也能把主题地貌建出来）----
                //
                // 为什么要有这条命令：主题地貌/外围地貌是在"玩家进入"时才现场生成的，
                // 服务端 headless 验证（RCON）与救档补建都拿不到玩家上下文。
                // 这里直接调玩家进入流程用的同一套 ensure()：幂等、带版本号、失败只记日志。
                //
                //   /hongmeng scene arena <维度>   → 竞技场本体 + 主题环（ArenaTheme）
                //   /hongmeng scene realm <维度>   → 战斗场地 + 大本营 + 外围地貌（RealmTheme）
                //
                // 权限 2（管理员/命令方块），普通玩家用不到、也进不了没权限的维度。
                .then(Commands.literal("scene")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("arena")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> {
                                            for (var d : com.hongmeng.xiuxian.dungeon.Dungeon
                                                    .values()) {
                                                b.suggest(d.dimensionId());
                                                b.suggest(d.cn());
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            var d = com.hongmeng.xiuxian.dungeon.Dungeon.byName(name);
                                            if (d == null) {
                                                d = com.hongmeng.xiuxian.dungeon.Dungeon
                                                        .byDimension(name);
                                            }
                                            if (d == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c没有这个副本：" + name));
                                                return 0;
                                            }
                                            var dd = d;
                                            var server = ctx.getSource().getServer();
                                            net.minecraft.server.level.ServerLevel lvl =
                                                    com.hongmeng.xiuxian.dungeon.ArenaRealm
                                                            .level(server, dd);
                                            if (lvl == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c竞技场维度未加载：" + dd.dimensionId()));
                                                return 0;
                                            }
                                            try {
                                                net.minecraft.core.BlockPos c =
                                                        com.hongmeng.xiuxian.dungeon.ArenaRealm
                                                                .center(lvl);
                                                com.hongmeng.xiuxian.dungeon.ArenaRealm
                                                        .ensureBuilt(lvl, dd, c);
                                                com.hongmeng.xiuxian.dungeon.ArenaTheme
                                                        .ensure(lvl, dd, c);
                                                com.hongmeng.xiuxian.dungeon.DungeonForecourt
                                                        .ensureArena(lvl, dd, c);
                                                // 再核对一次主题版本，以便 RCON 一眼看出结果
                                                int ver = com.hongmeng.xiuxian.dungeon.ArenaRealm
                                                        .themeVersion(lvl);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "\u00a7a竞技场场景就绪：" + dd.cn()
                                                                + " \u00a77中心=" + c
                                                                + " \u00a77主题版本=" + ver), true);
                                            } catch (Throwable t) {
                                                com.hongmeng.xiuxian.HongmengMod.LOGGER.error(
                                                        "[scene] arena 施工失败 {}", dd.dimensionId(), t);
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c施工失败（详见日志）：" + t));
                                                return 0;
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("realm")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> {
                                            for (var d : com.hongmeng.xiuxian.dungeon.Dungeon
                                                    .values()) {
                                                b.suggest(d.dimensionId());
                                                b.suggest(d.cn());
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            var d = com.hongmeng.xiuxian.dungeon.Dungeon.byName(name);
                                            if (d == null) {
                                                d = com.hongmeng.xiuxian.dungeon.Dungeon
                                                        .byDimension(name);
                                            }
                                            if (d == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c没有这个副本：" + name));
                                                return 0;
                                            }
                                            var dd = d;
                                            var server = ctx.getSource().getServer();
                                            net.minecraft.server.level.ServerLevel lvl =
                                                    com.hongmeng.xiuxian.dungeon.DungeonRealm
                                                            .level(server, dd);
                                            if (lvl == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c副本维度未加载：" + dd.dimensionId()));
                                                return 0;
                                            }
                                            try {
                                                int fy =
                                                        com.hongmeng.xiuxian.dungeon.DungeonArena
                                                                .floorY(lvl, dd);
                                                com.hongmeng.xiuxian.dungeon.DungeonArena
                                                        .ensureBuilt(lvl, dd);
                                                com.hongmeng.xiuxian.dungeon.RealmTheme
                                                        .ensure(lvl, dd, fy);
                                                com.hongmeng.xiuxian.dungeon.DungeonForecourt
                                                        .ensureRealm(lvl, dd, fy);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "\u00a7a副本场景就绪：" + dd.cn()
                                                                + " \u00a77floorY=" + fy), true);
                                            } catch (Throwable t) {
                                                com.hongmeng.xiuxian.HongmengMod.LOGGER.error(
                                                        "[scene] realm 施工失败 {}", dd.dimensionId(), t);
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "\u00a7c施工失败（详见日志）：" + t));
                                                return 0;
                                            }
                                            return 1;
                                        })))
                        // ---- 调试：现场触发下界/末地 Boss 巢穴（BossLair）----
                        // 正常玩法里由 NativeBossSpawner 在 Boss 刷出后调用；
                        // headless 验证没有"刷怪"上下文，这里手工召唤 + 生成一次。
                        .then(Commands.literal("lair")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            net.minecraft.server.level.ServerLevel lvl = src.getLevel();
                                            String which = StringArgumentType.getString(ctx, "name")
                                                    .toLowerCase();
                                            net.minecraft.world.entity.Mob mob;
                                            if (which.startsWith("blaze")) {
                                                mob = com.hongmeng.xiuxian.entity.ModEntities
                                                        .BLAZE_EMPEROR.get().create(lvl);
                                            } else if (which.startsWith("dragon")) {
                                                mob = com.hongmeng.xiuxian.entity.ModEntities
                                                        .VOID_DRAGON_EMPEROR.get().create(lvl);
                                            } else {
                                                src.sendFailure(Component.literal(
                                                        "\u00a7c用法：/hongmeng scene lair blaze|dragon"));
                                                return 0;
                                            }
                                            if (mob == null) {
                                                src.sendFailure(Component.literal(
                                                        "\u00a7cBoss 实体创建失败"));
                                                return 0;
                                            }
                                            net.minecraft.core.BlockPos p =
                                                    net.minecraft.core.BlockPos.containing(src.getPosition());
                                            int gy = lvl.getHeight(net.minecraft.world.level.levelgen
                                                    .Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                                    p.getX(), p.getZ());
                                            p = new net.minecraft.core.BlockPos(p.getX(), gy, p.getZ());
                                            mob.moveTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D,
                                                    0.0F, 0.0F);
                                            lvl.addFreshEntity(mob);
                                            com.hongmeng.xiuxian.entity.BossLair.ensureFor(lvl, mob);
                                            var mp = p;
                                            src.sendSuccess(() -> Component.literal(
                                                    "\u00a7aBoss 巢穴已触发：" + which + " \u00a77位置=" + mp), true);
                                            return 1;
                                        })))
                        // ---- 调试：现场建造真世界 EvilArena（云上战场）----
                        .then(Commands.literal("evilarena").executes(ctx -> {
                            var src = ctx.getSource();
                            net.minecraft.server.level.ServerLevel lvl = src.getLevel();
                            var c = com.hongmeng.xiuxian.realworld.EvilArena.ensure(lvl,
                                    new net.minecraft.core.BlockPos(0,
                                            com.hongmeng.xiuxian.realworld.EvilArena.Y, 0));
                            if (c == null) {
                                src.sendFailure(Component.literal("\u00a7cEvilArena 生成失败（详见日志）"));
                                return 0;
                            }
                            src.sendSuccess(() -> Component.literal(
                                    "\u00a7a云上战场就绪：中心=" + c
                                            + " \u00a77半径="
                                            + com.hongmeng.xiuxian.realworld.EvilArena.R), true);
                            return 1;
                        }))
                        // ---- 调试：现场铺第五维度「天外天」的地形场景（HeavenScenes）----
                        // 正常游戏里由 HeavenScenes 自己在玩家进入时按"分帧施工"铺；
                        // 这条命令是给服务端 headless 验收/救档用的：一次性同步铺完。
                        .then(Commands.literal("heaven").executes(ctx -> {
                            var src = ctx.getSource();
                            net.minecraft.server.level.ServerLevel lvl =
                                    com.hongmeng.xiuxian.heaven.HeavenRealm
                                            .serverLevel(src.getServer());
                            if (lvl == null) {
                                src.sendFailure(Component.literal(
                                        "\u00a7c天外天维度未加载（heaven_realm）"));
                                return 0;
                            }
                            try {
                                int placed = com.hongmeng.xiuxian.heaven.HeavenScenes
                                        .ensureAll(lvl);
                                int ver = com.hongmeng.xiuxian.heaven.HeavenScenes
                                        .sceneVersion();
                                boolean done = com.hongmeng.xiuxian.heaven.HeavenScenes
                                        .isSceneBuilt(lvl);
                                src.sendSuccess(() -> Component.literal(
                                        "\u00a7a天外天地形场景已铺：placed=" + placed
                                                + " \u00a77版本=v" + ver
                                                + " \u00a77完成=" + done), true);
                            } catch (Throwable t) {
                                com.hongmeng.xiuxian.HongmengMod.LOGGER.error(
                                        "[scene] heaven 施工失败", t);
                                src.sendFailure(Component.literal(
                                        "\u00a7c天外天施工失败（详见日志）：" + t));
                                return 0;
                            }
                            return 1;
                        })))
                        // ---- Boss 场地奖励箱（headless 验收 / 救档补箱）----
                        //   /hongmeng bossreward <epoch|beast|demon|void|supply|heaven|evil|native>
                        //   在对应场地按设计锚点放奖励箱，返回实际放置数量与坐标；
                        //   给服务端跑测用（正常游戏由各场地 detail pass 自己放）。
                        .then(Commands.literal("bossreward")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("venue", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            for (String v : new String[]{"epoch", "beast",
                                                    "demon", "void", "supply", "heaven",
                                                    "evil", "native"}) {
                                                b.suggest(v);
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> com.hongmeng.xiuxian.command
                                                .HongmengCommand.bossReward(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "venue")))))

                // ---- 纪元秘境 ----
                .then(Commands.literal("epoch")
                        .then(Commands.literal("enter").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.hongmeng.xiuxian.epoch.EpochRealm.enter(
                                    player, CultivationCapability.get(player));
                            return 1;
                        }))
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.hongmeng.xiuxian.epoch.EpochRealm.leave(
                                    player, CultivationCapability.get(player));
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean in = com.hongmeng.xiuxian.epoch.EpochRealm.inChallenge(player);
                            int left = com.hongmeng.xiuxian.epoch.EpochRealm.remainingMobs(player);
                            CultivationData data = CultivationCapability.get(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7f挑战中：" + (in ? "\u00a7a是" : "\u00a77否")
                                            + "   \u00a7f剩余敌人：" + left
                                            + "   \u00a7f赛季：\u00a7eS" + data.getEpochSeason()
                                            + "   \u00a7f累计通关：" + data.getDungeonClears()), false);
                            return 1;
                        }))

                        // 立刻清空当前波，用于测试波次推进
                        .then(Commands.literal("skipwave").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int killed = com.hongmeng.xiuxian.epoch.EpochRealm.debugKillWave(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7a已清空当前波（清除 " + killed + " 个敌人）"), false);
                            return 1;
                        })))

                // ---- 诊断：把玩家属性的真实值打出来（排 bug 用） ----
                .then(Commands.literal("debug")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
            // debug 命令绝不能自己崩：它是玩家唯一的自查手段。
            // 玩家报过「试图执行该命令时出现意外错误」—— 那等于自查手段失效。
            try {

                                var src = ctx.getSource();
                                if (!(src.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) {
                                    src.sendFailure(Component.literal("需要玩家"));
                                    return 0;
                                }
                                var data = com.hongmeng.xiuxian.core.CultivationCapability.get(p);
                                var realm = data.realm();

                                src.sendSuccess(() -> Component.literal(
                                        "\u00a7b=== 玩家属性诊断 ==="), false);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f境界 \u00a7e%s\u00a77(第%d境)  修为 \u00a7e%d",
                                        realm.cn(), realm.tier(), data.getTotalCultivation())), false);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f觉醒 \u00a7e%s  \u00a7f灵根 \u00a7e%s  \u00a7f难度 \u00a7e%s",
                                        data.isAwakened(),
                                        data.getRoot() == null ? "无" : data.getRoot().cn(),
                                        data.difficulty().cn())), false);

                                // 生命
                                var hp = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f最大生命 \u00a7c%.1f  \u00a77(基础 20.0, 期望 = (20 + %s) × (1 + 灵根%%) )",
                                        hp == null ? -1.0D : hp.getValue(),
                                        String.format("%.1f", realm.healthBonus()
                                                * com.hongmeng.xiuxian.compat.HongmengConfig.SERVER.realmHpScale.get()))), false);
                                if (hp != null) {
                                    for (var m : hp.getModifiers()) {
                                        src.sendSuccess(() -> Component.literal(String.format(
                                                "      \u00a78· %s \u00a77%.1f \u00a78%s",
                                                m.getName(), m.getAmount(), m.getOperation())), false);
                                    }
                                }

                                // 攻击
                                var atk = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f攻击力 \u00a7c%.2f",
                                        atk == null ? -1.0D : atk.getValue())), false);
                                if (atk != null) {
                                    for (var m : atk.getModifiers()) {
                                        src.sendSuccess(() -> Component.literal(String.format(
                                                "      \u00a78· %s \u00a77%.2f \u00a78%s",
                                                m.getName(), m.getAmount(), m.getOperation())), false);
                                    }
                                }

                                // 移速 / 飞行
                                var spd = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f移速 \u00a7e%.4f  \u00a7f可飞 \u00a7e%s  \u00a7f在飞 \u00a7e%s  \u00a7f创造 \u00a7e%s",
                                        spd == null ? -1.0D : spd.getValue(),
                                        p.getAbilities().mayfly, p.getAbilities().flying,
                                        p.isCreative())), false);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f护甲 \u00a7b%.1f  \u00a7f韧性 \u00a7b%.1f  \u00a7f背包法宝 \u00a7e%s",
                                        p.getArmorValue(),
                                        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) == null
                                                ? 0.0D
                                                : p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).getValue(),
                                        com.hongmeng.xiuxian.item.gear.GearManager.summary(p))), false);

                                // 挖掘
                                var tier = com.hongmeng.xiuxian.core.RealmMining.realmTier(data);
                                src.sendSuccess(() -> Component.literal(String.format(
                                        "  \u00a7f空手挖掘 \u00a7b%s  \u00a77(内部等级 %d)  \u00a7f可破坏一切 \u00a7e%s",
                                        tier == null ? "无" : com.hongmeng.xiuxian.core.RealmMining.describeTierObject(tier),
                                        realm.harvestLevel(), realm.canBreakAnything())), false);

                                var hit = p.pick(6.0D, 0.0F, false);
                                // 准星方块那一段。
                                //
                                // 整段包在 try/catch 里：玩家反馈过"试图执行该命令时
                                // 出现意外错误"，而输出正好停在这段之前——说明是这里抛的。
                                // 现在无论出什么问题都会**把原因打出来**，而不是只报一句
                                // 让人无从下手的通用错误。
                                try {
                                    if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr
                                            && !bhr.getType().equals(
                                            net.minecraft.world.phys.HitResult.Type.MISS)) {
                                        var st = p.level().getBlockState(bhr.getBlockPos());
                                        var req = com.hongmeng.xiuxian.core.RealmMining
                                                .requiredTier(st);
                                        var tool = com.hongmeng.xiuxian.core.RealmMining
                                                .toolTypeOf(st);
                                        boolean can = com.hongmeng.xiuxian.core.RealmMining
                                                .realmCanHarvest(tier, st);
                                        String blockName = String.valueOf(
                                                net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                                        .getKey(st.getBlock()));
                                        String reqName = req == null ? "徒手即可"
                                                : com.hongmeng.xiuxian.core.RealmMining
                                                .describeTierObject(req);
                                        String coverage = com.hongmeng.xiuxian.core.RealmMining
                                                .describeToolCoverage(data);
                                        boolean vanillaOk = p.hasCorrectToolForDrops(st);

                                        String l1 = "  \u00a7f准星方块 \u00a7e" + blockName
                                                + "  \u00a77类型 \u00a7b" + tool.cn()
                                                + "  \u00a77需求 \u00a7b" + reqName;
                                        src.sendSuccess(() -> Component.literal(l1), false);

                                        String l2 = "  \u00a77徒手能掉 \u00a7e" + vanillaOk
                                                + "  \u00a77境界判定可采 \u00a7e" + can
                                                + "  \u00a77境界覆盖 \u00a7b" + coverage;
                                        src.sendSuccess(() -> Component.literal(l2), false);

                                        // 挖掘速度：这是玩家最关心的数字，直接报出来
                                        float speed = com.hongmeng.xiuxian.core.RealmMining
                                                .realmMiningSpeedOf(data);
                                        String l3 = "  \u00a77境界挖掘速度 \u00a7b"
                                                + String.format("%.1f", speed)
                                                + (can ? " \u00a7a（对这个方块生效）"
                                                : " \u00a7c（对这个方块不生效）");
                                        src.sendSuccess(() -> Component.literal(l3), false);
                                    } else {
                                        src.sendSuccess(() -> Component.literal(
                                                "  \u00a78（准星没对着方块，无法做挖掘判定）"),
                                                false);
                                    }
                                } catch (Throwable t) {
                                    String msg = t.getClass().getSimpleName()
                                            + ": " + t.getMessage();
                                    src.sendSuccess(() -> Component.literal(
                                            "  \u00a7c挖掘判定失败 —— " + msg), false);
                                    com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                                            "[hongmeng] debug 的挖掘判定段抛异常", t);
                                }
                                return 1;
                                    } catch (Throwable t) {
                ctx.getSource().sendFailure(net.minecraft.network.chat.Component
                        .literal("§c诊断输出异常：§f" + t));
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[hongmeng] debug 命令异常：", t);
                // lambda 必须有返回值：异常路径也要给一个，否则编译报
                // 「lambda 主体既不返回值，也不是 void 兼容」
                return 0;
            }
        }))

                // ---- 坊市（商店）----
                .then(Commands.literal("shop")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            if (!(src.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) {
                                src.sendFailure(Component.literal("需要玩家"));
                                return 0;
                            }
                            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(p);
                            int tier = data.realm().tier();
                            int leaves = com.hongmeng.xiuxian.shop.ShopManager.leavesOf(p);
                            // 纪元令还是实物物品（打赢副本/BOSS 掉），所以这里数背包
                            int tokens = 0;
                            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                                var st = p.getInventory().getItem(i);
                                if (!st.isEmpty()
                                        && st.is(com.hongmeng.xiuxian.item.ModItems.JIDAN.get())) {
                                    tokens += st.getCount();
                                }
                            }

                            p.sendSystemMessage(Component.literal(""));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a76\u00a7l════ 坊 市 ════"));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a7f灵叶余额：\u00a7e" + leaves
                                            + " \u00a77（杀怪 / 采灵草 / 任务都能得）"
                                            + "  \u00a7f纪元令：\u00a7d" + tokens));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a78按 \u00a7fJ \u00a78打开\u00a7f修行录\u00a78，切到\u00a7f坊市\u00a78页签购买"));
                            p.sendSystemMessage(Component.literal(""));
                            for (int i = 0; i < com.hongmeng.xiuxian.shop.ShopCatalog.size(); i++) {
                                var e = com.hongmeng.xiuxian.shop.ShopCatalog.all().get(i);
                                boolean ok = e.unlockedFor(tier);
                                // 价格文案交给 ShopEntry 自己出——它会按**货币种类**
                                // 决定写"叶"还是"纪元令"。这里以前写死了"叶"，
                                // 于是纪元令兑换项会显示成"1 叶"。
                                p.sendSystemMessage(Component.literal(
                                        (ok ? "\u00a7e▪ \u00a7f" : "\u00a78✖ ")
                                                + e.cn()
                                                + (ok ? "\u00a77 — \u00a7e" + e.priceText()
                                                : "\u00a78 — 需 "
                                                + e.requirementText())));
                            }
                            p.sendSystemMessage(Component.literal(""));
                            return 1;
                        }))

                // ---- 主线引导：告诉玩家现在该做什么 ----
                .then(Commands.literal("guide")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            if (!(src.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) {
                                src.sendFailure(Component.literal("需要玩家"));
                                return 0;
                            }
                            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(p);
                            var progress = data.getQuestProgress();
                            progress.refresh(data);
                            var quest = com.hongmeng.xiuxian.quest.QuestManager.tracked(progress);

                            p.sendSystemMessage(Component.literal(""));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a76\u00a7l════ 主 线 引 导 ════"));
                            if (quest == null) {
                                p.sendSystemMessage(Component.literal(
                                        "\u00a7a所有任务都已完成。"));
                                p.sendSystemMessage(Component.literal(
                                        "\u00a77接下来：把境界推到 \u00a7d鸿蒙\u00a77，或者去纪元秘境刷季节奖励。"));
                            } else {
                                p.sendSystemMessage(Component.literal(
                                        "\u00a7f当前任务 \u00a7e[" + com.hongmeng.xiuxian.quest.Quests
                                                .indexOf(quest) + "/"
                                                + com.hongmeng.xiuxian.quest.Quests.total()
                                                + "] \u00a7f" + quest.title()));
                                p.sendSystemMessage(Component.literal(
                                        "\u00a77" + quest.description()));
                                boolean met = quest.requirement().isMet(progress);
                                p.sendSystemMessage(Component.literal(
                                        (met ? "\u00a7a✔ " : "\u00a7e▸ ")
                                                + quest.requirement().describe(progress).getString()));
                                p.sendSystemMessage(Component.literal(
                                        "\u00a78提示：" + quest.hint()));
                                p.sendSystemMessage(Component.literal(met
                                        ? "\u00a7a→ 按 \u00a7fJ \u00a7a打开任务面板领取奖励"
                                        : "\u00a77→ 按 \u00a7fJ \u00a77看全部任务与进度"));
                            }
                            p.sendSystemMessage(Component.literal(
                                    "\u00a78常用：\u00a7fR\u00a78 修炼面板 · \u00a7fJ\u00a78 修行录（任务/坊市） · \u00a7fG\u00a78 道宫 · \u00a7fB\u00a78 突破"));
                            p.sendSystemMessage(Component.literal(""));
                            return 1;
                        }))

                // ---- 新手引导：开局该干什么 ----
                .then(Commands.literal("start")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            if (!(src.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) {
                                src.sendFailure(Component.literal("需要玩家"));
                                return 0;
                            }
                            String[][] steps = {
                                    {"1", "选难度", "进世界会弹难度界面。越高越容易出好灵根，但怪也越强。"},
                                    {"2", "看灵根", "觉醒后按 R 打开面板，看自己的灵根品质与加成。"},
                                    {"3", "打坐", "合成蒲团（4 羊毛 + 1 线），右键坐下吐纳。"},
                                    {"4", "杀怪", "夜里怪多。杀怪既能涨修为，也攒道宫能量。"},
                                    {"5", "采灵草", "野外发光的草就是灵草，空手可采，宗门差事要用。"},
                                    {"6", "拜入神霄剑宗", "练气就能进：/sect join shenxiao"},
                                    {"7", "突破", "修为够了按 B 主动破境，或用破境丹。"},
                                    {"8", "道印之后", "按 G 或 R 面板里的道宫区，凝聚道宫、开本命领域。"},
                            };
                            p.sendSystemMessage(Component.literal(""));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a76\u00a7l════ 新 手 八 步 ════"));
                            for (String[] s : steps) {
                                p.sendSystemMessage(Component.literal(
                                        "\u00a7e" + s[0] + ". \u00a7f" + s[1]
                                                + " \u00a78— " + s[2]));
                            }
                            p.sendSystemMessage(Component.literal(""));
                            p.sendSystemMessage(Component.literal(
                                    "\u00a7a随时输入 \u00a7f/hongmeng guide \u00a7a查看当前该做什么。"));
                            p.sendSystemMessage(Component.literal(""));
                            return 1;
                        }))

                // ---- 属性自测：用独立 FakePlayer 逐个境界验证加成是否真的落地 ----
                // 不需要玩家在线，从服务端控制台跑，结果打进日志。
                .then(Commands.literal("attrselftest").executes(ctx -> {
                    var src = ctx.getSource();
                    var server = src.getServer();
                    var level = server.overworld();
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n[hongmeng] ===== 属性自测（独立 FakePlayer，逐境界）=====\n");
                    sb.append(String.format("%-8s %10s %10s %10s %10s %10s %10s  %s%n",
                            "境界", "修为", "境界生命", "灵根%", "期望生命", "实际生命", "期望攻", "实际攻"));

                    // 1336 是玩家截图里的修为点，专门带灵根复现一次
                    long[] probes = {100L, 600L, 1336L, 3000L, 12000L, 40000L,
                            130000L, 450000L, 1_300_000L, 3_000_000L};
                    int failures = 0;
                    for (int i = 0; i < probes.length; i++) {
                        long probe = probes[i];
                        try {
                            var profile = new com.mojang.authlib.GameProfile(
                                    java.util.UUID.nameUUIDFromBytes(
                                            ("hm_probe_" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                    "hm_probe_" + i);
                            var fp = net.minecraftforge.common.util.FakePlayerFactory
                                    .get(level, profile);

                            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(fp);
                            data.setAwakened(true);
                            // 用玩家的鸿蒙雷灵根，把灵根乘算也一起验了
                            data.setRoot(com.hongmeng.xiuxian.spirit.SpiritRoot
                                    .byKeyCompat("hongmeng_thunder"));
                            data.setCultivation(0L);
                            data.addCultivation(probe);

                            com.hongmeng.xiuxian.core.ModEvents.applyRealmAttributes(fp, data);

                            var hp = fp.getAttribute(
                                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                            var atk = fp.getAttribute(
                                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                            double hpV = hp == null ? -1.0D : hp.getValue();
                            double atkV = atk == null ? -1.0D : atk.getValue();

                            double rootPct = data.getRoot() == null ? 0.0D
                                    : data.getRoot().rarity().healthBonus();
                            // 期望：境界加法 + 灵根乘算
                            double wantHp = (20.0D + data.realm().healthBonus()) * (1.0D + rootPct);
                            double wantAtk = (1.0D + data.realm().damageBonus()) * (1.0D + rootPct);

                            boolean hpOk = Math.abs(hpV - wantHp) < 0.05D;
                            boolean atkOk = Math.abs(atkV - wantAtk) < 0.05D;
                            if (!hpOk) {
                                failures++;
                            }
                            if (!atkOk) {
                                failures++;
                            }

                            sb.append(String.format("%-8s %10d %10.1f %9.0f%% %10.2f %10.2f %10.2f %10.2f  %s%n",
                                    data.realm().cn(), probe,
                                    data.realm().healthBonus(), rootPct * 100.0D,
                                    wantHp, hpV, wantAtk, atkV,
                                    (hpOk && atkOk) ? "OK" : "!! 不符"));

                            // 不符时把每个修饰符都摊开，看清是谁算错了
                            if (!hpOk || !atkOk) {
                                sb.append("        MAX_HEALTH 上的修饰符：\n");
                                if (hp != null) {
                                    hp.getModifiers().forEach(m -> sb.append(String.format(
                                            "            %s = %.3f (%s)%n",
                                            m.getName(), m.getAmount(), m.getOperation())));
                                }
                                sb.append("        ATTACK_DAMAGE 上的修饰符：\n");
                                if (atk != null) {
                                    atk.getModifiers().forEach(m -> sb.append(String.format(
                                            "            %s = %.3f (%s)%n",
                                            m.getName(), m.getAmount(), m.getOperation())));
                                }
                            }
                        } catch (Throwable t) {
                            sb.append("   !! 探针 ").append(probe).append(" 出错：")
                                    .append(t).append('\n');
                            failures++;
                        }
                    }
                    sb.append(failures == 0
                            ? "[hongmeng] 自测结论：所有探针的生命/攻击加成均已生效\n"
                            : "[hongmeng] 自测结论：发现 " + failures + " 处不符\n");

                    // ---- 工具类型覆盖 ----
                    // 境界是"空手工具等级"，镐斧铲锄都要按同一档次判定。
                    // 这部分需要真实 BlockState，所以只能在服务端跑。
                    sb.append("\n[hongmeng] ===== 工具类型覆盖（镐/斧/铲/锄）=====\n");
                    int toolFail = 0;
                    var samples = new Object[][]{
                            {"石头", net.minecraft.world.level.block.Blocks.STONE,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE},
                            {"铁矿", net.minecraft.world.level.block.Blocks.IRON_ORE,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE},
                            {"钻石矿", net.minecraft.world.level.block.Blocks.DIAMOND_ORE,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE},
                            {"黑曜石", net.minecraft.world.level.block.Blocks.OBSIDIAN,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE},
                            {"橡木原木", net.minecraft.world.level.block.Blocks.OAK_LOG,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.AXE},
                            {"橡木木板", net.minecraft.world.level.block.Blocks.OAK_PLANKS,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.AXE},
                            {"泥土", net.minecraft.world.level.block.Blocks.DIRT,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.SHOVEL},
                            {"沙子", net.minecraft.world.level.block.Blocks.SAND,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.SHOVEL},
                            {"干草块", net.minecraft.world.level.block.Blocks.HAY_BLOCK,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.HOE},
                            {"树叶", net.minecraft.world.level.block.Blocks.OAK_LEAVES,
                                    com.hongmeng.xiuxian.core.RealmMining.RequiredTool.HOE},
                    };
                    for (Object[] row : samples) {
                        String name = (String) row[0];
                        var blk = (net.minecraft.world.level.block.Block) row[1];
                        var want = (com.hongmeng.xiuxian.core.RealmMining.RequiredTool) row[2];
                        var st = blk.defaultBlockState();
                        var got = com.hongmeng.xiuxian.core.RealmMining.toolTypeOf(st);
                        var req = com.hongmeng.xiuxian.core.RealmMining.requiredTier(st);
                        boolean ok = got == want;
                        if (!ok) {
                            toolFail++;
                        }
                        sb.append(String.format("  %-10s 类型=%-4s (期望 %-4s) 需求=%-6s %s%n",
                                name, got.cn(), want.cn(),
                                req == null ? "徒手即可"
                                        : com.hongmeng.xiuxian.core.RealmMining
                                        .describeTierObject(req),
                                ok ? "OK" : "!! 不符"));
                    }

                    // 关键行为：境界够的时候，斧/铲/锄方块也要能"像用对工具一样"挖
                    sb.append("  --- 以第 10 境（下界合金级）判定 ---\n");
                    var topTier = com.hongmeng.xiuxian.core.RealmMining.tierFor(5);
                    for (Object[] row : samples) {
                        String name = (String) row[0];
                        var blk = (net.minecraft.world.level.block.Block) row[1];
                        var st = blk.defaultBlockState();
                        boolean can = com.hongmeng.xiuxian.core.RealmMining
                                .realmCanHarvest(topTier, st);
                        if (!can) {
                            toolFail++;
                        }
                        sb.append(String.format("  %-10s 境界判定可采=%s %s%n",
                                name, can, can ? "OK" : "!! 应可采"));
                    }

                    sb.append(toolFail == 0
                            ? "[hongmeng] 工具覆盖结论：镐/斧/铲/锄全部通过\n"
                            : "[hongmeng] 工具覆盖结论：发现 " + toolFail + " 处不符\n");
                    final String out = sb.toString();
                    final int failCount = failures;
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(out);
                    // 同时写一份 UTF-8 明文文件，避免控制台编码把中文搞坏
                    try {
                        java.nio.file.Path dump = server.getWorldPath(
                                        net.minecraft.world.level.storage.LevelResource.ROOT)
                                .resolve("hongmeng-selftest.txt");
                        java.nio.file.Files.writeString(dump, out,
                                java.nio.charset.StandardCharsets.UTF_8);
                        final String dumpPath = dump.toAbsolutePath().toString();
                        src.sendSuccess(() -> Component.literal(
                                "\u00a77结果已写入：" + dumpPath), false);
                    } catch (Throwable ignored) {
                        // 写文件失败不影响命令本身
                    }
                    src.sendSuccess(() -> Component.literal(
                            "\u00a7a属性自测完成。不符项："
                                    + failCount), false);
                    return failCount == 0 ? 1 : 0;
                }))

                // ---- 境界挖掘等级表（不需要玩家在线，方便核对映射） ----
                .then(Commands.literal("tiers").executes(ctx -> {
                    String[] realmNames = {"练气", "筑基", "金丹", "元婴", "化神",
                            "道印", "飞升", "仙人", "帝境", "鸿蒙"};
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7b=== 境界 → 空手挖掘等级 ==="), false);
                    long total = 0L;
                    int lastTier = 0;
                    while (lastTier < 10) {
                        Realm r = Realm.of(total);
                        if (r.tier() != lastTier) {
                            lastTier = r.tier();
                            int lvl = Realm.harvestLevelOf(total);
                            var tier = com.hongmeng.xiuxian.core.RealmMining.tierFor(lvl);
                            String name = tier == null ? "空手" : com.hongmeng.xiuxian.core
                                    .RealmMining.describeTierObject(tier);
                            int lvlShown = tier == null ? -1 : tier.getLevel();
                            // **这里是"实际生效的速度"**：境界的速度下限来自
                            // Realm.speedForTier（飞升 22 / 仙人 29 / 帝境 38 / 鸿蒙 113），
                            // 而不是原版档位自己的速度（钻石 8 / 下界合金 9）。
                            // 原来只打原版档位速度，会让人以为飞升以后挖得比钻石镐还慢。
                            float realmSpeed = Realm.speedForTier(r.tier());
                            float tierSpeed = tier == null ? 0.0F : tier.getSpeed();
                            String line = String.format(
                                    "  \u00a7f%-4s \u00a77内部等级 %d  \u00a7b%s"
                                            + "  \u00a78(原版档位速度 %.1f \u00a7e实际境界速度 %.1f\u00a78)",
                                    realmNames[r.tier() - 1], lvl, name, tierSpeed, realmSpeed);
                            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                        }
                        total = r.minTotal() + r.need();
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a78境界就是「空手工具等级」——镐、斧、铲、锄一视同仁，"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a78到哪个境界，徒手就能像拿对了工具一样挖那一档的方块。"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a78鸿蒙额外：可破坏一切方块（含基岩），刷怪笼连笼子掉落。"), false);
                    return 1;
                }))

                // ---- 功法 ----
                // 注意：整条命令是 dispatcher.register(Commands.literal("hongmeng")...)
                // 的链式调用，没有名为 root 的变量，只能在链上接 .then(...)。
                // ---- 属性上限诊断（权威版：绕过 getter 直接读字段） ----
                .then(Commands.literal("attrcap").executes(ctx -> {
                    String rep = com.hongmeng.xiuxian.compat.AttributeCapDiagnostics.report();
                    for (String line : rep.split("\n")) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a7f" + line), false);
                    }
                    // 顺便报一下在线玩家的真实生命上限。
                    //
                    // 这段原来写成了"一行套三层三元表达式、还嵌在 lambda 里"，
                    // 玩家执行时直接抛异常（"试图执行该命令时出现意外错误"）。
                    // 改成**先把值算干净再交给 lambda**——别在 lambda 里做
                    // 容易 NPE 的链式调用。
                    var p = ctx.getSource().getPlayer();
                    if (p != null) {
                        var hp = p.getAttribute(net.minecraft.world.entity.ai.attributes
                                .Attributes.MAX_HEALTH);
                        double base = hp == null ? -1.0D : hp.getBaseValue();
                        double fin = p.getMaxHealth();
                        double cap = hp == null ? -1.0D
                                : com.hongmeng.xiuxian.compat.AttributeCaps
                                .maxOf(hp.getAttribute());
                        String line = String.format(
                                "\u00a7e你的 MAX_HEALTH：基础 %.1f  最终 %.1f  上限 %.1f",
                                base, fin, cap);
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return 1;
                }))

                // ---- 维度进度与指路（方案 B5）----
                .then(Commands.literal("realm")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7b=== 四座秘境 · 进度与地标 ==="), false);
                            for (var d : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
                                for (String line : com.hongmeng.xiuxian.dungeon
                                        .RealmProgression.guide(p, d)) {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(line), false);
                                }
                            }
                            return 1;
                        })
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, b) -> {
                                    for (var d : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
                                        b.suggest(d.cn());
                                        b.suggest(d.dimensionId());
                                    }
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    for (var d : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
                                        if (d.cn().equals(name)
                                                || d.dimensionId().equals(name)) {
                                            for (String line : com.hongmeng.xiuxian.dungeon
                                                    .RealmProgression.guide(p, d)) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal(line), false);
                                            }
                                            return 1;
                                        }
                                    }
                                    ctx.getSource().sendFailure(Component.literal(
                                            "\u00a7c没有叫「" + name + "」的维度。"));
                                    return 0;
                                })))

                // ---- 配方自检（调试用：确认数据包里的配方有没有真的加载） ----
                // ---- 破坏方块诊断（排查"挖了不掉、方块还在"的幽灵方块）----
                //
                // 背景：玩家报过"挖掉之后方块消失、踩上去还有、没有掉落物"。
                // 幽灵方块在服务端的成因只有一条链：`ForgeHooks.onBlockBreakEvent`
                // 返回 -1（事件被取消）→ `ServerPlayerGameMode.destroyBlock` 直接
                // `return false`，既不删方块也不给掉落。而把事件取消的判据就三个：
                //   ① 手上物品的 `canAttackBlock()` 返回 false（灵兽袋那个 bug 就是这条）
                //   ② `blockActionRestricted()`（冒险模式 / 出生点保护）
                //   ③ 别的模组把 BreakEvent 取消了
                // 这条命令把①②的原版判定**原样调一遍**打出来（不触发破坏），
                // 再加上本模组自己的采掘判定，一次就能定位是哪一环。
                .then(Commands.literal("breakdiag").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var hit = p.pick(8.0D, 0.0F, false);
                    if (!(hit instanceof net.minecraft.world.phys.BlockHitResult bhr)
                            || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a7c准星没有对着方块（最多看 8 格）。"), false);
                        return 0;
                    }
                    net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                    var level = p.serverLevel();
                    var state = level.getBlockState(pos);
                    var stack = p.getMainHandItem();
                    CultivationData data = CultivationCapability.get(p);

                    // ① 原版"这个物品能不能用来攻击/破坏方块"
                    boolean canAttack;
                    try {
                        canAttack = stack.isEmpty() || stack.getItem()
                                .canAttackBlock(state, level, pos, p);
                    } catch (Throwable t) {
                        canAttack = false;
                    }
                    // ② 原版"这个位置允不允许破坏"
                    boolean restricted = p.blockActionRestricted(level, pos,
                            p.gameMode.getGameModeForPlayer());
                    // ③ 本模组自己的采掘判定
                    var tier = com.hongmeng.xiuxian.core.RealmMining.realmTier(data);
                    boolean realmCan = com.hongmeng.xiuxian.core.RealmMining
                            .realmCanHarvest(tier, state);
                    boolean correctTool = p.hasCorrectToolForDrops(state);
                    float realmSpeed = com.hongmeng.xiuxian.core.RealmMining
                            .realmMiningSpeedOf(data);
                    float destSpeed = state.getDestroySpeed(level, pos);

                    final String l1 = "\u00a7b=== 破坏诊断 @ " + pos.getX() + " " + pos.getY()
                            + " " + pos.getZ() + " ===";
                    final String l2 = "\u00a7f方块：\u00a7e" + state.getBlock()
                            + "\u00a77 硬度 " + destSpeed
                            + "\u00a77 需工具 " + com.hongmeng.xiuxian.core.RealmMining
                            .toolTypeOf(state).cn()
                            + "\u00a77 需求等级 " + String.valueOf(
                            com.hongmeng.xiuxian.core.RealmMining.requiredTier(state) == null
                                    ? "徒手"
                                    : com.hongmeng.xiuxian.core.RealmMining
                                    .describeTierObject(
                                            com.hongmeng.xiuxian.core.RealmMining
                                                    .requiredTier(state)));
                    final String l3 = "\u00a7f手持：\u00a7e"
                            + (stack.isEmpty() ? "（空手）" : stack.getHoverName().getString()
                            + " x" + stack.getCount())
                            + "\u00a77 游戏模式 " + p.gameMode.getGameModeForPlayer();
                    final String l4 = (canAttack ? "\u00a7a① canAttackBlock = true"
                            : "\u00a7c① canAttackBlock = FALSE  ← 这就是幽灵方块的成因（破坏事件会被取消）");
                    final String l5 = (restricted ? "\u00a7c② blockActionRestricted = TRUE  ← 这个位置不让破坏"
                            : "\u00a7a② blockActionRestricted = false");
                    final String l6 = "\u00a7f境界：\u00a7e" + data.realm().cn()
                            + "\u00a77 采掘等级 " + (tier == null ? "无"
                            : com.hongmeng.xiuxian.core.RealmMining.describeTierObject(tier))
                            + "\u00a77 境界速度 " + realmSpeed
                            + "\u00a77 原版正确工具 " + correctTool
                            + "\u00a77 境界可采 " + (realmCan ? "\u00a7a是" : "\u00a7c否");
                    for (String line : new String[]{l1, l2, l3, l4, l5, l6}) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    if (!canAttack || restricted) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a7c结论：破坏事件会被服务端取消 → 方块不删、不掉落、"
                                        + "客户端却已经本地预测挖掉了（幽灵方块）。"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a7a结论：破坏链上没有取消项；真挖一次应该正常破、正常掉。"
                                        + "如果仍然不掉，请把 logs/latest.log 整个发来。"), false);
                    }

                    // ---- 黑匣子：最近的服务端破坏痕迹 ----
                    // 三种情况要分开：缓冲区空 ⇒ 服务端根本没收到挖掘请求；
                    // 只有「采掘判定」没有「破坏事件」⇒ 破坏事件被取消；
                    // 两者都有 ⇒ 破坏是成功的，问题在掉落/拾取环节。
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7b--- 服务端破坏痕迹（最近 " + com.hongmeng.xiuxian.core
                                    .MiningDiag.recent().length + " 条，共 "
                                    + com.hongmeng.xiuxian.core.MiningDiag.totalRecorded()
                                    + " 条）---"), false);
                    String[] recent = com.hongmeng.xiuxian.core.MiningDiag.recent();
                    if (recent.length == 0) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a7c（空）服务端**一次挖掘都没看到**：说明挖方块的请求根本没到服务端，"
                                        + "或这个位置压根没进过破坏流程。"), false);
                    } else {
                        for (String line : recent) {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a78" + line), false);
                        }
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a77判读：三环=①左键按下 ②采掘判定 ③破坏事件。"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a77  ①②有、缺③ → 破坏事件被取消（幽灵方块，就是它）"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a77  只有① → 服务端收到了开始挖，但没走到破坏流程"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a77  三环齐全 → 破坏成功，问题在掉落/拾取环节"), false);
                    return 1;
                }))

                .then(Commands.literal("recipes").executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    var all = server.getRecipeManager().getRecipes();
                    int mine = 0;
                    int total = 0;
                    var names = new java.util.ArrayList<String>();
                    for (var holder : all) {
                        total++;
                        var id = holder.getId();
                        if (id.getNamespace().equals("hongmeng")) {
                            mine++;
                            if (names.size() < 90) {
                                names.add(id.getPath());
                            }
                        }
                    }
                    final int m = mine;
                    final int tot = total;
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7b=== 配方自检 ==="), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f已加载配方总数：\u00a7e" + tot
                                    + "   \u00a7f其中本模组：\u00a7e" + m), false);
                    java.util.Collections.sort(names);
                    for (String n : names) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a78  hongmeng:" + n), false);
                    }
                    return m;
                }))

                .then(Commands.literal("technique")
                .requires(s -> s.hasPermission(0))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CultivationData data = CultivationCapability.get(p);
                    var tech = data.getTechniques();
                    var known = tech.knownList();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a76\u00a7l═══ 已修习功法（" + known.size() + " / "
                                    + com.hongmeng.xiuxian.technique.Technique.values().length
                                    + "）═══"), false);
                    if (known.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a77还没修习任何功法。用 \u00a7f/hongmeng technique manual"
                                        + " \u00a77拿秘籍，或 \u00a7f/hongmeng technique learn <名>"
                                        + "\u00a77直接学。"), false);
                    }
                    long now = p.level().getGameTime();
                    for (var t : known) {
                        long cd = tech.remainingSeconds(t, now);
                        boolean selected = tech.selectedTechnique() == t;
                        String line = (selected ? "\u00a7e▶ " : "\u00a78  ")
                                + t.colored()
                                + " \u00a78[" + t.element().cn() + "/" + t.kind().glyph() + "]"
                                + (cd > 0 ? " \u00a7c冷却 " + cd + "s" : " \u00a7a就绪")
                                + "\u00a78 · " + t.trait();
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a78按 \u00a7fV \u00a78施展，\u00a7fC \u00a78切换（Shift 反向）。"), false);
                    return known.size();
                })
                .then(Commands.literal("learn")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, b) -> {
                                    for (var t : com.hongmeng.xiuxian.technique.Technique.values()) {
                                        b.suggest(t.cn());
                                        b.suggest(t.name());
                                    }
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    var t = com.hongmeng.xiuxian.technique.Technique.byName(name);
                                    if (t == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "\u00a7c没有叫「" + name + "」的功法。"));
                                        return 0;
                                    }
                                    CultivationData data = CultivationCapability.get(p);
                                    if (data.getTechniques().has(t)) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "\u00a7c已经学过" + t.colored() + "\u00a7c了。"));
                                        return 0;
                                    }
                                    data.getTechniques().learn(t);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "\u00a7a修习 " + t.colored() + " \u00a78· "
                                                    + t.trait()), false);
                                    com.hongmeng.xiuxian.network.ModNetworkBridge.sync(p);
                                    return 1;
                                })))
                .then(Commands.literal("collect")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(p);
                            int[] got = collectAll(p, data);
                            ctx.getSource().sendSuccess(() -> Component.literal(got[0] <= 0
                                    ? "\u00a77背包里没有可上交的采集物。"
                                    : "\u00a7a上交 \u00a7e" + got[0] + " \u00a7a件 → 灵叶 \u00a7e"
                                            + got[1] + " \u00a7a· 修为 \u00a7e" + got[2]), false);
                            com.hongmeng.xiuxian.network.ModNetworkBridge.sync(p);
                            return got[0];
                        }))
                .then(Commands.literal("learnall")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CultivationData data = CultivationCapability.get(p);
                            int learned = 0;
                            for (var t : com.hongmeng.xiuxian.technique.Technique.values()) {
                                if (data.getTechniques().learn(t)) {
                                    learned++;
                                }
                            }
                            final int n = learned;
                            // 玩家要求「一键获取所有法术」——学会之后**顺手装好**，
                            // 不然学完还要一门门拖进槽位。规则与法术页一致：
                            // 前 5 门被动 → 被动槽，前 4 门主动 → 主动槽。
                            int[] equipped = autoEquipAll(data);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7a一次修习了 " + n + " 门功法\u00a77（调试用）\u00a7a，"
                                            + "已自动装配 \u00a7e" + equipped[0] + " \u00a7a门被动 / "
                                            + "\u00a7e" + equipped[1] + " \u00a7a门主动。"), false);
                            com.hongmeng.xiuxian.network.ModNetworkBridge.sync(p);
                            return n;
                        }))
                .then(Commands.literal("manual")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int given = 0;
                            for (var entry
                                    : com.hongmeng.xiuxian.technique.TechniqueItems.all().entrySet()) {
                                var ro = entry.getValue();
                                if (!ro.isPresent()) {
                                    continue;
                                }
                                var stack = new net.minecraft.world.item.ItemStack(ro.get());
                                if (!p.getInventory().add(stack)) {
                                    p.drop(stack, false);
                                }
                                given++;
                            }
                            final int n = given;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "\u00a7a已发放全部 " + n + " 本功法秘籍到你的背包。"), false);
                            return n;
                        }))));

        // B6 收集点：/collect —— 把采到的灵植花瓣 / 灵草 / 灵矿上交，换灵叶与修为。
        dispatcher.register(Commands.literal("collect")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CultivationData data = CultivationCapability.get(p);
                    int[] got = collectAll(p, data);
                    if (got[0] <= 0) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a77背包里没有可上交的采集物。\u00a78"
                                        + "（六个维度的遗迹与采集点：灵植花瓣 / 灵草 / 灵矿）"),
                                false);
                        return 0;
                    }
                    final int items = got[0];
                    final int leaves = got[1];
                    final int cult = got[2];
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7a上交 \u00a7e" + items + " \u00a7a件采集物 → 灵叶 \u00a7e"
                                    + leaves + " \u00a7a· 修为 \u00a7e" + cult), false);
                    com.hongmeng.xiuxian.network.ModNetworkBridge.sync(p);
                    return items;
                }));

        // 顶层别名：/learnall —— 一键学会并装好全部法术。
        //
        // 玩家要求「把可以一键获取所有法术的命令给我」：
        // `/hongmeng learnall` 要打一长串，这里给一个最短的入口。
        //
        // **必须放在 `/hongmeng` 那条 register 语句之外** ——
        // 它是独立的命令，塞进 `.then(...)` 链里会变成"hongmeng 的子命令"，
        // 而且会破坏那条链的括号结构（第一版就插错了位置，编译直接报
        // "已在方法 register 中定义了变量 ctx"）。
        dispatcher.register(Commands.literal("learnall")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CultivationData data = CultivationCapability.get(p);
                    int learned = 0;
                    for (var t : com.hongmeng.xiuxian.technique.Technique.values()) {
                        if (data.getTechniques().learn(t)) {
                            learned++;
                        }
                    }
                    int[] equipped = autoEquipAll(data);
                    final int n = learned;
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7a已学会全部 " + n + " 门功法，并自动装配 "
                                    + "\u00a7e" + equipped[0] + " \u00a7a门被动 / "
                                    + "\u00a7e" + equipped[1] + " \u00a7a门主动。"
                                    + "\n\u00a77（法术页：按 1~5 装被动、6~9 装主动、0 清空）"),
                            false);
                    com.hongmeng.xiuxian.network.ModNetworkBridge.sync(p);
                    return n;
                }));
    }

    // ------------------------------------------------------------------
    // Boss 场地奖励箱验收（/hongmeng bossreward）
    // ------------------------------------------------------------------

    /**
     * 在对应场地按设计锚点放奖励箱（幂等：已有箱子的格子会跳过）。
     *
     * <p>为什么要这条命令：奖励箱 placement 依赖“场地已建 + 区块已加载”，
     * 而服务端 headless 跑测没有玩家去触发 detail pass / 击杀流程；
     * 这条命令让 RCON 能直接验证「坐标 + LootTable + 战利品表可解析」。</p>
     */
    private static int bossReward(net.minecraft.commands.CommandSourceStack src, String venue) {
        var server = src.getServer();
        net.minecraft.server.level.ServerLevel level = null;
        net.minecraft.core.BlockPos origin = null;
        int placed;
        String label;
        int[][] lastOffsets = new int[0][];
        boolean showDetail = false;
        try {
            switch (venue) {
                case "epoch", "beast", "demon", "void" -> {
                    var d = com.hongmeng.xiuxian.dungeon.Dungeon.byName(venue);
                    if (d == null) {
                        src.sendFailure(Component.literal("\u00a7c没有这个副本：" + venue));
                        return 0;
                    }
                    level = com.hongmeng.xiuxian.dungeon.ArenaRealm.level(server, d);
                    if (level == null) {
                        src.sendFailure(Component.literal("\u00a7c竞技场维度未加载"));
                        return 0;
                    }
                    var c = com.hongmeng.xiuxian.dungeon.ArenaRealm.center(level);
                    com.hongmeng.xiuxian.dungeon.ArenaRealm.ensureBuilt(level, d, c);
                    level = com.hongmeng.xiuxian.dungeon.ArenaRealm.level(server, d);
                    // headless 没有玩家触发自驱 detail pass：这里强制跑完（含 8 个宝库箱）
                    com.hongmeng.xiuxian.dungeon.ArenaDetail.forceRun(level, d);
                    // 中心可能是祭坛顶面：落到实际地面再放验收箱
                    var stand = com.hongmeng.xiuxian.dungeon.BossRewards.findFloorStand(
                            level, c.getX(), c.getZ(), c.getY() + 2);
                    origin = stand;
                    int[][] offs = {{-7, 0, 0}, {7, 0, 0}};
                    lastOffsets = offs;
                    showDetail = true;
                    placed = com.hongmeng.xiuxian.dungeon.BossRewards.placeChests(
                            level, origin, offs,
                            com.hongmeng.xiuxian.dungeon.BossRewards.arenaLootFor(d), 20261102L);
                    label = d.dimensionId() + " 决战宝库 + 细节";
                }
                case "supply" -> {
                    var d = com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH;
                    level = com.hongmeng.xiuxian.dungeon.DungeonRealm.level(server, d);
                    if (level == null) {
                        src.sendFailure(Component.literal("\u00a7c副本维度未加载"));
                        return 0;
                    }
                    int fy = com.hongmeng.xiuxian.dungeon.DungeonArena.floorY(level, d);
                    origin = new net.minecraft.core.BlockPos(0, fy, 0);
                    // 与 RealmDetail 的战场补给点走同一份偏移表（避免两处锚点漂移）
                    int[][] offs = com.hongmeng.xiuxian.dungeon.RealmDetail.supplyOffsets();
                    lastOffsets = offs;
                    showDetail = true;
                    placed = com.hongmeng.xiuxian.dungeon.BossRewards.placeChests(
                            level, origin, offs,
                            com.hongmeng.xiuxian.dungeon.BossRewards.LOOT_SUPPLY, 20261103L);
                    label = "epoch_realm 战场补给";
                }
                case "heaven" -> {
                    level = com.hongmeng.xiuxian.heaven.HeavenRealm.serverLevel(server);
                    if (level == null) {
                        src.sendFailure(Component.literal("\u00a7c天外天维度未加载"));
                        return 0;
                    }
                    var landing = com.hongmeng.xiuxian.heaven.HeavenScenes.landingCenter(level);
                    origin = landing;
                    int[][] offs = {{4, 0, 0}, {-4, 0, 0}};
                    lastOffsets = offs;
                    showDetail = true;
                    placed = com.hongmeng.xiuxian.dungeon.BossRewards.placeChests(
                            level, origin, offs,
                            com.hongmeng.xiuxian.dungeon.BossRewards.LOOT_HEAVEN_SUPPLY, 20261104L);
                    label = "heaven_realm 登天台宝箱";
                }
                case "evil" -> {
                    level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    origin = new net.minecraft.core.BlockPos(0,
                            com.hongmeng.xiuxian.realworld.EvilArena.Y + 1, 0);
                    int[][] offs = {{-6, 0, 0}, {6, 0, 0}};
                    lastOffsets = offs;
                    showDetail = true;
                    placed = com.hongmeng.xiuxian.dungeon.BossRewards.placeChests(
                            level, origin, offs,
                            com.hongmeng.xiuxian.dungeon.BossRewards.LOOT_EVIL, 20261105L);
                    label = "真实世界天穹 恶之宝箱";
                }
                case "native" -> {
                    level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    origin = level.getSharedSpawnPos().above(1);
                    int[][] offs = {{2, 1, 2}};
                    lastOffsets = offs;
                    showDetail = true;
                    placed = com.hongmeng.xiuxian.dungeon.BossRewards.placeChests(
                            level, origin, offs,
                            com.hongmeng.xiuxian.dungeon.BossRewards.LOOT_NATIVE_LAIR, 20261106L);
                    label = "世界出生点 巢穴宝箱（位置验证用）";
                }
                default -> {
                    src.sendFailure(Component.literal("\u00a7c未知场地：" + venue
                            + "（epoch/beast/demon/void/supply/heaven/evil/native）"));
                    return 0;
                }
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.error("[reward] bossreward 失败 {}", venue, t);
            src.sendFailure(Component.literal("\u00a7c放箱失败（详见日志）：" + t));
            return 0;
        }
        final int n = placed;
        final net.minecraft.core.BlockPos o = origin;
        final net.minecraft.server.level.ServerLevel lv = level;
        String detail;
        try {
            // 基准点通常不是箱子位（箱子在相对偏移处）：用第一个偏移位取 LootTable
            net.minecraft.core.BlockPos first = lastOffsets.length > 0
                    ? o.offset(lastOffsets[0][0], lastOffsets[0][1], lastOffsets[0][2]) : o;
            detail = com.hongmeng.xiuxian.dungeon.BossRewards.lootTableAt(lv, first) + " | "
                    + (showDetail ? com.hongmeng.xiuxian.dungeon.BossRewards
                    .describeChests(lv, o, lastOffsets) : "(略)");
        } catch (Throwable t) {
            detail = "err:" + t;
        }
        final String d2 = detail;
        src.sendSuccess(() -> Component.literal(
                "\u00a7a奖励箱就绪：" + label + " \u00a77新增=" + n
                        + " \u00a77基准=" + o
                        + " \u00a77维度=" + lv.dimension().location()
                        + " \u00a77箱子=" + d2), true);
        return placed;
    }
}
