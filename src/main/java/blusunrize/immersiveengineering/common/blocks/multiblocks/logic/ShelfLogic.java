/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks.logic;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.CrateItem;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic.State;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.interfaces.MBOverlayText;
import blusunrize.immersiveengineering.common.blocks.multiblocks.shapes.ShelfShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ShelfLogic implements IMultiblockLogic<State>, MBOverlayText<State>
{
	public static int NUM_CRATES = 24;

	@Override
	public State createInitialState(IInitialMultiblockContext<State> capabilitySource)
	{
		return new State(capabilitySource);
	}

	@Override
	public void dropExtraItems(State state, Consumer<ItemStack> drop)
	{
		state.crates.forEach(stack -> {
			if(!stack.isEmpty())
				drop.accept(stack.copy());
		});
	}

	@Override
	public ItemInteractionResult click(
			IMultiblockContext<State> ctx, BlockPos posInMultiblock,
			Player player, InteractionHand hand, BlockHitResult absoluteHit, boolean isClient
	)
	{
		if(posInMultiblock.getY() < 1)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!isClient)
		{
			int crateIndex = (posInMultiblock.getY()-1)*8+posInMultiblock.getX()*2+posInMultiblock.getZ();
			final ItemStack heldItem = player.getItemInHand(hand);
			final State state = ctx.getState();
			final ItemStack storedCrate = state.crates.get(crateIndex);

			if(player.isCrouching()&&heldItem.isEmpty()&&!storedCrate.isEmpty())
			{
				// Get crate from shelf
				player.setItemInHand(hand, storedCrate);
				state.crates.set(crateIndex, ItemStack.EMPTY);
				ctx.markMasterDirty();
				ctx.requestMasterBESync();
			}
			else if(heldItem.getItem() instanceof CrateItem&&storedCrate.isEmpty())
			{
				// Place crate on shelf
				state.crates.set(crateIndex, heldItem);
				player.setItemInHand(hand, ItemStack.EMPTY);
				ctx.markMasterDirty();
				ctx.requestMasterBESync();
			}
			else
				player.openMenu(IEMenuTypes.SHELF.provide(ctx, posInMultiblock));
		}
		return ItemInteractionResult.sidedSuccess(isClient);
	}

	@Override
	public @Nullable List<Component> getOverlayText(State state, BlockPos posInMultiblock, BlockHitResult absoluteHit, Player player, boolean hammer)
	{
		if(posInMultiblock.getY() < 1||state==null)
			return List.of();
		int crateIndex = (posInMultiblock.getY()-1)*8+posInMultiblock.getX()*2+posInMultiblock.getZ();
		Component name = state.names[crateIndex];
		if(name!=null&&!Component.empty().equals(name))
			return List.of(name);
		return List.of();
	}

	@Override
	public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType)
	{
		return ShelfShapes.SHAPE_GETTER;
	}

	public static class State implements IMultiblockState
	{
		public final NonNullList<ItemStack> crates = NonNullList.withSize(NUM_CRATES, ItemStack.EMPTY);

		public int renderCrates = 0;
		public Component[] names = new Component[NUM_CRATES];
		private final Runnable doUpdate;

		public State(IInitialMultiblockContext<State> ctx)
		{
			doUpdate = ctx.getBlockUpdateRunnable();
		}

		public List<ItemStack> getCrates(int level)
		{
			return crates.subList(level*8, (level+1)*8).stream().filter(s -> !s.isEmpty()).toList();
		}

		@Override
		public void writeSaveNBT(CompoundTag nbt, Provider provider)
		{
			ContainerHelper.saveAllItems(nbt, crates, provider);
		}

		@Override
		public void readSaveNBT(CompoundTag nbt, Provider provider)
		{
			ContainerHelper.loadAllItems(nbt, crates, provider);
		}

		@Override
		public void writeSyncNBT(CompoundTag nbt, Provider provider)
		{
			ListTag names = new ListTag();
			int cratesAsInt = 0;
			for(ItemStack stack : this.crates)
			{
				Component name = stack.isEmpty()?Component.empty(): stack.getHoverName();
				names.add(StringTag.valueOf(Component.Serializer.toJson(name, provider)));
				cratesAsInt = (cratesAsInt<<1)|(stack.isEmpty()?0: 1);
			}
			nbt.put("names", names);
			nbt.putInt("renderCrates", cratesAsInt);
		}

		@Override
		public void readSyncNBT(CompoundTag nbt, Provider provider)
		{
			ListTag names = nbt.getList("names", 8);
			for(int i = 0; i < NUM_CRATES; i++)
				this.names[i] = Component.Serializer.fromJson(names.getString(i), provider);
			this.renderCrates = nbt.getInt("renderCrates");
			this.doUpdate.run();
		}
	}

}
