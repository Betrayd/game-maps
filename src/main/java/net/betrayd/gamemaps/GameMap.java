package net.betrayd.gamemaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class GameMap {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final Registry<Biome> biomeRegistry;
    private final Map<ChunkSectionPos, GameChunk> chunks = new HashMap<>();

    private final Collection<GameMapEntity> entities = new ArrayList<>();
    
    public GameMap(Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
    }

    public Map<ChunkSectionPos, GameChunk> getChunks() {
        return chunks;
    }

    @Nullable
    public GameChunk getChunk(ChunkSectionPos pos) {
        return chunks.get(pos);
    }
    
    public GameChunk getOrCreateChunk(ChunkSectionPos pos) {
        return chunks.computeIfAbsent(pos, p -> new GameChunk(biomeRegistry));
    }

    public void putChunk(ChunkSectionPos pos, GameChunk chunk) {
        chunks.put(pos, chunk);
    }

    public BlockState getBlock(int x, int y, int z) {
        ChunkSectionPos chunkPos = getChunkPos(x, y, z);

        GameChunk chunk = getOrCreateChunk(chunkPos);
        if (chunk == null) {
            return AIR;
        }
        return chunk.getBlockState(x & 0xF, y & 0xF, z & 0xF);
    }

    public BlockState getBlock(BlockPos pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setBlock(int x, int y, int z, BlockState block) {
        ChunkSectionPos chunkPos = ChunkSectionPos.from(x >> 4, y >> 4, z >> 4); // x / 16

        GameChunk chunk = getOrCreateChunk(chunkPos);
        chunk.setBlockState(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public void setBlock(BlockPos pos, BlockState block) {
        setBlock(pos.getX(), pos.getY(), pos.getZ(), block);
    }

    @Nullable
    public NbtCompound getBlockEntity(int x, int y, int z) {
        ChunkSectionPos chunkPos = getChunkPos(x, y, z);

        GameChunk chunk = getChunk(chunkPos);
        if (chunk == null)
            return null;
        return chunk.getBlockEntity(x & 0xF, y & 0xF, z & 0xF);
    }

    @Nullable
    public NbtCompound getBlockEntity(BlockPos pos) {
        return getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public void putBlockEntity(int x, int y, int z, NbtCompound blockEntity) {
        ChunkSectionPos chunkPos = getChunkPos(x, y, z);
        GameChunk chunk = getOrCreateChunk(chunkPos);
        chunk.putBlockEntity(x & 0xF, y & 0xF, z & 0xF, blockEntity);
    }

    public void putBlockEntity(BlockPos pos, NbtCompound blockEntity) {
        putBlockEntity(pos.getX(), pos.getY(), pos.getZ(), blockEntity);
    }

    public void putBlockEntity(NbtCompound blockEntity) {
        int x = blockEntity.getInt("x");
        int y = blockEntity.getInt("y");
        int z = blockEntity.getInt("z");

        putBlockEntity(x, y, z, blockEntity);
    }

    public void putBlockEntity(BlockEntity blockEntity) {
        putBlockEntity(blockEntity.createNbtWithIdentifyingData());
    }

    public Collection<GameMapEntity> getEntities() {
        return entities;
    }

    public void addEntity(GameMapEntity entity) {
        entities.add(entity);
    }

    public GameMapEntity addEntity(Entity entity) {
        GameMapEntity ent = GameMapEntity.fromEntity(entity);
        if (ent == null) return null;
        entities.add(ent);
        return ent;
    }

    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }

    public RegistryEntry<Biome> getBiome(int x, int y, int z) {
        ChunkSectionPos chunkPos = getChunkPos(x, y, z);
        GameChunk chunk = getChunk(chunkPos);

        if (chunk == null) return biomeRegistry.entryOf(BiomeKeys.THE_VOID);
        return chunk.getBiome(x & 0xF, y & 0xF, z & 0xF);
    }

    public RegistryEntry<Biome> getBiome(BlockPos pos) {
        return getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    private static ChunkSectionPos getChunkPos(int x, int y, int z) {
        return ChunkSectionPos.from(x >> 4, y >> 4, z >> 4); // x >> 4 == x / 16
    }
}
 