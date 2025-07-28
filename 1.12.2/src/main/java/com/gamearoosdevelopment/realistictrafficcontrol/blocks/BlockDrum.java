package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

public class BlockDrum extends Block {

	public static final PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);
	
	
	public BlockDrum()
	{
		super(Material.ROCK);
		setRegistryName("drum");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".drum");
		setLightOpacity(1);
		setHardness(1f);
        setHarvestLevel("pickaxe", 0);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
	}
	
	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}
	
	
	
	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}
	
	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		// TODO Auto-generated method stub
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ROTATION);
	}
	
	@Override
	public int getMetaFromState(IBlockState state) {
		return CustomAngleCalculator.rotationToMeta(state.getValue(ROTATION));
	}
	
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(ROTATION, CustomAngleCalculator.metaToRotation(meta));
	}
	
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
	        float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
	    
	    //BlockPos blockBelowPos = pos.down();
	    
	    double heightOfBlockBelow =  getBlockHeight(world, pos);
	    
	    return getDefaultState().withProperty(ROTATION, CustomAngleCalculator.getRotationForYaw(placer.rotationYaw));
	}

	public double getBlockHeight(IBlockAccess world, BlockPos pos) {
		BlockPos blockBelowPos = pos.down();
	    IBlockState blockStateBelow = world.getBlockState(blockBelowPos);
	    Block blockBelow = blockStateBelow.getBlock();

	    // Check if the block below is a slab or a block with variable height
	    if (blockBelow instanceof BlockSlab) {
	        // For slabs, the height is 0.5 blocks
	        return 0.5;
	    } else if (blockBelow instanceof BlockStairs) {
	        // For stairs, the height is determined by the specific block state
	        EnumHalf half = blockStateBelow.getValue(BlockStairs.HALF);
	        return (half == EnumHalf.BOTTOM) ? 1.0 : 0.5;
	    } else {
	        // For other blocks, get the maximum Y-coordinate of the collision bounding box
	        AxisAlignedBB boundingBox = blockBelow.getBoundingBox(blockStateBelow, world, blockBelowPos);
	        return boundingBox.maxY - boundingBox.minY;
	    }
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.875, 0.75);
	}

	@Override
	public float getAmbientOcclusionLightValue(IBlockState state) {
		return 1F;
	}

	@Override
	public boolean causesSuffocation(IBlockState state) {
		return false;
	}
	
}
