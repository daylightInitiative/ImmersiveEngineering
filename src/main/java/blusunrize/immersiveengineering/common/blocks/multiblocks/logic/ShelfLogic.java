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
import blusunrize.immersiveengineering.common.gui.ShelfMenu;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ShelfLogic implements IMultiblockLogic<State>, MBOverlayText<State>
{
	public static int NUM_CRATES = 24;

	public static final Supplier<Map<Item, CrateVariant>> CRATE_VARIANTS = Suppliers.memoize(() -> {
		Map<Item, CrateVariant> map = new HashMap<>();
		map.put(WoodenDevices.CRATE.asItem(), new CrateVariant("block/multiblocks/shelf_crate"));
		map.put(WoodenDevices.REINFORCED_CRATE.asItem(), new CrateVariant("block/multiblocks/shelf_crate_reinforced"));
		map.put(Blocks.SHULKER_BOX.asItem(), new CrateVariant("block/shulker_box", 0x503750));
		map.put(Blocks.WHITE_SHULKER_BOX.asItem(), new CrateVariant("block/white_shulker_box", DyeColor.WHITE.getTextColor()));
		map.put(Blocks.ORANGE_SHULKER_BOX.asItem(), new CrateVariant("block/orange_shulker_box", DyeColor.ORANGE.getTextColor()));
		map.put(Blocks.MAGENTA_SHULKER_BOX.asItem(), new CrateVariant("block/magenta_shulker_box", DyeColor.MAGENTA.getTextColor()));
		map.put(Blocks.LIGHT_BLUE_SHULKER_BOX.asItem(), new CrateVariant("block/light_blue_shulker_box", DyeColor.LIGHT_BLUE.getTextColor()));
		map.put(Blocks.YELLOW_SHULKER_BOX.asItem(), new CrateVariant("block/yellow_shulker_box", DyeColor.YELLOW.getTextColor()));
		map.put(Blocks.LIME_SHULKER_BOX.asItem(), new CrateVariant("block/lime_shulker_box", DyeColor.LIME.getTextColor()));
		map.put(Blocks.PINK_SHULKER_BOX.asItem(), new CrateVariant("block/pink_shulker_box", DyeColor.PINK.getTextColor()));
		map.put(Blocks.GRAY_SHULKER_BOX.asItem(), new CrateVariant("block/gray_shulker_box", DyeColor.GRAY.getTextColor()));
		map.put(Blocks.LIGHT_GRAY_SHULKER_BOX.asItem(), new CrateVariant("block/light_gray_shulker_box", DyeColor.LIGHT_GRAY.getTextColor()));
		map.put(Blocks.CYAN_SHULKER_BOX.asItem(), new CrateVariant("block/cyan_shulker_box", DyeColor.CYAN.getTextColor()));
		map.put(Blocks.PURPLE_SHULKER_BOX.asItem(), new CrateVariant("block/purple_shulker_box", DyeColor.PURPLE.getTextColor()));
		map.put(Blocks.BLUE_SHULKER_BOX.asItem(), new CrateVariant("block/blue_shulker_box", DyeColor.BLUE.getTextColor()));
		map.put(Blocks.BROWN_SHULKER_BOX.asItem(), new CrateVariant("block/brown_shulker_box", DyeColor.BROWN.getTextColor()));
		map.put(Blocks.GREEN_SHULKER_BOX.asItem(), new CrateVariant("block/green_shulker_box", DyeColor.GREEN.getTextColor()));
		map.put(Blocks.RED_SHULKER_BOX.asItem(), new CrateVariant("block/red_shulker_box", DyeColor.RED.getTextColor()));
		map.put(Blocks.BLACK_SHULKER_BOX.asItem(), new CrateVariant("block/black_shulker_box", DyeColor.BLACK.getTextColor()));
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
			final State state = ctx.getState();
			int crateIndex = state.getCrateIndex(posInMultiblock);
			final ItemStack heldItem = player.getItemInHand(hand);
			boolean isHoldingCrate = CRATE_VARIANTS.get().containsKey(heldItem.getItem());
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

		public List<ItemStack> getCratesForMenu(BlockPos posInMultiblock, boolean backside)
		{
			int startIdx = (posInMultiblock.getY()-1)*8+posInMultiblock.getZ();
			if(backside)
				startIdx += posInMultiblock.getZ() > 0?-1: +1;
			return Stream.of(
					crates.get(startIdx),
					crates.get(startIdx+2),
					crates.get(startIdx+4),
					crates.get(startIdx+6)
			).filter(s -> !s.isEmpty()).toList();
		}

		public int getCrateIndex(BlockPos posInMultiblock)
		{
			return (posInMultiblock.getY()-1)*8+posInMultiblock.getX()*2+posInMultiblock.getZ();
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
				CrateVariant variant = CRATE_VARIANTS.get().get(item);
				this.renderCrates[i] = variant!=null?variant.crateTexture(): null;
				this.names[i] = Component.Serializer.fromJson(tag.getString("name"), provider);
			}
			this.doUpdate.run();
		}
	}

	public record CrateVariant(ResourceLocation crateTexture, int screenVOffset, int color)
	{
		public CrateVariant(String crateTexture)
		{
			this(IEApi.ieLoc(crateTexture), 0, -1);
		}

		public CrateVariant(String crateTexture, int color)
		{
			this(ResourceLocation.withDefaultNamespace(crateTexture), ShelfMenu.CRATE_SEGMENT, color);
		}
	}
}
