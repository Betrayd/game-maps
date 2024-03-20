package net.betrayd.gamemaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.PalettedContainer;

public class GameMapSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapSerializer.class);

    private static final Codec<PalettedContainer<BlockState>> BLOCK_CODEC = PalettedContainer
            .createPalettedContainerCodec(Block.STATE_IDS, BlockState.CODEC,
                    PalettedContainer.PaletteProvider.BLOCK_STATE, Blocks.AIR.getDefaultState());
    
    public static NbtCompound serializeMap(GameMap map) {
        NbtCompound nbt = new NbtCompound();

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

        return nbt;
    }

    public static GameMap deserializeMap(NbtCompound nbt, Registry<Biome> biomeRegistry) {
        GameMap map = new GameMap(biomeRegistry);

        NbtList chunkList = nbt.getList("chunks", NbtElement.COMPOUND_TYPE);

        if (chunkList != null) {
            for (NbtElement chunkTag : chunkList) {
                NbtCompound chunkNbt = (NbtCompound) chunkTag;
                int[] posList = chunkNbt.getIntArray("pos");
                ChunkSectionPos pos = ChunkSectionPos.from(posList[0], posList[1], posList[2]);
                
                map.putChunk(pos, deserializeChunk(chunkNbt.getCompound("chunk"), biomeRegistry));
            }
        }

        NbtList entityList = nbt.getList("entities", NbtElement.COMPOUND_TYPE);

        if (entityList != null) {
            for (NbtElement entNbt : entityList) {
                map.addEntity(GameMapEntity.fromNbt((NbtCompound) entNbt));
            }
        }

        return map;
    }
    

    public static NbtCompound serializeChunk(GameChunk chunk) {
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

    public static GameChunk deserializeChunk(NbtCompound nbt, Registry<Biome> biomeRegistry) {
        PalettedContainer<BlockState> blocks = null;
        if (nbt.contains("blocks", NbtElement.COMPOUND_TYPE))
            blocks = BLOCK_CODEC
                    .parse(NbtOps.INSTANCE, nbt.getCompound("blocks"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        PalettedContainer<RegistryEntry<Biome>> biomes = null;
        if (nbt.contains("biomes", NbtElement.COMPOUND_TYPE))
            biomes = createBiomeCodec(biomeRegistry)
                    .parse(NbtOps.INSTANCE, nbt.getCompound("biomes"))
                    .promotePartial(LOGGER::error)
                    .get().left().orElse(null);

        GameChunk chunk = new GameChunk(blocks, biomes, biomeRegistry);

        NbtList blockEntities = nbt.getList("blockEntities", NbtElement.LIST_TYPE);
        if (blockEntities != null) {
            for (NbtElement blockEnt : blockEntities) {
                chunk.putBlockEntity((NbtCompound) blockEnt);
            }
        }

        return chunk;
    }

    protected static Codec<PalettedContainer<RegistryEntry<Biome>>> createBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.createPalettedContainerCodec(biomeRegistry.getIndexedEntries(),
                biomeRegistry.createEntryCodec(), PalettedContainer.PaletteProvider.BIOME,
                biomeRegistry.entryOf(BiomeKeys.THE_VOID));
    }
}
