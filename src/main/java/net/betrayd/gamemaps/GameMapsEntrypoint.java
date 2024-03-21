package net.betrayd.gamemaps;

import net.betrayd.gamemaps.test.TestCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameMapsEntrypoint implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("game-maps");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(TestCommands::register);
	}
}