package net.betrayd.gamemaps.serialization;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;

import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.betrayd.gamemaps.map_markers.MapMarker;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public class GameMapSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapSerializer.class);

    protected static final Codec<PalettedContainer<BlockState>> BLOCK_CODEC = PalettedContainer
            .codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC,
                    PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());

    public void serializeMap(GameMap map, OutputStream out) throws IOException {
        NbtIo.writeCompressed(serializeMap(map), out);
    }
    
    public CompoundTag serializeMap(GameMap map) {
        CompoundTag nbt = new CompoundTag();
        map.getMeta().writeNbt(nbt);

        ListTag chunkList = new ListTag();

        map.getChunks().forEach((pos, chunk) -> {
            CompoundTag chunkNbt = new CompoundTag();
            chunkNbt.putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            chunkNbt.put("chunk", serializeChunk(chunk));
            chunkList.add(chunkNbt);
        });
        nbt.put("chunks", chunkList);

        ListTag entities = new ListTag();

        for (GameMapEntity ent : map.getEntities()) {
            entities.add(ent.createEntityNbt());
        }
        nbt.put("entities", entities);
        
        ListTag markers = new ListTag();
        for (MapMarker marker : map.getMarkers()) {
            markers.add(marker.writeNbt(new CompoundTag()));
        }
        nbt.put("markers", markers);

        return nbt;
    } 

    public CompoundTag serializeChunk(GameChunk chunk) {
        CompoundTag nbt = new CompoundTag();

        Tag blocks = BLOCK_CODEC
                .encodeStart(NbtOps.INSTANCE, chunk.getBlockStateContainer())
                .getOrThrow(false, LOGGER::error);

        nbt.put("blocks", blocks);

        Tag biomes = createBiomeCodec(chunk.getBiomeRegistry())
                .encodeStart(NbtOps.INSTANCE, chunk.getBiomeContainer())
                .getOrThrow(false, LOGGER::error);

        nbt.put("biomes", biomes);

        ListTag blockEntities = new ListTag();
        chunk.serializeBlockEntities().forEach(blockEntities::add);
        nbt.put("blockEntities", blockEntities);

        return nbt;
    }


    protected static Codec<PalettedContainer<Holder<Biome>>> createBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRW(biomeRegistry.asHolderIdMap(),
                biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES,
                biomeRegistry.getHolderOrThrow(Biomes.THE_VOID));
    }
}
