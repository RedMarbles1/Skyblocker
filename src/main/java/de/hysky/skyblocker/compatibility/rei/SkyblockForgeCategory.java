package de.hysky.skyblocker.compatibility.rei;

import com.google.common.collect.Lists;
import de.hysky.skyblocker.utils.SkyblockTime;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Label;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Skyblock forge recipe category class for REI
 */
public class SkyblockForgeCategory implements DisplayCategory<SkyblockForgeDisplay> {
	@Override
	public CategoryIdentifier<SkyblockForgeDisplay> getCategoryIdentifier() {
		return SkyblockerREIClientPlugin.SKYBLOCK_FORGE;
	}

	@Override
	public Text getTitle() {
		return Text.translatable("emi.category.skyblocker.skyblock");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Items.LAVA_BUCKET);
	}

	@Override
	public int getDisplayHeight() {
		return 73;
	}

	/*
	 * Draws display for SkyblockForgeDisplay
	 *
	 * @param display the display
	 * @param bounds  the bounds of the display, configurable with overriding the width, height methods.
	 */
@Override
	public List<Widget> setupDisplay(SkyblockForgeDisplay display, Rectangle bounds) {
		List<Widget> out = new ArrayList<>();
		out.add(Widgets.createRecipeBase(bounds));

		Point startPoint = new Point(bounds.getCenterX() - 58, bounds.getCenterY() - 31);

		Point resultPoint = new Point(startPoint.x + 95, startPoint.y + 19);
		out.add(Widgets.createArrow(new Point(startPoint.x + 60, startPoint.y + 18)));
		out.add(Widgets.createResultSlotBackground(resultPoint));

		/*
		Generates the slots
		This took way too long, and it probably wasnt worth it but I definitely will use it later
		 */
		List<EntryIngredient> input = display.getInputEntries();
		List<Slot> slots = Lists.newArrayList();

		for (int y = 0; y < (input.size()+2)/3; y++) {
			int a = y*3;
			for (int x = 0; x < Math.min(input.size()-a, 3); x++) {
					slots.add(Widgets.createSlot(new Point(startPoint.x + 1 + x * 18 + Math.max(0, (27 - (input.size() * 9))), startPoint.y + 1 + y * 18 + 18-(((input.size()-1)/3)*9))).markInput());
			}
		}

		for (int i = 0; i < input.size(); i++) {
			slots.get(i).entries(input.get(i)).markInput();
		}
		out.addAll(slots);
		out.add(Widgets.createSlot(resultPoint).entries(display.getOutputEntries().getFirst()).disableBackground().markOutput());

		// Add duration label, possibly adjust the duration for outside effects
		int duration = display.getDuration();
		Label durationLabel = Widgets.createLabel(new Point(bounds.getCenterX(), startPoint.y + 55),Text.literal("Forge Duration: ").append(SkyblockTime.formatTime(duration)));
		out.add(durationLabel);
		return out;
	}
}

