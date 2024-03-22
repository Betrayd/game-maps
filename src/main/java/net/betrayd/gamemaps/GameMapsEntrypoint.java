package net.betrayd.gamemaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class GameMapsEntrypoint implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("game-maps");

	@Override
	public void onInitialize() {
		// CommandRegistrationCallback.EVENT.register(TestCommands::register);
	}
}