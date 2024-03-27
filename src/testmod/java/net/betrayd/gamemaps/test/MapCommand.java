package net.betrayd.gamemaps.test;

import com.mojang.brigadier.CommandDispatcher;

import net.betrayd.gamemaps.GameMap;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.*;

public class MapCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        dispatcher.register(literal("map").then(
            literal("helloworld").executes(context -> {
                context.getSource().sendFeedback(() -> Text.literal("Hello World!"), false);
                new GameMap(null);
                return 1;
            })
        ));
    }
    
}
