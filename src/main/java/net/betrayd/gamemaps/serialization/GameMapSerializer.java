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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.PalettedContainer;

public class GameMapSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapSerializer.class);

    protected static final Codec<PalettedContainer<BlockState>> BLOCK_CODEC = PalettedContainer
            .createPalettedContainerCodec(Block.STATE_IDS, BlockState.CODEC,
                    PalettedContainer.PaletteProvider.BLOCK_STATE, Blocks.AIR.getDefaultState());

    public void serializeMap(GameMap map, OutputStream out) throws IOException {
        NbtIo.writeCompressed(serializeMap(map), out);
    }
    
    public NbtCompound serializeMap(GameMap map) {
        NbtCompound nbt = new NbtCompound();
        map.getMeta().writeNbt(nbt);

        NbtList chunkList = new NbtList();

        map.getChunks().forEach((pos, chunk) -> {
            NbtCompound chunkNbt = new NbtCompound();
            chunkNbt.putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            chunkNbt.put("chunk", serializeChunk(chunk));
            chunkList.add(chunkNbt);
        });
        nbt.put("chunks", chunkList);

        NbtList entities = new NbtList();

        for (GameMapEntity ent : map.getEntities()) {
            entities.add(ent.createEntityNbt());
        }
        nbt.put("entities", entities);
        
        NbtList markers = new NbtList();
        for (MapMarker marker : map.getMarkers()) {
            markers.add(marker.writeNbt(new NbtCompound()));
        }
        nbt.put("markers", markers);

        return nbt;
    } 

    public NbtCompound serializeChunk(GameChunk chunk) {
        NbtCompound nbt = new NbtCompound();

        NbtElement blocks = BLOCK_CODEC
                .encodeStart(NbtOps.INSTANCE, chunk.getBlockStateContainer())
                .getOrThrow(false, LOGGER::error);

        nbt.put("blocks", blocks);

        NbtElement biomes = createBiomeCodec(chunk.getBiomeRegistry())
                .encodeStart(NbtOps.INSTANCE, chunk.getBiomeContainer())
                .getOrThrow(false, LOGGER::error);

        nbt.put("biomes", biomes);

        NbtList blockEntities = new NbtList();
        chunk.serializeBlockEntities().forEach(blockEntities::add);
        nbt.put("blockEntities", blockEntities);

        return nbt;
    }


    protected static Codec<PalettedContainer<RegistryEntry<Biome>>> createBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.createPalettedContainerCodec(biomeRegistry.getIndexedEntries(),
                biomeRegistry.createEntryCodec(), PalettedContainer.PaletteProvider.BIOME,
                biomeRegistry.entryOf(BiomeKeys.THE_VOID));
    }
}
