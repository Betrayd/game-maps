package net.betrayd.gamemaps.world_interface;

import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;

import net.betrayd.gamemaps.EntityFilter;
import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

/**
 * Reads an mc world and creates a game map from it. Unlike
 * {@link GameMapCapture}, this implementation copies raw chunk section data
 * into the file. While this means it can only express bounds in 16m^2 sections,
 * it's <em>significantly</em> faster on large scales.
 */
public class WorldAlignedMapCapture {

    public static GameMap capture(World world, ChunkSectionPos pos1, ChunkSectionPos pos2, @Nullable ChunkSectionPos origin) {
        return capture(world, pos1, pos2, origin, null);
    }

    /**
     * Capture a game world from an MC world, querying chunk data directly.
     * 
     * @param world  Minecraft world.
     * @param pos1   One end of the bounding box (world space).
     * @param pos2   Other end of the bounding box (world space).
     * @param origin Chunk section pos to use as the origin.
     * @return The captured game map.
     */
    public static GameMap capture(World world, ChunkSectionPos pos1, ChunkSectionPos pos2,
            @Nullable ChunkSectionPos origin, @Nullable EntityFilter entityFilter) {

        ChunkSectionPos minPos = min(pos1, pos2, ChunkSectionPos::from);
        ChunkSectionPos maxPos = max(pos1, pos2, ChunkSectionPos::from);

        if (origin == null) origin = ChunkSectionPos.from(0, 0, 0);

        Registry<Biome> biomeRegistry = world.getRegistryManager().get(RegistryKeys.BIOME);
        GameMap map = new GameMap(biomeRegistry);

        for (int x = minPos.getX(); x < maxPos.getX(); x++) {
            for (int z = minPos.getZ(); z < maxPos.getZ(); z++) {
                Chunk chunk = world.getChunk(x, z);
                if (chunk == null)
                    continue;

                GameChunk[] gameChunks = captureChunk(chunk, biomeRegistry);
                for (int i = 0; i < gameChunks.length; i++) {
                    if (gameChunks[i] == null)
                        continue;

                    ChunkSectionPos pos = ChunkSectionPos.from(
                            x - origin.getX(),
                            chunk.sectionIndexToCoord(i) - origin.getY(),
                            z - origin.getZ());

                    map.putChunk(pos, gameChunks[i]);
                }
            }
        }

        if (world instanceof ServerWorld serverWorld) {
            BlockPos blockMin = new BlockPos(minPos.getMinX(), minPos.getMinY(), minPos.getMinZ());
            BlockPos blockMax = new BlockPos(maxPos.getMaxX(), maxPos.getMaxY(), maxPos.getMaxZ());
            BlockPos originPos = origin.getMinPos();

            for (Entity ent : serverWorld.iterateEntities()) {
                if (boxContains(blockMin, blockMax, ent.getBlockPos())) {
                    Entity filtered;
                    if (entityFilter != null) {
                        filtered = entityFilter.apply(ent, map.getCustomData());
                    } else {
                        filtered = ent;
                    }
                    
                    if (filtered == null || filtered instanceof PlayerEntity)
                        continue;

                    map.addEntity(GameMapEntity.fromEntity(
                            ent.getPos().subtract(originPos.getX(), originPos.getY(), originPos.getZ()), ent));
                }
            }

        } else {
            LogUtils.getLogger().warn("Can only write entities if we're in a server world.");
        }

        map.getMeta().setDimensionType(world.getDimensionKey());

        return map;
    }

    /**
     * Capture a set of game chunks from an MC chunk.
     * 
     * @param chunk         Chunk to capture.
     * @param biomeRegistry Biome registry of the world.
     * @return An array of all captured game chunks. Use
     *         {@link Chunk#sectionIndexToCoord(int)} on the supplied chunk to get
     *         chunk Y from index. Some elements may be null.
     */
    public static GameChunk[] captureChunk(Chunk chunk, Registry<Biome> biomeRegistry) {
        ChunkSection[] sections = chunk.getSectionArray();
        GameChunk[] gameChunks = new GameChunk[sections.length];

        for (int i = 0; i < sections.length; i++) {
            gameChunks[i] = captureChunkSection(sections[i], biomeRegistry);
        }

        for (BlockPos pos : chunk.getBlockEntityPositions()) {
            BlockEntity ent = chunk.getBlockEntity(pos);

            int index = chunk.getSectionIndex(pos.getY());
            if (index < 0 || index >= gameChunks.length) continue;

            gameChunks[index].putBlockEntity(ent);
        }

        return gameChunks;
    }

    private static GameChunk captureChunkSection(ChunkSection chunkSection, Registry<Biome> biomeRegistry) {
        return new GameChunk(chunkSection.getBlockStateContainer().copy(),
                chunkSection.getBiomeContainer().slice().copy(), biomeRegistry);
    }

    private static interface TriIntFunction<T> {
        public T apply(int a, int b, int c);
    }

    private static <T> T min(Vec3i a, Vec3i b, TriIntFunction<T> factory) {
        return factory.apply(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
    }


    private static <T> T max(Vec3i a, Vec3i b, TriIntFunction<T> factory) {
        return factory.apply(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
    }

    private static boolean boxContains(BlockPos minPos, BlockPos maxPos, BlockPos pos) {
        return minPos.getX() <= pos.getX() && pos.getX() <= maxPos.getX()
            && minPos.getY() <= pos.getY() && pos.getY() <= maxPos.getY()
            && minPos.getZ() <= pos.getZ() && pos.getZ() <= maxPos.getZ();
    }
}
