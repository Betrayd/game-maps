package net.betrayd.gamemaps.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.betrayd.gamemaps.map_markers.MapMarker;
import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public class GameMapDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapSerializer.class);

    private final Registry<Biome> biomeRegistry;

    private final List<Function<GameMapEntity, GameMapEntity>> entityFilters = new ArrayList<>();
    private final List<Function<CompoundTag, CompoundTag>> blockEntityFilters = new ArrayList<>();

    public GameMapDeserializer(Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
    }
    
    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }
    
    public List<Function<GameMapEntity, GameMapEntity>> getEntityFilters() {
        return entityFilters;
    }

    public List<Function<CompoundTag, CompoundTag>> getBlockEntityFilters() {
        return blockEntityFilters;
    }

    public GameMap deserializeMap(InputStream in) throws IOException {
        return deserializeMap(NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
    }

    public GameMap deserializeMap(CompoundTag nbt) {
        GameMap map = new GameMap(biomeRegistry);
        map.getMeta().readNbt(nbt);

        ListTag chunkList = nbt.getList("chunks", Tag.TAG_COMPOUND);

        if (chunkList != null) {
            for (Tag chunkTag : chunkList) {
                CompoundTag chunkNbt = (CompoundTag) chunkTag;
                int[] posList = chunkNbt.getIntArray("pos");
                SectionPos pos = SectionPos.of(posList[0], posList[1], posList[2]);
                
                map.putChunk(pos, deserializeChunk(chunkNbt.getCompound("chunk")));
            }
        }

        ListTag entityList = nbt.getList("entities", Tag.TAG_COMPOUND);

        if (entityList != null) {
            for (Tag entNbt : entityList) {
                GameMapEntity entity = applyEntityMappers(GameMapEntity.fromNbt((CompoundTag) entNbt));
                if (entity != null)
                    map.addEntity(GameMapEntity.fromNbt((CompoundTag) entNbt));
            }
        }

        ListTag markers = nbt.getList("markers", Tag.TAG_COMPOUND);

        for (Tag element : markers) {
            MapMarker marker = MapMarkerType.deserialize((CompoundTag) element);
            if (marker != null) {
                map.addMarker(marker);
            }
        }

        return map;
    }

    private GameMapEntity applyEntityMappers(GameMapEntity entity) {
        if (entity == null)
            return null;
        for (var func : entityFilters) {
            entity = func.apply(entity);
            if (entity == null)
                return null;
        }
        return entity;
    }

    public GameChunk deserializeChunk(CompoundTag nbt) {
        PalettedContainer<BlockState> blocks = null;
        if (nbt.contains("blocks", Tag.TAG_COMPOUND))
            blocks = GameMapSerializer.BLOCK_CODEC
                    .parse(NbtOps.INSTANCE, nbt.getCompound("blocks"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        PalettedContainer<Holder<Biome>> biomes = null;
        if (nbt.contains("biomes", Tag.TAG_COMPOUND))
            biomes = GameMapSerializer.createBiomeCodec(biomeRegistry)
                    .parse(NbtOps.INSTANCE, nbt.getCompound("biomes"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        GameChunk chunk = new GameChunk(blocks, biomes, biomeRegistry);

        ListTag blockEntities = nbt.getList("blockEntities", Tag.TAG_COMPOUND);
        if (blockEntities != null) {
            for (Tag blockEnt : blockEntities) {
                CompoundTag compound = applyBlockEntityMappers((CompoundTag) blockEnt);
                if (compound != null) {
                    chunk.putBlockEntity(compound);
                }
            }
        }

        return chunk;
    }

    private CompoundTag applyBlockEntityMappers(CompoundTag nbt) {
        if (nbt == null)
            return null;
        for (var mapper : blockEntityFilters) {
            nbt = mapper.apply(nbt);
            if (nbt == null)
                return null;
        }
        return nbt;
    }

}
 