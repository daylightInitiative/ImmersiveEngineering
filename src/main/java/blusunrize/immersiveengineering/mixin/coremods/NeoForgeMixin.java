/*
 *  BluSunrize
 *  Copyright (c) 2025
 *
 *  This code is licensed under "Blu's License of Common Sense"
 *  Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.mixin.coremods;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeoForgeMod.class)
public abstract class NeoForgeMixin
{
	@Shadow
	@Final
	private static DeferredRegister<FluidIngredientType<?>> FLUID_INGREDIENT_TYPES;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(IEventBus modEventBus, Dist dist, ModContainer container, CallbackInfo ci)
	{
		// Neo just fully forgot to register this? Fun times.
		try
		{
			FLUID_INGREDIENT_TYPES.register(modEventBus);
		} catch(IllegalStateException e)
		{
			// Already registered, presumably by someone else's mixin =P
		}
	}
}
