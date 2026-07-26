package net.betrayd.gamemaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

import net.betrayd.gamemaps.map_markers.MapMarker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GameMap {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final RegistryAccess registryAccess;
    private final Map<SectionPos, GameChunk> chunks = new HashMap<>();

    private final Collection<GameMapEntity> entities = new ArrayList<>();
    
    private final Collection<MapMarker> markers = new ArrayList<>();

    private final GameMapMeta meta = new GameMapMeta();

    public GameMap(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    public final GameMapMeta getMeta() {
        return meta;
    }

    public final CompoundTag getCustomData() {
        return meta.getCustomData();
    }

    public Map<SectionPos, GameChunk> getChunks() {
        return chunks;
    }

    public @Nullable GameChunk getChunk(SectionPos pos) {
        return chunks.get(pos);
    }
    
    public GameChunk getOrCreateChunk(SectionPos pos) {
        return chunks.computeIfAbsent(pos, p -> new GameChunk(registryAccess));
    }

    public void putChunk(SectionPos pos, GameChunk chunk) {
        chunks.put(pos, chunk);
    }

    public BlockState getBlock(int x, int y, int z) {
        SectionPos chunkPos = getChunkPos(x, y, z);

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
        SectionPos chunkPos = SectionPos.of(x >> 4, y >> 4, z >> 4); // x / 16

        GameChunk chunk = getOrCreateChunk(chunkPos);
        chunk.setBlockState(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public void setBlock(BlockPos pos, BlockState block) {
        setBlock(pos.getX(), pos.getY(), pos.getZ(), block);
    }

    @Nullable
    public CompoundTag getBlockEntity(int x, int y, int z) {
        SectionPos chunkPos = getChunkPos(x, y, z);

        GameChunk chunk = getChunk(chunkPos);
        if (chunk == null)
            return null;
        return chunk.getBlockEntity(x & 0xF, y & 0xF, z & 0xF);
    }

    @Nullable
    public CompoundTag getBlockEntity(BlockPos pos) {
        return getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public void putBlockEntity(int x, int y, int z, CompoundTag blockEntity) {
        SectionPos chunkPos = getChunkPos(x, y, z);
        GameChunk chunk = getOrCreateChunk(chunkPos);
        chunk.putBlockEntity(x & 0xF, y & 0xF, z & 0xF, blockEntity);
    }

    public void putBlockEntity(BlockPos pos, CompoundTag blockEntity) {
        putBlockEntity(pos.getX(), pos.getY(), pos.getZ(), blockEntity);
    }

    public void putBlockEntity(CompoundTag blockEntity) {
        int x = blockEntity.getInt("x").orElse(0);
        int y = blockEntity.getInt("y").orElse(0);
        int z = blockEntity.getInt("z").orElse(0);

        putBlockEntity(x, y, z, blockEntity);
    }

    public void putBlockEntity(BlockEntity blockEntity) {
        putBlockEntity(blockEntity.saveWithFullMetadata(registryAccess));
    }

    public void putBlockEntity(BlockPos pos, BlockEntity blockEntity) {
        putBlockEntity(pos, blockEntity.saveWithFullMetadata(registryAccess));
    }

    public Collection<GameMapEntity> getEntities() {
        return entities;
    }

    public void addEntity(GameMapEntity entity) {
        if (entity == null) return;
        entities.add(entity);
    }

    public GameMapEntity addEntity(Entity entity) {
        GameMapEntity ent = GameMapEntity.fromEntity(entity);
        if (ent == null) return null;
        entities.add(ent);
        return ent;
    }

    public Collection<MapMarker> getMarkers() {
        return markers;
    }

    public void addMarker(MapMarker mapMarker) {
        this.markers.add(mapMarker);
    }

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // If any of these are empty, we have a problem
    public Holder<Biome> getBiome(int x, int y, int z) {
        SectionPos chunkPos = getChunkPos(x, y, z);
        GameChunk chunk = getChunk(chunkPos);

        if (chunk == null)
            return registryAccess.get(Registries.BIOME).get().value().get(Biomes.THE_VOID).get();
        return chunk.getBiome(x & 0xF, y & 0xF, z & 0xF);
    }

    public Holder<Biome> getBiome(BlockPos pos) {
        return getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setBiome(int x, int y, int z, Holder<Biome> biome) {
        SectionPos chunkPos = getChunkPos(x, y, z);
        GameChunk chunk = getOrCreateChunk(chunkPos);

        chunk.setBiome(x & 0xF, y & 0xF, z & 0xF, biome);
    }

    public void setBiome(BlockPos pos, Holder<Biome> biome) {
        setBiome(pos.getX(), pos.getY(), pos.getZ(), biome);
    }

    private static SectionPos getChunkPos(int x, int y, int z) {
        return SectionPos.of(x >> 4, y >> 4, z >> 4); // x >> 4 == x / 16
    }
}
 