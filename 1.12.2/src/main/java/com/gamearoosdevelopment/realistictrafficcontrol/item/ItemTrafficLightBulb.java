package com.gamearoosdevelopment.realistictrafficcontrol.item;

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Yellow;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

public class ItemTrafficLightBulb extends Item {
	public ItemTrafficLightBulb()
	{
		setRegistryName("traffic_light_bulb");
		setMaxStackSize(1);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
	}
	
	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.Red.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.Red2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedX.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_x"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowX.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_x"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.Yellow.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.Green.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenDownArrow.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_down"));
		
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.StraightRed.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_straight_red"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.StraightYellow.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_straight_yellow"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.StraightGreen.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_straight_green"));
		
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowLeft.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_left"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowLeft2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_left2"));
		
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowLeft.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_left"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowLeft2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_left2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowLeft3.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_left2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowLeft.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_left"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.Cross.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_cross"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.DontCross.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_dont_cross"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowRight.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_right"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowRight2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_right2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowRight.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_right"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowRight2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_right2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowRight3.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_right2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowRight.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_right"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.NoRightTurn.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_no_right_turn"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.NoLeftTurn.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_no_left_turn"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowUTurn.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_uturn"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowUTurn.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_uturn"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowUTurn2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_uturn2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowUTurn.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_uturn"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.YellowArrowUTurn3.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_yellow_arrow_uturn2"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.RedArrowUTurn2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_red_arrow_uturn2"));
		
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowRight2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_right"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowLeft2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_left"));
		ModelLoader.setCustomModelResourceLocation(this, EnumTrafficLightBulbTypes.GreenArrowUTurn2.getIndex(), new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":traffic_light_bulb_green_arrow_uturn"));
	}
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab != ModRealisticTrafficControl.CREATIVE_TAB) return;
		
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.Red.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.Red2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowX.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedX.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.Yellow.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.Green.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenDownArrow.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.StraightRed.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.StraightYellow.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.StraightGreen.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowLeft.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowLeft2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowLeft.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowLeft2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowLeft3.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowLeft.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowLeft2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.Cross.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.DontCross.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowRight.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowRight2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowRight.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowRight2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowRight3.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowRight.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowRight2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.NoRightTurn.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.NoLeftTurn.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowUTurn.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.RedArrowUTurn2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowUTurn.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowUTurn2.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.YellowArrowUTurn3.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowUTurn.getIndex()));
		items.add(new ItemStack(this, 1, EnumTrafficLightBulbTypes.GreenArrowUTurn2.getIndex()));
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		String unlocalizedName = ModRealisticTrafficControl.MODID + ".traffic_light_bulb.";
		int meta = stack.getMetadata();
		
		if (meta == EnumTrafficLightBulbTypes.Red.getIndex())
		{
			unlocalizedName += "red";
		} else if (meta == EnumTrafficLightBulbTypes.Red2.getIndex())
		{
			unlocalizedName += "red2";
		} else if (meta == EnumTrafficLightBulbTypes.RedX.getIndex())
		{
			unlocalizedName += "red_x";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowX.getIndex())
		{
			unlocalizedName += "yellow_x";
		}
		else if (meta == EnumTrafficLightBulbTypes.Yellow.getIndex())
		{
			unlocalizedName += "yellow";
		}
		else if (meta == EnumTrafficLightBulbTypes.Green.getIndex())
		{
			unlocalizedName += "green";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenDownArrow.getIndex())
		{
			unlocalizedName += "green_down";
		}
		else if (meta == EnumTrafficLightBulbTypes.StraightRed.getIndex())
		{
			unlocalizedName += "straightRed";
		}
		else if (meta == EnumTrafficLightBulbTypes.StraightYellow.getIndex())
		{
			unlocalizedName += "straightYellow";
		}
		else if (meta == EnumTrafficLightBulbTypes.StraightGreen.getIndex())
		{
			unlocalizedName += "straightGreen";
		}
		
		else if (meta == EnumTrafficLightBulbTypes.RedArrowLeft.getIndex())
		{
			unlocalizedName += "redArrowLeft";
		}
		else if (meta == EnumTrafficLightBulbTypes.RedArrowLeft2.getIndex())
		{
			unlocalizedName += "redArrowLeft2";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowLeft.getIndex())
		{
			unlocalizedName += "yellowArrowLeft";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowLeft2.getIndex())
		{
			unlocalizedName += "yellowArrowLeft2";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowLeft3.getIndex())
		{
			unlocalizedName += "yellowArrowLeft3";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowLeft.getIndex())
		{
			unlocalizedName += "greenArrowLeft";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowLeft2.getIndex())
		{
			unlocalizedName += "greenArrowLeft2";
		}
		else if (meta == EnumTrafficLightBulbTypes.Cross.getIndex())
		{
			unlocalizedName += "cross";
		}
		else if (meta == EnumTrafficLightBulbTypes.DontCross.getIndex())
		{
			unlocalizedName += "dont_cross";
		}
		else if (meta == EnumTrafficLightBulbTypes.RedArrowRight.getIndex())
		{
			unlocalizedName += "redArrowRight";
		}
		else if (meta == EnumTrafficLightBulbTypes.RedArrowRight2.getIndex())
		{
			unlocalizedName += "redArrowRight2";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowRight.getIndex())
		{
			unlocalizedName += "yellowArrowRight";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowRight3.getIndex())
		{
			unlocalizedName += "yellowArrowRight3";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowRight2.getIndex())
		{
			unlocalizedName += "yellowArrowRight2";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowRight.getIndex())
		{
			unlocalizedName += "greenArrowRight";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowRight2.getIndex())
		{
			unlocalizedName += "greenArrowRight2";
		}
		else if (meta == EnumTrafficLightBulbTypes.NoRightTurn.getIndex())
		{
			unlocalizedName += "noRightTurn";
		}
		else if (meta == EnumTrafficLightBulbTypes.NoLeftTurn.getIndex())
		{
			unlocalizedName += "noLeftTurn";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowUTurn.getIndex())
		{
			unlocalizedName += "greenArrowUTurn";
		}
		else if (meta == EnumTrafficLightBulbTypes.GreenArrowUTurn2.getIndex())
		{
			unlocalizedName += "greenArrowUTurn2";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowUTurn.getIndex())
		{
			unlocalizedName += "yellowArrowUTurn";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowUTurn2.getIndex())
		{
			unlocalizedName += "yellowArrowUTurn2";
		}
		else if (meta == EnumTrafficLightBulbTypes.YellowArrowUTurn3.getIndex())
		{
			unlocalizedName += "yellowArrowUTurn3";
		}
		else if (meta == EnumTrafficLightBulbTypes.RedArrowUTurn.getIndex())
		{
			unlocalizedName += "redArrowUTurn";
		}
		else if (meta == EnumTrafficLightBulbTypes.RedArrowUTurn2.getIndex())
		{
			unlocalizedName += "redArrowUTurn2";
		}
		else
		{
			unlocalizedName += "unknown";
		}
		
		return unlocalizedName;
	}
}
