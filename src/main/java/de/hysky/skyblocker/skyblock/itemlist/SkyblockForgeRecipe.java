package de.hysky.skyblocker.skyblock.itemlist;

import de.hysky.skyblocker.utils.ItemUtils;
import io.github.moulberry.repo.data.NEUForgeRecipe;
import io.github.moulberry.repo.data.NEUIngredient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SkyblockForgeRecipe {
	private static final Logger LOGGER = LoggerFactory.getLogger(SkyblockForgeRecipe.class);
	private final int duration;
	private final List<ItemStack> grid = new ArrayList<>();
	private ItemStack result;

	public SkyblockForgeRecipe(int duration) {
		this.duration = duration;
	}

	public static SkyblockForgeRecipe fromNEURecipe(NEUForgeRecipe neuForgeRecipe) {
		SkyblockForgeRecipe recipe = new SkyblockForgeRecipe(neuForgeRecipe.getDuration());
		for (NEUIngredient input : neuForgeRecipe.getInputs()) {
			recipe.grid.add(getItemStack(input));
		}
		recipe.result = getItemStack(neuForgeRecipe.getOutputStack());
		return recipe;
	}

	private static ItemStack getItemStack(NEUIngredient input) {
		if (input != NEUIngredient.SENTINEL_EMPTY) {
			ItemStack stack = ItemRepository.getItemStack(input.getItemId());
			if (stack != null) {
				return stack.copyWithCount((int) input.getAmount());
			} else {
				LOGGER.warn("[Skyblocker Recipe] Unable to find item {}", input.getItemId());
			}
		}
		return Items.AIR.getDefaultStack();
	}

	public List<ItemStack> getGrid() {
		return grid;
	}

	public ItemStack getResult() {
		return result;
	}

	public int getDuration() { return duration;}

	public Identifier getId() {
		return Identifier.of("skyblock", ItemUtils.getItemId(getResult()).toLowerCase().replace(';', '_') + "_" + getResult().getCount());
	}
}
