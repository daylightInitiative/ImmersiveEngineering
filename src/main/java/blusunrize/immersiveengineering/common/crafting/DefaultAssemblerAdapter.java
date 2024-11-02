/*
 * BluSunrize
 * Copyright (c) 2023
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.crafting;

import blusunrize.immersiveengineering.api.tool.assembler.AssemblerHandler;
import blusunrize.immersiveengineering.api.tool.assembler.AssemblerHandler.IRecipeAdapter;
import blusunrize.immersiveengineering.api.tool.assembler.RecipeQuery;
import blusunrize.immersiveengineering.common.util.FakePlayerUtil;
import blusunrize.immersiveengineering.common.util.InventoryCraftingFalse;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

public class DefaultAssemblerAdapter implements IRecipeAdapter<Recipe<CraftingInput>>
{
	@Override
	public List<RecipeQuery> getQueriedInputs(Recipe<CraftingInput> recipe, NonNullList<ItemStack> input, Level world)
	{
		NonNullList<Ingredient> ingred = recipe.getIngredients();
		CraftingInput craftingInput = InventoryCraftingFalse.createFilledCraftingInventory(3, 3, input);
		// Check that the ingredients roughly match what the recipe actually requires.
		// This is necessary to prevent infinite crafting for recipes like FireworkRocketRecipe which don't return
		// meaningful values in getIngredients.
		NonNullList<Ingredient> ingredientsForMatching = NonNullList.create();
		for(Ingredient i : ingred)
			if(!i.isEmpty())
				ingredientsForMatching.add(i);
		while(ingredientsForMatching.size() < craftingInput.size())
			ingredientsForMatching.add(Ingredient.EMPTY);
		CommonHooks.setCraftingPlayer(FakePlayerUtil.getFakePlayer(world));
		int[] ingredientAssignment = RecipeMatcher.findMatches(craftingInput.items(), ingredientsForMatching);
		CommonHooks.setCraftingPlayer(null);

		// Collect remaining items
		NonNullList<ItemStack> remains = recipe.getRemainingItems(craftingInput);

		List<RecipeQuery> queries = new ArrayList<>();
		for(int i = 0; i < craftingInput.size(); i++)
		{
			final RecipeQuery query;
			if(ingredientAssignment!=null)
				// If the ingredients provided by the recipe are plausible request those
				// Try to request each ingredient at the index where it is in the input pattern, this is needed for
				// some CraftTweaker recipes
				query = AssemblerHandler.createQueryFromIngredient(
						ingredientsForMatching.get(ingredientAssignment[i]), remains.get(i)
				);
			else
				// Otherwise request the exact stacks used in the input
				query = AssemblerHandler.createQueryFromItemStack(craftingInput.getItem(i), remains.get(i));
			if(query!=null)
				queries.add(query);
		}
		return queries;
	}
}
