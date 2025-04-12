/*
 * BluSunrize
 * Copyright (c) 2025
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tooltip;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.client.ClientUtils;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TODO:
 * We can hopefully remove all this stuff in 1.21.4 when we have proper text shadow colours!
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Lib.MODID, bus = Bus.GAME)
public class TooltipTextShadowUtils
{
	public static boolean hasCustomShadows(FormattedText component)
	{
		return component.visit((style, s) -> {
			String ins = style.getInsertion();
			if(ins!=null&&ins.startsWith(Lib.TEXT_SHADOW_KEY))
				return Optional.of(true);
			return Optional.empty();
		}, Style.EMPTY).orElse(false);
	}

	public static FormattedText getShadowText(FormattedText component)
	{
		List<FormattedText> list = new ArrayList<>();
		component.visit((style, text) -> {
			if(style.getColor()!=null)
			{
				int color = style.getColor().getValue();
				String ins = style.getInsertion();
				if(ins!=null&&ins.startsWith(Lib.TEXT_SHADOW_KEY))
					color = Integer.parseInt(ins.substring(Lib.TEXT_SHADOW_KEY.length()));
				else
				{
					// Vanilla calculation, reduce RGB values to 25%
					int[] rgba = {
							Math.round((color>>16&255)*.25f),
							Math.round((color>>8&255)*.25f),
							Math.round((color&255)*.25f),
							(color>>24&255),
					};
					color = (rgba[3]<<24)|(rgba[0]<<16)|(rgba[1]<<8)|rgba[2];
				}
				style = style.withColor(color);
			}
			list.add(FormattedText.of(text, style));
			return Optional.empty();
		}, Style.EMPTY);
		return FormattedText.composite(list);
	}

	public static void drawWithCustomShadows(Matrix4f matrix, FormattedText component, int x, int y, MultiBufferSource.BufferSource buffer)
	{
		FormattedText shadowed = getShadowText(component);
		ClientUtils.font().drawInBatch(
				Language.getInstance().getVisualOrder(shadowed),
				x+1, y+1, 0xff404040, false,
				matrix, buffer, DisplayMode.NORMAL, 0, 0xf000f0
		);
		matrix.translate(0, 0, 0.03F);
		ClientUtils.font().drawInBatch(
				Language.getInstance().getVisualOrder(component),
				x, y, 0xffffffff, false,
				matrix, buffer, DisplayMode.NORMAL, 0, 0xf000f0
		);
	}


	@SubscribeEvent
	public static void onTooltipComponents(RenderTooltipEvent.GatherComponents ev)
	{
		ev.getTooltipElements().replaceAll(either -> either.map(
				formattedText -> {
					if(TooltipTextShadowUtils.hasCustomShadows(formattedText))
						return Either.right(new TextShadowServerTooltip(formattedText));
					return either;
				},
				tooltipComponent -> either)
		);
	}

	public record TextShadowServerTooltip(FormattedText component) implements TooltipComponent
	{
	}

	public record TextShadowClientTooltip(TextShadowServerTooltip data) implements ClientTooltipComponent
	{
		@Override
		public int getWidth(Font font)
		{
			return font.width(this.data.component());
		}

		@Override
		public int getHeight()
		{
			return 10;
		}

		@Override
		public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource)
		{
			drawWithCustomShadows(matrix, this.data.component(), mouseX, mouseY, bufferSource);
		}
	}
}