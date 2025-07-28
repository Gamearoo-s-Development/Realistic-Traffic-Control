package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BellBaseTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCrossingRelayTuner extends Item {

	public ItemCrossingRelayTuner()
	{
		setRegistryName("crossing_relay_tuner");
		setUnlocalizedName(ModRealisticTrafficControl.MODID + ".crossing_relay_tuner");
		setMaxStackSize(1);
		setCreativeTab(ModRealisticTrafficControl.CREATIVE_TAB);
	}

	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		if(GuiScreen.isShiftKeyDown())
		{
			String info = I18n.format("realistic.tooltip.tuner");
			tooltip.addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(info, Config.tooltipCharWrapLength));
		}
		else
		{
			tooltip.add(TextFormatting.YELLOW + I18n.format("relaistictrafficcontrol.tooltip.help"));
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote)
		{
			return EnumActionResult.SUCCESS;
		}

		TileEntity selectedTE = worldIn.getTileEntity(pos);
		if (!performPairCheck(player, worldIn, selectedTE))
		{
			return EnumActionResult.SUCCESS;
		}

		NBTTagCompound nbt = player.inventory.getCurrentItem().getTagCompound();
		int[] relayPosArray = nbt.getIntArray("pairingpos");
		BlockPos relayPos = new BlockPos(relayPosArray[0], relayPosArray[1], relayPosArray[2]);
		TileEntity pairedTE = worldIn.getTileEntity(relayPos);

		if (selectedTE != null)
		{
			checkUseOnTileEntity(worldIn, selectedTE, pairedTE, player);
		}
		else
		{
			checkUseOnBlock(worldIn, pos, pairedTE, player);
		}

		return EnumActionResult.SUCCESS;
	}

	private void checkUseOnBlock(World world, BlockPos pos, TileEntity te, EntityPlayer player)
	{
		IBlockState state = world.getBlockState(pos);
		
		if (te instanceof TrafficLightControlBoxTileEntity)
		{
			TrafficLightControlBoxTileEntity tlBox = (TrafficLightControlBoxTileEntity)te;

			if (state.getBlock() instanceof BlockTrafficSensorLeft || state.getBlock() instanceof BlockTrafficSensorStraight || state.getBlock() instanceof BlockTrafficSensorRight)
			{
				if (tlBox.addOrRemoveSensor(pos))
				{
					player.sendMessage(new TextComponentString("Paired sensor to Traffic Light Control Box"));
				}
				else
				{
					player.sendMessage(new TextComponentString("Unpaired sensor from Traffic Light Control Box"));
				}
			}
		}
	}

	private boolean performPairCheck(EntityPlayer player, World world, TileEntity te)
	{
		NBTTagCompound nbt = player.inventory.getCurrentItem().getTagCompound();

		if (nbt == null || !nbt.hasKey("pairingpos"))
		{
			if (te == null || (!(te instanceof TrafficLightControlBoxTileEntity)))
			{
				return false;
			}
			BlockPos relayPos = null;

			if (nbt == null)
			{
				nbt = new NBTTagCompound();
			}

			String typeOfPairing = "";
			

			if (te instanceof TrafficLightControlBoxTileEntity)
			{
				TrafficLightControlBoxTileEntity controlBox = (TrafficLightControlBoxTileEntity)te;
				relayPos = controlBox.getPos();
				addTileEntityPosToNBT(nbt, "pairingpos", controlBox);

				typeOfPairing = "Traffic Light Control Box";
			}

			player.inventory.getCurrentItem().setTagCompound(nbt);
			player.sendMessage(new TextComponentString("Started pairing with " + typeOfPairing + " at "
					+ relayPos.getX() + ", "
					+ relayPos.getY() + ", "
					+ relayPos.getZ()));

		}
		else
		{
			int[] pairingpos = nbt.getIntArray("pairingpos");
			if (te != null && ( te instanceof TrafficLightControlBoxTileEntity))
			{
				BlockPos relayPos = null;
				String typeOfPairing = "";
				

				if (te instanceof TrafficLightControlBoxTileEntity)
				{
					TrafficLightControlBoxTileEntity controlBoxTE = (TrafficLightControlBoxTileEntity)te;
					relayPos = controlBoxTE.getPos();

					typeOfPairing = "Traffic Light Control Box";
				}

				nbt.removeTag("pairingpos");
				player.sendMessage(new TextComponentString("Stopped pairing with " + typeOfPairing + " at "
						+ pairingpos[0] + ", "
						+ pairingpos[1] + ", "
						+ pairingpos[2]));

				if (pairingpos[0] ==  relayPos.getX() && pairingpos[1] == relayPos.getY() && pairingpos[2] == relayPos.getZ())
				{
					player.inventory.getCurrentItem().setTagCompound(nbt);
					return false;
				}

				
				

				player.inventory.getCurrentItem().setTagCompound(nbt);

				pairingpos = nbt.getIntArray("pairingpos");

				player.sendMessage(new TextComponentString("Started pairing with " + typeOfPairing + " at "
						+ pairingpos[0] + ", "
						+ pairingpos[1] + ", "
						+ pairingpos[2]));
			}
			else if (te != null)
			{
				BlockPos pos = new BlockPos(pairingpos[0], pairingpos[1], pairingpos[2]);
				TileEntity teAtPairingPos = world.getTileEntity(pos);

				
			}
		}

		return true;
	}

	private void addTileEntityPosToNBT(NBTTagCompound nbt, String key, TileEntity te)
	{
		BlockPos tePos = te.getPos();
		int[] pos = new int[] { tePos.getX(), tePos.getY(), tePos.getZ() };
		nbt.setIntArray(key, pos);
	}

	private void checkUseOnTileEntity(World world, TileEntity te, TileEntity pairedTE, EntityPlayer player)
	{
		

		if (pairedTE instanceof TrafficLightControlBoxTileEntity)
		{
			TrafficLightControlBoxTileEntity controlBox = (TrafficLightControlBoxTileEntity)pairedTE;
			if (te instanceof BaseTrafficLightTileEntity)
			{
				IBlockState state = world.getBlockState(te.getPos());

				if (state.getBlock() instanceof BlockBaseTrafficLight)
				{
					int rotation = state.getValue(BlockBaseTrafficLight.ROTATION);

					boolean operationResult = false;
					if (CustomAngleCalculator.isEast(rotation) || CustomAngleCalculator.isWest(rotation))
					{
						operationResult = controlBox.addOrRemoveWestEastTrafficLight(te.getPos());
					}
					else
					{
						operationResult = controlBox.addOrRemoveNorthSouthTrafficLight(te.getPos());
					}

					if (operationResult)
					{
						player.sendMessage(new TextComponentString("Paired Traffic Light to Traffic Light Control Box"));
					}
					else
					{
						player.sendMessage(new TextComponentString("Unpaired Traffic Light to Traffic Light Control Box"));
					}
				}
			}

			if (te instanceof PedestrianButtonTileEntity)
			{
				IBlockState state = world.getBlockState(te.getPos());

				if (state.getBlock() instanceof BlockPedestrianButton)
				{
					int rotation = state.getValue(BlockPedestrianButton.ROTATION);

					boolean operationResult = false;
					if (!CustomAngleCalculator.isNorthSouth(rotation))
					{
						operationResult = controlBox.addOrRemoveNorthSouthPedButton(te.getPos());
					}
					else
					{
						operationResult = controlBox.addOrRemoveWestEastPedButton(te.getPos());
					}

					PedestrianButtonTileEntity pedTE = (PedestrianButtonTileEntity)te;
					if (operationResult)
					{
						pedTE.addPairedBox(controlBox.getPos());
						player.sendMessage(new TextComponentString("Paired Pedestrian Button to Traffic Light Control Box"));
					}
					else
					{
						pedTE.removePairedBox(controlBox.getPos());
						player.sendMessage(new TextComponentString("Unpaired Pedestrian Button to Traffic Light Control Box"));
					}
				}
			}
		}
	}
}