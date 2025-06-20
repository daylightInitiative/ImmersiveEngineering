/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.gui;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic;
import blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity;
import blusunrize.immersiveengineering.common.gui.sync.GenericContainerData;
import blusunrize.immersiveengineering.common.gui.sync.GenericDataSerializers;
import blusunrize.immersiveengineering.common.gui.sync.GenericDataSerializers.DataPair;
import blusunrize.immersiveengineering.common.gui.sync.GetterAndSetter;
import blusunrize.immersiveengineering.mixin.accessors.ContainerAccess;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

public class ShelfMenu extends IEContainerMenu
{
	public static final int MAX_SLOTS = 8*WoodenCrateBlockEntity.CONTAINER_SIZE;

	private final Inventory inventoryPlayer;
	public final GetterAndSetter<List<ItemStack>> crates;

	public static ShelfMenu makeServer(
			MenuType<?> type, int id, Inventory invPlayer, MultiblockMenuContext<ShelfLogic.State> ctx
	)
	{
		final ShelfLogic.State state = ctx.mbContext().getState();
		BlockPos pos = ctx.mbContext().getLevel().toRelative(ctx.clickedPos());
		int level = pos.getY()-1;
		return new ShelfMenu(
				multiblockCtx(type, id, ctx), invPlayer,
				GetterAndSetter.getterOnly(() -> state.getCrates(level))
		);
	}

	public static ShelfMenu makeClient(MenuType<?> type, int id, Inventory invPlayer)
	{
		return new ShelfMenu(
				clientCtx(type, id),
				invPlayer,
				GetterAndSetter.standalone(List.of())
		);
	}

	public ShelfMenu(
			MenuContext ctx, Inventory inventoryPlayer, GetterAndSetter<List<ItemStack>> crates
	)
	{
		super(ctx);
		this.inventoryPlayer = inventoryPlayer;
		this.crates = crates;
		this.rebindSlots();
		addGenericData(new GenericContainerData<>(GenericDataSerializers.ITEM_STACKS, crates));
	}

	@Override
	public void receiveSync(List<Pair<Integer, DataPair<?>>> synced)
	{
		super.receiveSync(synced);
		this.rebindSlots();
	}


	public void rebindSlots()
	{
		this.slots.clear();
		((ContainerAccess)this).getLastSlots().clear();
		((ContainerAccess)this).getRemoteSlots().clear();

		ownSlotCount = 0;

		int iCrate = 0;
		for(ItemStack crate : this.crates.get())
		{
			NonNullList<ItemStack> crateItems = NonNullList.withSize(WoodenCrateBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY);
			final ItemContainerContents contents = crate.get(DataComponents.CONTAINER);
			if(contents!=null)
				contents.copyInto(crateItems);
			final ItemStackHandler crateInventory = new ItemStackHandler(crateItems)
			{
				@Override
				protected void onContentsChanged(int slot)
				{
					crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.stacks));
				}
			};

			int x = iCrate%2*176;
			int y = iCrate/2*64;
			for(int iSlot = 0; iSlot < crateInventory.getSlots(); iSlot++)
				this.addSlot(new SlotItemHandler(crateInventory, iSlot, 8+x+(iSlot%9)*18, 18+y+(iSlot/9)*18)
				{
					@Override
					public boolean mayPlace(ItemStack stack)
					{
						return IEApi.isAllowedInCrate(stack);
					}
				});

			iCrate++;
			ownSlotCount += crateInventory.getSlots();
		}

		// Add "useless" slots to keep the number of slots (and therefore the IDs of the player inventory slots)
		// constant. MC doesn't handle changing slot IDs well, causing desyncs
		for(; ownSlotCount < MAX_SLOTS; ++ownSlotCount)
			addSlot(new IESlot.AlwaysEmptySlot(this));

		int playerInvX = iCrate > 1?96: 8;
		int playerInvY = (iCrate+1)/2*64+10+13;

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlot(new Slot(inventoryPlayer, j+i*9+9, playerInvX+j*18, playerInvY+i*18));
		for(int i = 0; i < 9; i++)
			addSlot(new Slot(inventoryPlayer, i, playerInvX+i*18, playerInvY+58));

		ImmersiveEngineering.proxy.reInitGui();
	}

}