package com.gamearoosdevelopment.realistictrafficcontrol.item;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTrafficLightCard extends Item {
	public ItemTrafficLightCard()
	{
		setRegistryName(ModRealisticTrafficControl.MODID, "traffic_light_card");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".traffic_light_card");
		setMaxStackSize(1);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
		setHasSubtypes(true);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		String unlocalizedName = getUnlocalizedName() + ".";
		switch(stack.getMetadata())
		{
			case 1:
				unlocalizedName += "tier2";
				break;
			case 2:
				unlocalizedName += "tier3";
				break;
			case 3:
				unlocalizedName += "creative";
				break;
			default:
				unlocalizedName += "tier1";
		}
		
		return unlocalizedName;
	}
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab == CreativeTabs.SEARCH || tab == ModRealisticTrafficControl.CREATIVE_TAB)
		{
			for(int i = 0; i < 4; i++)
			{
				items.add(new ItemStack(this, 1, i));
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void initModel()
	{
		for (int i = 0; i < 4; i++)
		{
			String suffix = "_tier" + (i + 1);
			if (i == 3)
			{
				suffix = "_creative";
			}
			
			ModelLoader.setCustomModelResourceLocation(this, i, new ModelResourceLocation(new ResourceLocation(ModRealisticTrafficControl.MODID, getRegistryName().getResourcePath() + suffix), "inventory"));
		}
	}
	
	public static int getMaxTrafficLights(int metadata)
	{
		switch(metadata)
		{
			case 1:
				return Config.trafficLightCardT2Capacity;
			case 2:
				return Config.trafficLightCardT3Capacity;
			case 3:
				return Integer.MAX_VALUE;
			default:
				return Config.trafficLightCardT1Capacity;
		}
	}
	
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
	        EnumFacing facing, float hitX, float hitY, float hitZ) {
	    if (worldIn.isRemote) return EnumActionResult.PASS;

	    Block block = worldIn.getBlockState(pos).getBlock();
	    boolean isTrafficLight = block instanceof BlockBaseTrafficLight;
	    boolean isSensor = block instanceof BlockTrafficSensorLeft
	                    || block instanceof BlockTrafficSensorRight
	                    || block instanceof BlockTrafficSensorStraight;

	    if (!isTrafficLight && !isSensor) return EnumActionResult.PASS;

	    ItemStack heldStack = (hand == EnumHand.MAIN_HAND)
	        ? player.getHeldItemMainhand()
	        : player.getHeldItemOffhand();

	    NBTTagCompound stackTag = heldStack.getTagCompound();
	    if (stackTag == null) {
	        stackTag = new NBTTagCompound();
	        heldStack.setTagCompound(stackTag);
	    }

	    long id = pos.toLong();

	    // --- Sensor Pairing Logic ---
	    if (isSensor) {
	        int maxSensors = ItemTrafficLightCard.getMaxSensors();
	        HashSet<Long> sensors = new HashSet<>();
	        for (int i = 0; i < maxSensors; i++) {
	            if (stackTag.hasKey("sensor" + i)) {
	                sensors.add(stackTag.getLong("sensor" + i));
	            }
	        }

	        if (sensors.contains(id)) {
	            for (int i = 0; i < maxSensors; i++) {
	                if (stackTag.hasKey("sensor" + i) && stackTag.getLong("sensor" + i) == id) {
	                    stackTag.removeTag("sensor" + i);
	                    player.sendMessage(new TextComponentString(String.format("Unpaired sensor at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())));
	                    return EnumActionResult.SUCCESS;
	                }
	            }
	        } else {
	            if (sensors.size() >= maxSensors) {
	                player.sendMessage(new TextComponentString("Card has reached max sensor capacity."));
	                return EnumActionResult.SUCCESS;
	            }
	            for (int i = 0; i < maxSensors; i++) {
	                if (!stackTag.hasKey("sensor" + i)) {
	                    stackTag.setLong("sensor" + i, id);
	                    player.sendMessage(new TextComponentString(String.format("Paired sensor at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())));
	                    return EnumActionResult.SUCCESS;
	                }
	            }
	        }
	    }

	    // --- Traffic Light Pairing Logic ---
	    if (isTrafficLight) {
	        int maxTrafficLights = ItemTrafficLightCard.getMaxTrafficLights(heldStack.getMetadata());

	        // Unpair if already present
	        HashSet<String> keysToRemove = new HashSet<>();
	        for (String key : stackTag.getKeySet()) {
	            if (key.startsWith("light") && stackTag.getLong(key) == id) {
	                keysToRemove.add(key);
	            }
	        }
	        if (!keysToRemove.isEmpty()) {
	            for (String k : keysToRemove) stackTag.removeTag(k);
	            player.sendMessage(new TextComponentString(String.format("Removed traffic light at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())));
	            return EnumActionResult.SUCCESS;
	        }

	        // Add new light
	        int totalLights = 0;
	        HashSet<Integer> usedSlots = new HashSet<>();
	        for (String key : stackTag.getKeySet()) {
	            if (key.startsWith("light")) {
	                totalLights++;
	                try {
	                    usedSlots.add(Integer.parseInt(key.substring(5)));
	                } catch (Exception ignored) {}
	            }
	        }

	        if (totalLights >= maxTrafficLights) {
	            player.sendMessage(new TextComponentString("Card is full! Remove a traffic light or upgrade this card."));
	        } else {
	            int nextSlot = 0;
	            while (usedSlots.contains(nextSlot)) nextSlot++;
	            stackTag.setLong("light" + nextSlot, id);
	            String msg = String.format("Added traffic light at [%d, %d, %d].", pos.getX(), pos.getY(), pos.getZ());
	            if (maxTrafficLights != Integer.MAX_VALUE) {
	                msg += String.format(" %d/%d slots remaining.", maxTrafficLights - totalLights - 1, maxTrafficLights);
	            }
	            player.sendMessage(new TextComponentString(msg));
	        }
	    }

	    return EnumActionResult.SUCCESS;
	}

	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		if (stack.getMetadata() == 3)
		{
			super.addInformation(stack, worldIn, tooltip, flagIn);
			return;
		}
		
		NBTTagCompound tag = stack.getTagCompound();
		int totalUsed = 0;
		if (tag != null)
		{
			for(String item : tag.getKeySet().stream().filter(k -> k.startsWith("light")).collect(Collectors.toList()))
			{
				totalUsed++;
			}
		}
		int maxAvailable = getMaxTrafficLights(stack.getMetadata());
		tooltip.add("" + TextFormatting.DARK_PURPLE + TextFormatting.ITALIC + totalUsed + "/" + maxAvailable + " slots filled");
		super.addInformation(stack, worldIn, tooltip, flagIn);
	}
	
	public static int getMaxSensors() {
	    return 20; // Or make it configurable
	}

}
