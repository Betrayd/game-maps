package net.betrayd.gamemaps.test;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;

import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.serialization.GameMapDeserializer;
import net.betrayd.gamemaps.serialization.GameMapSerializer;
import net.betrayd.gamemaps.world_interface.GameMapCapture;
import net.betrayd.gamemaps.world_interface.GameMapChunkGenerator;
import net.betrayd.gamemaps.world_interface.GameMapPlacer;
import net.betrayd.gamemaps.world_interface.WorldAlignedMapCapture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public class MapTestCommands {

    private static Map<MinecraftServer, Map<Identifier, RuntimeWorldHandle>> worlds = new WeakHashMap<>();

    private static final SimpleCommandExceptionType MAP_NOT_OPEN = new SimpleCommandExceptionType(Text.literal("The map world is not open."));
    private static final SimpleCommandExceptionType ALREADY_OPEN = new SimpleCommandExceptionType(Text.literal("Map is already open!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        dispatcher.register(literal("map").then(
            literal("save").then(
                literal("blocks").then(
                    argument("pos1", BlockPosArgumentType.blockPos()).then(
                        argument("pos2", BlockPosArgumentType.blockPos()).then(
                            argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::save)
                        )
                    )
                )
            ).then(
                literal("chunks").then(
                    argument("radius", IntegerArgumentType.integer(1)).then(
                        argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::saveChunks)
                    )
                )
            )
        ).then(
            literal("place").then(
                argument("pos", BlockPosArgumentType.blockPos()).then(
                    argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::place)
                )
            )
        ).then(
            literal("open").then(
                argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::open)
            )
        ).then(
            literal("join").then(
                argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::join)
            )
        ).then(
            literal("close").then(
                argument("id", IdentifierArgumentType.identifier()).executes(MapTestCommands::close)
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
            GameMap map = GameMapCapture.read(context.getSource().getWorld(), pos1, pos2,
                    MapTestCommands::processChickens);

            Files.createDirectories(path.getParent());
            try(BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                new GameMapSerializer().serializeMap(map, out);
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Text.literal("Error exporting map. See console for details.")).create();
        }

        context.getSource().sendFeedback(() -> Text.literal("Saved to " + path), true);
        return 1;
    }

    private static int saveChunks(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");

        context.getSource().sendFeedback(() -> Text.literal("Exporting world as ").append(Text.of(id)), true);

        ChunkSectionPos centerPos = ChunkSectionPos.from(context.getSource().getPosition());

        int minY = ChunkSectionPos.getSectionCoord(context.getSource().getWorld().getBottomY());
        int maxY = ChunkSectionPos.getSectionCoord(context.getSource().getWorld().getTopY());

        ChunkSectionPos minPos = ChunkSectionPos.from(centerPos.getX() - radius, minY, centerPos.getZ() - radius);
        ChunkSectionPos maxPos = ChunkSectionPos.from(centerPos.getX() + radius + 1, maxY,
                centerPos.getZ() + radius + 1);

        GameMap map = WorldAlignedMapCapture.capture(context.getSource().getWorld(), minPos, maxPos, null,
                MapTestCommands::processChickens);

        Path path = idToPath(id);

        try {
            Files.createDirectories(path.getParent());
            try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                new GameMapSerializer().serializeMap(map, out);
            }

        } catch (IOException e) {
            LogUtils.getLogger().error("Error exporting map: " + id, e);
            throw new SimpleCommandExceptionType(Text.literal("Error saving map to file. See console for details."))
                    .create();
        }

        context.getSource().sendFeedback(() -> Text.literal("Exported to " + path), false);

        return 1;
    }

    /**
     * A silly method for testing entity filters
     */
    private static Entity processChickens(Entity ent, NbtCompound nbt) {
        if (ent instanceof ChickenEntity) {
            NbtList list;
            if (nbt.contains("chickens", NbtElement.LIST_TYPE)) {
                list = nbt.getList("chickens", NbtElement.COMPOUND_TYPE);
            } else {
                list = new NbtList();
                nbt.put("chickens", list);
            }

            NbtCompound compound = new NbtCompound();
            compound.putDouble("x", ent.getX());
            compound.putDouble("y", ent.getY());
            compound.putDouble("z", ent.getZ());

            list.add(compound);
            return null;
        }
        return ent;
    }

    private static int place(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        Path path = idToPath(id);
        ServerWorld world = context.getSource().getWorld();

        GameMapDeserializer deserializer = new GameMapDeserializer(world.getRegistryManager().get(RegistryKeys.BIOME));
        try(BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            GameMap map = deserializer.deserializeMap(in);
            GameMapPlacer.placeGameMap(world, map, pos);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Text.literal("Error exporting map. See console for details.")).create();
        }
        
        return 1;
    }

    private static int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");

        if (getWorlds(context.getSource().getServer()).containsKey(id)) {
            throw ALREADY_OPEN.create();
        }

        Path path = idToPath(id);
        ServerWorld world = context.getSource().getWorld();
        
        GameMap map;
        GameMapDeserializer deserializer = new GameMapDeserializer(world.getRegistryManager().get(RegistryKeys.BIOME));
        // deserializer.getEntityFilters().add(ent -> {
        //     ent.setId(EntityType.getId(EntityType.ARMOR_STAND));
        //     return ent;
        // });
        try(BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            map = deserializer.deserializeMap(in);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error opening map.", e);
            throw new SimpleCommandExceptionType(Text.literal("Error opening map. See console for details.")).create();
        }

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(map.getMeta().getDimensionType())
                .setGenerator(new GameMapChunkGenerator(map));

        RuntimeWorldHandle runtimeWorld = Fantasy.get(context.getSource().getServer()).openTemporaryWorld(config);
        getWorlds(context.getSource().getServer()).put(id, runtimeWorld);

        context.getSource().sendFeedback(
                () -> Text.literal("Opened map ").append(Text.of(id)).append(". Use /map join to join it."), false);
        
        if (!map.getCustomData().isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("The custom data is ")
                            .append(NbtHelper.toPrettyPrintedText(map.getCustomData())),
                    false);
        }

        return 1;
    }

    private static int join(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");

        RuntimeWorldHandle world = getWorlds(context.getSource().getServer()).get(id);
        if (world == null) {
            throw MAP_NOT_OPEN.create();
        }

        Entity ent = context.getSource().getEntityOrThrow();

        Vec3d pos = context.getSource().getPosition();
        Vec2f rot = context.getSource().getRotation();

        ent.teleport(world.asWorld(), pos.getX(), pos.getY(), pos.getZ(), Collections.emptySet(), rot.y, rot.x);

        return 1;
    }
    
    private static int close(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");

        var worlds = getWorlds(context.getSource().getServer());
        RuntimeWorldHandle world = worlds.get(id);
        if (world == null) {
            throw MAP_NOT_OPEN.create();
        }

        world.delete();
        worlds.remove(id);

        return 1;
    }
    
    private static Path idToPath(Identifier identifier) {
        return FabricLoader.getInstance().getGameDir().resolve("maps").resolve(identifier.getNamespace()).resolve(identifier.getPath() + ".nbt");
    }

    private static Map<Identifier, RuntimeWorldHandle> getWorlds(MinecraftServer server) {
        return worlds.computeIfAbsent(server, s -> new HashMap<>());
    }
}
