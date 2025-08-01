package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.ActionResult;


import javax.annotation.Nullable;
import java.util.List;

public class ItemCoverHook extends Item
{
    public ItemCoverHook()
    {
        setRegistryName("cover_hook");
        setUnlocalizedName(ModRealisticTrafficControl.MODID + ".cover_hook");
        setMaxStackSize(1);
        setMaxDamage(40);
        setCreativeTab(ModRealisticTrafficControl.tools_tab);
    }
    
    public void initModel()
    {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }
    
    
//    @Override
//    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
//        ItemStack itemStack = playerIn.getHeldItem(handIn);
//
//        // Get the position the player is looking at
//       
//        
//     // Get the position the player is looking at
//        BlockPos pos = playerIn.getPosition(); // Use player's position instead of rayTrace
//
//        // Check if the block at the position is the one you're interested in
//        Block block = worldIn.getBlockState(pos).getBlock();
//
//        if (block == ModBlocks.traffic_light) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_2) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_4) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_5) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_6) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_7) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//        if (block == ModBlocks.traffic_light_doghouse) {
//            damageHookItem(itemStack, playerIn, 1);
//            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
//        }
//
//        return new ActionResult<>(EnumActionResult.PASS, itemStack);
//    }
//    
//    private void damageHookItem(ItemStack hookItemIn, EntityPlayer playerIn, int amountOfDamage)
//    {
//        if(!playerIn.isCreative())
//        {
//            hookItemIn.damageItem(amountOfDamage, playerIn);
//        }
//    }
}