package net.betrayd.gamemaps.world_interface;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

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

        minX = MapUtils.calcMinX(gameMap.getChunks().keySet());
        maxX = MapUtils.calcMaxX(gameMap.getChunks().keySet());

        minY = MapUtils.calcMinY(gameMap.getChunks().keySet());
        maxY = MapUtils.calcMaxY(gameMap.getChunks().keySet());

        minZ = MapUtils.calcMinZ(gameMap.getChunks().keySet());
        maxZ = MapUtils.calcMaxZ(gameMap.getChunks().keySet());
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig,
            StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        
        Map<ChunkSectionPos, GameChunk> gameChunks = gameMap.getChunks().entrySet().stream().filter(e -> {
            return (e.getKey().getX() == chunkPos.x)
                && (e.getKey().getZ() == chunkPos.z);
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        if (gameChunks.isEmpty()) return CompletableFuture.completedFuture(chunk);

        int minSectionY = ChunkSectionPos.getSectionCoord(minY);
        int maxSectionY = ChunkSectionPos.getSectionCoord(maxY);

        return CompletableFuture.supplyAsync(() -> {
            for (int sectionY = maxSectionY; sectionY >= minSectionY; sectionY--) {
                
                ChunkSectionPos sectionPos = ChunkSectionPos.from(chunkPos, sectionY);
                GameChunk gameChunk = gameMap.getChunk(sectionPos);
                if (gameChunk == null)
                    continue;

                var section = chunk.getSection(chunk.sectionCoordToIndex(sectionY));
                section.lock();

                try {
                    addSection(chunk, section, gameChunk, sectionPos);
                } finally {
                    section.unlock();
                }
            }
            return chunk;
        }, executor);

    }
    
    private void addSection(Chunk chunk, ChunkSection section, GameChunk gameChunk, ChunkSectionPos chunkPos) {
        var oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        var worldSurface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = gameChunk.getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    section.setBlockState(x, y, z, state, false);

                    oceanFloor.trackUpdate(x, y, z, state);
                    worldSurface.trackUpdate(x, y, z, state);
                }
            }
        }

        BlockPos chunkBlockPos = chunkPos.getMinPos();
        gameChunk.getBlockEntities().forEach((pos, nbt) -> {
            if (nbt == null) return;

            BlockPos globalPos = chunkBlockPos.add(pos);
            nbt = nbt.copy();
            nbt.putInt("x", globalPos.getX());
            nbt.putInt("y", globalPos.getY());
            nbt.putInt("z", globalPos.getZ());

            chunk.addPendingBlockEntityNbt(nbt);
        });
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        ChunkPos chunkPos = region.getCenterPos();

        ProtoChunk chunk = (ProtoChunk) region.getChunk(chunkPos.x, chunkPos.z);

        BlockPos minChunkPos = new BlockPos(chunkPos.getStartX(), region.getBottomY(), chunkPos.getStartZ());
        BlockPos maxChunkPos = new BlockPos(chunkPos.getEndX(), region.getTopY(), chunkPos.getEndZ());

        // TODO: optimize this?
        for (GameMapEntity entity : gameMap.getEntities()) {
            if (boxContains(minChunkPos, maxChunkPos, BlockPos.ofFloored(entity.pos()))) {
                chunk.addEntity(entity.createEntityNbt());
            }
        }
    }

    @Override
    public int getMinimumY() {
        return minY;
    }

    @Override
    public int getWorldHeight() {
        return maxY - minY;
    }
    
    @Override
    public int getHeight(int x, int z, Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (!isInBounds(x, z))
            return 0;

        return heightmapCache.computeIfAbsent(x, z, heightmap,
                () -> MapUtils.getTopY(x, z, gameMap, minY, maxY, heightmap));
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        if (!isInBounds(x, z)) return VOID_SAMPLE;

        BlockState[] column = new BlockState[maxY - minY + 1];
        for (int y = maxY; y >= minY; y--) {
            column[y] = gameMap.getBlock(x, y, z);
        }

        return new VerticalBlockSample(minY, column);
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
