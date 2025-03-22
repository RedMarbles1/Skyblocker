package de.hysky.skyblocker.compatibility.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.Collection;
import java.util.List;

import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class SkyblockForgeDisplay extends BasicDisplay{
	private final int duration;
	public SkyblockForgeDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, int duration){
		super(inputs,outputs);
		this.duration = duration;
	}
	@Override
	public List<InputIngredient<EntryStack<?>>> getInputIngredients(@Nullable ScreenHandler menu, @Nullable PlayerEntity player) {
		return super.getInputIngredients(menu, player);
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return SkyblockerREIClientPlugin.SKYBLOCK_FORGE;
	}
	public int getDuration(){
		return duration;
	}

	@Override
	public @Nullable DisplaySerializer<? extends Display> getSerializer() {
		return null;
	}

};

