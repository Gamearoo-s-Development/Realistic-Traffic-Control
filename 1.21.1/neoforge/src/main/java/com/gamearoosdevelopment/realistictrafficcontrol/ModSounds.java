package com.gamearoosdevelopment.realistictrafficcontrol;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound registry for the 1.21.1 port. Replaces {@code ModSounds.initSounds()} +
 * {@code CommonProxy.registerSounds}. The 1.12.2 registry names are preserved so existing
 * {@code sounds.json} keys and worlds keep working. Actual .ogg files are not committed in the repo and
 * must be supplied under {@code assets/realistictrafficcontrol/sounds/}.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ModRealisticTrafficControl.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GATE = register("gate");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAFETRAN_TYPE_3 = register("safetran_type_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAFETRAN_MECHANICAL = register("safetran_mechanical");
    public static final DeferredHolder<SoundEvent, SoundEvent> WCH = register("wch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PED_BUTTON = register("ped_button");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIGWAG = register("wigwag");
    public static final DeferredHolder<SoundEvent, SoundEvent> WCH_MECHANICAL_BELL = register("wch_mechanical_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREWDRIVER = register("screwdriver");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEARDROP_BELL = register("teardrop_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAFETRAN_TYPE_1 = register("safetran_type_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAYSIDE_HORN = register("wayside_horn");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {
    }
}
