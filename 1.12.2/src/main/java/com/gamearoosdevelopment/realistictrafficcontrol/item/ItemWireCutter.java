package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TileEntityWireAnchor;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

public class ItemWireCutter extends Item {

    public ItemWireCutter() {
        setUnlocalizedName("wire_cutter");
        setRegistryName("wire_cutter");
        setMaxStackSize(1);
        setMaxDamage(40);
        setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
    }

    // NBT key for storing the first position
    private static final String TAG_FIRST_POS = "FirstWireAnchor";
    
    public void initModel()
    {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        return new ActionResult<>(EnumActionResult.PASS, playerIn.getHeldItem(handIn));
    }

   
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                             EnumHand hand, EnumFacing facing,
                             float hitX, float hitY, float hitZ) {

        TileEntity te = worldIn.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);

        if (!(te instanceof TileEntityWireAnchor)) return EnumActionResult.FAIL;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        if (!nbt.hasKey(TAG_FIRST_POS)) {
            // First anchor selected
            nbt.setLong(TAG_FIRST_POS, pos.toLong());
            if (worldIn.isRemote) {
                player.sendStatusMessage(new TextComponentString("First anchor set."), true);
            }
        } else {
            // Second anchor clicked
            BlockPos firstPos = BlockPos.fromLong(nbt.getLong(TAG_FIRST_POS));
            TileEntity firstTE = worldIn.getTileEntity(firstPos);
            TileEntity secondTE = worldIn.getTileEntity(pos);

            if (firstTE instanceof TileEntityWireAnchor && secondTE instanceof TileEntityWireAnchor) {
                if (player.isSneaking()) {
                    // Sneaking = remove connection
                    ((TileEntityWireAnchor) firstTE).setConnectedTo(null);
                    ((TileEntityWireAnchor) secondTE).setConnectedTo(null);
                    player.sendStatusMessage(new TextComponentString("Connection removed."), true);
                } else {
                    // Connect both ends
                    ((TileEntityWireAnchor) firstTE).setConnectedTo(pos);
                    ((TileEntityWireAnchor) secondTE).setConnectedTo(firstPos);
                    player.sendStatusMessage(new TextComponentString("Anchors connected."), true);
                }
            }

            // Clear stored pos
            nbt.removeTag(TAG_FIRST_POS);
        }

        return EnumActionResult.SUCCESS;
    }

}

