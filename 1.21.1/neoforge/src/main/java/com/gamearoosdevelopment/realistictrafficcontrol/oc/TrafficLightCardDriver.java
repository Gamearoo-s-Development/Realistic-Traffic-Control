package com.gamearoosdevelopment.realistictrafficcontrol.oc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightCardItem;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.gamearoosdevelopment.realistictrafficcontrol.util.TrafficLightOcApproachHelper;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.DriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * OpenComputers item driver for RTC traffic-light cards.
 *
 * <p>This class is loaded only by {@link OpenComputersIntegration}; code outside this package must not
 * reference it directly because OpenComputers is optional.
 */
public final class TrafficLightCardDriver extends DriverItem {
    public TrafficLightCardDriver() {
        super(TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 0),
                TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 1),
                TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 2),
                TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 3));
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
        return Math.min(TrafficLightCardItem.getTier(stack), 2);
    }

    public static final class CardEnvironment extends AbstractManagedEnvironment {
        private static final int MAX_PAIRED_BUTTONS = 8;
        private final ItemStack card;
        private final EnvironmentHost host;
        private final Map<String, EnumTrafficLightBulbTypes> bulbs = Arrays.stream(EnumTrafficLightBulbTypes.values())
                .collect(Collectors.toMap(EnumTrafficLightBulbTypes::toString, Function.identity()));

        CardEnvironment(ItemStack card, EnvironmentHost host) {
            this.card = card;
            this.host = host;
            setNode(Network.newNode(this, Visibility.Neighbors)
                    .withComponent("traffic_light_card").withConnector(300).create());
        }

        @Callback(doc = "pairSensor(x:int, y:int, z:int):boolean, string OR pairSensor(id:long):boolean, string -- Pairs or unpairs a sensor")
        public Object[] pairSensor(Context context, Arguments args) {
            BlockPos pos = position(args);
            if (!(level().getBlockState(pos).getBlock() instanceof TrafficSensorBlock)) {
                return result(false, "No traffic sensor at given position");
            }
            return togglePair("sensor", TrafficLightCardItem.getMaxSensors(), pos, "sensors");
        }

        @Callback(doc = "listSensors():array -- Returns paired sensor positions")
        public Object[] listSensors(Context context, Arguments args) {
            return one(listPositions("sensor", TrafficLightCardItem.getMaxSensors()));
        }

        @Callback(doc = "isSensorTripped(x:int, y:int, z:int, width:int?, height:int?, length:int?):boolean OR isSensorTripped(id:long, width:int?, height:int?, length:int?):boolean")
        public Object[] isSensorTripped(Context context, Arguments args) {
            PositionArgument parsed = positionArgument(args);
            BlockPos pos = parsed.pos();
            if (!(level().getBlockState(pos).getBlock() instanceof TrafficSensorBlock)) {
                return one(false);
            }
            int width = Math.max(0, args.optInteger(parsed.nextIndex(), 1));
            int height = Math.max(0, args.optInteger(parsed.nextIndex() + 1, Config.sensorScanHeight));
            int length = Math.max(0, args.optInteger(parsed.nextIndex() + 2, 1));
            AABB area = new AABB(pos).inflate(width / 2.0, height / 2.0, length / 2.0);
            boolean tripped = level().getEntities((Entity) null, area, this::tripsSensor).stream().findAny().isPresent();
            return one(tripped);
        }

        @Callback(doc = "pairPedButton(x:int, y:int, z:int):boolean, string OR pairPedButton(id:long):boolean, string -- Pairs or unpairs a pedestrian button")
        public Object[] pairPedButton(Context context, Arguments args) {
            BlockPos pos = position(args);
            if (!(level().getBlockEntity(pos) instanceof PedestrianButtonBlockEntity)
                    || !(level().getBlockState(pos).getBlock() instanceof BlockPedestrianButton)) {
                return result(false, "No pedestrian button at given position");
            }
            return togglePair("pedButton", MAX_PAIRED_BUTTONS, pos, "ped buttons");
        }

        @Callback(doc = "listPedButtons():array -- Returns paired pedestrian button positions")
        public Object[] listPedButtons(Context context, Arguments args) {
            return one(listPositions("pedButton", MAX_PAIRED_BUTTONS));
        }

        @Callback(doc = "pressPedButton(x:int, y:int, z:int):boolean, string OR pressPedButton(id:long):boolean, string -- Simulates pressing a paired pedestrian button")
        public Object[] pressPedButton(Context context, Arguments args) {
            BlockPos pos = position(args);
            if (!contains("pedButton", MAX_PAIRED_BUTTONS, pos)) {
                return result(false, "Card does not contain this pedestrian button");
            }
            BlockEntity be = level().getBlockEntity(pos);
            BlockState state = level().getBlockState(pos);
            if (!(be instanceof PedestrianButtonBlockEntity button)
                    || !(state.getBlock() instanceof BlockPedestrianButton)) {
                return result(false, "No pedestrian button at given position");
            }
            int queued = 0;
            for (BlockPos controllerPos : button.getPairedBoxes()) {
                BlockEntity controllerBe = level().getBlockEntity(controllerPos);
                if (!(controllerBe instanceof TrafficLightControlBoxBlockEntity controller)) {
                    button.removePairedBox(controllerPos);
                    continue;
                }
                int rotation = state.getValue(RTCProperties.ROTATION);
                if (CustomAngleCalculator.isNorthSouth(rotation)) {
                    controller.getAutomator().setWestEastPedQueued(true);
                } else {
                    controller.getAutomator().setNorthSouthPedQueued(true);
                }
                controller.setChanged();
                level().sendBlockUpdated(controllerPos, level().getBlockState(controllerPos),
                        level().getBlockState(controllerPos), 3);
                queued++;
            }
            return result(true, "Queued for " + queued + " controller(s)");
        }

        @Callback(direct = true, doc = "listBlockPos():array -- Retrieves paired traffic-light positions")
        public Object[] listBlockPos(Context context, Arguments args) {
            return one(listPositions("light", maxLights()));
        }

        @Callback(direct = true, doc = "listBlockIDs():array -- Retrieves paired traffic-light packed position ids")
        public Object[] listBlockIDs(Context context, Arguments args) {
            return one(pairedIds("light", maxLights()));
        }

        @Callback(doc = "listDirections():array -- Lists approach directions having paired traffic lights")
        public Object[] listDirections(Context context, Arguments args) {
            LinkedHashSet<String> directions = new LinkedHashSet<>();
            for (TrafficLightBlockEntity light : pairedLights()) {
                directions.add(TrafficLightOcApproachHelper.approachName(
                        TrafficLightOcApproachHelper.resolveApproach(light)));
            }
            return one(new ArrayList<>(directions));
        }

        @Callback(doc = "listPairedLights():array -- Returns paired lights with x, y, z, id, and direction fields")
        public Object[] listPairedLights(Context context, Arguments args) {
            ArrayList<Map<String, Object>> entries = new ArrayList<>();
            for (TrafficLightBlockEntity light : pairedLights()) {
                BlockPos pos = light.getBlockPos();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("x", pos.getX());
                entry.put("y", pos.getY());
                entry.put("z", pos.getZ());
                entry.put("id", pos.asLong());
                entry.put("direction", TrafficLightOcApproachHelper.approachName(
                        TrafficLightOcApproachHelper.resolveApproach(light)));
                entries.add(entry);
            }
            return one(entries);
        }

        @Callback(doc = "listBlockPosByDirection(direction:string):array -- Returns paired positions for an approach")
        public Object[] listBlockPosByDirection(Context context, Arguments args) {
            ArrayList<Integer[]> positions = new ArrayList<>();
            for (TrafficLightBlockEntity light : pairedLights(direction(args.checkString(0)))) {
                BlockPos pos = light.getBlockPos();
                positions.add(new Integer[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            return one(positions);
        }

        @Callback(getter = true, direct = true, doc = "states():table -- Retrieves all possible bulb states")
        public Object[] states(Context context, Arguments args) {
            Map<String, String> names = new LinkedHashMap<>();
            bulbs.keySet().forEach(name -> names.put(name, name));
            return one(names);
        }

        @Callback(doc = "setState(x:int, y:int, z:int, state:string, active:boolean, flash:boolean):boolean, string OR setState(id:long, state:string, active:boolean, flash:boolean)")
        public Object[] setState(Context context, Arguments args) {
            PositionArgument parsed = positionArgument(args);
            BlockPos pos = parsed.pos();
            if (!containsLight(pos)) return result(false, "Card does not contain this block position");
            EnumTrafficLightBulbTypes bulb = bulb(args.checkString(parsed.nextIndex()));
            boolean active = args.checkBoolean(parsed.nextIndex() + 1);
            boolean flash = args.checkBoolean(parsed.nextIndex() + 2);
            TrafficLightBlockEntity light = light(pos);
            if (light == null) return result(false, "A traffic light no longer exists at this block position");
            if (bulb == null) return result(false, "Invalid state specified");
            if (!light.hasBulb(bulb)) return result(false, "Traffic light does not contain the specified bulb");
            if (!consume(pos)) return result(false, "Not enough energy");
            light.setActive(bulb, active, flash);
            return one(true);
        }

        @Callback(doc = "clearStates(x:int, y:int, z:int):boolean, string OR clearStates(id:long):boolean, string -- Clears all states")
        public Object[] clearStates(Context context, Arguments args) {
            BlockPos pos = position(args);
            if (!containsLight(pos)) return result(false, "Card does not contain this block position");
            TrafficLightBlockEntity light = light(pos);
            if (light == null) return result(false, "A traffic light no longer exists at this block position");
            if (!consume(pos)) return result(false, "Not enough energy");
            clear(light);
            return one(true);
        }

        @Callback(doc = "getStates(x:int, y:int, z:int):boolean, table/string OR getStates(id:long):boolean, table/string -- Returns active and flash values by state")
        public Object[] getStates(Context context, Arguments args) {
            BlockPos pos = position(args);
            if (!containsLight(pos)) return result(false, "Card does not contain this block position");
            TrafficLightBlockEntity light = light(pos);
            if (light == null) return result(false, "A traffic light no longer exists at this block position");
            if (!consume(pos)) return result(false, "Not enough energy");
            return new Object[] { true, stateInfo(light) };
        }

        @Callback(doc = "changeBulb(x:int, y:int, z:int, oldBulb:string, newBulb:string):boolean, string OR changeBulb(id:long, oldBulb:string, newBulb:string)")
        public Object[] changeBulb(Context context, Arguments args) {
            PositionArgument parsed = positionArgument(args);
            BlockPos pos = parsed.pos();
            if (!containsLight(pos)) return result(false, "Card does not contain this block position");
            EnumTrafficLightBulbTypes oldBulb = bulb(args.checkString(parsed.nextIndex()));
            EnumTrafficLightBulbTypes newBulb = bulb(args.checkString(parsed.nextIndex() + 1));
            if (oldBulb == null || newBulb == null) return result(false, "Invalid bulb type(s)");
            TrafficLightBlockEntity light = light(pos);
            if (light == null) return result(false, "No traffic light at given position");
            int changedSlot = replaceBulb(light, oldBulb, newBulb);
            return changedSlot < 0 ? result(false, "Old bulb not found in traffic light")
                    : result(true, "Bulb replaced at frame " + changedSlot);
        }

        @Callback(doc = "setStateForDirection(direction:string, state:string, active:boolean, flash:boolean):boolean, string -- Sets a bulb on all paired lights for an approach")
        public Object[] setStateForDirection(Context context, Arguments args) {
            Direction direction = direction(args.checkString(0));
            EnumTrafficLightBulbTypes bulb = bulb(args.checkString(1));
            if (bulb == null) return result(false, "Invalid state specified");
            List<TrafficLightBlockEntity> lights = pairedLights(direction);
            if (lights.isEmpty()) return noLights(direction);
            int applied = 0;
            int skipped = 0;
            for (TrafficLightBlockEntity light : lights) {
                if (!light.hasBulb(bulb)) {
                    skipped++;
                    continue;
                }
                if (!consume(light.getBlockPos())) {
                    return result(false, "Not enough energy after " + applied + " light(s)");
                }
                light.setActive(bulb, args.checkBoolean(2), args.checkBoolean(3));
                applied++;
            }
            if (applied == 0) return result(false, "No paired lights for direction contain bulb " + args.checkString(1));
            return result(true, "Applied to " + applied + " light(s)"
                    + (skipped == 0 ? "" : " (" + skipped + " skipped, missing bulb)"));
        }

        @Callback(doc = "clearStatesForDirection(direction:string):boolean, string -- Clears paired lights for an approach")
        public Object[] clearStatesForDirection(Context context, Arguments args) {
            Direction direction = direction(args.checkString(0));
            List<TrafficLightBlockEntity> lights = pairedLights(direction);
            if (lights.isEmpty()) return noLights(direction);
            int cleared = 0;
            for (TrafficLightBlockEntity light : lights) {
                if (!consume(light.getBlockPos())) return result(false, "Not enough energy after " + cleared + " light(s)");
                clear(light);
                cleared++;
            }
            return result(true, "Cleared " + cleared + " light(s)");
        }

        @Callback(doc = "getStatesForDirection(direction:string):boolean, table/string -- Returns states keyed by packed position id")
        public Object[] getStatesForDirection(Context context, Arguments args) {
            Direction direction = direction(args.checkString(0));
            List<TrafficLightBlockEntity> lights = pairedLights(direction);
            if (lights.isEmpty()) return noLights(direction);
            Map<Long, Map<String, Map<String, Object>>> states = new LinkedHashMap<>();
            for (TrafficLightBlockEntity light : lights) {
                if (!consume(light.getBlockPos())) return result(false, "Not enough energy");
                states.put(light.getBlockPos().asLong(), stateInfo(light));
            }
            return new Object[] { true, states };
        }

        @Callback(doc = "changeBulbForDirection(direction:string, oldBulb:string, newBulb:string):boolean, string -- Replaces a bulb on paired lights for an approach")
        public Object[] changeBulbForDirection(Context context, Arguments args) {
            Direction direction = direction(args.checkString(0));
            EnumTrafficLightBulbTypes oldBulb = bulb(args.checkString(1));
            EnumTrafficLightBulbTypes newBulb = bulb(args.checkString(2));
            if (oldBulb == null || newBulb == null) return result(false, "Invalid bulb type(s)");
            List<TrafficLightBlockEntity> lights = pairedLights(direction);
            if (lights.isEmpty()) return noLights(direction);
            int changed = 0;
            int skipped = 0;
            for (TrafficLightBlockEntity light : lights) {
                if (replaceBulb(light, oldBulb, newBulb) < 0) skipped++; else changed++;
            }
            if (changed == 0) return result(false, "Old bulb not found on any paired light for direction");
            return result(true, "Changed bulb on " + changed + " light(s)"
                    + (skipped == 0 ? "" : " (" + skipped + " skipped)"));
        }

        @Callback(doc = "linkDigitalSign(controllerX:int, controllerY:int, controllerZ:int, signX:int, signY:int, signZ:int):boolean, string")
        public Object[] linkDigitalSign(Context context, Arguments args) {
            BlockEntity controller = level().getBlockEntity(xyz(args, 0));
            BlockPos signPos = xyz(args, 3);
            if (!(controller instanceof DigitalSignControllerBlockEntity digital)
                    || !(level().getBlockEntity(signPos) instanceof DigitalSignBlockEntity)) {
                return result(false, "Invalid digital sign controller or sign position");
            }
            return result(digital.linkSign(signPos), "Link updated");
        }

        @Callback(doc = "setDigitalSign(controllerX:int, controllerY:int, controllerZ:int, signId:string):boolean, number -- Displays a sign-pack UUID")
        public Object[] setDigitalSign(Context context, Arguments args) {
            BlockEntity be = level().getBlockEntity(xyz(args, 0));
            if (!(be instanceof DigitalSignControllerBlockEntity controller)) {
                return result(false, "No digital sign controller at position");
            }
            return new Object[] { true, controller.setSelectedSign(UUID.fromString(args.checkString(3))) };
        }

        @Callback(doc = "addDigitalSignRotation(controllerX:int, controllerY:int, controllerZ:int, signId:string, gameTime:string?):boolean, string")
        public Object[] addDigitalSignRotation(Context context, Arguments args) {
            DigitalSignControllerBlockEntity controller = digitalController(args);
            if (controller == null) return result(false, "No digital sign controller at position");
            if (controller.isSyncFollower()) return result(false, "Timing is controlled by the master controller");
            UUID id = UUID.fromString(args.checkString(3));
            String time = args.count() >= 5 ? args.checkString(4) : null;
            if (time != null && !time.isBlank() && DisplaySchedule.parseGameTime(time) < 0) {
                return result(false, "Invalid game time");
            }
            boolean added = controller.addRotationSign(id);
            boolean timed = added && (time == null
                    || controller.setRotationPageTime(controller.getRotationPageCount() - 1, time));
            return result(timed, timed ? "Rotation page added" : "Invalid time or rotation is full");
        }

        @Callback(doc = "setDigitalSignRotationTime(controllerX:int, controllerY:int, controllerZ:int, signId:string, gameTime:string):boolean, string")
        public Object[] setDigitalSignRotationTime(Context context, Arguments args) {
            DigitalSignControllerBlockEntity controller = digitalController(args);
            if (controller == null) return result(false, "No digital sign controller at position");
            boolean updated = controller.setRotationSignTime(UUID.fromString(args.checkString(3)), args.checkString(4));
            return result(updated, updated ? "Rotation sign time updated" : "Sign is not in rotation or time is invalid");
        }

        @Callback(doc = "clearDigitalSignRotation(controllerX:int, controllerY:int, controllerZ:int):boolean")
        public Object[] clearDigitalSignRotation(Context context, Arguments args) {
            DigitalSignControllerBlockEntity controller = digitalController(args);
            if (controller == null) return result(false, "No digital sign controller at position");
            controller.clearRotationSigns();
            return one(true);
        }

        @Callback(doc = "setDigitalSignSchedule(controllerX:int, controllerY:int, controllerZ:int, mode:string, amount:int?):boolean, string")
        public Object[] setDigitalSignSchedule(Context context, Arguments args) {
            DigitalSignControllerBlockEntity controller = digitalController(args);
            if (controller == null) return result(false, "No digital sign controller at position");
            if (controller.isSyncFollower()) return result(false, "Timing is controlled by the master controller");
            DisplaySchedule.Mode mode = DisplaySchedule.Mode.fromName(args.checkString(3).toUpperCase(Locale.ROOT));
            controller.setScheduleMode(mode);
            if (args.count() >= 5 && args.isInteger(4)) controller.setScheduleIntervalAmount(args.checkInteger(4));
            return result(true, mode.name());
        }

        @Callback(doc = "linkMessageBoard(controllerX:int, controllerY:int, controllerZ:int, boardX:int, boardY:int, boardZ:int):boolean, string")
        public Object[] linkMessageBoard(Context context, Arguments args) {
            BlockEntity controller = level().getBlockEntity(xyz(args, 0));
            BlockPos boardPos = xyz(args, 3);
            BlockEntity board = level().getBlockEntity(boardPos);
            if (!(controller instanceof MessageBoardControllerBlockEntity message)
                    || !(board instanceof MessageBoardBlockEntity)
                    || board instanceof MessageBoardControllerBlockEntity) {
                return result(false, "Invalid message board controller or board position");
            }
            return result(message.linkBoard(boardPos), "Link updated");
        }

        @Callback(doc = "setMessageBoardText(controllerX:int, controllerY:int, controllerZ:int, line:int, text:string):boolean, number/string")
        public Object[] setMessageBoardText(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            int line = args.checkInteger(3);
            if (line < 0 || line >= MessageBoardBlockEntity.MAX_LINES) {
                return result(false, "Line must be between 0 and " + (MessageBoardBlockEntity.MAX_LINES - 1));
            }
            controller.setLine(line, args.checkString(4));
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "clearMessageBoards(controllerX:int, controllerY:int, controllerZ:int):boolean, number -- Clears linked boards")
        public Object[] clearMessageBoards(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            for (int i = 0; i < MessageBoardBlockEntity.MAX_LINES; i++) controller.setLine(i, "");
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "setMessageBoardBrightness(controllerX:int, controllerY:int, controllerZ:int, brightness:number):boolean, number")
        public Object[] setMessageBoardBrightness(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            controller.setBrightness((float) args.checkDouble(3));
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "setMessageBoardTextScale(controllerX:int, controllerY:int, controllerZ:int, scale:number):boolean, number")
        public Object[] setMessageBoardTextScale(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            controller.setTextScale((float) args.checkDouble(3));
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "setMessageBoardFontStyle(controllerX:int, controllerY:int, controllerZ:int, style:string):boolean, number/string")
        public Object[] setMessageBoardFontStyle(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            String requested = args.checkString(3).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            try {
                controller.setFontStyle(MessageBoardBlockEntity.FontStyle.valueOf(requested));
            } catch (IllegalArgumentException exception) {
                return result(false, "Unknown font style: " + requested);
            }
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "setMessageBoardMode(controllerX:int, controllerY:int, controllerZ:int, mode:string):boolean, number/string")
        public Object[] setMessageBoardMode(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            String requested = args.checkString(3).trim().toUpperCase(Locale.ROOT);
            if ("ARROW_MERGE_LEFT".equals(requested)) requested = "ARROW_LEFT";
            if ("ARROW_MERGE_RIGHT".equals(requested)) requested = "ARROW_RIGHT";
            try {
                controller.setMode(MessageBoardBlockEntity.DisplayMode.valueOf(requested));
            } catch (IllegalArgumentException exception) {
                return result(false, "Unknown mode: " + requested);
            }
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "setMessageBoardColor(controllerX:int, controllerY:int, controllerZ:int, rgb:int):boolean, number")
        public Object[] setMessageBoardColor(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            controller.setColor(args.checkInteger(3));
            return new Object[] { true, controller.getLinkedBoards().size() };
        }

        @Callback(doc = "addMessageBoardRotationPage(controllerX:int, controllerY:int, controllerZ:int):boolean, string")
        public Object[] addMessageBoardRotationPage(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            boolean added = controller.addCurrentPage();
            return result(added, added ? "Rotation page added" : "Rotation is full");
        }

        @Callback(doc = "clearMessageBoardRotation(controllerX:int, controllerY:int, controllerZ:int):boolean")
        public Object[] clearMessageBoardRotation(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            controller.clearRotationPages();
            return one(true);
        }

        @Callback(doc = "setMessageBoardSchedule(controllerX:int, controllerY:int, controllerZ:int, mode:string, amountOrTimes:any?, times:string?):boolean, string")
        public Object[] setMessageBoardSchedule(Context context, Arguments args) {
            MessageBoardControllerBlockEntity controller = messageController(args);
            if (controller == null) return result(false, "No message board controller at position");
            DisplaySchedule.Mode mode = DisplaySchedule.Mode.fromName(args.checkString(3).toUpperCase(Locale.ROOT));
            controller.setScheduleMode(mode);
            if (args.count() >= 5 && args.isInteger(4)) controller.setScheduleIntervalAmount(args.checkInteger(4));
            if (args.count() >= 5 && args.isString(4)) controller.setScheduleTimes(args.checkString(4));
            if (args.count() >= 6 && args.isString(5)) controller.setScheduleTimes(args.checkString(5));
            return result(true, mode.name());
        }

        private Object[] togglePair(String prefix, int max, BlockPos pos, String label) {
            CompoundTag data = data();
            long id = pos.asLong();
            for (int i = 0; i < max; i++) {
                String key = prefix + i;
                if (data.contains(key) && data.getLong(key) == id) {
                    data.remove(key);
                    saveData(data);
                    return result(false, "Unpaired");
                }
            }
            for (int i = 0; i < max; i++) {
                String key = prefix + i;
                if (!data.contains(key)) {
                    data.putLong(key, id);
                    saveData(data);
                    return result(true, "Paired");
                }
            }
            return result(false, "Max " + label + " reached");
        }

        private List<Integer[]> listPositions(String prefix, int max) {
            ArrayList<Integer[]> positions = new ArrayList<>();
            for (long id : pairedIds(prefix, max)) {
                BlockPos pos = BlockPos.of(id);
                positions.add(new Integer[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            return positions;
        }

        private List<Long> pairedIds(String prefix, int max) {
            CompoundTag data = data();
            return data.getAllKeys().stream()
                    .filter(key -> key.startsWith(prefix))
                    .map(key -> new IndexedId(parseIndex(key, prefix), data.getLong(key)))
                    .filter(value -> value.index() >= 0 && value.index() < max && value.id() != 0)
                    .sorted((left, right) -> Integer.compare(left.index(), right.index()))
                    .map(IndexedId::id)
                    .collect(Collectors.toList());
        }

        private static int parseIndex(String key, String prefix) {
            try {
                return Integer.parseInt(key.substring(prefix.length()));
            } catch (NumberFormatException exception) {
                return -1;
            }
        }

        private boolean contains(String prefix, int max, BlockPos pos) {
            CompoundTag data = data();
            for (int i = 0; i < max; i++) {
                if (data.contains(prefix + i) && data.getLong(prefix + i) == pos.asLong()) return true;
            }
            return false;
        }

        private boolean containsLight(BlockPos pos) {
            CompoundTag data = data();
            for (String key : data.getAllKeys()) {
                if (key.startsWith("light") && data.getLong(key) == pos.asLong()) return true;
            }
            return false;
        }

        private CompoundTag data() {
            CompoundTag value = card.get(RTCDataComponents.CARD_DATA.get());
            return value == null ? new CompoundTag() : value.copy();
        }

        private void saveData(CompoundTag value) {
            card.set(RTCDataComponents.CARD_DATA.get(), value);
            host.markChanged();
        }

        private Level level() {
            return host.getEnvironmentLevel();
        }

        private int maxLights() {
            return TrafficLightCardItem.getMaxTrafficLights(TrafficLightCardItem.getTier(card));
        }

        private List<TrafficLightBlockEntity> pairedLights() {
            if (level() == null) return Collections.emptyList();
            ArrayList<TrafficLightBlockEntity> result = new ArrayList<>();
            for (long id : pairedIds("light", maxLights())) {
                BlockEntity be = level().getBlockEntity(BlockPos.of(id));
                if (be instanceof TrafficLightBlockEntity light) result.add(light);
            }
            return result;
        }

        private List<TrafficLightBlockEntity> pairedLights(Direction direction) {
            return pairedLights().stream()
                    .filter(light -> TrafficLightOcApproachHelper.resolveApproach(light) == direction)
                    .collect(Collectors.toList());
        }

        private TrafficLightBlockEntity light(BlockPos pos) {
            BlockEntity be = level().getBlockEntity(pos);
            return be instanceof TrafficLightBlockEntity light ? light : null;
        }

        private int replaceBulb(TrafficLightBlockEntity light, EnumTrafficLightBulbTypes oldBulb,
                EnumTrafficLightBulbTypes newBulb) {
            for (int slot = 0; slot < light.getBulbCount(); slot++) {
                for (int layer = 0; layer < 2; layer++) {
                    if (light.getBulbTypeBySlot(slot, layer) == oldBulb) {
                        light.setBulbType(slot, layer, newBulb);
                        light.setActive(newBulb, true, false);
                        return slot;
                    }
                }
            }
            return -1;
        }

        private Map<String, Map<String, Object>> stateInfo(TrafficLightBlockEntity light) {
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (String name : bulbs.keySet()) {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("active", false);
                state.put("flash", false);
                result.put(name, state);
            }
            for (int slot = 0; slot < light.getBulbCount(); slot++) {
                EnumTrafficLightBulbTypes displayed = light.getDisplayedBulbForSlot(slot);
                for (EnumTrafficLightBulbTypes type : light.getBulbTypesBySlot(slot)) {
                    if (type != null) {
                        boolean selected = type == displayed;
                        result.get(type.toString()).put("active", selected && light.getActiveBySlot(slot));
                        result.get(type.toString()).put("flash", selected && light.getFlashBySlot(slot));
                    }
                }
            }
            return result;
        }

        private void clear(TrafficLightBlockEntity light) {
            light.powerOff();
            light.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
        }

        private boolean consume(BlockPos target) {
            BlockPos origin = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
            double draw = Config.trafficLightCardDrawPerBlock * origin.distSqr(target);
            return node() instanceof ComponentConnector connector && connector.tryChangeBuffer(-draw);
        }

        private boolean tripsSensor(Entity entity) {
            if (entity instanceof ServerPlayer) return true;
            for (String configured : Config.sensorClasses) {
                Class<?> type = entity.getClass();
                while (type != null) {
                    if (configured.equals(type.getName())) return true;
                    type = type.getSuperclass();
                }
            }
            return false;
        }

        private DigitalSignControllerBlockEntity digitalController(Arguments args) {
            BlockEntity be = level().getBlockEntity(xyz(args, 0));
            return be instanceof DigitalSignControllerBlockEntity controller ? controller : null;
        }

        private MessageBoardControllerBlockEntity messageController(Arguments args) {
            BlockEntity be = level().getBlockEntity(xyz(args, 0));
            return be instanceof MessageBoardControllerBlockEntity controller ? controller : null;
        }

        private EnumTrafficLightBulbTypes bulb(String name) {
            return bulbs.get(name);
        }

        private static Direction direction(String name) {
            return TrafficLightOcApproachHelper.parseApproach(name);
        }

        private static BlockPos position(Arguments args) {
            return positionArgument(args).pos();
        }

        private static PositionArgument positionArgument(Arguments args) {
            if (args.count() >= 3 && args.isInteger(0) && args.isInteger(1) && args.isInteger(2)) {
                return new PositionArgument(new BlockPos(args.checkInteger(0), args.checkInteger(1),
                        args.checkInteger(2)), 3);
            }
            if (args.count() >= 1 && (args.isLong(0) || args.isInteger(0) || args.isDouble(0))) {
                return new PositionArgument(BlockPos.of(args.checkLong(0)), 1);
            }
            throw new IllegalArgumentException("Could not determine block position");
        }

        private static BlockPos xyz(Arguments args, int offset) {
            return new BlockPos(args.checkInteger(offset), args.checkInteger(offset + 1), args.checkInteger(offset + 2));
        }

        private static Object[] one(Object value) {
            return new Object[] { value };
        }

        private static Object[] result(boolean success, Object detail) {
            return new Object[] { success, detail };
        }

        private static Object[] noLights(Direction direction) {
            return result(false, "No paired lights for direction "
                    + TrafficLightOcApproachHelper.approachName(direction));
        }

        private record PositionArgument(BlockPos pos, int nextIndex) {
        }

        private record IndexedId(int index, long id) {
        }
    }
}
