package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import java.util.Arrays;
import java.util.HashMap;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public abstract class BlockBaseTrafficLight extends Block {

	public static PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);
	public static PropertyBool VALIDHORIZONTALBAR = PropertyBool.create("validhorizontalbar");
	public static PropertyBool VALIDBACKBAR = PropertyBool.create("validbackbar");
	public static PropertyBool COVER = PropertyBool.create("cover");
	public static PropertyBool POLE = PropertyBool.create("pole");
	public BlockBaseTrafficLight(String name)
	{
		super(Material.IRON);
		setRegistryName(name);
		setUnlocalizedName(ModRealisticTrafficControl.MODID + "." + name);
		setHardness(2F);
		setHarvestLevel("pickaxe", 2);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
	}
	
	public abstract void initModel();
	
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
		return new BlockStateContainer(this, ROTATION, VALIDBACKBAR, VALIDHORIZONTALBAR, COVER, POLE);
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
	public boolean causesSuffocation(IBlockState state) {
		return false;
	}
	

	
	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		boolean hasValidHorizontalBar = false;
		boolean autoValidHorizontalBar = true;

		TileEntity tileEntity2 = worldIn.getTileEntity(pos);
		if (tileEntity2 instanceof BaseTrafficLightTileEntity) {
		    BaseTrafficLightTileEntity baseTE = (BaseTrafficLightTileEntity) tileEntity2;

		    if (baseTE.isHorizontalBarSuppressed()) {
		    	autoValidHorizontalBar = false;
		    	
		    }
		}

		boolean hasValidBackBar = false;
		
		int rotation = state.getValue(ROTATION);
		boolean isCardinal = CustomAngleCalculator.isCardinal(rotation);
		
		if (isCardinal)
		{
			if (CustomAngleCalculator.isNorth(rotation))
			{
				if(autoValidHorizontalBar) {
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()),
																				EnumFacing.WEST, EnumFacing.EAST) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()),
																				EnumFacing.WEST, EnumFacing.EAST);
				}
				
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()), 
																				EnumFacing.NORTH, EnumFacing.SOUTH);
			}
			else if (CustomAngleCalculator.isSouth(rotation))
			{
				if(autoValidHorizontalBar) {
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()),
																EnumFacing.WEST, EnumFacing.EAST) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()),
																EnumFacing.WEST, EnumFacing.EAST);
				}
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()), 
																	EnumFacing.NORTH, EnumFacing.SOUTH);
			}
			else if (CustomAngleCalculator.isWest(rotation))
			{
				if(autoValidHorizontalBar) {
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()),
																EnumFacing.NORTH, EnumFacing.SOUTH) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()),
																EnumFacing.NORTH, EnumFacing.SOUTH);
				}
				
				hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.west()), 
																	EnumFacing.WEST, EnumFacing.EAST);
			}
			else if (CustomAngleCalculator.isEast(rotation))
			{
				if(autoValidHorizontalBar) {
				hasValidHorizontalBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.north()),
																	EnumFacing.NORTH, EnumFacing.SOUTH) ||
										getValidStateForAttachableSubModels(worldIn.getBlockState(pos.south()),
																	EnumFacing.NORTH, EnumFacing.SOUTH);
				}
					hasValidBackBar = getValidStateForAttachableSubModels(worldIn.getBlockState(pos.east()), 
																		EnumFacing.WEST, EnumFacing.EAST);
			}
		}
		
		TileEntity tileEntity = worldIn.getTileEntity(pos);

        // Check if the TileEntity exists and is an instance of your custom TileEntity
        if (tileEntity instanceof BaseTrafficLightTileEntity) {
            BaseTrafficLightTileEntity baseTrafficLightTileEntity = (BaseTrafficLightTileEntity) tileEntity;

            // Check if your NBT data (e.g., "cover") is present and set to true
            boolean hasCover = baseTrafficLightTileEntity.hasCover();
            boolean hasPole = baseTrafficLightTileEntity.hasPole();

            // Update the state with the new COVER property
            state = state.withProperty(POLE, hasPole);
            state = state.withProperty(COVER, hasCover);
        }
		if(autoValidHorizontalBar) {
			return state.withProperty(VALIDHORIZONTALBAR, hasValidHorizontalBar).withProperty(VALIDBACKBAR, hasValidBackBar);
		} else {
			return state.withProperty(VALIDHORIZONTALBAR, false).withProperty(VALIDBACKBAR, hasValidBackBar);
		}
		
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
	
	private ItemStack getItemVersionOfBlock(IBlockAccess world, BlockPos pos)
	{
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof BaseTrafficLightTileEntity))
		{
			return new ItemStack(getItemVersionOfBlock());
		}
		
		BaseTrafficLightTileEntity trafficLight = (BaseTrafficLightTileEntity)tileEntity;
		ItemStack frameStack = new ItemStack(getItemVersionOfBlock());
		IItemHandler handler = frameStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		
		NBTTagCompound stackCompound = frameStack.getTagCompound();
		if (stackCompound == null)
		{
			stackCompound = new NBTTagCompound();
			frameStack.setTagCompound(stackCompound);
		}
		
		for(int i = 0; i < getItemVersionOfBlock().getBulbCount(); i++)
		{
			EnumTrafficLightBulbTypes bulbTypeInSlot = trafficLight.getBulbTypeBySlot(i);
			if (bulbTypeInSlot == null)
			{
				handler.insertItem(i, ItemStack.EMPTY, false);
			}
			else
			{
				handler.insertItem(i, new ItemStack(ModItems.traffic_light_bulb, 1, bulbTypeInSlot.getIndex()), false);
			}
			
			stackCompound.setBoolean("always-flash-" + i, trafficLight.getAllowFlashBySlot(i));
		}
		
		frameStack.setTagCompound(frameStack.getItem().getNBTShareTag(frameStack));
		
		return frameStack;
	}
	
	protected abstract BaseItemTrafficLightFrame getItemVersionOfBlock();
	
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
			EntityPlayer player) {
		return getItemVersionOfBlock(world, pos);
	}
	
	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
			int fortune) {
		drops.add(getItemVersionOfBlock(world, pos));
	}
	
	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}
	
	@Override
	public abstract TileEntity createTileEntity(World world, IBlockState state);

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof BaseTrafficLightTileEntity)
		{
			if (((BaseTrafficLightTileEntity)te).anyActive())
			{
				return 0;
			}
		}
		
		return 0;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		if (!(state.getBlock() instanceof BlockBaseTrafficLight))
		{
			return FULL_BLOCK_AABB;
		}
		
		int rotation = state.getValue(ROTATION);
		
		switch(rotation)
		{
			case 0:
				return new AxisAlignedBB(0.1875, -0.3125, 0.4375, 0.8125, 1, 0.75);
			case 8:
				return new AxisAlignedBB(0.1875, -0.3125, 0.25, 0.8125, 1, 0.5625);
			case 4:
				return new AxisAlignedBB(0.25, -0.3125, 0.1875, 0.5625, 1, 0.8125);
			case 12:
				return new AxisAlignedBB(0.4375, -0.3125, 0.1875, 0.75, 1, 0.8125);
			case 1:
			case 15:
			case 7:
			case 9:
			case 3:
			case 5:
			case 11:
			case 13:
				return new AxisAlignedBB(0.375, 0, 0.375, 0.75, 1, 0.75);
			case 2:
			case 6:
			case 10:
			case 14:
				return new AxisAlignedBB(0.2, 0, 0.2, 0.8, 1, 0.8);
		}
		
		return FULL_BLOCK_AABB;
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
			boolean willHarvest) {
		if (willHarvest) return true;
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}
	
	@Override
	public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te,
			ItemStack stack) {
		super.harvestBlock(worldIn, player, pos, state, te, stack);
		worldIn.setBlockToAir(pos);
	}
	
	private void toggleCover(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        boolean hasCover = state.getValue(COVER);

        // Create a new state with the updated COVER property and the existing rotation property
        IBlockState newState = state.withProperty(COVER, !hasCover);
        
        

        // Retrieve the TileEntity at the current position
        TileEntity tileEntity = world.getTileEntity(pos);
        
        

        // Check if the TileEntity exists and is an instance of your custom TileEntity
        if (tileEntity instanceof BaseTrafficLightTileEntity) {
            BaseTrafficLightTileEntity baseTrafficLightTileEntity = (BaseTrafficLightTileEntity) tileEntity;
            
            baseTrafficLightTileEntity.setCover(!hasCover);
            // Save the NBT data before updating the block state
            NBTTagCompound tileEntityNBT = new NBTTagCompound();
            baseTrafficLightTileEntity.writeToNBT(tileEntityNBT);

            // Set the block state with the new state
            world.setBlockState(pos, newState);

            // Notify neighbors of the block update
            world.notifyBlockUpdate(pos, state, newState, 3);

            // Retrieve the TileEntity at the updated position
            TileEntity updatedTileEntity = world.getTileEntity(pos);

            // Check if the TileEntity exists and is an instance of your custom TileEntity
            if (updatedTileEntity instanceof BaseTrafficLightTileEntity) {
                BaseTrafficLightTileEntity updatedTrafficLightTileEntity = (BaseTrafficLightTileEntity) updatedTileEntity;

                // Restore the NBT data after updating the block state
                updatedTrafficLightTileEntity.readFromNBT(tileEntityNBT);

                // Mark the TileEntity as dirty to ensure changes are saved
                updatedTrafficLightTileEntity.markDirty();
                
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
        }
    }

	private void togglePole(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        boolean hasPole = state.getValue(POLE);

        // Create a new state with the updated COVER property and the existing rotation property
        IBlockState newState = state.withProperty(POLE, !hasPole);
        
        

        // Retrieve the TileEntity at the current position
        TileEntity tileEntity = world.getTileEntity(pos);
        
        

        // Check if the TileEntity exists and is an instance of your custom TileEntity
        if (tileEntity instanceof BaseTrafficLightTileEntity) {
            BaseTrafficLightTileEntity baseTrafficLightTileEntity = (BaseTrafficLightTileEntity) tileEntity;
            
            baseTrafficLightTileEntity.setPole(!hasPole);
            // Save the NBT data before updating the block state
            NBTTagCompound tileEntityNBT = new NBTTagCompound();
            baseTrafficLightTileEntity.writeToNBT(tileEntityNBT);

            // Set the block state with the new state
            world.setBlockState(pos, newState);

            // Notify neighbors of the block update
            world.notifyBlockUpdate(pos, state, newState, 3);

            // Retrieve the TileEntity at the updated position
            TileEntity updatedTileEntity = world.getTileEntity(pos);

            // Check if the TileEntity exists and is an instance of your custom TileEntity
            if (updatedTileEntity instanceof BaseTrafficLightTileEntity) {
                BaseTrafficLightTileEntity updatedTrafficLightTileEntity = (BaseTrafficLightTileEntity) updatedTileEntity;

                // Restore the NBT data after updating the block state
                updatedTrafficLightTileEntity.readFromNBT(tileEntityNBT);

                // Mark the TileEntity as dirty to ensure changes are saved
                updatedTrafficLightTileEntity.markDirty();
                
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                world.markBlockRangeForRenderUpdate(pos, pos);
                world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);

            }
        }
    }


	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
	    ItemStack heldItem = playerIn.getHeldItem(hand);

	    // Check if the player used their main hand (right-click) and is holding a cover_hook
	    if (hand == EnumHand.MAIN_HAND && heldItem.getItem() == ModItems.cover_hook) {
	        if (!worldIn.isRemote) {
	        	if(!playerIn.isCreative())
	        	{
	        	  heldItem.damageItem(1, playerIn);
	        	}
	            toggleCover(worldIn, pos); // Toggle the COVER property
	        }
	        return true; // Return true to indicate that the block was activated
	    } else  if (hand == EnumHand.MAIN_HAND && heldItem.getItem() == Items.STICK) {
	        if (!worldIn.isRemote) {
	        	if(!playerIn.isCreative())
	        	{
	        	  heldItem.damageItem(1, playerIn);
	        	}
	            togglePole(worldIn, pos); // Toggle the COVER property
	        }
	        return true; // Return true to indicate that the block was activated
	    } else if (hand == EnumHand.MAIN_HAND && heldItem.getItem() == Items.BLAZE_ROD) {
	        if (!worldIn.isRemote) {
	            TileEntity tileEntity = worldIn.getTileEntity(pos);
	            if (tileEntity instanceof BaseTrafficLightTileEntity) {
	                BaseTrafficLightTileEntity baseTE = (BaseTrafficLightTileEntity) tileEntity;
	                boolean newState = !baseTE.isHorizontalBarSuppressed();
	                baseTE.setHorizontalBarSuppressed(newState);
	                baseTE.markDirty();
	                IBlockState currentState = worldIn.getBlockState(pos);
	                worldIn.notifyBlockUpdate(pos, currentState, currentState, 3);
	            }
	        }
	        return true;
	    }


	    return false; // Return false for other cases (e.g., off-hand interaction or not holding cover_hook)
	}
}
