package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

public class ItemStreetSign extends ItemBlock {
	public ItemStreetSign(BlockStreetSign streetSignBlock)
	{
		super(streetSignBlock);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
	}
	
	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.getBlockState(pos).getBlock() == ModBlocks.street_sign)
		{
			StreetSignTileEntity streetSignTileEntity = (StreetSignTileEntity)worldIn.getTileEntity(pos);
			
			StreetSign newSign = new StreetSign();
			newSign.setRotation(CustomAngleCalculator.getRotationForYaw(player.rotationYaw));
			
			if (streetSignTileEntity.addStreetSign(newSign))
			{
				player.openGui(ModRealisticTrafficControl.instance, GuiProxy.GUI_IDs.STREET_SIGN, worldIn, pos.getX(), pos.getY(), pos.getZ());
				(hand == EnumHand.MAIN_HAND ? player.getHeldItemMainhand() : player.getHeldItemOffhand()).shrink(1);
				return EnumActionResult.SUCCESS;
			}
		}
		
		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}
}
