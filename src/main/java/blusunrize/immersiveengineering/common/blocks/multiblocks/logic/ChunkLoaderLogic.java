/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks.logic;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl.RSState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockBlockEntityMaster;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MBInventoryUtils;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.ChunkLoaderMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ChunkLoaderLogic.State;
import blusunrize.immersiveengineering.common.blocks.multiblocks.shapes.ChunkLoaderShapes;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraint;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraintGroup;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChunkLoaderLogic
		implements IMultiblockLogic<State>, IServerTickableComponent<State>, IClientTickableComponent<State>
{
	public static final TicketController TICKET_CONTROLLER = new TicketController(
			ResourceLocation.fromNamespaceAndPath(Lib.MODID, "resonant_observer"),
			(serverLevel, ticketHelper) -> {
				for(Entry<BlockPos, TicketSet> check : ticketHelper.getBlockTickets().entrySet())
				{
					boolean stillValid = false;
					if(serverLevel.getBlockEntity(check.getKey()) instanceof MultiblockBlockEntityMaster<?> mb)
						if(mb.getHelper().getState() instanceof State chunkLoaderState)
							stillValid = chunkLoaderState.refreshTimer > 0;
					if(!stillValid)
						ticketHelper.removeAllTickets(check.getKey());
				}
			}
	);

	public static final int ENERGY_CAPACITY = 32000;
	private static final int RADIUS = 1;

	private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(2, 1, 1, RelativeBlockFace.LEFT);
	public static final BlockPos REDSTONE_POS = new BlockPos(0, 1, 2);
	private static final CapabilityPosition INPUT_POS = new CapabilityPosition(0, 1, 1, RelativeBlockFace.RIGHT);

	@Override
	public void tickServer(IMultiblockContext<State> context)
	{
		final State state = context.getState();
		if(!(context.getLevel().getRawLevel() instanceof ServerLevel serverLevel))
			return;
		boolean isActive = state.refreshTimer > 0;
		BlockPos masterPos = context.getLevel().toAbsolute(ChunkLoaderMultiblock.MASTER_OFFSET);
		int energy_required = IEServerConfig.MACHINES.resonant_observer_consumption.get();
		if(state.rsState.isEnabled(context)&&state.energy.extractEnergy(energy_required, true)==energy_required)
		{
			if(!isActive&&!state.inventory.getStackInSlot(0).isEmpty())
			{
				// consume paper
				state.inventory.getStackInSlot(0).shrink(1);
				// consume energy
				state.energy.extractEnergy(energy_required, false);
				// set timer to 1 minute
				state.refreshTimer = 60*20;
				// mark chunks for loading
				forceChunks(serverLevel, masterPos, true);
			}
			else if(isActive)
				state.refreshTimer--;
			else
				forceChunks(serverLevel, masterPos, false);
		}
		else if(isActive)
		{
			state.refreshTimer = 0;
			forceChunks(serverLevel, masterPos, false);
		}
	}

	private void forceChunks(ServerLevel level, BlockPos masterPos, boolean add)
	{
		for(ChunkPos chunk : getChunks(masterPos))
			TICKET_CONTROLLER.forceChunk(level, masterPos, chunk.x, chunk.z, add, true);
	}

	private static ChunkPos[] getChunks(BlockPos masterPos)
	{
		int chunkX = SectionPos.blockToSectionCoord(masterPos.getX());
		int chunkZ = SectionPos.blockToSectionCoord(masterPos.getZ());
		ChunkPos[] array = new ChunkPos[(1+2*RADIUS)*(1+2*RADIUS)];
		int idx = 0;
		for(int xx = -RADIUS; xx <= RADIUS; xx++)
			for(int zz = -RADIUS; zz <= RADIUS; zz++)
				array[idx++] = new ChunkPos(chunkX+xx, chunkZ+zz);
		return array;
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
		public int refreshTimer = 0;

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
			nbt.putInt("refreshTimer", refreshTimer);
		}

		@Override
		public void readSyncNBT(CompoundTag nbt, Provider provider)
		{
			refreshTimer = nbt.getInt("refreshTimer");
		}

		public List<String> getNearbyBlockEntities(IMultiblockContext<State> ctx)
		{
			BlockPos masterPos = ctx.getLevel().toAbsolute(ChunkLoaderMultiblock.MASTER_OFFSET);
			ChunkPos[] chunks = ChunkLoaderLogic.getChunks(masterPos);
			Level level = ctx.getLevel().getRawLevel();
			Map<MutableComponent, Long> all = Arrays.stream(chunks)
					// find all block entities in the area
					.flatMap(pos -> level.getChunk(pos.x, pos.z).getBlockEntities().values().stream())
					// filter to ticking ones
					.filter(blockEntity -> !masterPos.equals(blockEntity.getBlockPos())&&blockEntity.getBlockState().getTicker(level, blockEntity.getType())!=null)
					// collect them into a map by count
					.collect(Collectors.groupingBy(blockEntity -> blockEntity.getBlockState().getBlock().getName(), Collectors.counting()));
			// then make that into strings - yes this will be serverside localization, I don't care.
			return all.entrySet().stream().map(entry -> Component.literal(+entry.getValue()+"x ").append(entry.getKey()).getString()).toList();
		}
	}
}
