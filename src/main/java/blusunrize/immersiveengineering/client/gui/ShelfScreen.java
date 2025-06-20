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

import javax.annotation.Nonnull;
import java.util.List;

public class ShelfScreen extends IEContainerScreen<ShelfMenu>
{
	private static final ResourceLocation TEXTURE = makeTextureLocation("shelf");

	private static final int COLUMN_WIDTH = 176;
	private static final int TOP_BAR = 7;
	private static final int CRATE_SEGMENT = 64;
	private static final int BOTTOM_BAR = 3;
	private static final int INV_SEGMENT = 94;

	private int leftCount = 0;
	private int rightCount = 0;
	private int playerInvX = 0;
	private int playerInvY = 0;

	public ShelfScreen(ShelfMenu container, Inventory inventoryPlayer, Component title)
	{
		super(container, inventoryPlayer, title, TEXTURE);
	}

	@Override
	protected void init()
	{
		int crates = this.menu.crates.get().size();
		if(crates > 1)
		{
			this.leftCount = (crates+1)/2;
			this.rightCount = crates/2;
			this.playerInvX = COLUMN_WIDTH/2;
			this.imageWidth = COLUMN_WIDTH*2;
		}
		else
		{
			this.leftCount = 1;
			this.playerInvX = 0;
		}
		this.playerInvY = Math.max(leftCount, rightCount)*CRATE_SEGMENT+TOP_BAR+BOTTOM_BAR;
		this.imageHeight = this.playerInvY+INV_SEGMENT;
		super.init();
	}

	@Override
	protected void drawBackgroundTexture(GuiGraphics graphics)
	{
		// Crates
		List<ItemStack> crates = this.menu.crates.get();
		if(leftCount > 0)
		{
			graphics.blit(background, leftPos, topPos, 0, 0, COLUMN_WIDTH, TOP_BAR);
			graphics.blit(background, leftPos, topPos+TOP_BAR+leftCount*CRATE_SEGMENT, 0, 73, 176, BOTTOM_BAR);
		}
		if(rightCount > 0)
		{
			graphics.blit(background, leftPos+COLUMN_WIDTH, topPos, 0, 0, COLUMN_WIDTH, TOP_BAR);
			graphics.blit(background, leftPos+COLUMN_WIDTH, topPos+TOP_BAR+rightCount*CRATE_SEGMENT, 0, 73, COLUMN_WIDTH, BOTTOM_BAR);
		}
		for(int i = 0; i < crates.size(); i++)
		{
			int x = i%2*COLUMN_WIDTH;
			int y = i/2*CRATE_SEGMENT;
			graphics.blit(background, leftPos+x, topPos+TOP_BAR+y, 0, 8, COLUMN_WIDTH, CRATE_SEGMENT);
		}
		// Player Inventory
		graphics.blit(background, leftPos+playerInvX, topPos+playerInvY, 0, 77, COLUMN_WIDTH, INV_SEGMENT);
	}

	@Override
	protected void drawContainerBackgroundPre(@Nonnull GuiGraphics graphics, float f, int mx, int my)
	{

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
					x+titleLabelX, y+titleLabelY+2,
					Lib.COLOUR_I_ImmersiveOrange, true
			);
		}
		graphics.drawString(
				this.font, playerInventoryTitle,
				playerInvX+inventoryLabelX, playerInvY+2,
				Lib.COLOUR_I_ImmersiveOrange, true
		);
	}
}
