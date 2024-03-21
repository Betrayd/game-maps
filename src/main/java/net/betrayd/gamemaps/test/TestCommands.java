package net.betrayd.gamemaps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        dispatcher.register(literal("map").then(
            literal("save").then(
                argument("pos1", BlockPosArgumentType.blockPos()).then(
                    argument("pos2", BlockPosArgumentType.blockPos()).then(
                        argument("id", IdentifierArgumentType.identifier()).executes(TestCommands::save)
                    )
                )
            )
        ).then(
            literal("load").then(
                argument("pos", BlockPosArgumentType.blockPos()).then(
                    argument("id", IdentifierArgumentType.identifier()).executes(TestCommands::load)
                )
            )
        ));
    }

    private static int save(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
        BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        Path path = idToPath(id);

        context.getSource().sendFeedback(() -> Text.literal("Saving map ").append(Text.of(id)), false);

        try {
            GameMap map = GameMapCreator.read(context.getSource().getWorld(), pos1, pos2);

            Files.createDirectories(path.getParent());
            try(BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                GameMapSerializer.serializeMap(map, out);
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Text.literal("Error exporting map. See console for details.")).create();
        }

        context.getSource().sendFeedback(() -> Text.literal("Saved to " + path), true);
        return 1;
    }

    private static int load(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        Path path = idToPath(id);
        ServerWorld world = context.getSource().getWorld();

        try(BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            GameMap map = GameMapSerializer.deserializeMap(world.getRegistryManager().get(RegistryKeys.BIOME), in);
            GameMapPlacer.placeGameMap(world, map, pos);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Text.literal("Error exporting map. See console for details.")).create();
        }
        
        return 1;
    }
    
    private static Path idToPath(Identifier identifier) {
        return FabricLoader.getInstance().getGameDir().resolve("maps").resolve(identifier.getNamespace()).resolve(identifier.getPath() + ".nbt");
    }
}
