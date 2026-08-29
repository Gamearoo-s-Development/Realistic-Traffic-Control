package com.gamearoosdevelopment.realistictrafficcontrol;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for the 1.21.1 port. These replace the 1.12.2 item-NBT / capability system that the
 * traffic-light frames used to carry their configured bulbs.
 *
 * <p>{@link #FRAME_DATA} stores a raw {@link CompoundTag} with the same layout the block entity uses,
 * so a frame item and its placed block entity can round-trip their state directly (mirroring the old
 * {@code getNBTShareTag}/{@code readFromNBT} handshake).
 */
public final class RTCDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ModRealisticTrafficControl.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> FRAME_DATA =
            COMPONENTS.register("frame_data", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    /** Bulb type index (0-34, see {@code EnumTrafficLightBulbTypes}) carried by a traffic-light bulb item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BULB_TYPE =
            COMPONENTS.register("bulb_type", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Control-box / relay pairing position stored on the crossing relay tuner item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> TUNER_PAIRING_DATA =
            COMPONENTS.register("tuner_pairing_data", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    /** CC traffic-light card tier (0-3). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARD_TIER =
            COMPONENTS.register("card_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Paired lights/sensors stored on the CC traffic-light card. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CARD_DATA =
            COMPONENTS.register("card_data", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    /** Dye index (0–15) for concrete barrier items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BARRIER_DYE =
            COMPONENTS.register("barrier_dye", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private RTCDataComponents() {
    }
}
