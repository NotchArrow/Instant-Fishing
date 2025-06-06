package com.notcharrow.instantfishing;

import com.notcharrow.instantfishing.config.ConfigManager;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantFishing implements ModInitializer {
	@Override
	public void onInitialize() {
		ConfigManager.loadConfig();
	}
}