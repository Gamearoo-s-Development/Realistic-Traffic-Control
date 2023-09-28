package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import java.util.Arrays;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
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

public class BlockTrafficLight6Cover extends Block {
	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);
	public static PropertyBool VALIDHORIZONTALBAR = PropertyBool.create("validhorizontalbar");
	public static PropertyBool VALIDBACKBAR = PropertyBool.create("validbackbar");
	public BlockTrafficLight6Cover()
	{
		super(Material.IRON);
		setRegistryName("traffic_light6_cover");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".traffic_light6_cover");
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
		setLightOpacity(1);
	}

	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return new AxisAlignedBB(0.375, 0, .375, 0.625, 1, 0.625);
	}
	
	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		boolean hasValidHorizontalBar = false;
		boolean hasValidBackBar = false;
		
		int rotation = state.getValue(ROTATION);
		boolean isCardinal = CustomAngleCalculator.isCardinal(rotation);
		
		if (isCardinal)
		{
			if (CustomAngleCalculator.isNorth(rotation))
			{
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()),
																				EnumFacing.WEST, EnumFacing.EAST) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()),
																				EnumFacing.WEST, EnumFacing.EAST);
				
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()), 
																				EnumFacing.NORTH, EnumFacing.SOUTH);
			}
			else if (CustomAngleCalculator.isSouth(rotation))
			{
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()),
																EnumFacing.WEST, EnumFacing.EAST) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()),
																EnumFacing.WEST, EnumFacing.EAST);
										
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()), 
																	EnumFacing.NORTH, EnumFacing.SOUTH);
			}
			else if (CustomAngleCalculator.isWest(rotation))
			{
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()),
																EnumFacing.NORTH, EnumFacing.SOUTH) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()),
																EnumFacing.NORTH, EnumFacing.SOUTH);
				
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()), 
																	EnumFacing.WEST, EnumFacing.EAST);
			}
			else if (CustomAngleCalculator.isEast(rotation))
			{
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()),
																	EnumFacing.NORTH, EnumFacing.SOUTH) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()),
																	EnumFacing.NORTH, EnumFacing.SOUTH);
				
					hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()), 
																		EnumFacing.WEST, EnumFacing.EAST);
			}
		}
		
		return state.withProperty(VALIDHORIZONTALBAR, hasValidHorizontalBar).withProperty(VALIDBACKBAR, hasValidBackBar);
	}
	
	public static boolean getValidStateForAttachableSubModels(IBlockState state, EnumFacing... validFacings)
	{		
		if (state.getBlock() == ModBlocks.horizontal_pole)
		{
			EnumFacing facing = state.getValue(BlockHorizontalPole.FACING);
			
			if (Arrays.stream(validFacings).anyMatch(f -> f.equals(facing)))
			{
				return true;
			}
		}
		
		if (state.getBlock() == ModBlocks.crossing_gate_pole)
		{
			return true;
		}
		
		if (state.getBlock() instanceof BlockBaseTrafficLight)
		{
			int rotation = state.getValue(ROTATION);
			if (!CustomAngleCalculator.isCardinal(rotation))
			{
				return false;
			}
			
			final EnumFacing facing = CustomAngleCalculator.getFacingFromRotation(rotation);
			
			return Arrays.stream(validFacings).noneMatch(f -> f == facing); // Reverse logic because want traffic lights facing the same way
		}
		
		if (state.getBlock() == ModBlocks.sign)
		{
			int signRotation = state.getValue(BlockSign.ROTATION);
			if (!CustomAngleCalculator.isCardinal(signRotation))
			{
				return false;
			}
			
			final EnumFacing facing = CustomAngleCalculator.getFacingFromRotation(signRotation);
			
			return Arrays.stream(validFacings).noneMatch(vf -> vf.equals(facing));
		}
		
		return false; 
	}

	@Override
	public boolean causesSuffocation(IBlockState state)
	{
		return false;
	}

	@Override
	public float getAmbientOcclusionLightValue(IBlockState state)
	{
		return 1;
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
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ROTATION, VALIDBACKBAR, VALIDHORIZONTALBAR);
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
			float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		return getDefaultState().withProperty(ROTATION, CustomAngleCalculator.getRotationForYaw(placer.rotationYaw));
	}
}
