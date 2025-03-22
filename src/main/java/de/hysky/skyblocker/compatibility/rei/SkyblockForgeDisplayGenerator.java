package de.hysky.skyblocker.compatibility.rei;

import de.hysky.skyblocker.skyblock.itemlist.ItemRepository;
import de.hysky.skyblocker.skyblock.itemlist.SkyblockForgeRecipe;
import de.hysky.skyblocker.utils.ItemUtils;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkyblockForgeDisplayGenerator implements DynamicDisplayGenerator<SkyblockForgeDisplay> {

	@Override
	public Optional<List<SkyblockForgeDisplay>> getRecipeFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack)) return Optional.empty();
		EntryStack<ItemStack> inputItem = EntryStacks.of((ItemStack) entry.getValue());
		List<SkyblockForgeRecipe> filteredRecipes = ItemRepository.getForgeRecipesStream()
				.filter(recipe -> {
					ItemStack itemStack = inputItem.getValue();
					ItemStack itemStack1 = recipe.getResult();
					return ItemUtils.getItemId(itemStack1).equals(ItemUtils.getItemId(itemStack));
				})
				.toList();

		return Optional.of(generateDisplays(filteredRecipes));
	}

	@Override
	public Optional<List<SkyblockForgeDisplay>> getUsageFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack)) return Optional.empty();
		EntryStack<ItemStack> inputItem = EntryStacks.of((ItemStack) entry.getValue());
		List<SkyblockForgeRecipe> filteredForgeRecipes = ItemRepository.getForgeRecipesStream()
				.filter(recipe -> {
					for (ItemStack item : recipe.getGrid()) {
						if(!ItemUtils.getItemId(item).isEmpty()) {
							ItemStack itemStack = inputItem.getValue();
							if (ItemUtils.getItemId(item).equals(ItemUtils.getItemId(itemStack))) return true;
						}
					}
					return false;
				})
				.toList();
		return Optional.of(generateDisplays(filteredForgeRecipes));
	}


	private List<SkyblockForgeDisplay> generateDisplays(List<SkyblockForgeRecipe> recipes) {
		List<SkyblockForgeDisplay> displays = new ArrayList<>();
		for (SkyblockForgeRecipe recipe : recipes) {
			List<EntryIngredient> inputs = new ArrayList<>();
			List<EntryIngredient> outputs = new ArrayList<>();

			ArrayList<EntryStack<ItemStack>> inputEntryStacks = new ArrayList<>();
			recipe.getGrid().forEach((item) -> inputEntryStacks.add(EntryStacks.of(item)));

			for (EntryStack<ItemStack> entryStack : inputEntryStacks) {
				if (entryStack.isEmpty()) {
					inputs.add(EntryIngredient.empty());
				} else {
					inputs.add(EntryIngredient.of(entryStack));
				}
			}
			outputs.add(EntryIngredient.of(EntryStacks.of(recipe.getResult())));

			displays.add(new SkyblockForgeDisplay(inputs, outputs, recipe.getDuration()));
		}
		return displays;
	}
}
