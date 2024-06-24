package de.hysky.skyblocker.utils.networth;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import de.hysky.skyblocker.utils.CodecUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.azureaaron.networth.item.AccessoryUpgrades;
import net.azureaaron.networth.item.AuctionBidInfo;
import net.azureaaron.networth.item.Cosmetics;
import net.azureaaron.networth.item.DrillInfo;
import net.azureaaron.networth.item.DungeonUpgrades;
import net.azureaaron.networth.item.GearUpgrades;
import net.azureaaron.networth.item.Gemstone;
import net.azureaaron.networth.item.ItemMetadata;
import net.azureaaron.networth.item.LimitedEditionInfo;
import net.azureaaron.networth.item.MiscModifiers;
import net.azureaaron.networth.item.PetInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Util;

public record SkyblockItemMetadata(Object2IntMap<String> enchantments, int rarityUpgrades, Optional<String> reforge, int upgradeLevel, DungeonUpgrades dungeonUpgrades,
		GearUpgrades gearUpgrades, List<String> gemstoneSlots, Map<String, Either<String, Gemstone>> gemstones, DrillInfo drillInfo, Object2IntMap<String> attributes,
		AuctionBidInfo auctionBidInfo, MiscModifiers miscModifiers, AccessoryUpgrades accessoryUpgrades, Cosmetics cosmetics, PetInfo petInfo,
		LimitedEditionInfo limitedEditionInfo, IntList cakeBagCakeYears) implements ItemMetadata {
	private static final Codec<Object2IntMap<String>> OBJECT_2_INT_MAP_CODEC = CodecUtils.createObject2IntMapCodec(Codec.STRING);

	static SkyblockItemMetadata of(NbtCompound customData, String itemId) {
		return new SkyblockItemMetadata(
				OBJECT_2_INT_MAP_CODEC.parse(NbtOps.INSTANCE, customData.getCompound("enchantments")).getOrThrow(),
				customData.getInt("rarity_upgrades"),
				customData.contains("modifier") ? Optional.of(customData.getString("modifier")) : Optional.empty(),
				customData.getInt("upgrade_level"),
				DungeonUpgrades.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				GearUpgrades.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				customData.getCompound("gems").getList("unlocked_slots", NbtElement.STRING_TYPE).stream().map(NbtElement::asString).toList(),
				Util.make(() -> Gemstone.MAP_EITHER_CODEC.parse(NbtOps.INSTANCE, Util.make(customData.getCompound("gems").copy(), gemsCopy -> gemsCopy.remove("unlocked_slots"))).getOrThrow()),
				DrillInfo.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				OBJECT_2_INT_MAP_CODEC.parse(NbtOps.INSTANCE, customData.getCompound("attributes")).getOrThrow(),
				AuctionBidInfo.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				MiscModifiers.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				AccessoryUpgrades.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				Cosmetics.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				customData.contains("petInfo") ? PetInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(customData.getString("petInfo"))).getOrThrow() : null,
				LimitedEditionInfo.CODEC.parse(NbtOps.INSTANCE, customData).getOrThrow(),
				itemId.equals("NEW_YEAR_CAKE_BAG") ? IntList.of() : IntList.of());
	}
}
