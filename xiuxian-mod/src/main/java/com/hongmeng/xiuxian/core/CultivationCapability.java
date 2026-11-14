package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家修炼数据：灵根、修为、境界、业力、任务进度、门派、副本。
 * 服务端权威，变化后通过 {@link com.hongmeng.xiuxian.network.SyncCultivationPacket} 同步到客户端。
 */
public class CultivationCapability implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = new ResourceLocation(HongmengMod.MOD_ID, "cultivation");
    public static final Capability<CultivationData> CAP =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private CultivationData data = new CultivationData();
    private final LazyOptional<CultivationData> optional = LazyOptional.of(() -> data);

    /** 把能力挂到每个玩家身上。 */
    public static void attach(final AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player player) {
            CultivationCapability cap = new CultivationCapability();
            cap.owner = player.getUUID();
            event.addCapability(ID, cap);
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addGenericListener(
                net.minecraft.world.entity.Entity.class, CultivationCapability::attach);
    }

    /** 能力数据类必须在 RegisterCapabilitiesEvent 里登记。 */
    @Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class CapabilityRegistrar {
        private CapabilityRegistrar() {
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(CultivationData.class);
        }
    }

    public static LazyOptional<CultivationData> getOptional(Player player) {
        if (player == null) {
            return LazyOptional.empty();
        }
        return player.getCapability(CAP);
    }

    /** 安全取数据：万一能力缺失（例如其他模组搞事）也不炸。 */
    public static CultivationData get(Player player) {
        return getOptional(player).orElseGet(CultivationData::new);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    /**
     * 能力的宿主玩家（attach 时记录）。
     *
     * <p>⚠ 为什么需要它：天赋/副业/机制资源存在<b>按 UUID 索引的 Map</b> 里，
     * 而 {@code CultivationData.save()/load()} 拿不到玩家对象。
     * 所以这里把 UUID 记下来，存档时塞进 tag、回读时取出来定位玩家。</p>
     */
    private java.util.UUID owner;

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = data.save();
        if (owner != null) {
            tag.putUUID("TalentOwner", owner);
            // ⚠⚠ <b>必须在这里再写一次天赋数据。</b>
            //
            //   `data.save()` 内部会调 `TalentSave.saveByTag(tag)`，而那个方法
            //   第一行是 `if (!tag.hasUUID("TalentOwner")) return;` ——
            //   可那一刻 owner **还没被放进去**（它在下面这行才 put）。
            //
            //   结果：天赋 / 道途 / 副业熟练度 / 机制资源
            //   **从来没有被写进存档过**，每次重启服务器就全丢。
            //   而读取侧是对的（下面先设 owner 再 load），所以问题被掩盖了：
            //   读的时候永远读不到，却不会报错。
            //
            //   修法：owner 进 tag 之后再补写一次（saveByTag 是幂等的，重复写没问题）。
            com.hongmeng.xiuxian.talent.TalentSave.saveByTag(tag);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.hasUUID("TalentOwner")) {
            this.owner = nbt.getUUID("TalentOwner");
        }
        data.load(nbt);
    }
}
