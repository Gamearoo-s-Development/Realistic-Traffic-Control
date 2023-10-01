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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

public class BlockCone extends Block {
	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);
	public static PropertyInteger HIGHT = PropertyInteger.create("hight", 0, 1000);
	public BlockCone()
	{
		super(Material.ROCK);
		setRegistryName("cone");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".cone");
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
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		double heightOfBlockBelow = getBlockHeight(worldIn, pos);
		
		return state.withProperty(HIGHT, (int)(heightOfBlockBelow * 16));
	}
	

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		return false;
	}

	@Override
	public float getAmbientOcclusionLightValue(IBlockState state) {
		return 1;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		
	
		
		
		return new AxisAlignedBB(0.3,0,0.3,0.7,1,0.7);
	}

	@Override
	public boolean causesSuffocation(IBlockState state) {
		return false;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ROTATION, HIGHT);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(ROTATION);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(ROTATION, meta);
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
	        float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
	    
	    //BlockPos blockBelowPos = pos.down();
	    
	    double heightOfBlockBelow =  getBlockHeight(world, pos);
	    
	    return getDefaultState().withProperty(ROTATION, CustomAngleCalculator.getRotationForYaw(placer.rotationYaw)).withProperty(HIGHT, (int)(heightOfBlockBelow * 16));
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
}
