package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TileEntityWireAnchor;


import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.block.ITileEntityProvider;

public class BlockWireAnchor extends Block implements ITileEntityProvider {

    public BlockWireAnchor() {
        super(Material.IRON); // ✅ This works if you have correct imports in 1.12.2
        setUnlocalizedName(ModRealisticTrafficControl.MODID + ".wire_anchor");
        setRegistryName("wire_anchor");
        setHardness(2.0F);
        setResistance(5.0F);
        setSoundType(SoundType.METAL);
        setLightOpacity(0);
        setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
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
}


