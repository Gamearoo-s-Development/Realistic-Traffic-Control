package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TileEntityWireAnchor;


import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.world.IBlockAccess;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;

public class BlockWireAnchor extends Block implements ITileEntityProvider {
	public static PropertyBool WOOD = PropertyBool.create("wood");

     public BlockWireAnchor() {
         super(Material.IRON); // ✅ This works if you have correct imports in 1.12.2
         setUnlocalizedName(ModRealisticTrafficControl.MODID + ".wire_anchor");
         setRegistryName("wire_anchor");
         setHardness(2.0F);
         setResistance(5.0F);
         setSoundType(SoundType.METAL);
         setLightOpacity(0);
         setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
        // set default state for the WOOD property
        setDefaultState(this.blockState.getBaseState().withProperty(WOOD, Boolean.valueOf(false)));
     }
     
     public void initModel()
 	{
 		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
 	}

     @Override
     public TileEntity createNewTileEntity(World worldIn, int meta) {
         return new TileEntityWireAnchor();
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
     public boolean hasTileEntity(IBlockState state) {
         return true;
     }

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, WOOD);
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		// If the block below is a wood pole, use the wood variant
		if (worldIn != null) {
			IBlockState below = worldIn.getBlockState(pos.down());
			if (below != null && below.getBlock() == ModBlocks.wood_pole) {
				return state.withProperty(WOOD, true);
			}
		}
		return state.withProperty(WOOD, false);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		// map WOOD boolean to meta bit 0
		return state.getValue(WOOD) ? 1 : 0;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		boolean wood = (meta & 1) == 1;
		return getDefaultState().withProperty(WOOD, wood);
	}
 }