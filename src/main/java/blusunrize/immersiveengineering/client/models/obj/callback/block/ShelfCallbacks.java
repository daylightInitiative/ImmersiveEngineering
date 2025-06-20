/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.client.models.obj.callback.block;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.client.ieobj.BlockCallback;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.models.obj.callback.block.ShelfCallbacks.Key;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic;
import blusunrize.immersiveengineering.common.register.IEBlocks.WoodenDevices;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShelfCallbacks implements BlockCallback<Key>
{
	public static final ShelfCallbacks INSTANCE = new ShelfCallbacks();

	private static final ResourceLocation TEXTURE_CRATE = IEApi.ieLoc("block/multiblocks/shelf_crate");
	private static final ResourceLocation TEXTURE_CRATE_REINFORCED = IEApi.ieLoc("block/multiblocks/shelf_crate_reinforced");

	@Override
	public Key extractKey(@Nonnull BlockAndTintGetter level, @Nonnull BlockPos pos, @Nonnull BlockState blockState, BlockEntity blockEntity)
	{
		if(blockEntity instanceof IMultiblockBE<?> multiblockBE&&
				multiblockBE.getHelper().getState() instanceof ShelfLogic.State state)
			return extractKey(state);
		return getDefaultKey();
	}

	public Key extractKey(ShelfLogic.State state)
	{
		Map<String, TextureAtlasSprite> texMap = new HashMap<>();
		Block block;
		for(int i = 0; i < state.renderCrates.length; i++)
			if(state.renderCrates[i]!=null&&(block = Block.byItem(state.renderCrates[i]))!=Blocks.AIR)
				if(block==WoodenDevices.CRATE.get())
					texMap.put("crate_"+i, ClientUtils.mc().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE_CRATE));
				else if(block==WoodenDevices.REINFORCED_CRATE.get())
					texMap.put("crate_"+i, ClientUtils.mc().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE_CRATE_REINFORCED));
		return new Key(texMap);
	}

	@Override
	public Key getDefaultKey()
	{
		return new Key(Collections.emptyMap());
	}

	@Override
	public boolean shouldRenderGroup(Key key, String group, RenderType layer)
	{
		if(group.startsWith("crate_"))
			return key.texMap().containsKey(group);
		return true;
	}

	@Override
	public @Nullable TextureAtlasSprite getTextureReplacement(Key key, String group, String material)
	{
		return key.texMap().get(group);
	}

	public record Key(Map<String, TextureAtlasSprite> texMap)
	{
	}
}
