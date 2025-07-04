package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

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

public class BlockPole extends Block {
	public static PropertyBool NORTH = PropertyBool.create("north");
	public static PropertyBool WEST = PropertyBool.create("west");
	public static PropertyBool SOUTH = PropertyBool.create("south");
	public static PropertyBool EAST = PropertyBool.create("east");
	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);

	public BlockPole()
	{
		
		super(Material.IRON);
		setRegistryName("pole");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".pole");
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
		setLightOpacity(1);
		setHardness(2f);
	}

	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ROTATION, NORTH, WEST, SOUTH, EAST);
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		int rotation = state.getValue(ROTATION);
		boolean isCardinal = CustomAngleCalculator.isCardinal(rotation);

		boolean north = isCardinal && getStateIsValidForSubModel(rotation, pos, worldIn, EnumFacing.NORTH);
		boolean west = isCardinal && getStateIsValidForSubModel(rotation, pos, worldIn, EnumFacing.WEST);
		boolean south = isCardinal && getStateIsValidForSubModel(rotation, pos, worldIn, EnumFacing.SOUTH);
		boolean east = isCardinal && getStateIsValidForSubModel(rotation, pos, worldIn, EnumFacing.EAST);

		return state
				.withProperty(NORTH, north)
				.withProperty(WEST, west)
				.withProperty(SOUTH, south)
				.withProperty(EAST, east);
	}

	public boolean getStateIsValidForSubModel(int rotation, BlockPos currentPos, IBlockAccess world, EnumFacing facing)
	{
		

		return false;
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
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return new AxisAlignedBB(0.4325, 0, 0.4325, 0.5575, 1, 0.5575);
	}

	@Override
	public boolean causesSuffocation(IBlockState state) {
		return false;
	}

	@Override
	public float getAmbientOcclusionLightValue(IBlockState state)
	{
		return 1;
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
			float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		return getDefaultState().withProperty(ROTATION, CustomAngleCalculator.getRotationForYaw(placer.rotationYaw));
	}
}
