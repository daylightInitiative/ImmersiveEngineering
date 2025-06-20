/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.gui;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.gui.ShelfMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static blusunrize.immersiveengineering.common.gui.ShelfMenu.*;

public class ShelfScreen extends IEContainerScreen<ShelfMenu>
{
	private static final ResourceLocation TEXTURE = makeTextureLocation("shelf");

	private int playerInvX = 0;
	private int playerInvY = 0;

	public ShelfScreen(ShelfMenu container, Inventory inventoryPlayer, Component title)
	{
		super(container, inventoryPlayer, title, TEXTURE);
		this.titleLabelY = 3;
	}

	@Override
	protected void init()
	{
		int crates = this.menu.crates.get().size();
		int leftCount;
		int rightCount = 0;
		if(crates > 1)
		{
			leftCount = (crates+1)/2;
			rightCount = crates/2;
			this.playerInvX = COLUMN_WIDTH/2;
			this.imageWidth = COLUMN_WIDTH*2;
		}
		else
		{
			leftCount = 1;
			this.playerInvX = 0;
		}
		this.playerInvY = Math.max(leftCount, rightCount)*CRATE_SEGMENT;
		this.imageHeight = this.playerInvY+INV_SEGMENT;
		this.inventoryLabelY = this.playerInvY+3;
		super.init();
	}

	@Override
	protected void drawBackgroundTexture(GuiGraphics graphics)
	{
		// Crates
		List<ItemStack> crates = this.menu.crates.get();
		for(int i = 0; i < crates.size(); i++)
		{
			int x = i%2*COLUMN_WIDTH;
			int y = i/2*CRATE_SEGMENT;
			graphics.blit(background, leftPos+x, topPos+y, 0, INV_SEGMENT, COLUMN_WIDTH, CRATE_SEGMENT);
		}
		// Player Inventory
		graphics.blit(background, leftPos+playerInvX, topPos+playerInvY, 0, 0, COLUMN_WIDTH, INV_SEGMENT);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		List<ItemStack> crates = this.menu.crates.get();
		for(int i = 0; i < crates.size(); i++)
		{
			int x = i%2*COLUMN_WIDTH;
			int y = i/2*CRATE_SEGMENT;
			graphics.drawString(
					this.font, crates.get(i).getHoverName(),
					x+titleLabelX, y+titleLabelY,
					Lib.COLOUR_I_ImmersiveOrange, true
			);
		}
		graphics.drawString(
				this.font, playerInventoryTitle,
				playerInvX+inventoryLabelX, inventoryLabelY,
				Lib.COLOUR_I_ImmersiveOrange, true
		);
	}
}
