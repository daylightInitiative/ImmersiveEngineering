/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.gui;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.energy.IMutableEnergyStorage;
import blusunrize.immersiveengineering.api.energy.MutableEnergyStorage;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ChunkLoaderLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ChunkLoaderLogic.State;
import blusunrize.immersiveengineering.common.gui.sync.GenericContainerData;
import blusunrize.immersiveengineering.common.gui.sync.GetterAndSetter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class ChunkLoaderMenu extends IEContainerMenu
{
	public static ChunkLoaderMenu makeServer(
			MenuType<?> type, int id, Inventory invPlayer, MultiblockMenuContext<State> ctx
	)
	{
		final State state = ctx.mbContext().getState();
		return new ChunkLoaderMenu(
				multiblockCtx(type, id, ctx),
				invPlayer,
				state.inventory,
				state.energy,
				state.getNearbyBlockEntities(ctx.mbContext()),
				GetterAndSetter.getterOnly(()->state.refreshTimer)
		);
	}

	public static ChunkLoaderMenu makeClient(MenuType<?> type, int id, Inventory invPlayer)
	{
		return new ChunkLoaderMenu(
				clientCtx(type, id),
				invPlayer,
				new ItemStackHandler(1),
				new MutableEnergyStorage(ChunkLoaderLogic.ENERGY_CAPACITY),
				new ArrayList<>(),
				GetterAndSetter.standalone(0)
		);
	}

	public final IEnergyStorage energy;
	public final List<String> blockEntityList;
	public final GetterAndSetter<Integer> refreshTimer;

	public ChunkLoaderMenu(
			MenuContext ctx, Inventory inventoryPlayer, IItemHandler inv,
			IMutableEnergyStorage energy, List<String> blockEntityList, GetterAndSetter<Integer> refreshTimer
	)
	{
		super(ctx);
		this.energy = energy;
		this.blockEntityList = blockEntityList;
		this.refreshTimer = refreshTimer;

		this.addSlot(new IESlot.Tagged(inv, ownSlotCount++, 124, 94, IETags.paper));

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlot(new Slot(inventoryPlayer, j+i*9+9, 8+j*18, 160+i*18));
		for(int i = 0; i < 9; i++)
			addSlot(new Slot(inventoryPlayer, i, 8+i*18, 218));
		addGenericData(GenericContainerData.energy(energy));
		addGenericData(GenericContainerData.strings(blockEntityList));
		addGenericData(GenericContainerData.int32(refreshTimer.getter(), refreshTimer.setter()));
	}
}