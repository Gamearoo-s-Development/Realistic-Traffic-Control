package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.UUID;

/** Port of 1.12.2 {@code SignTileEntity}. */
public class SignBlockEntity extends BlockEntity {

    private int typeLegacy = -1;
    private int variantLegacy = -1;
    private UUID id;
    private ArrayList<String> textLines;
    private boolean suppressHorizontalBar;

    public SignBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SIGN.get(), pos, state);
    }

    protected SignBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isHorizontalBarSuppressed() {
        return suppressHorizontalBar;
    }

    public void setHorizontalBarSuppressed(boolean suppress) {
        suppressHorizontalBar = suppress;
        setChanged();
    }

    public void setTypeLegacy(int type) {
        typeLegacy = type;
    }

    public int getTypeLegacy() {
        return typeLegacy;
    }

    public void setVariantLegacy(int variant) {
        variantLegacy = variant;
    }

    public int getVariantLegacy() {
        return variantLegacy;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public UUID getID() {
        return id;
    }

    public String getTextLine(int index) {
        if (textLines == null || index >= textLines.size()) {
            return null;
        }
        return textLines.get(index);
    }

    public void setTextLine(int index, String text) {
        if (textLines == null) {
            textLines = new ArrayList<>();
        }
        while (textLines.size() <= index) {
            textLines.add(null);
        }
        textLines.set(index, text);
    }

    public void clearTextLines() {
        if (textLines != null) {
            textLines.clear();
        }
    }

    public Sign getSign() {
        Sign sign = null;
        if (variantLegacy != -1) {
            sign = ModRealisticTrafficControl.signRepo.getSignByTypeVariant(getSignTypeName(typeLegacy), variantLegacy);
        }
        if (sign == null && id != null) {
            sign = ModRealisticTrafficControl.signRepo.getSignByID(id);
        }
        if (sign == null) {
            return ModRealisticTrafficControl.signRepo.getFallbackSign();
        }
        return sign;
    }

    public static String getSignTypeName(int type) {
        return switch (type) {
            case 0 -> "circle";
            case 1 -> "diamond";
            case 2 -> "misc";
            case 3 -> "rectangle";
            case 4 -> "square";
            case 5 -> "triangle";
            default -> null;
        };
    }

    public static int getSignTypeNumber(String name) {
        return switch (name) {
            case "circle" -> 0;
            case "diamond" -> 1;
            case "misc" -> 2;
            case "rectangle" -> 3;
            case "square" -> 4;
            case "triangle" -> 5;
            default -> -1;
        };
    }

    public void applyUpdate(int type, int variant, UUID signId, ArrayList<String> lines) {
        typeLegacy = type;
        variantLegacy = variant;
        id = signId;
        textLines = lines != null ? new ArrayList<>(lines) : null;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void readSignData(CompoundTag tag) {
        typeLegacy = tag.getInt("type");
        variantLegacy = tag.getInt("variant");
        if (tag.hasUUID("signid")) {
            id = tag.getUUID("signid");
        } else if (tag.contains("signid")) {
            id = NbtUtils.loadUUID(tag.getCompound("signid"));
        }
        if (textLines != null) {
            textLines.clear();
        }
        if (tag.contains("text0")) {
            textLines = new ArrayList<>();
            int i = 0;
            while (tag.contains("text" + i)) {
                textLines.add(tag.getString("text" + i));
                i++;
            }
        }
        suppressHorizontalBar = tag.getBoolean("suppressHorizontalBar");
    }

    private CompoundTag writeSignData(CompoundTag tag) {
        tag.putInt("type", typeLegacy);
        tag.putInt("variant", variantLegacy);
        if (id != null) {
            tag.putUUID("signid", id);
        }
        if (textLines != null) {
            for (int i = 0; i < textLines.size(); i++) {
                tag.putString("text" + i, textLines.get(i) != null ? textLines.get(i) : "");
            }
        }
        tag.putBoolean("suppressHorizontalBar", suppressHorizontalBar);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeSignData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSignData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readSignData(tag);
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
