package de.hysky.skyblocker.compatibility.rei;


import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.scheduler.MessageScheduler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SkyblockREITransferHandler implements TransferHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SkyblockREITransferHandler.class);
	public SkyblockREITransferHandler() {

	}

	@Override
	public ApplicabilityResult checkApplicable(Context context) {
		//Should a check for player rank be implemented?

		if(Utils.isOnSkyblock() && (context.getDisplay() instanceof SkyblockCraftingDisplay)){
			return ApplicabilityResult.createApplicable();
		}
		return ApplicabilityResult.createNotApplicable();
	}

		@Override
		public Result handle(Context context) {

			if (!context.isActuallyCrafting()){
				return Result.createSuccessful();
			}
			EntryStack<?> output = context.getDisplay().getOutputEntries().getFirst().getFirst();
			EntryStack<ItemStack> recipeEntry = EntryStacks.of((ItemStack) output.getValue());
			ItemStack recipeOutputItem = recipeEntry.getValue();
			MessageScheduler.INSTANCE.sendMessageAfterCooldown("/viewrecipe "+ recipeOutputItem.getSkyblockId(),true);
			return Result.createSuccessful();
		}
	}
