package net.betrayd.gamemaps.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.RegistryAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.betrayd.gamemaps.map_markers.MapMarker;
import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.chunk.PalettedContainerFactory;

public class GameMapDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapDeserializer.class);

    private final RegistryAccess registryAccess;
    private final PalettedContainerFactory containerFactory;

    private final List<Function<GameMapEntity, GameMapEntity>> entityFilters = new ArrayList<>();
    private final List<Function<CompoundTag, CompoundTag>> blockEntityFilters = new ArrayList<>();

    public GameMapDeserializer(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
        this.containerFactory = PalettedContainerFactory.create(registryAccess);
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
        GameMap map = new GameMap(registryAccess);
        map.getMeta().readNbt(nbt);

        ListTag chunkList = nbt.getList("chunks").orElse(null);

        if (chunkList != null) {
            for (Tag chunkTag : chunkList) {
                CompoundTag chunkNbt = (CompoundTag) chunkTag;
                int[] posList = chunkNbt.getIntArray("pos").orElse(new int[3]);
                SectionPos pos = SectionPos.of(posList[0], posList[1], posList[2]);

                chunkNbt.getCompound("chunk").ifPresent(c ->
                        map.putChunk(pos, deserializeChunk(c)));

            }
        }

        nbt.getList("entities").ifPresent(entList -> {
            for (Tag entNbt : entList) {
                GameMapEntity entity = applyEntityMappers(GameMapEntity.fromNbt((CompoundTag) entNbt));
                if (entity != null)
                    map.addEntity(GameMapEntity.fromNbt((CompoundTag) entNbt));
            }
        });

        nbt.getList("markers").ifPresent(markers -> {
            for (Tag element : markers) {
                MapMarker marker = MapMarkerType.deserialize((CompoundTag) element);
                if (marker != null) {
                    map.addMarker(marker);
                }
            }
        });

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
        PalettedContainer<BlockState> blocks = nbt.getCompound("blocks")
                .map(tag -> containerFactory.blockStatesContainerCodec()
                        .parse(NbtOps.INSTANCE, tag)
                        .promotePartial(LOGGER::error)
                        .getOrThrow())
                .orElseGet(containerFactory::createForBlockStates);

        PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) nbt.getCompound("biomes")
                .map(tag -> containerFactory.biomeContainerCodec()
                        .parse(NbtOps.INSTANCE, tag)
                        .promotePartial(LOGGER::error)
                        .getOrThrow())
                .orElseGet(containerFactory::createForBiomes);

        GameChunk chunk = new GameChunk(blocks, biomes, registryAccess);

        ListTag blockEntities = nbt.getList("blockEntities").orElse(null);
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
 