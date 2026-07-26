package net.betrayd.gamemaps.test;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

public class MapTestCommands {

    private static Map<MinecraftServer, Map<Identifier, RuntimeLevelHandle>> worlds = new WeakHashMap<>();

    private static final SimpleCommandExceptionType MAP_NOT_OPEN = new SimpleCommandExceptionType(Component.literal("The map world is not open."));
    private static final SimpleCommandExceptionType ALREADY_OPEN = new SimpleCommandExceptionType(Component.literal("Map is already open!"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            CommandSelection environment) {
        
        dispatcher.register(literal("map").then(
            literal("save").then(
                literal("blocks").then(
                    argument("pos1", BlockPosArgument.blockPos()).then(
                        argument("pos2", BlockPosArgument.blockPos()).then(
                            argument("id", IdentifierArgument.id()).executes(MapTestCommands::save)
                        )
                    )
                )
            ).then(
                literal("chunks").then(
                    argument("radius", IntegerArgumentType.integer(1)).then(
                        argument("id", IdentifierArgument.id()).executes(MapTestCommands::saveChunks)
                    )
                )
            )
        ).then(
            literal("place").then(
                argument("pos", BlockPosArgument.blockPos()).then(
                    argument("id", IdentifierArgument.id()).executes(MapTestCommands::place)
                )
            )
        ).then(
            literal("open").then(
                argument("id", IdentifierArgument.id()).executes(MapTestCommands::open)
            )
        ).then(
            literal("join").then(
                argument("id", IdentifierArgument.id()).executes(MapTestCommands::join)
            )
        ).then(
            literal("close").then(
                argument("id", IdentifierArgument.id()).executes(MapTestCommands::close)
            )
        ));
    }

    private static int save(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");
        Identifier id = IdentifierArgument.getId(context, "id");
        Path path = idToPath(id);

        context.getSource().sendSuccess(() -> Component.literal("Saving map ").append(Component.translationArg(id)), false);

        try {
            GameMap map = GameMapCapture.read(context.getSource().getLevel(), pos1, pos2,
                    null);

            Files.createDirectories(path.getParent());
            try(BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                new GameMapSerializer().serializeMap(map, out);
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Component.literal("Error exporting map. See console for details.")).create();
        }

        context.getSource().sendSuccess(() -> Component.literal("Saved to " + path), true);
        return 1;
    }

    private static int saveChunks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Identifier id = IdentifierArgument.getId(context, "id");

        context.getSource().sendSuccess(() -> Component.literal("Exporting world as ").append(Component.translationArg(id)), true);

        SectionPos centerPos = SectionPos.of(context.getSource().getPosition());

        int minY = SectionPos.blockToSectionCoord(context.getSource().getLevel().getMinBuildHeight());
        int maxY = SectionPos.blockToSectionCoord(context.getSource().getLevel().getMaxBuildHeight());

        SectionPos minPos = SectionPos.of(centerPos.getX() - radius, minY, centerPos.getZ() - radius);
        SectionPos maxPos = SectionPos.of(centerPos.getX() + radius + 1, maxY,
                centerPos.getZ() + radius + 1);

        GameMap map = WorldAlignedMapCapture.capture(context.getSource().getLevel(), minPos, maxPos, null,
                MapTestCommands::processChickens);

        Path path = idToPath(id);

        try {
            Files.createDirectories(path.getParent());
            try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                new GameMapSerializer().serializeMap(map, out);
            }

        } catch (IOException e) {
            LogUtils.getLogger().error("Error exporting map: " + id, e);
            throw new SimpleCommandExceptionType(Component.literal("Error saving map to file. See console for details."))
                    .create();
        }

        context.getSource().sendSuccess(() -> Component.literal("Exported to " + path), false);

