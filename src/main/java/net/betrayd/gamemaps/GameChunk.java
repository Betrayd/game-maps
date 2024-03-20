package net.betrayd.gamemaps;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;

public class GameChunk {
    private final PalettedContainer<BlockState> blockStateContainer;
    private final PalettedContainer<RegistryEntry<Biome>> biomeContainer;

    private final Map<BlockPos, NbtCompound> blockEntities = new HashMap<>();

    public GameChunk(PalettedContainer<BlockState> blockStateContainer, PalettedContainer<RegistryEntry<Biome>> biomes) {
        this.blockStateContainer = blockStateContainer;
        this.biomeContainer = biomes;
    }

    public GameChunk(Registry<Biome> biomeRegistry) {
        this.blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(),
                PalettedContainer.PaletteProvider.BLOCK_STATE);
        this.biomeContainer = new PalettedContainer<>(biomeRegistry.getIndexedEntries(),
                biomeRegistry.entryOf(BiomeKeys.THE_VOID), PalettedContainer.PaletteProvider.BIOME);
    }

    public Map<BlockPos, NbtCompound> getBlockEntities() {
        return blockEntities;
    }

    /**
     * Add a block entity to this chunk based on its NBT position.
     * @param blockEntity Block entity NBT.
     */
    public void putBlockEntity(NbtCompound blockEntity) {
        blockEntity = blockEntity.copy();
        // Block entities are stored relative to the chunk.
        int x = blockEntity.getInt("x") & 0xF;
        int y = blockEntity.getInt("y") & 0xF;
        int z = blockEntity.getInt("z") & 0xF;

        blockEntities.put(new BlockPos(x, y, z), blockEntity);
    }

    /**
     * Add a block entity to this chunk based on it's NBT position.
     * @param blockEntity Block entity to add.
     */
    public void putBlockEntity(BlockEntity blockEntity) {
        putBlockEntity(blockEntity.createNbtWithIdentifyingData());
    }

    public void putBlockEntity(BlockPos pos, NbtCompound blockEntity) {
        assertInBounds(pos.getX());
        assertInBounds(pos.getY());
        assertInBounds(pos.getZ());

        blockEntities.put(pos, blockEntity);
    }

    public void putBlockEntity(int x, int y, int z, NbtCompound blockEntity) {
        putBlockEntity(new BlockPos(x, y, z), blockEntity);
    }

    @Nullable
    public NbtCompound getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Nullable
    public NbtCompound getBlockEntity(int x, int y, int z) {
        return blockEntities.get(new BlockPos(x, y, z));
    }

    public BlockState getBlockState(int x, int y, int z) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        return blockStateContainer.get(x, y, z);
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        blockStateContainer.set(x, y, z, state);
    }
    
    public ReadableContainer<RegistryEntry<Biome>> getBiomeContainer() {
        return biomeContainer;
    }

    public RegistryEntry<Biome> getBiome(int x, int y, int z) {
        return this.biomeContainer.get(x, y, z);
    }

    private int assertInBounds(int x) throws IndexOutOfBoundsException {
        if (x < 0 || x >= 16) throw new IndexOutOfBoundsException(x);
        return x;
    }
}
