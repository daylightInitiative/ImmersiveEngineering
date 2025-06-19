/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.client.models.obj.callback.block;

import blusunrize.immersiveengineering.api.client.ieobj.BlockCallback;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockBlockEntityMaster;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class ShelfCallbacks implements BlockCallback<Integer>
{
	public static final ShelfCallbacks INSTANCE = new ShelfCallbacks();

	private static final boolean[] EMPTY = new boolean[ShelfLogic.NUM_CRATES];

	@Override
	public Integer extractKey(@Nonnull BlockAndTintGetter level, @Nonnull BlockPos pos, @Nonnull BlockState blockState, BlockEntity blockEntity)
	{
		if(blockEntity instanceof IMultiblockBE<?> multiblockBE&&
				multiblockBE.getHelper().getState() instanceof ShelfLogic.State state)
			return state.renderCrates;
		return getDefaultKey();
	}

	@Override
	public Integer getDefaultKey()
	{
		return 0;
	}

	@Override
	public boolean shouldRenderGroup(Integer key, String group, RenderType layer)
	{
		if(group.startsWith("crate_"))
		{
			int idx = Integer.parseInt(group.substring("crate_".length()));
			return ((key>>(23-idx))&1)==1;
//			return key[idx];
		}
		return true;
	}
}
