package net.betrayd.gamemaps.world_interface;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Generates chunks based on a game map. This generator assumes the game map
 * never changes during the generation process.
 */
public class GameMapChunkGenerator extends SimpleChunkGenerator {

    private final GameMap gameMap;

    private final int minX;
    private final int maxX;

    private final int minY;
    private final int maxY;

    private final int minZ;
    private final int maxZ;

    private final HeightmapCache heightmapCache = new HeightmapCache();

    public GameMapChunkGenerator(GameMap gameMap) {
        super(new GameMapBiomeSource(gameMap));
        this.gameMap = gameMap;

        minX = GameMapUtils.calcMinX(gameMap.getChunks().keySet());
        maxX = GameMapUtils.calcMaxX(gameMap.getChunks().keySet());

        minY = GameMapUtils.calcMinY(gameMap.getChunks().keySet());
        maxY = GameMapUtils.calcMaxY(gameMap.getChunks().keySet());

        minZ = GameMapUtils.calcMinZ(gameMap.getChunks().keySet());
        maxZ = GameMapUtils.calcMaxZ(gameMap.getChunks().keySet());
    }

    public GameMap getGameMap() {
        return gameMap;
    }

//    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        
        Map<SectionPos, GameChunk> gameChunks = gameMap.getChunks().entrySet().stream().filter(e -> {
            return (e.getKey().getX() == chunkPos.x())
                && (e.getKey().getZ() == chunkPos.z());
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        if (gameChunks.isEmpty()) return CompletableFuture.completedFuture(chunk);

        int minSectionY = SectionPos.blockToSectionCoord(minY);
        int maxSectionY = SectionPos.blockToSectionCoord(maxY);

        return CompletableFuture.supplyAsync(() -> {
            for (int sectionY = maxSectionY; sectionY >= minSectionY; sectionY--) {
                
                SectionPos sectionPos = SectionPos.of(chunkPos, sectionY);
                GameChunk gameChunk = gameMap.getChunk(sectionPos);
                if (gameChunk == null)
                    continue;

                var section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                section.acquire();

                try {
                    addSection(chunk, section, gameChunk, sectionPos);
                } finally {
                    section.release();
                }
            }
            return chunk;
        }, Util.backgroundExecutor().forName("GameMaps World Loader"));

    }
    
    private void addSection(ChunkAccess chunk, LevelChunkSection section, GameChunk gameChunk, SectionPos chunkPos) {
        var oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        var worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = gameChunk.getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    section.setBlockState(x, y, z, state, false);

                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }

        BlockPos chunkBlockPos = chunkPos.origin();
        gameChunk.getBlockEntities().forEach((pos, nbt) -> {
            if (nbt == null) return;

            BlockPos globalPos = chunkBlockPos.offset(pos);
            nbt = nbt.copy();
            nbt.putInt("x", globalPos.getX());
            nbt.putInt("y", globalPos.getY());
            nbt.putInt("z", globalPos.getZ());

            chunk.setBlockEntityNbt(nbt);
        });
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {

    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos chunkPos = region.getCenter();

        ProtoChunk chunk = (ProtoChunk) region.getChunk(chunkPos.x(), chunkPos.z());

        BlockPos minChunkPos = new BlockPos(chunkPos.getMinBlockX(), region.getMinY(), chunkPos.getMinBlockZ());
        BlockPos maxChunkPos = new BlockPos(chunkPos.getMaxBlockX(), region.getMaxY(), chunkPos.getMaxBlockZ());

        // TODO: optimize this?
        for (GameMapEntity entity : gameMap.getEntities()) {
            if (boxContains(minChunkPos, maxChunkPos, BlockPos.containing(entity.pos()))) {
                chunk.addEntity(entity.createEntityNbt());
            }
        }
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getGenDepth() {
        return maxY - minY;
    }

    @Override
    public int getBaseHeight(int x, int z, Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        if (!isInBounds(x, z))
            return 0;

        return heightmapCache.computeIfAbsent(x, z, heightmap,
                () -> GameMapUtils.getTopY(x, z, gameMap, minY, maxY, heightmap));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        if (!isInBounds(x, z)) return VOID_SAMPLE;

        BlockState[] column = new BlockState[maxY - minY + 1];
        for (int y = maxY; y >= minY; y--) {
            column[y] = gameMap.getBlock(x, y, z);
        }

        return new NoiseColumn(minY, column);
    }

    public boolean isInBounds(int x, int y, int z) {
        return minX <= x && x <= maxX
            && minY <= y && y <= maxY
            && minZ <= z && z <= maxZ;
    }

    public boolean isInBounds(int x, int z) {
        return minX <= x && x <= maxX
            && minZ <= z && z <= maxZ;
    }

    private static boolean boxContains(BlockPos minPos, BlockPos maxPos, BlockPos pos) {
        return minPos.getX() <= pos.getX() && pos.getX() <= maxPos.getX()
            && minPos.getY() <= pos.getY() && pos.getY() <= maxPos.getY()
            && minPos.getZ() <= pos.getZ() && pos.getZ() <= maxPos.getZ();
    }
    
}
