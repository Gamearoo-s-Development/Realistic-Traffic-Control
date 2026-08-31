package com.gamearoosdevelopment.realistictrafficcontrol.oc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightCard;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.gamearoosdevelopment.realistictrafficcontrol.util.TrafficLightOcApproachHelper;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.DriverItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import scala.collection.concurrent.Debug;

public class TrafficLightCardDriver extends DriverItem {

	public TrafficLightCardDriver()
	{
		super(new ItemStack(ModItems.traffic_light_card, 1, 0),
			  new ItemStack(ModItems.traffic_light_card, 1, 1),
			  new ItemStack(ModItems.traffic_light_card, 1, 2),
			  new ItemStack(ModItems.traffic_light_card, 1, 3));
	}
	
	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
		return new CardEnvironment(stack, host);
	}
	
	

	@Override
	public String slot(ItemStack stack) {
		return Slot.Card;
	}
	
	@Override
	public int tier(ItemStack stack) {
		switch(stack.getMetadata())
		{
			case 0:
				return 0;
			case 1:
				return 1;
			default:
				return 2;
		}
	}
	
	

	public class CardEnvironment extends AbstractManagedEnvironment
	{
		private final ItemStack card;
		private final EnvironmentHost host;
		public CardEnvironment(ItemStack card, EnvironmentHost host)
		{
			this.card = card;
			this.host = host;
			setNode(Network.newNode(this, Visibility.Neighbors).withComponent("traffic_light_card").withConnector(300).create());
		}
		
		private boolean cardContainsSensor(BlockPos pos) {
		    if (card == null || card.getTagCompound() == null) return false;
		    long id = pos.toLong();
		    NBTTagCompound tag = card.getTagCompound();
		    for (String key : tag.getKeySet()) {
		        if (key.startsWith("sensor") && tag.getLong(key) == id) {
		            return true;
		        }
		    }
		    return false;
		}

		
		@Callback(doc = "pairSensor(x:int, y:int, z:int):boolean OR pairSensor(id:long):boolean -- Pairs or unpairs a sensor to this card")
		public Object[] pairSensor(Context c, Arguments args) throws Exception {
		    BlockPos pos = getBlockPosFromArgs(args);
		    NBTTagCompound tag = card.getTagCompound();
		    if (tag == null) {
		        tag = new NBTTagCompound();
		        card.setTagCompound(tag);
		    }

		    int maxSensors = 8; // Limit number of sensors
		    ArrayList<Long> sensors = new ArrayList<>();
		    for (int i = 0; i < maxSensors; i++) {
		        if (tag.hasKey("sensor" + i)) {
		            sensors.add(tag.getLong("sensor" + i));
		        }
		    }

		    long id = pos.toLong();
		    if (sensors.contains(id)) {
		        // Unpair
		        for (int i = 0; i < maxSensors; i++) {
		            if (tag.hasKey("sensor" + i) && tag.getLong("sensor" + i) == id) {
		                tag.removeTag("sensor" + i);
		                return new Object[] { false, "Unpaired" };
		            }
		        }
		    } else {
		        // Pair
		        if (sensors.size() >= maxSensors) return new Object[] { false, "Max sensors reached" };
		        tag.setLong("sensor" + sensors.size(), id);
		        return new Object[] { true, "Paired" };
		    }

		    return new Object[] { false, "Unexpected" };
		}

		@Callback(doc = "pairPedButton(x:int, y:int, z:int):boolean, string OR pairPedButton(id:long):boolean, string -- Pairs or unpairs a pedestrian button to this card")
		public Object[] pairPedButton(Context c, Arguments args) throws Exception {
			BlockPos pos = getBlockPosFromArgs(args);
			NBTTagCompound tag = card.getTagCompound();
			if (tag == null) {
				tag = new NBTTagCompound();
				card.setTagCompound(tag);
			}

			int max = 8;
			ArrayList<Long> paired = new ArrayList<>();
			for (int i = 0; i < max; i++) {
				if (tag.hasKey("pedButton" + i)) {
					paired.add(tag.getLong("pedButton" + i));
				}
			}

			long id = pos.toLong();
			if (paired.contains(id)) {
				for (int i = 0; i < max; i++) {
					if (tag.hasKey("pedButton" + i) && tag.getLong("pedButton" + i) == id) {
						tag.removeTag("pedButton" + i);
						return new Object[] { false, "Unpaired" };
					}
				}
				return new Object[] { false, "Unexpected" };
			}

			if (paired.size() >= max) {
				return new Object[] { false, "Max ped buttons reached" };
			}
			tag.setLong("pedButton" + paired.size(), id);
			return new Object[] { true, "Paired" };
		}

		@Callback(doc = "listPedButtons():array -- Returns a list of pedestrian button positions paired to this card")
		public Object[] listPedButtons(Context c, Arguments args) {
			NBTTagCompound tag = card.getTagCompound();
			if (tag == null) return new Object[] { new ArrayList<Integer[]>() };

			ArrayList<Integer[]> buttons = new ArrayList<>();
			for (int i = 0; i < 8; i++) {
				if (tag.hasKey("pedButton" + i)) {
					BlockPos p = BlockPos.fromLong(tag.getLong("pedButton" + i));
					buttons.add(new Integer[] { p.getX(), p.getY(), p.getZ() });
				}
			}

			return new Object[] { buttons };
		}

		@Callback(doc = "pressPedButton(x:int, y:int, z:int):boolean, string OR pressPedButton(id:long):boolean, string -- Simulates pressing a pedestrian button")
		public Object[] pressPedButton(Context c, Arguments args) throws Exception {
			BlockPos pos = getBlockPosFromArgs(args);

			TileEntity te = host.world().getTileEntity(pos);
			if (!(te instanceof PedestrianButtonTileEntity) || !(host.world().getBlockState(pos).getBlock() instanceof BlockPedestrianButton)) {
				return new Object[] { false, "No pedestrian button at given position" };
			}

			int rotation = host.world().getBlockState(pos).getValue(BlockPedestrianButton.ROTATION);
			PedestrianButtonTileEntity pedTE = (PedestrianButtonTileEntity) te;
			int queued = 0;

			for (BlockPos controller : new ArrayList<>(pedTE.getPairedBoxes())) {
				TileEntity ctrlTE = host.world().getTileEntity(controller);
				if (!(ctrlTE instanceof TrafficLightControlBoxTileEntity)) {
					pedTE.removePairedBox(controller);
					continue;
				}

				TrafficLightControlBoxTileEntity ctrlr = (TrafficLightControlBoxTileEntity) ctrlTE;
				if (CustomAngleCalculator.isNorthSouth(rotation)) {
					ctrlr.getAutomator().setWestEastPedQueued(true);
				} else {
					ctrlr.getAutomator().setNorthSouthPedQueued(true);
				}

				ctrlr.markDirty();
				host.world().notifyBlockUpdate(controller, host.world().getBlockState(controller), host.world().getBlockState(controller), 3);
				queued++;
			}

			return new Object[] { true, "Queued for " + queued + " controller(s)" };
		}

		
		@Callback(direct = true, doc = "listBlockPos():array -- Retrieves a list of block positions currently in use by the card")
		public Object[] listBlockPos(Context c, Arguments args)
		{
			NBTTagCompound cardTag = card.getTagCompound();
			if (cardTag == null)
			{
				return new Object[] { new ArrayList<Integer[]>() };
			}
			
			int maxTrafficLights = ItemTrafficLightCard.getMaxTrafficLights(card.getMetadata());
			ArrayList<Integer[]> blockPositions = new ArrayList<>();
			
			for(int i = 0; i < maxTrafficLights; i++)
			{
				if (cardTag.hasKey("light" + i) && cardTag.getLong("light" + i) != 0)
				{
					BlockPos position = BlockPos.fromLong(cardTag.getLong("light" + i));
					blockPositions.add(new Integer[] { position.getX(), position.getY(), position.getZ() });
				}
			}
			
			return new Object[] { blockPositions };
		}

		@Callback(doc = "linkDigitalSign(controllerX:int, controllerY:int, controllerZ:int, signX:int, signY:int, signZ:int):boolean, string -- Links a digital sign to its controller")
		public Object[] linkDigitalSign(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			BlockPos signPos = getBlockPosFromArgsAt(args, 3);
			TileEntity controller = host.world().getTileEntity(controllerPos);
			TileEntity sign = host.world().getTileEntity(signPos);
			if (!(controller instanceof DigitalSignControllerTileEntity) || !(sign instanceof DigitalSignTileEntity)) {
				return new Object[] { false, "Invalid digital sign controller or sign position" };
			}
			return new Object[] { ((DigitalSignControllerTileEntity) controller).linkSign(signPos), "Link updated" };
		}

		@Callback(doc = "setDigitalSign(controllerX:int, controllerY:int, controllerZ:int, signId:string):boolean, string -- Displays a sign-pack UUID on linked digital signs")
		public Object[] setDigitalSign(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				return new Object[] { false, "No digital sign controller at position" };
			}
			UUID signId = UUID.fromString(args.checkString(3));
			int updated = ((DigitalSignControllerTileEntity) tile).setSelectedSign(signId);
			return new Object[] { true, updated };
		}

		@Callback(doc = "addDigitalSignRotation(controllerX:int, controllerY:int, controllerZ:int, signId:string, gameTime:string?):boolean, string -- Adds or updates a sign; gameTime is that sign's HH:MM activation time")
		public Object[] addDigitalSignRotation(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				return new Object[] { false, "No digital sign controller at position" };
			}
			UUID signId = UUID.fromString(args.checkString(3));
			String gameTime = args.count() >= 5 ? args.checkString(4) : null;
			if (gameTime != null && !gameTime.trim().isEmpty() && DisplaySchedule.parseGameTime(gameTime) < 0) {
				return new Object[] { false, "Invalid game time" };
			}
			DigitalSignControllerTileEntity controller = (DigitalSignControllerTileEntity) tile;
			boolean existed = controller.getRotationSigns().contains(signId);
			boolean added = controller.addRotationSign(signId);
			boolean timed = gameTime == null || controller.setRotationSignTime(signId, gameTime);
			boolean success = timed && (added || existed);
			return new Object[] { success, success ? (added ? "Rotation sign added" : "Rotation sign time updated")
					: "Invalid time or rotation is full" };
		}

		@Callback(doc = "setDigitalSignRotationTime(controllerX:int, controllerY:int, controllerZ:int, signId:string, gameTime:string):boolean, string -- Sets one rotation sign's HH:MM activation time")
		public Object[] setDigitalSignRotationTime(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				return new Object[] { false, "No digital sign controller at position" };
			}
			UUID signId = UUID.fromString(args.checkString(3));
			boolean updated = ((DigitalSignControllerTileEntity) tile).setRotationSignTime(signId, args.checkString(4));
			return new Object[] { updated, updated ? "Rotation sign time updated" : "Sign is not in rotation or time is invalid" };
		}

		@Callback(doc = "clearDigitalSignRotation(controllerX:int, controllerY:int, controllerZ:int):boolean -- Clears the controller sign rotation")
		public Object[] clearDigitalSignRotation(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				return new Object[] { false, "No digital sign controller at position" };
			}
			((DigitalSignControllerTileEntity) tile).clearRotationSigns();
			return new Object[] { true };
		}

		@Callback(doc = "setDigitalSignSchedule(controllerX:int, controllerY:int, controllerZ:int, mode:string, amount:int?):boolean, string -- Interval modes use playlist order; GAME_TIMES uses each sign's assigned time")
		public Object[] setDigitalSignSchedule(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				return new Object[] { false, "No digital sign controller at position" };
			}
			DigitalSignControllerTileEntity controller = (DigitalSignControllerTileEntity) tile;
			DisplaySchedule.Mode mode = DisplaySchedule.Mode.fromName(args.checkString(3));
			controller.setScheduleMode(mode);
			if (args.count() >= 5 && args.isInteger(4)) controller.setScheduleIntervalAmount(args.checkInteger(4));
			return new Object[] { true, mode.name() };
		}

		@Callback(doc = "linkMessageBoard(controllerX:int, controllerY:int, controllerZ:int, boardX:int, boardY:int, boardZ:int):boolean, string -- Links a message board to its controller")
		public Object[] linkMessageBoard(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			BlockPos boardPos = getBlockPosFromArgsAt(args, 3);
			TileEntity controller = host.world().getTileEntity(controllerPos);
			TileEntity board = host.world().getTileEntity(boardPos);
			if (!(controller instanceof MessageBoardControllerTileEntity) || !(board instanceof MessageBoardTileEntity)) {
				return new Object[] { false, "Invalid message board controller or board position" };
			}
			return new Object[] { ((MessageBoardControllerTileEntity) controller).linkBoard(boardPos), "Link updated" };
		}

		@Callback(doc = "setMessageBoardText(controllerX:int, controllerY:int, controllerZ:int, line:int, text:string):boolean, string -- Sets one line on all linked message boards")
		public Object[] setMessageBoardText(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			int line = args.checkInteger(3);
			if (line < 0 || line >= MessageBoardTileEntity.MAX_LINES) {
				return new Object[] { false, "Line must be between 0 and " + (MessageBoardTileEntity.MAX_LINES - 1) };
			}
			int updated = ((MessageBoardControllerTileEntity) tile).setText(line, args.checkString(4));
			return new Object[] { true, updated };
		}

		@Callback(doc = "clearMessageBoards(controllerX:int, controllerY:int, controllerZ:int):boolean, string -- Clears all linked message boards")
		public Object[] clearMessageBoards(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).clearBoards() };
		}

		@Callback(doc = "setMessageBoardBrightness(controllerX:int, controllerY:int, controllerZ:int, brightness:number):boolean, string -- Sets linked message board brightness from 0.1 to 1")
		public Object[] setMessageBoardBrightness(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).setBrightness((float) args.checkDouble(3)) };
		}

		@Callback(doc = "setMessageBoardTextScale(controllerX:int, controllerY:int, controllerZ:int, scale:number):boolean, string -- Sets text scale from 0.5 to 1.5")
		public Object[] setMessageBoardTextScale(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).setTextScale((float) args.checkDouble(3)) };
		}

		@Callback(doc = "setMessageBoardFontStyle(controllerX:int, controllerY:int, controllerZ:int, style:string):boolean, string -- Styles: REGULAR, BOLD, ITALIC, BOLD_ITALIC")
		public Object[] setMessageBoardFontStyle(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			String requested = args.checkString(3).trim().toUpperCase().replace(' ', '_');
			MessageBoardTileEntity.FontStyle style;
			try {
				style = MessageBoardTileEntity.FontStyle.valueOf(requested);
			} catch (IllegalArgumentException ex) {
				return new Object[] { false, "Unknown font style: " + requested };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).setFontStyle(style) };
		}

		@Callback(doc = "setMessageBoardMode(controllerX:int, controllerY:int, controllerZ:int, mode:string):boolean, string -- Sets TEXT, ARROW_LEFT, ARROW_RIGHT, CAUTION, or OFF")
		public Object[] setMessageBoardMode(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			String requested = args.checkString(3).trim().toUpperCase();
			if ("ARROW_MERGE_LEFT".equals(requested)) requested = "ARROW_LEFT";
			if ("ARROW_MERGE_RIGHT".equals(requested)) requested = "ARROW_RIGHT";
			MessageBoardTileEntity.DisplayMode mode;
			try {
				mode = MessageBoardTileEntity.DisplayMode.valueOf(requested);
			} catch (IllegalArgumentException ex) {
				return new Object[] { false, "Unknown mode: " + requested };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).setMode(mode) };
		}

		@Callback(doc = "setMessageBoardColor(controllerX:int, controllerY:int, controllerZ:int, rgb:int):boolean, string -- Sets the RGB text/lamp color on linked boards")
		public Object[] setMessageBoardColor(Context c, Arguments args) throws Exception {
			BlockPos controllerPos = getBlockPosFromArgsAt(args, 0);
			TileEntity tile = host.world().getTileEntity(controllerPos);
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			return new Object[] { true, ((MessageBoardControllerTileEntity) tile).setColor(args.checkInteger(3)) };
		}

		@Callback(doc = "addMessageBoardRotationPage(controllerX:int, controllerY:int, controllerZ:int):boolean, string -- Adds the controller's current text, arrow mode, color, and brightness as a rotation page")
		public Object[] addMessageBoardRotationPage(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			boolean added = ((MessageBoardControllerTileEntity) tile).addCurrentPage();
			return new Object[] { added, added ? "Rotation page added" : "Rotation is full" };
		}

		@Callback(doc = "clearMessageBoardRotation(controllerX:int, controllerY:int, controllerZ:int):boolean -- Clears the controller page rotation")
		public Object[] clearMessageBoardRotation(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			((MessageBoardControllerTileEntity) tile).clearRotationPages();
			return new Object[] { true };
		}

		@Callback(doc = "setMessageBoardSchedule(controllerX:int, controllerY:int, controllerZ:int, mode:string, amountOrTimes:any?, times:string?):boolean, string -- Interval modes accept an amount; GAME_TIMES accepts HH:MM times")
		public Object[] setMessageBoardSchedule(Context c, Arguments args) throws Exception {
			TileEntity tile = host.world().getTileEntity(getBlockPosFromArgsAt(args, 0));
			if (!(tile instanceof MessageBoardControllerTileEntity)) {
				return new Object[] { false, "No message board controller at position" };
			}
			MessageBoardControllerTileEntity controller = (MessageBoardControllerTileEntity) tile;
			DisplaySchedule.Mode mode = DisplaySchedule.Mode.fromName(args.checkString(3));
			controller.setScheduleMode(mode);
			if (args.count() >= 5 && args.isInteger(4)) controller.setScheduleIntervalAmount(args.checkInteger(4));
			if (args.count() >= 5 && args.isString(4)) controller.setScheduleTimes(args.checkString(4));
			if (args.count() >= 6 && args.isString(5)) controller.setScheduleTimes(args.checkString(5));
			return new Object[] { true, mode.name() };
		}
		
		@Callback(doc = "listSensors():array -- Returns a list of sensor positions")
		public Object[] listSensors(Context c, Arguments args) {
		    NBTTagCompound tag = card.getTagCompound();
		    if (tag == null) return new Object[] { new ArrayList<Integer[]>() };

		    ArrayList<Integer[]> sensors = new ArrayList<>();
		    for (int i = 0; i < 8; i++) {
		        if (tag.hasKey("sensor" + i)) {
		            BlockPos p = BlockPos.fromLong(tag.getLong("sensor" + i));
		            sensors.add(new Integer[] { p.getX(), p.getY(), p.getZ() });
		        }
		    }

		    return new Object[] { sensors };
		}
		
		

		@Callback(doc = "changeBulb(x:int, y:int, z:int, oldBulb:string, newBulb:string):boolean, string OR changeBulb(id:long, oldBulb:string, newBulb:string)")
		public Object[] changeBulb(Context c, Arguments args) throws Exception {
		    BlockPos pos = getBlockPosFromArgs(args);

		    if (!cardContainsPos(pos)) {
		        return new Object[] { false, "Card does not contain this block position" };
		    }

		    String oldBulbStr, newBulbStr;

		    if (args.isInteger(1)) {
		        oldBulbStr = args.checkString(3);
		        newBulbStr = args.checkString(4);
		    } else {
		        oldBulbStr = args.checkString(1);
		        newBulbStr = args.checkString(2);
		    }

		    if (!bulbTypesByString.containsKey(oldBulbStr) || !bulbTypesByString.containsKey(newBulbStr)) {
		        return new Object[] { false, "Invalid bulb type(s)" };
		    }

		    EnumTrafficLightBulbTypes oldBulb = bulbTypesByString.get(oldBulbStr);
		    EnumTrafficLightBulbTypes newBulb = bulbTypesByString.get(newBulbStr);

		    TileEntity te = host.world().getTileEntity(pos);
		    if (!(te instanceof BaseTrafficLightTileEntity)) {
		        return new Object[] { false, "No traffic light at given position" };
		    }

		    BaseTrafficLightTileEntity tile = (BaseTrafficLightTileEntity) te;

		    // Find frame with oldBulb
		    int frameToReplace = -1;
		    for (int i = 0; i < tile.getBulbCount(); i++) {
		        if (tile.getBulbTypeBySlot(i) == oldBulb) {
		            frameToReplace = i;
		            break;
		        }
		    }

		    if (frameToReplace == -1) {
		        return new Object[] { false, "Old bulb not found in traffic light" };
		    }

		    // Optional: briefly activate the old bulb before swap
		    tile.setActive(oldBulb, true, false);
		    host.world().markBlockRangeForRenderUpdate(pos, pos); // force render update

		    // Swap
		    tile.setBulbType(frameToReplace, newBulb);
		    tile.markDirty();
		    host.world().notifyBlockUpdate(pos, tile.getWorld().getBlockState(pos), tile.getWorld().getBlockState(pos), 3);

		    // Activate new bulb
		    tile.setActive(newBulb, true, false);
		    return new Object[] { true, "Bulb replaced at frame " + frameToReplace };
		}

		
		
		
		@Callback(doc = "isSensorTripped(x:int, y:int, z:int):boolean OR isSensorTripped(id:long):boolean")
		public Object[] isSensorTripped(Context c, Arguments args) throws Exception {
			
			
		    BlockPos pos = getBlockPosFromArgs2(args);
		    // 🔧 Log the sensor position
		    System.out.println("Pos! " + pos.toString());

		    IBlockState state = host.world().getBlockState(pos);
		    if (!(state.getBlock() instanceof BlockTrafficSensorLeft ||
		          state.getBlock() instanceof BlockTrafficSensorRight ||
		          state.getBlock() instanceof BlockTrafficSensorStraight)) {
		        return new Object[] { false };
		    }

		    int width = args.count() >= 4 ? args.checkInteger(3) : -1;   // X axis
		    int height = args.count() >= 5 ? args.checkInteger(4) : Config.sensorScanHeight; // Y axis
		    int length = args.count() >= 6 ? args.checkInteger(5) : 1;  // Z axis
		    new Thread(() -> {
		        for (int i = 0; i < 15 * 20; i++) { // 20 ticks per second * 15s
		            AxisAlignedBB box = new AxisAlignedBB(pos).grow(width / 2.0, height / 2.0, length / 2.0);
		            for (double x = box.minX; x <= box.maxX; x += 0.5) {
		                for (double y = box.minY; y <= box.maxY; y += 0.5) {
		                    for (double z = box.minZ; z <= box.maxZ; z += 0.5) {
		                        host.world().spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 1.0, 0.0, 0.0);
		                    }
		                }
		            }
		            try {
		                Thread.sleep(50); // 1 tick = 50ms
		            } catch (InterruptedException ignored) {}
		        }
		    }).start();


		   
		    boolean tripped = host.world().getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(pos).grow(width / 2.0, height/ 2.0,length/ 2.0)).stream().anyMatch(e -> (e instanceof EntityPlayerMP) || Arrays.stream(Config.sensorClasses).anyMatch(eName -> // 
			{
				System.out.print(eName);
				
				
				Class<?> nextClass = e.getClass();
				
				
				
				while(nextClass != null)
				{
					if (eName.equals(nextClass.getName()))
					{
						return true;
					}
					
					nextClass = nextClass.getSuperclass();
				}
				
				return false;
			}));

		    return new Object[] { tripped };

		   
		}
		
		@Callback(direct = true, doc = "listBlockIDs():array -- Retrieves a list of block ids currently in use by the card")
		public Object[] listBlockIDs(Context c, Arguments args)
		{
			NBTTagCompound cardTag = card.getTagCompound();
			if (cardTag == null)
			{
				return new Object[] { new ArrayList<Long>() };
			}
			
			int maxTrafficLights = ItemTrafficLightCard.getMaxTrafficLights(card.getMetadata());
			ArrayList<Long> blockPositions = new ArrayList<>();
			
			for(int i = 0; i < maxTrafficLights; i++)
			{
				if (cardTag.hasKey("light" + i) && cardTag.getLong("light" + i) != 0)
				{
					blockPositions.add(cardTag.getLong("light" + i));
				}
			}
			
			return new Object[] { blockPositions };
		}

		@Callback(direct = true, doc = "listDirections():array -- Lists approach directions (north/south/east/west) that have paired traffic lights")
		public Object[] listDirections(Context c, Arguments args) {
			java.util.LinkedHashSet<String> directions = new java.util.LinkedHashSet<>();
			for (BaseTrafficLightTileEntity light : getPairedTrafficLights()) {
				directions.add(TrafficLightOcApproachHelper.approachName(TrafficLightOcApproachHelper.resolveApproach(light)));
			}
			return new Object[] { new ArrayList<>(directions) };
		}

		@Callback(direct = true, doc = "listPairedLights():array -- Returns paired lights as tables with x, y, z, and direction fields")
		public Object[] listPairedLights(Context c, Arguments args) {
			ArrayList<Map<String, Object>> entries = new ArrayList<>();
			for (BaseTrafficLightTileEntity light : getPairedTrafficLights()) {
				BlockPos pos = light.getPos();
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("x", pos.getX());
				entry.put("y", pos.getY());
				entry.put("z", pos.getZ());
				entry.put("id", pos.toLong());
				entry.put("direction", TrafficLightOcApproachHelper.approachName(TrafficLightOcApproachHelper.resolveApproach(light)));
				entries.add(entry);
			}
			return new Object[] { entries };
		}

		@Callback(direct = true, doc = "listBlockPosByDirection(direction:string):array -- Returns positions of paired lights for the given approach direction")
		public Object[] listBlockPosByDirection(Context c, Arguments args) throws Exception {
			EnumFacing direction = TrafficLightOcApproachHelper.parseApproach(args.checkString(0));
			ArrayList<Integer[]> blockPositions = new ArrayList<>();
			for (BaseTrafficLightTileEntity light : getPairedTrafficLightsForDirection(direction)) {
				BlockPos pos = light.getPos();
				blockPositions.add(new Integer[] { pos.getX(), pos.getY(), pos.getZ() });
			}
			return new Object[] { blockPositions };
		}

		@Callback(doc = "setStateForDirection(direction:string, state:string, active:boolean, flash:boolean):boolean, string -- Sets the bulb state on all paired lights for the given approach direction")
		public Object[] setStateForDirection(Context c, Arguments args) throws Exception {
			EnumFacing direction = TrafficLightOcApproachHelper.parseApproach(args.checkString(0));
			String state = args.checkString(1);
			boolean active = args.checkBoolean(2);
			boolean flash = args.checkBoolean(3);

			if (!bulbTypesByString.containsKey(state)) {
				return new Object[] { false, "Invalid state specified" };
			}

			EnumTrafficLightBulbTypes bulbType = bulbTypesByString.get(state);
			List<BaseTrafficLightTileEntity> lights = getPairedTrafficLightsForDirection(direction);
			if (lights.isEmpty()) {
				return new Object[] { false, "No paired lights for direction " + TrafficLightOcApproachHelper.approachName(direction) };
			}

			int applied = 0;
			int skipped = 0;
			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			for (BaseTrafficLightTileEntity trafficLight : lights) {
				if (!trafficLight.hasBulb(bulbType)) {
					skipped++;
					continue;
				}
				double draw = Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(trafficLight.getPos());
				if (!((ComponentConnector) node()).tryChangeBuffer(-draw)) {
					return new Object[] { false, "Not enough energy after " + applied + " light(s)" };
				}
				trafficLight.setActive(bulbType, active, flash);
				applied++;
			}

			if (applied == 0) {
				return new Object[] { false, "No paired lights for direction contain bulb " + state };
			}
			String message = "Applied to " + applied + " light(s)";
			if (skipped > 0) {
				message += " (" + skipped + " skipped, missing bulb)";
			}
			return new Object[] { true, message };
		}

		@Callback(doc = "clearStatesForDirection(direction:string):boolean, string -- Clears all bulb states on paired lights for the given approach direction")
		public Object[] clearStatesForDirection(Context c, Arguments args) throws Exception {
			EnumFacing direction = TrafficLightOcApproachHelper.parseApproach(args.checkString(0));
			List<BaseTrafficLightTileEntity> lights = getPairedTrafficLightsForDirection(direction);
			if (lights.isEmpty()) {
				return new Object[] { false, "No paired lights for direction " + TrafficLightOcApproachHelper.approachName(direction) };
			}

			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			int cleared = 0;
			for (BaseTrafficLightTileEntity trafficLight : lights) {
				double draw = Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(trafficLight.getPos());
				if (!((ComponentConnector) node()).tryChangeBuffer(-draw)) {
					return new Object[] { false, "Not enough energy after " + cleared + " light(s)" };
				}
				trafficLight.powerOff();
				trafficLight.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
				cleared++;
			}
			return new Object[] { true, "Cleared " + cleared + " light(s)" };
		}

		@Callback(doc = "getStatesForDirection(direction:string):boolean, table -- Returns bulb states keyed by paired light position id for the given approach direction")
		public Object[] getStatesForDirection(Context c, Arguments args) throws Exception {
			EnumFacing direction = TrafficLightOcApproachHelper.parseApproach(args.checkString(0));
			List<BaseTrafficLightTileEntity> lights = getPairedTrafficLightsForDirection(direction);
			if (lights.isEmpty()) {
				return new Object[] { false, "No paired lights for direction " + TrafficLightOcApproachHelper.approachName(direction) };
			}

			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			Map<Long, Map<String, StateInfo>> results = new LinkedHashMap<>();
			for (BaseTrafficLightTileEntity trafficLight : lights) {
				double draw = Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(trafficLight.getPos());
				if (!((ComponentConnector) node()).tryChangeBuffer(-draw)) {
					return new Object[] { false, "Not enough energy" };
				}
				results.put(trafficLight.getPos().toLong(), buildStateInfoMap(trafficLight));
			}
			return new Object[] { true, results };
		}

		@Callback(doc = "changeBulbForDirection(direction:string, oldBulb:string, newBulb:string):boolean, string -- Replaces a bulb type on all paired lights for the given approach direction")
		public Object[] changeBulbForDirection(Context c, Arguments args) throws Exception {
			EnumFacing direction = TrafficLightOcApproachHelper.parseApproach(args.checkString(0));
			String oldBulbStr = args.checkString(1);
			String newBulbStr = args.checkString(2);

			if (!bulbTypesByString.containsKey(oldBulbStr) || !bulbTypesByString.containsKey(newBulbStr)) {
				return new Object[] { false, "Invalid bulb type(s)" };
			}

			EnumTrafficLightBulbTypes oldBulb = bulbTypesByString.get(oldBulbStr);
			EnumTrafficLightBulbTypes newBulb = bulbTypesByString.get(newBulbStr);
			List<BaseTrafficLightTileEntity> lights = getPairedTrafficLightsForDirection(direction);
			if (lights.isEmpty()) {
				return new Object[] { false, "No paired lights for direction " + TrafficLightOcApproachHelper.approachName(direction) };
			}

			int changed = 0;
			int skipped = 0;
			for (BaseTrafficLightTileEntity tile : lights) {
				int frameToReplace = -1;
				for (int i = 0; i < tile.getBulbCount(); i++) {
					if (tile.getBulbTypeBySlot(i) == oldBulb) {
						frameToReplace = i;
						break;
					}
				}
				if (frameToReplace == -1) {
					skipped++;
					continue;
				}
				tile.setActive(oldBulb, true, false);
				host.world().markBlockRangeForRenderUpdate(tile.getPos(), tile.getPos());
				tile.setBulbType(frameToReplace, newBulb);
				tile.markDirty();
				host.world().notifyBlockUpdate(tile.getPos(), tile.getWorld().getBlockState(tile.getPos()), tile.getWorld().getBlockState(tile.getPos()), 3);
				tile.setActive(newBulb, true, false);
				changed++;
			}

			if (changed == 0) {
				return new Object[] { false, "Old bulb not found on any paired light for direction" };
			}
			String message = "Changed bulb on " + changed + " light(s)";
			if (skipped > 0) {
				message += " (" + skipped + " skipped)";
			}
			return new Object[] { true, message };
		}
		
		private final Map<String, EnumTrafficLightBulbTypes> bulbTypesByString = Arrays.stream(EnumTrafficLightBulbTypes.values()).collect(Collectors.toMap(EnumTrafficLightBulbTypes::toString, Function.identity()));
		private final Map<String, String> bulbTypeStringsByString = Arrays.stream(EnumTrafficLightBulbTypes.values()).collect(Collectors.toMap(EnumTrafficLightBulbTypes::toString, EnumTrafficLightBulbTypes::toString));
		@Callback(direct = true, getter = true, doc = "states():array -- Retrieves a list of all possible states")
		public Object[] states(Context c, Arguments args)
		{
			return new Object[] { bulbTypeStringsByString };
		}
		
		@Callback(doc = "setState(x:int, y:int, z:int, state:string, active:boolean, flash:boolean):boolean, string OR setState(id:long, state:string, active:boolean, flash:boolean) -- Sets the state of the specified traffic light")
		public Object[] setState(Context c, Arguments args) throws Exception
		{
			BlockPos pos = getBlockPosFromArgs(args);
			if (!(args.isString(3) && args.isBoolean(4) && args.isBoolean(5)) &&
				!(args.isString(1) && args.isBoolean(2) && args.isBoolean(3)))
			{
				throw new IllegalArgumentException("Invalid argument format");
			}
			
			if (!cardContainsPos(pos))
			{
				return new Object[] { false, "Card does not contain this block position" };
			}
			
			String state;
			boolean active;
			boolean flash;
			if (args.isInteger(1))
			{
				state = args.checkString(3);
				active = args.checkBoolean(4);
				flash = args.checkBoolean(5);
			}
			else
			{
				state = args.checkString(1);
				active = args.checkBoolean(2);
				flash = args.checkBoolean(3);
			}
			
			if (!bulbTypesByString.containsKey(state))
			{
				return new Object[] { false, "Invalid state specified" };
			}
			
			TileEntity posTE = host.world().getTileEntity(pos);
			if (!(posTE instanceof BaseTrafficLightTileEntity))
			{
				return new Object[] { false, "A traffic light no longer exists at this block position" };
			}
			
			BaseTrafficLightTileEntity trafficLight = (BaseTrafficLightTileEntity)posTE;
			if (!trafficLight.hasBulb(bulbTypesByString.get(state)))
			{
				return new Object[] { false, "Traffic light does not contain the specified bulb" };
			}
			
			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			
			double draw = (Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(pos));
			if (!((ComponentConnector)node()).tryChangeBuffer(-draw))
			{
				return new Object[] { false, "Not enough energy" };
			}
			
			trafficLight.setActive(bulbTypesByString.get(state), active, flash);
			return new Object[] { true };
		}
		
		@Callback(doc = "clearStates(x:int, y:int, z:int):boolean, string OR clearStates(id:long):boolean, string -- Sets all states to inactive for the specified traffic light")
		public Object[] clearStates(Context c, Arguments args) throws Exception
		{
			BlockPos pos = getBlockPosFromArgs(args);
			
			if (!cardContainsPos(pos))
			{
				return new Object[] { false, "Card does not contain this block position" };
			}
			
			TileEntity posTE = host.world().getTileEntity(pos);
			if (!(posTE instanceof BaseTrafficLightTileEntity))
			{
				return new Object[] { false, "A traffic light no longer exists at this block position" };
			}
			
			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			double draw = (Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(pos));
			if (!((ComponentConnector)node()).tryChangeBuffer(-draw))
			{
				return new Object[] { false, "Not enough energy" };
			}
			
			BaseTrafficLightTileEntity trafficLight = (BaseTrafficLightTileEntity)posTE;
			trafficLight.powerOff();
			trafficLight.setActive(EnumTrafficLightBulbTypes.DontCross, false, false); // Power off automatically turns on Dont Cross, which is wrong in this context
			return new Object[] { true };
		}
		
		@Callback(doc = "getStates(x:int, y:int, z:int):boolean, string/table OR getStates(id:long):boolean, string/table -- Returns a table where the key is the state and value is a table that has an active and flash key")
		public Object[] getStates(Context c, Arguments args) throws Exception
		{
			BlockPos pos = getBlockPosFromArgs(args);
			
			if (!cardContainsPos(pos))
			{
				return new Object[] { false, "Card does not contain this block position" };
			}
			
			TileEntity posTE = host.world().getTileEntity(pos);
			if (!(posTE instanceof BaseTrafficLightTileEntity))
			{
				return new Object[] { false, "A traffic light no longer exists at this block position" };
			}
			
			BlockPos hostPos = new BlockPos(host.xPosition(), host.yPosition(), host.zPosition());
			double draw = (Config.trafficLightCardDrawPerBlock * hostPos.distanceSq(pos));
			if (!((ComponentConnector)node()).tryChangeBuffer(-draw))
			{
				return new Object[] { false, "Not enough energy" };
			}
			
			BaseTrafficLightTileEntity trafficLight = (BaseTrafficLightTileEntity)posTE;
			return new Object[] { true, buildStateInfoMap(trafficLight) };
		}
		
		private class StateInfo implements Value
		{
			public boolean active;
			public boolean flash;
			
			@Override
			public void load(NBTTagCompound nbt) {
				active = nbt.getBoolean("active");
				flash = nbt.getBoolean("flash");
			}
			@Override
			public void save(NBTTagCompound nbt) {
				nbt.setBoolean("active", active);
				nbt.setBoolean("flash", flash);
			}
			@Override
			public Object apply(Context context, Arguments arguments) {
				switch(arguments.checkString(0))
				{
					case "active":
						return active;
					case "flash":
						return flash;
				}
				
				return null;
			}
			@Override
			public void unapply(Context context, Arguments arguments) { }
			@Override
			public Object[] call(Context context, Arguments arguments) {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public void dispose(Context context) {
				// TODO Auto-generated method stub
				
			}
		}
		
		private BlockPos getBlockPosFromArgs2(Arguments args) throws Exception {
		    if (args.count() >= 3 && args.isInteger(0) && args.isInteger(1) && args.isInteger(2)) {
		        return new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
		    }
		    if (args.count() >= 1 && args.isDouble(0)) {
		        double posIDDouble = args.checkDouble(0);
		        return BlockPos.fromLong((long) posIDDouble);
		    }
		    throw new IllegalArgumentException("Could not determine block position");
		}

		private BlockPos getBlockPosFromArgsAt(Arguments args, int offset) throws Exception {
			if (args.count() >= offset + 3 && args.isInteger(offset) && args.isInteger(offset + 1)
					&& args.isInteger(offset + 2)) {
				return new BlockPos(args.checkInteger(offset), args.checkInteger(offset + 1),
						args.checkInteger(offset + 2));
			}
			throw new IllegalArgumentException("Expected x, y, z integer coordinates");
		}
		
		private BlockPos getBlockPosFromArgs(Arguments args) throws Exception
		{
			if (args.count() >= 3 && args.isInteger(0) && args.isInteger(1) && args.isInteger(2)) {
				return new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
			}
			if (args.count() >= 1 && args.isDouble(0)) {
				double posIDDouble = args.checkDouble(0);
				return BlockPos.fromLong((long) posIDDouble);
			}
			throw new IllegalArgumentException("Could not determine block position");
		}
		
		private List<BaseTrafficLightTileEntity> getPairedTrafficLights() {
			NBTTagCompound cardTag = card.getTagCompound();
			if (cardTag == null) {
				return Collections.emptyList();
			}

			int maxTrafficLights = ItemTrafficLightCard.getMaxTrafficLights(card.getMetadata());
			ArrayList<BaseTrafficLightTileEntity> lights = new ArrayList<>();
			for (int i = 0; i < maxTrafficLights; i++) {
				if (!cardTag.hasKey("light" + i) || cardTag.getLong("light" + i) == 0) {
					continue;
				}
				BlockPos position = BlockPos.fromLong(cardTag.getLong("light" + i));
				TileEntity te = host.world().getTileEntity(position);
				if (te instanceof BaseTrafficLightTileEntity) {
					lights.add((BaseTrafficLightTileEntity) te);
				}
			}
			return lights;
		}

		private List<BaseTrafficLightTileEntity> getPairedTrafficLightsForDirection(EnumFacing direction) {
			ArrayList<BaseTrafficLightTileEntity> lights = new ArrayList<>();
			for (BaseTrafficLightTileEntity light : getPairedTrafficLights()) {
				if (TrafficLightOcApproachHelper.resolveApproach(light) == direction) {
					lights.add(light);
				}
			}
			return lights;
		}

		private Map<String, StateInfo> buildStateInfoMap(BaseTrafficLightTileEntity trafficLight) {
			HashMap<String, StateInfo> stateInfos = new HashMap<>();
			for (String bulbTypeString : bulbTypeStringsByString.keySet()) {
				stateInfos.put(bulbTypeString, new StateInfo());
			}

			HashSet<String> discoveredStates = new HashSet<>();
			for (int i = 0; i < trafficLight.getBulbCount(); i++) {
				EnumTrafficLightBulbTypes bulbType = trafficLight.getBulbTypeBySlot(i);
				if (bulbType == null) {
					continue;
				}
				if (discoveredStates.add(bulbType.toString())) {
					stateInfos.get(bulbType.toString()).active = trafficLight.getActiveBySlot(i);
					stateInfos.get(bulbType.toString()).flash = trafficLight.getFlashBySlot(i);
				}
			}
			return stateInfos;
		}

		private boolean cardContainsPos(BlockPos pos)
		{
			NBTTagCompound cardTag = card.getTagCompound();
			if (cardTag == null) {
				return false;
			}
			long id = pos.toLong();
			for(String lightKey : cardTag.getKeySet().stream().filter(key -> key.startsWith("light")).collect(Collectors.toList()))
			{
				long tagKey = cardTag.getLong(lightKey);
				if (tagKey == id)
				{
					return true;
				}
			}
			
			return false;
		}
	}
}
