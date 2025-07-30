package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;


import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierTileEntity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch(ID)
		{
			case GUI_IDs.TRAFFIC_LIGHT_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_frame)
				{
					return new TrafficLightFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
				
			case GUI_IDs.TRAFFIC_LIGHT_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_hoz_frame)
				{
					return new TrafficLightHozFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break; 
			
			case GUI_IDs.TRAFFIC_LIGHT_5_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_5_frame)
				{
					return new TrafficLight5FrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_5_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_5_hoz_frame)
				{
					return new TrafficLight5HozFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_DOGHOUSE_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_doghouse_frame)
				{
					return new TrafficLightDoghouseFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_1_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_1_frame)
				{
					return new TrafficLight1FrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_2_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_2_frame)
				{
					return new TrafficLight2FrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_2_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_2_hoz_frame)
				{
					return new TrafficLight2HozFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_4_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_4_frame)
				{
					return new TrafficLight4FrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_4_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_4_hoz_frame)
				{
					return new TrafficLight4HozFrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
				case GUI_IDs.TRAFFIC_LIGHT_6_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_6_frame)
				{
					return new TrafficLight6FrameContainer(player.inventory, player.getHeldItemMainhand());
				}
				break;
				case GUI_IDs.TRAFFIC_LIGHT_8_FRAME:
					if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_8_frame)
					{
						return new TrafficLight8FrameContainer(player.inventory, player.getHeldItemMainhand());
					}
					break;
				case GUI_IDs.TRAFFIC_LIGHT_7_FRAME:
					if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_7_frame)
					{
						return new TrafficLight7FrameContainer(player.inventory, player.getHeldItemMainhand());
					}
					break;
				
		}

		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {

		switch(ID)
		{
			case GUI_IDs.SIGN:
				BlockPos pos = new BlockPos(x, y, z);
				TileEntity te = world.getTileEntity(pos);
				if (te instanceof SignTileEntity)
				{
					SignTileEntity signTE = (SignTileEntity)te;
					return new SignGui(signTE);
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_frame)
				{
					return new TrafficLightFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_hoz_frame)
				{
					return new TrafficLightHozFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_CONTROL_BOX:
				BlockPos preControlBoxPos = new BlockPos(x, y, z);
				TileEntity preControlBoxTE = world.getTileEntity(preControlBoxPos);
				if (preControlBoxTE instanceof TrafficLightControlBoxTileEntity)
				{
					TrafficLightControlBoxTileEntity controlBoxTE = (TrafficLightControlBoxTileEntity)preControlBoxTE;
					return new TrafficLightControlBoxGui(controlBoxTE);
				}
				break;
			case GUI_IDs.TYPE_3_BARRIER:
				BlockPos type3BarrierPos = new BlockPos(x, y, z);
				TileEntity type3BarrierTE = world.getTileEntity(type3BarrierPos);
				if (type3BarrierTE instanceof Type3BarrierTileEntity)
				{
					Type3BarrierTileEntity type3Barrier = (Type3BarrierTileEntity)type3BarrierTE;
					return new GuiType3Barrier(type3Barrier);
				}
				break;
			
				
			case GUI_IDs.STREET_SIGN:
				BlockPos preStreetSignPos = new BlockPos(x, y, z);
				TileEntity preStreetSignTE = world.getTileEntity(preStreetSignPos);
				if (preStreetSignTE instanceof StreetSignTileEntity)
				{
					StreetSignTileEntity streetSignTileEntity = (StreetSignTileEntity)preStreetSignTE;
					return new StreetSignGui(streetSignTileEntity);
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_5_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_5_frame)
				{
					return new TrafficLight5FrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_5_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_5_hoz_frame)
				{
					return new TrafficLight5HozFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_DOGHOUSE_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_doghouse_frame)
				{
					return new TrafficLightDoghouseFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_1_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_1_frame)
				{
					return new TrafficLight1FrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_2_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_2_frame)
				{
					return new TrafficLight2FrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_2_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_2_hoz_frame)
				{
					return new TrafficLight2HozFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_4_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_4_frame)
				{
					return new TrafficLight4FrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
			case GUI_IDs.TRAFFIC_LIGHT_4_HOZ_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_4_hoz_frame)
				{
					return new TrafficLight4HozFrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
				case GUI_IDs.TRAFFIC_LIGHT_6_FRAME:
				if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_6_frame)
				{
					return new TrafficLight6FrameGui(player.inventory, player.getHeldItemMainhand());
				}
				break;
				case GUI_IDs.TRAFFIC_LIGHT_8_FRAME:
					if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_8_frame)
					{
						return new TrafficLight8FrameGui(player.inventory, player.getHeldItemMainhand());
					}
					break;
				case GUI_IDs.TRAFFIC_LIGHT_7_FRAME:
					if (player.getHeldItemMainhand().getItem() == ModItems.traffic_light_7_frame)
					{
						return new TrafficLight7FrameGui(player.inventory, player.getHeldItemMainhand());
					}
					break;
				
				
					
				
		}

		return null;
	}

	public static class GUI_IDs
	{
		public static final int SIGN = 1;
		public static final int TRAFFIC_LIGHT_FRAME = 2;
		public static final int TRAFFIC_LIGHT_CONTROL_BOX = 3;
		public static final int TYPE_3_BARRIER = 4;
		public static final int STREET_SIGN = 5;
		public static final int TRAFFIC_LIGHT_5_FRAME = 6;
		public static final int TRAFFIC_LIGHT_DOGHOUSE_FRAME = 7;
		public static final int TRAFFIC_LIGHT_1_FRAME = 8;
		public static final int TRAFFIC_LIGHT_2_FRAME = 9;
		public static final int TRAFFIC_LIGHT_4_FRAME = 10;
		public static final int TRAFFIC_LIGHT_6_FRAME = 11;
		public static final int TRAFFIC_LIGHT_7_FRAME = 12;
		public static final int TRAFFIC_LIGHT_HOZ_FRAME = 13;
		public static final int TRAFFIC_LIGHT_2_HOZ_FRAME = 14;
		public static final int TRAFFIC_LIGHT_4_HOZ_FRAME = 15;
		public static final int TRAFFIC_LIGHT_5_HOZ_FRAME = 16;
		public static final int TRAFFIC_LIGHT_8_FRAME = 17;
		
	}
}
