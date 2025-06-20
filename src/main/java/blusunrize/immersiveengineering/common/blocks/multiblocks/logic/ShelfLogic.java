/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks.logic;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic.State;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.interfaces.MBOverlayText;
import blusunrize.immersiveengineering.common.blocks.multiblocks.shapes.ShelfShapes;
import blusunrize.immersiveengineering.common.register.IEBlocks.WoodenDevices;
import blusunrize.immersiveengineering.common.register.IEMenuTypes;
import com.google.common.base.Suppliers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ShelfLogic implements IMultiblockLogic<State>, MBOverlayText<State>
{
	public static int NUM_CRATES = 24;

	public static final Supplier<Map<Item, ResourceLocation>> CRATE_VARIANTS = Suppliers.memoize(() -> {
		Map<Item, ResourceLocation> map = new HashMap<>();
		map.put(WoodenDevices.CRATE.asItem(), IEApi.ieLoc("block/multiblocks/shelf_crate"));
		map.put(WoodenDevices.REINFORCED_CRATE.asItem(), IEApi.ieLoc("block/multiblocks/shelf_crate_reinforced"));
		return map;
	});

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
			boolean isHoldingCrate = CRATE_VARIANTS.get().containsKey(heldItem.getItem());
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
			else if(isHoldingCrate&&storedCrate.isEmpty())
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

		public ResourceLocation[] renderCrates = new ResourceLocation[NUM_CRATES];
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
			ListTag crates = new ListTag();
			int cratesAsInt = 0;
			for(ItemStack stack : this.crates)
			{
				CompoundTag tag = new CompoundTag();
				Component name = stack.isEmpty()?Component.empty(): stack.getHoverName();
				tag.putString("name", Component.Serializer.toJson(name, provider));
				tag.putString("id", stack.getItemHolder().getKey().location().toString());
				crates.add(tag);
				cratesAsInt = (cratesAsInt<<1)|(stack.isEmpty()?0: 1);
			}
			nbt.put("crates", crates);
		}

		@Override
		public void readSyncNBT(CompoundTag nbt, Provider provider)
		{
			ListTag names = nbt.getList("crates", 10);
			for(int i = 0; i < NUM_CRATES; i++)
			{
				CompoundTag tag = names.getCompound(i);
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString("id")));
				this.renderCrates[i] = CRATE_VARIANTS.get().get(item);
				this.names[i] = Component.Serializer.fromJson(tag.getString("name"), provider);
			}
			this.doUpdate.run();
		}
	}

}
