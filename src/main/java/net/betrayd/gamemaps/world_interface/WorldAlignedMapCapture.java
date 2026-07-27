package net.betrayd.gamemaps.world_interface;

import net.minecraft.core.*;
import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;

import net.betrayd.gamemaps.EntityFilter;
import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Reads an mc world and creates a game map from it. Unlike
 * {@link GameMapCapture}, this implementation copies raw chunk section data
 * into the file. While this means it can only express bounds in 16m^2 sections,
 * it's <em>significantly</em> faster on large scales.
 */
public class WorldAlignedMapCapture {

    public static GameMap capture(Level world, SectionPos pos1, SectionPos pos2, @Nullable SectionPos origin) {
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
    public static GameMap capture(Level world, SectionPos pos1, SectionPos pos2,
            @Nullable SectionPos origin, @Nullable EntityFilter entityFilter) {

        SectionPos minPos = min(pos1, pos2, SectionPos::of);
        SectionPos maxPos = max(pos1, pos2, SectionPos::of);

        if (origin == null) origin = SectionPos.of(0, 0, 0);

        GameMap map = new GameMap(world.registryAccess());

        for (int x = minPos.getX(); x < maxPos.getX(); x++) {
            for (int z = minPos.getZ(); z < maxPos.getZ(); z++) {
                ChunkAccess chunk = world.getChunk(x, z);
                if (chunk == null)
                    continue;

                GameChunk[] gameChunks = captureChunk(chunk, world.registryAccess());
                for (int i = 0; i < gameChunks.length; i++) {
                    if (gameChunks[i] == null)
                        continue;

                    SectionPos pos = SectionPos.of(
                            x - origin.getX(),
                            chunk.getSectionYFromSectionIndex(i) - origin.getY(),
                            z - origin.getZ());

                    map.putChunk(pos, gameChunks[i]);
                }
            }
        }

        if (world instanceof ServerLevel serverWorld) {
            BlockPos blockMin = new BlockPos(minPos.minBlockX(), minPos.minBlockY(), minPos.minBlockZ());
            BlockPos blockMax = new BlockPos(maxPos.maxBlockX(), maxPos.maxBlockY(), maxPos.maxBlockZ());
            BlockPos originPos = origin.origin();

            for (Entity ent : serverWorld.getAllEntities()) {
                if (boxContains(blockMin, blockMax, ent.blockPosition())) {
                    Entity filtered;
                    if (entityFilter != null) {
                        filtered = entityFilter.apply(ent, map);
                    } else {
                        filtered = ent;
                    }
                    
                    if (filtered == null || filtered instanceof Player)
                        continue;

                    map.addEntity(GameMapEntity.fromEntity(
                            ent.position().subtract(originPos.getX(), originPos.getY(), originPos.getZ()), ent));
                }
            }

        } else {
            LogUtils.getLogger().warn("Can only write entities if we're in a server world.");
        }

        map.getMeta().setFromWorld(world);

        return map;
    }

    /**
     * Capture a set of game chunks from an MC chunk.
     *
     * @param chunk          Chunk to capture.
     * @param registryAccess Registry access for the world
     * @return An array of all captured game chunks. Use
     * {@link ChunkAccess#getSectionYFromSectionIndex(int)} on the supplied chunk to get
     * chunk Y from index. Some elements may be null.
     */
    public static GameChunk[] captureChunk(ChunkAccess chunk, RegistryAccess registryAccess) {
        LevelChunkSection[] sections = chunk.getSections();
        GameChunk[] gameChunks = new GameChunk[sections.length];

        for (int i = 0; i < sections.length; i++) {
            gameChunks[i] = captureChunkSection(sections[i], registryAccess);
        }

        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            BlockEntity ent = chunk.getBlockEntity(pos);

            int index = chunk.getSectionIndex(pos.getY());
            if (index < 0 || index >= gameChunks.length) continue;

            gameChunks[index].putBlockEntity(ent);
        }

        return gameChunks;
    }

    private static GameChunk captureChunkSection(LevelChunkSection chunkSection, RegistryAccess registryAccess) {
        return new GameChunk(chunkSection.getStates().copy(),
                chunkSection.getBiomes().recreate().copy(), registryAccess);
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
