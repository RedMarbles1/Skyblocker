package de.hysky.skyblocker.skyblock.itemlist.recipes;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.utils.ItemUtils;
import de.hysky.skyblocker.utils.SkyblockTime;
import io.github.moulberry.repo.data.NEUKatUpgradeRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkyblockKatUpgradeRecipe implements SkyblockRecipe{

	public static final Identifier IDENTIFIER = Identifier.of(SkyblockerMod.NAMESPACE, "skyblock_kat_upgrade");

	private final List<ItemStack> inputs;
	private final ItemStack output;
	private final String upgradeTimeString;
	public SkyblockKatUpgradeRecipe(NEUKatUpgradeRecipe katUpgradeRecipe){
		inputs = katUpgradeRecipe.getAllInputs().stream().map(SkyblockRecipe::getItemStack).toList();
		output = SkyblockRecipe.getItemStack(katUpgradeRecipe.getOutput());
		upgradeTimeString = SkyblockTime.formatTime(katUpgradeRecipe.getSeconds()).getLiteralString();
	}
	public Vector2i getGridSize() {
		int gridWidth;
		int gridHeight;

		int sqrt = gridHeight = gridWidth = (int) Math.sqrt(inputs.size());
		float percentage = (inputs.size() - sqrt * sqrt) / (float) ((sqrt+1)*(sqrt+1) - sqrt*sqrt);

		if (percentage > 0.005f) {
			gridWidth++;
			if (percentage > 0.6) gridHeight++;
		}
		return new Vector2i(gridWidth, gridHeight);
	}

	public ItemStack getResult() {
		return output;
	}
	@Override
	public List<RecipeSlot> getInputSlots(int width, int height) {
		List<RecipeSlot> out = new ArrayList<>();
		int centerX = width / 2;
		int centerY = height / 2;

		Vector2i radius = getGridSize();
		int startX = (int) (centerX / 2.f - (radius.x / 2.f) * 18);
		int startY = (int) (centerY - (radius.y / 2.f) * 18);

		for (int i = 0; i < inputs.size(); i++) {
			int x = startX + (i % radius.x) * 18;
			int y = startY + (i / radius.x) * 18;
			out.add(new RecipeSlot(x, y, inputs.get(i)));
		}
		return out;
	}
	@Override
	public List<RecipeSlot> getOutputSlots(int width, int height) {
		return Collections.singletonList(new RecipeSlot(Math.max(3 * width / 4, width / 2 + 30), height / 2 - 9, output));
	}
	@Override
	public List<ItemStack> getInputs() {
		return inputs;
	}
	@Override
	public List<ItemStack> getOutputs() {
		return Collections.singletonList(output);
	}

	@Override
	public Text getExtraText() {
		return Text.empty();
	}

	@Override
	public Identifier getCategoryIdentifier() {
		return IDENTIFIER;
	}

	public Identifier getRecipeIdentifier() {
		return Identifier.of("skyblock", ItemUtils.getItemId(output).toLowerCase().replace(';', '_') + "_" + output.getCount());
	}

	@Override
	public @Nullable ScreenPos getArrowLocation(int width, int height) {
		return new ScreenPos(width / 2, height / 2 - 9);
	}

	public String getUpgradeTimeString() {
		return upgradeTimeString;
	}


	@Override
	public void render(DrawContext context, int width, int height, double mouseX, double mouseY) {
		// Render the duration of the recipe in hours by dividing by 3600
		ScreenPos arrowLocation = getArrowLocation(width, height);
		if (arrowLocation != null)
			context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, upgradeTimeString, arrowLocation.x() + 12, arrowLocation.y() - 10, 0xFFFFFF);
	}
}

