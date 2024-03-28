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
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.PalettedContainer;

public class GameMapDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapSerializer.class);

    private final Registry<Biome> biomeRegistry;

    private final List<Function<GameMapEntity, GameMapEntity>> entityMappers = new ArrayList<>();
    private final List<Function<NbtCompound, NbtCompound>> blockEntityMappers = new ArrayList<>();

    public GameMapDeserializer(Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
    }
    
    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }
    
    public List<Function<GameMapEntity, GameMapEntity>> getEntityMappers() {
        return entityMappers;
    }

    public List<Function<NbtCompound, NbtCompound>> getBlockEntityMappers() {
        return blockEntityMappers;
    }

    public GameMap deserializeMap(InputStream in) throws IOException {
        return deserializeMap(NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes()));
    }

    public GameMap deserializeMap(NbtCompound nbt) {
        GameMap map = new GameMap(biomeRegistry);
        map.getMeta().readNbt(nbt);

        NbtList chunkList = nbt.getList("chunks", NbtElement.COMPOUND_TYPE);

        if (chunkList != null) {
            for (NbtElement chunkTag : chunkList) {
                NbtCompound chunkNbt = (NbtCompound) chunkTag;
                int[] posList = chunkNbt.getIntArray("pos");
                ChunkSectionPos pos = ChunkSectionPos.from(posList[0], posList[1], posList[2]);
                
                map.putChunk(pos, deserializeChunk(chunkNbt.getCompound("chunk")));
            }
        }

        NbtList entityList = nbt.getList("entities", NbtElement.COMPOUND_TYPE);

        if (entityList != null) {
            for (NbtElement entNbt : entityList) {
                GameMapEntity entity = applyEntityMappers(GameMapEntity.fromNbt((NbtCompound) entNbt));
                if (entity != null)
                    map.addEntity(GameMapEntity.fromNbt((NbtCompound) entNbt));
            }
        }

        return map;
    }

    private GameMapEntity applyEntityMappers(GameMapEntity entity) {
        if (entity == null)
            return null;
        for (var func : entityMappers) {
            entity = func.apply(entity);
            if (entity == null)
                return null;
        }
        return entity;
    }

    public GameChunk deserializeChunk(NbtCompound nbt) {
        PalettedContainer<BlockState> blocks = null;
        if (nbt.contains("blocks", NbtElement.COMPOUND_TYPE))
            blocks = GameMapSerializer.BLOCK_CODEC
                    .parse(NbtOps.INSTANCE, nbt.getCompound("blocks"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        PalettedContainer<RegistryEntry<Biome>> biomes = null;
        if (nbt.contains("biomes", NbtElement.COMPOUND_TYPE))
            biomes = GameMapSerializer.createBiomeCodec(biomeRegistry)
                    .parse(NbtOps.INSTANCE, nbt.getCompound("biomes"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        GameChunk chunk = new GameChunk(blocks, biomes, biomeRegistry);

        NbtList blockEntities = nbt.getList("blockEntities", NbtElement.COMPOUND_TYPE);
        if (blockEntities != null) {
            for (NbtElement blockEnt : blockEntities) {
                NbtCompound compound = applyBlockEntityMappers((NbtCompound) blockEnt);
                if (compound != null) {
                    chunk.putBlockEntity(compound);
                }
            }
        }

        return chunk;
    }

    private NbtCompound applyBlockEntityMappers(NbtCompound nbt) {
        if (nbt == null)
            return null;
        for (var mapper : blockEntityMappers) {
            nbt = mapper.apply(nbt);
            if (nbt == null)
                return null;
        }
        return nbt;
    }

}
 