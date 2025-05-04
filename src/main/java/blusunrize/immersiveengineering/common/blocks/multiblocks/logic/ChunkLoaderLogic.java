/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks.logic;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl.RSState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MBInventoryUtils;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ChunkLoaderLogic.State;
import blusunrize.immersiveengineering.common.blocks.multiblocks.shapes.ChunkLoaderShapes;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraint;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraintGroup;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChunkLoaderLogic
		implements IMultiblockLogic<State>, IServerTickableComponent<State>, IClientTickableComponent<State>
{
	public static final int ENERGY_CAPACITY = 32000;

	private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(2, -1, 1, RelativeBlockFace.RIGHT);
	public static final BlockPos REDSTONE_POS = new BlockPos(0, 1, 2);
	private static final CapabilityPosition INPUT_POS = new CapabilityPosition(0, 1, 0, RelativeBlockFace.RIGHT);

	@Override
	public void tickServer(IMultiblockContext<State> context)
	{

	}

	@Override
	public void tickClient(IMultiblockContext<State> context)
	{
	}

	@Override
	public State createInitialState(IInitialMultiblockContext<State> capabilitySource)
	{
		return new State(capabilitySource);
	}

	@Override
	public void registerCapabilities(CapabilityRegistrar<State> register)
	{
		register.registerAt(ItemHandler.BLOCK, INPUT_POS, state -> state.input);
		register.registerAt(EnergyStorage.BLOCK, ENERGY_INPUT, state -> state.energy);
	}

	@Override
	public void dropExtraItems(State state, Consumer<ItemStack> drop)
	{
		MBInventoryUtils.dropItems(state.inventory, drop);
	}

	@Override
	public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType)
	{
		return ChunkLoaderShapes.SHAPE_GETTER;
	}

	public static class State implements IMultiblockState
	{
		public final SlotwiseItemHandler inventory;
		public final AveragingEnergyStorage energy = new AveragingEnergyStorage(ENERGY_CAPACITY);
		public final RSState rsState = RSState.enabledByDefault();
		private int refreshTimer = 0;

		private final IItemHandler input;

		public State(IInitialMultiblockContext<State> ctx)
		{
			this.inventory = SlotwiseItemHandler.makeWithGroups(
					List.of(new IOConstraintGroup(new IOConstraint(true, i -> i.is(IETags.paper)), 1)),
					ctx.getMarkDirtyRunnable()
			);
			this.input = new WrappingItemHandler(inventory, true, false);
		}

		@Override
		public void writeSaveNBT(CompoundTag nbt, Provider provider)
		{
			nbt.put("inventory", inventory.serializeNBT(provider));
			nbt.put("energy", energy.serializeNBT(provider));
			nbt.putInt("refreshTimer", refreshTimer);
		}

		@Override
		public void readSaveNBT(CompoundTag nbt, Provider provider)
		{
			inventory.deserializeNBT(provider, nbt.getCompound("inventory"));
			energy.deserializeNBT(provider, nbt.getCompound("energy"));
			refreshTimer = nbt.getInt("refreshTimer");
		}

		@Override
		public void writeSyncNBT(CompoundTag nbt, Provider provider)
		{
			// write a dummy value to prevent NPE exceptions
			nbt.putBoolean("npe_avoid", true);
		}

		@Override
		public void readSyncNBT(CompoundTag nbt, Provider provider)
		{
		}
	}
}
