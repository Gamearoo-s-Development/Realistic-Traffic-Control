package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.ArrayList;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public abstract class BaseTrafficLightFrameContainer extends Container {
	IItemHandler frameStackHandler;
	ItemStack frameStack;
	protected final List<FrameSlotInfo> frameSlotInfos;
	protected final int frameSlotStartIndex;
	
	public BaseTrafficLightFrameContainer(InventoryPlayer inventory, ItemStack frameStack)
	{
		BaseItemTrafficLightFrame frameItem = getValidFrameItem();
		frameStack = ensureHandlerCapacity(inventory, frameStack, frameItem);
		this.frameStack = frameStack;

		int ySize = getYSize();
		
		// Hot bar
		for(int i = 0; i < 9; i++)
		{
			addSlotToContainer(new Slot(inventory, i, 7 + i * 18, 177 + (ySize - 200)));
		}
		
		// Player inventory
		for(int i = 9; i < 36; i++)
		{
			int rowWork = i;
			int row = 0;
			while (rowWork - 9 >= 9)
			{
				row++;
				rowWork -= 9;
			}
			
			int column = i % 9;
			
			addSlotToContainer(new Slot(inventory, i, 7 + column * 18, 119 + (ySize - 200) + 18 * row));
		}
		
		frameStackHandler = frameStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		int handlerSlots = frameStackHandler != null ? frameStackHandler.getSlots() : 0;
		int layerOffset = handlerSlots / 2;
		List<FrameSlotInfo> allSlotInfos = buildSlotInfo();
		List<FrameSlotInfo> usableSlotInfos = new ArrayList<>();
		for (FrameSlotInfo slotInfo : allSlotInfos)
		{
			if (slotInfo.getSlotIndex() < layerOffset)
			{
				usableSlotInfos.add(slotInfo);
			}
		}
		this.frameSlotInfos = usableSlotInfos;
		int containerIndex = inventorySlots.size();
		frameSlotStartIndex = containerIndex;
		for(FrameSlotInfo slotInfo : frameSlotInfos)
		{
			SlotItemHandlerListenable primary = new SlotItemHandlerListenable(frameStackHandler, slotInfo.getSlotIndex(), slotInfo.getPrimaryX(), slotInfo.getPrimaryY());
			addSlotToContainer(primary);
			slotInfo.attachPrimarySlot(primary, containerIndex++);
			int secondaryIndex = slotInfo.getSlotIndex() + layerOffset;
			if (secondaryIndex < handlerSlots)
			{
				SlotItemHandlerListenable secondary = new SlotItemHandlerListenable(frameStackHandler, secondaryIndex, slotInfo.getSecondaryX(), slotInfo.getSecondaryY());
				addSlotToContainer(secondary);
				slotInfo.attachSecondarySlot(secondary, containerIndex++);
			}
		}
	}
	
	protected abstract List<FrameSlotInfo> buildSlotInfo();
	
	protected abstract int getYSize();
	
	public ItemStack getFrameStack() { return frameStack; }

	public List<FrameSlotInfo> getFrameSlotInfos() { return frameSlotInfos; }
	
	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return playerIn.getHeldItemMainhand().getItem() == getValidFrameItem();
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		Slot slot = inventorySlots.get(index);
		ItemStack originStack = null;
		ItemStack destinationStack = ItemStack.EMPTY;
		
		if (slot.getHasStack())
		{
			originStack = slot.getStack();
			destinationStack = originStack.copy();
			
			if (index >= frameSlotStartIndex)
			{
				if (!mergeItemStack(originStack, 0, frameSlotStartIndex, false))
				{
					return ItemStack.EMPTY;
				}
			}
			else
			{
				if (!mergeIntoFrameSlots(originStack))
				{
					return ItemStack.EMPTY;
				}
			}
		}
		else
		{
			return ItemStack.EMPTY;
		}
		
		if (originStack.isEmpty())
		{
			slot.putStack(ItemStack.EMPTY);
		}
		else
		{
			slot.onSlotChanged();
		}
		
		return destinationStack;
	}

	private boolean mergeIntoFrameSlots(ItemStack originStack)
	{
		for (int slotIndex = frameSlotStartIndex; slotIndex < inventorySlots.size(); slotIndex++)
		{
			Slot frameSlot = inventorySlots.get(slotIndex);
			if (!frameSlot.getHasStack() && frameSlot.isItemValid(originStack))
			{
				mergeItemStack(originStack, slotIndex, slotIndex + 1, false);
				return true;
			}
		}
		return false;
	}
	
	protected abstract BaseItemTrafficLightFrame getValidFrameItem();

	private ItemStack ensureHandlerCapacity(InventoryPlayer inventory, ItemStack stack, BaseItemTrafficLightFrame frameItem)
	{
		if (stack == null || stack.isEmpty())
		{
			return stack;
		}

		IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		int expectedSlots = frameItem.getBulbCount() * 2;
		if (handler == null || handler.getSlots() >= expectedSlots)
		{
			return stack;
		}

		List<ItemStack> existingStacks = new ArrayList<>(handler.getSlots());
		for (int i = 0; i < handler.getSlots(); i++)
		{
			existingStacks.add(handler.getStackInSlot(i).copy());
		}

		ItemStack upgraded = new ItemStack(stack.getItem(), stack.getCount(), stack.getMetadata());
		if (stack.hasTagCompound())
		{
			upgraded.setTagCompound(stack.getTagCompound().copy());
		}

		IItemHandler upgradedHandler = upgraded.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		if (upgradedHandler instanceof IItemHandlerModifiable)
		{
			IItemHandlerModifiable modifiable = (IItemHandlerModifiable)upgradedHandler;
			int limit = Math.min(modifiable.getSlots(), existingStacks.size());
			for (int i = 0; i < limit; i++)
			{
				modifiable.setStackInSlot(i, existingStacks.get(i));
			}
		}

		if (inventory != null)
		{
			int slotIndex = inventory.currentItem;
			if (slotIndex >= 0 && slotIndex < inventory.getSizeInventory())
			{
				inventory.setInventorySlotContents(slotIndex, upgraded);
			}
		}

		return upgraded;
	}

	public static class FrameSlotInfo
	{
		private static final int DEFAULT_SECONDARY_OFFSET_X = 20;
		private static final int DEFAULT_SECONDARY_OFFSET_Y = 0;

		private final EnumCheckboxOrientation checkboxOrientation;
		private final int slotIndex;
		private final int primaryX;
		private final int primaryY;
		private final int secondaryOffsetX;
		private final int secondaryOffsetY;

		private SlotItemHandlerListenable primarySlot;
		private SlotItemHandlerListenable secondarySlot;
		private int primaryContainerSlotIndex = -1;
		private int secondaryContainerSlotIndex = -1;

		public FrameSlotInfo(EnumCheckboxOrientation orientation, int slotIndex, int x, int y)
		{
			this(orientation, slotIndex, x, y, DEFAULT_SECONDARY_OFFSET_X, DEFAULT_SECONDARY_OFFSET_Y);
		}

		public FrameSlotInfo(EnumCheckboxOrientation orientation, int slotIndex, int x, int y, int secondaryOffsetX, int secondaryOffsetY)
		{
			this.checkboxOrientation = orientation;
			this.slotIndex = slotIndex;
			this.primaryX = x;
			this.primaryY = y;
			this.secondaryOffsetX = secondaryOffsetX;
			this.secondaryOffsetY = secondaryOffsetY;
		}

		void attachPrimarySlot(SlotItemHandlerListenable slot, int containerIndex)
		{
			this.primarySlot = slot;
			this.primaryContainerSlotIndex = containerIndex;
		}

		void attachSecondarySlot(SlotItemHandlerListenable slot, int containerIndex)
		{
			this.secondarySlot = slot;
			this.secondaryContainerSlotIndex = containerIndex;
		}

		public EnumCheckboxOrientation getCheckboxOrientation()
		{
			return checkboxOrientation;
		}

		public int getSlotIndex()
		{
			return slotIndex;
		}

		public int getPrimaryX()
		{
			return primaryX;
		}

		public int getPrimaryY()
		{
			return primaryY;
		}

		public int getSecondaryX()
		{
			return primaryX + secondaryOffsetX;
		}

		public int getSecondaryY()
		{
			return primaryY + secondaryOffsetY;
		}

		public SlotItemHandlerListenable getPrimarySlot()
		{
			return primarySlot;
		}

		public SlotItemHandlerListenable getSecondarySlot()
		{
			return secondarySlot;
		}

		public boolean hasSecondarySlot()
		{
			return secondarySlot != null;
		}

		public int getPrimaryContainerSlotIndex()
		{
			return primaryContainerSlotIndex;
		}

		public int getSecondaryContainerSlotIndex()
		{
			return secondaryContainerSlotIndex;
		}

		public enum EnumCheckboxOrientation
		{
			LEFT,
			RIGHT,
			ABOVE,
			BELOW;
		}
	}
}
