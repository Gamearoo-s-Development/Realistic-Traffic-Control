package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.HashSet;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code PedestrianButtonTileEntity}. Stores the set of control boxes this button is
 * paired with; pressing it queues a pedestrian phase on each paired {@link TrafficLightControlBoxBlockEntity}.
 */
public class PedestrianButtonBlockEntity extends BlockEntity {

    private HashSet<BlockPos> pairedBoxes = new HashSet<>();

    public PedestrianButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTRIAN_BUTTON.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.loadAdditional(compound, registries);
        pairedBoxes = new HashSet<>();
        ListTag pairList = compound.getList("pairedBoxes", Tag.TAG_LONG);
        for (Tag tagBase : pairList) {
            pairedBoxes.add(BlockPos.of(((LongTag) tagBase).getAsLong()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        ListTag pairList = new ListTag();
        for (BlockPos pos : pairedBoxes) {
            pairList.add(LongTag.valueOf(pos.asLong()));
        }
        compound.put("pairedBoxes", pairList);
    }

    public void addPairedBox(BlockPos pos) {
        pairedBoxes.add(pos);
        setChanged();
    }

    public void removePairedBox(BlockPos pos) {
        pairedBoxes.remove(pos);
        setChanged();
    }

    public ImmutableList<BlockPos> getPairedBoxes() {
        return ImmutableList.copyOf(pairedBoxes);
    }

    public void onBreak(Level level, boolean isNorthSouth) {
        for (BlockPos pos : pairedBoxes) {
            BlockEntity prelimCtrlr = level.getBlockEntity(pos);
            if (!(prelimCtrlr instanceof TrafficLightControlBoxBlockEntity ctrlr)) {
                continue;
            }
            if (isNorthSouth) {
                ctrlr.addOrRemoveWestEastPedButton(getBlockPos());
            } else {
                ctrlr.addOrRemoveNorthSouthPedButton(getBlockPos());
            }
        }
    }
}