        return 1;
    }

    /**
     * A silly method for testing entity filters
     */
    private static Entity processChickens(Entity ent, GameMap map) {
        if (ent instanceof Chicken) {
            ChickenMapMarker marker = TestMapMarkers.CHICKEN.create();
            marker.setNamed(ent.hasCustomName());
            marker.setPositionFromEntity(ent);
            map.addMarker(marker);
            return null;
        }
        // if (ent instanceof ChickenEntity) {
        //     NbtList list;
        //     if (nbt.contains("chickens", NbtElement.LIST_TYPE)) {
        //         list = nbt.getList("chickens", NbtElement.COMPOUND_TYPE);
        //     } else {
        //         list = new NbtList();
        //         nbt.put("chickens", list);
        //     }

        //     NbtCompound compound = new NbtCompound();
        //     compound.putDouble("x", ent.getX());
        //     compound.putDouble("y", ent.getY());
        //     compound.putDouble("z", ent.getZ());

        //     list.add(compound);
        //     return null;
        // }
        return ent;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        Identifier id = IdentifierArgument.getId(context, "id");
        Path path = idToPath(id);
        ServerLevel world = context.getSource().getLevel();

        GameMapDeserializer deserializer = new GameMapDeserializer(world.registryAccess());
        try(BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            GameMap map = deserializer.deserializeMap(in);
            GameMapPlacer.placeGameMap(world, map, pos);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error exporting map.", e);
            throw new SimpleCommandExceptionType(Component.literal("Error exporting map. See console for details.")).create();
        }
        
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");

        if (getWorlds(context.getSource().getServer()).containsKey(id)) {
            throw ALREADY_OPEN.create();
        }

        Path path = idToPath(id);
        ServerLevel world = context.getSource().getLevel();
        
        GameMap map;
        GameMapDeserializer deserializer = new GameMapDeserializer(world.registryAccess());
        // deserializer.getEntityFilters().add(ent -> {
        //     ent.setId(EntityType.getId(EntityType.ARMOR_STAND));
        //     return ent;
        // });
        try(BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            map = deserializer.deserializeMap(in);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error opening map.", e);
            throw new SimpleCommandExceptionType(Component.literal("Error opening map. See console for details.")).create();
        }

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(map.getMeta().getDimensionType())
                .setGenerator(new GameMapChunkGenerator(map));

        RuntimeLevelHandle runtimeWorld = Fantasy.get(context.getSource().getServer()).openTemporaryLevel(config);
        getWorlds(context.getSource().getServer()).put(id, runtimeWorld);

        context.getSource().sendSuccess(
                () -> Component.literal("Opened map ").append(Component.translationArg(id)).append(". Use /map join to join it."), false);
        
        if (!map.getCustomData().isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("The custom data is ")
                            .append(NbtUtils.toPrettyComponent(map.getCustomData())),
                    false);
        }

        ServerLevel resultLevel = runtimeWorld.asLevel();
        resultLevel.getGameRules().setAll(map.getMeta().getGameRules(), resultLevel.getServer());
        resultLevel.dimensionType().defaultClock().ifPresent(clock ->
                resultLevel.getServer().clockManager().setTotalTicks(clock, map.getMeta().getDayTime()));

        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");

        RuntimeLevelHandle world = getWorlds(context.getSource().getServer()).get(id);
        if (world == null) {
            throw MAP_NOT_OPEN.create();
        }

        Entity ent = context.getSource().getEntityOrException();

        Vec3 pos = context.getSource().getPosition();
        Vec2 rot = context.getSource().getRotation();

        ent.teleportTo(world.asLevel(), pos.x(), pos.y(), pos.z(), Collections.emptySet(), rot.y, rot.x);

        return 1;
    }
    
    private static int close(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");

        var worlds = getWorlds(context.getSource().getServer());
        RuntimeLevelHandle world = worlds.get(id);
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

    private static Map<Identifier, RuntimeLevelHandle> getWorlds(MinecraftServer server) {
        return worlds.computeIfAbsent(server, s -> new HashMap<>());
    }
}
