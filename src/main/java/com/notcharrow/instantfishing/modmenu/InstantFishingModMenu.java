package com.notcharrow.instantfishing.modmenu;

import com.notcharrow.instantfishing.config.ConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class InstantFishingModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return this::createConfigScreen;
	}

	private Screen createConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Instant Fishing Config"));

		addSettings(builder);

		return builder.build();
	}

	private static void addSettings(ConfigBuilder builder) {
		ConfigCategory generalSettings = builder.getOrCreateCategory(Component.literal("General Settings"));

		addBoolean(generalSettings, "Mod Enabled", "Main toggle for the mod",
				ConfigManager.config.modEnabled,
				value -> ConfigManager.config.modEnabled = value);

		addInteger(generalSettings, "Max Catch XP", "Maximum XP to receive from a fish catch",
				ConfigManager.config.maxCatchXP,
				value -> ConfigManager.config.maxCatchXP = value, 1, Integer.MAX_VALUE);

		addBoolean(generalSettings, "Override Treasure Chance", "Should the mod override the chance of catching treasure",
				ConfigManager.config.overrideTreasureChance,
				value -> ConfigManager.config.overrideTreasureChance = value);

		addDouble(generalSettings, "Treasure Chance", "The new chance if overriding the vanilla treasure chance",
				ConfigManager.config.treasureChance,
				value -> ConfigManager.config.treasureChance = value, 0.0, 100.0);

		addBoolean(generalSettings, "Random Item", "Should the fishing loot be replaced with random items",
				ConfigManager.config.randomItem,
				value -> ConfigManager.config.randomItem = value);

		addBoolean(generalSettings, "Random Loot Table", "Should the fishing loot be replaced with loot tables (chests)",
				ConfigManager.config.randomLootTable,
				value -> ConfigManager.config.randomLootTable = value);
	}

	private static void addBoolean(ConfigCategory category, String label, String tooltip, boolean currentValue, Consumer<Boolean> onSave) {
		category.addEntry(
				ConfigBuilder.create().entryBuilder().startBooleanToggle(Component.literal(label), currentValue)
						.setTooltip(Component.literal(tooltip))
						.setDefaultValue(currentValue)
						.setSaveConsumer(onSave)
						.build()
		);
	}

	private static void addInteger(ConfigCategory category, String label, String tooltip, int currentValue, Consumer<Integer> onSave, int min, int max) {
		category.addEntry(
				ConfigBuilder.create().entryBuilder().startIntField(Component.literal(label), currentValue)
						.setTooltip(Component.literal(tooltip))
						.setDefaultValue(currentValue)
						.setSaveConsumer(newValue -> {
							newValue = Math.clamp(newValue, min, max);
							onSave.accept(newValue);
							ConfigManager.saveConfig();
						})
						.build()
		);
	}

	private static void addDouble(ConfigCategory category, String label, String tooltip, double currentValue, Consumer<Double> onSave, double min, double max) {
		category.addEntry(
				ConfigBuilder.create().entryBuilder().startDoubleField(Component.literal(label), currentValue)
						.setTooltip(Component.literal(tooltip))
						.setDefaultValue(currentValue)
						.setSaveConsumer(newValue -> {
							newValue = Math.clamp(newValue, min, max);
							onSave.accept(newValue);
							ConfigManager.saveConfig();
						})
						.build()
		);
	}
}
