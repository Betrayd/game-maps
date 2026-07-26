package net.betrayd.gamemaps;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.Strategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameChunk {
    private final PalettedContainer<BlockState> blockStateContainer;
    private final PalettedContainer<Holder<Biome>> biomeContainer;

    private final RegistryAccess registryAccess;

    private final Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();

    public GameChunk(@Nullable PalettedContainer<BlockState> blockStateContainer,
            @Nullable PalettedContainer<Holder<Biome>> biomeContainer, RegistryAccess registryAccess) {

        this.registryAccess = registryAccess;
        var containerFactory = PalettedContainerFactory.create(registryAccess);

        this.blockStateContainer = blockStateContainer != null
                ? blockStateContainer
                : containerFactory.createForBlockStates();

        this.biomeContainer = biomeContainer != null
                ? biomeContainer
                : containerFactory.createForBiomes();

    }

    public GameChunk(@NotNull RegistryAccess registryAccess) {
        this(null, null, registryAccess);
    }

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }

    public Map<BlockPos, CompoundTag> getBlockEntities() {
        return blockEntities;
    }

    /**
     * Add a block entity to this chunk based on its NBT position.
     * @param blockEntity Block entity NBT.
     */
    public void putBlockEntity(CompoundTag blockEntity) {
        blockEntity = blockEntity.copy();
        // Block entities are stored relative to the chunk.
        int x = blockEntity.getInt("x").orElse(0) & 0xF;
        int y = blockEntity.getInt("y").orElse(0) & 0xF;
        int z = blockEntity.getInt("z").orElse(0) & 0xF;

        blockEntities.put(new BlockPos(x, y, z), blockEntity);
    }

    /**
     * Add a block entity to this chunk based on it's NBT position.
     * @param blockEntity Block entity to add.
     */
    public void putBlockEntity(BlockEntity blockEntity) {
        putBlockEntity(blockEntity.saveWithFullMetadata(registryAccess));
//        putBlockEntity(blockEntity.saveWithFullMetadata());
    }

    public void putBlockEntity(BlockPos pos, CompoundTag blockEntity) {
        assertInBounds(pos.getX());
        assertInBounds(pos.getY());
        assertInBounds(pos.getZ());

        blockEntities.put(pos, blockEntity);
    }

    public void putBlockEntity(int x, int y, int z, CompoundTag blockEntity) {
        putBlockEntity(new BlockPos(x, y, z), blockEntity);
    }

    @Nullable
    public CompoundTag getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Nullable
    public CompoundTag getBlockEntity(int x, int y, int z) {
        return blockEntities.get(new BlockPos(x, y, z));
    }

    /**
     * Serialize all block entities with their correct positions.
     * @return Stream of serialized block entities.
     */
    public Stream<CompoundTag> serializeBlockEntities() {
        return blockEntities.entrySet().stream().map(e -> {
            CompoundTag nbt = e.getValue().copy();
            nbt.putInt("x", e.getKey().getX());
            nbt.putInt("y", e.getKey().getY());
            nbt.putInt("z", e.getKey().getZ());
            return nbt;
        });
    }

    public PalettedContainer<BlockState> getBlockStateContainer() {
        return blockStateContainer;
    }

    public BlockState getBlockState(int x, int y, int z) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        return blockStateContainer.get(x, y, z);
    }

    public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        blockStateContainer.set(x, y, z, state);
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        setBlockState(pos.getX(), pos.getY(), pos.getZ(), state);
    }
    
    
    public PalettedContainer<Holder<Biome>> getBiomeContainer() {
        return biomeContainer;
    }

    public Holder<Biome> getBiome(int x, int y, int z) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        return this.biomeContainer.get(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }

    public Holder<Biome> getBiome(BlockPos pos) {
        return getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setBiome(int x, int y, int z, Holder<Biome> biome) {
        assertInBounds(x);
        assertInBounds(y);
        assertInBounds(z);

        this.biomeContainer.set(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), biome);
    }

    public void setBiome(BlockPos pos, Holder<Biome> biome) {
        setBiome(pos.getX(), pos.getY(), pos.getZ(), biome);
    }

    protected static int assertInBounds(int val) throws IndexOutOfBoundsException {
        if (val < 0 || val >= 16)
            throw new IndexOutOfBoundsException("Biome coord out of bounds: " + val
                    + " Remember, biome voxels are worth 4 regular blocks. Therefore, chunks are only 4 biome voxels wide.");
        return val;
    }
}
