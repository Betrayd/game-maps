package net.betrayd.gamemaps.world_interface;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * A chunk generator with empty implementations for many boilerplate methods.
 */
public abstract class SimpleChunkGenerator extends ChunkGenerator {

    public static final Codec<? extends ChunkGenerator> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return Biome.REGISTRY_CODEC.decode(ops, ops.createString(BiomeKeys.THE_VOID.getValue().toString()))
                    .map(pair -> pair.mapFirst(VoidChunkGenerator::new));
        }

        @Override
        public <T> DataResult<T> encode(ChunkGenerator input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(prefix);
        }
    };

    public static final VerticalBlockSample VOID_SAMPLE = new VerticalBlockSample(0, new BlockState[0]);

    public SimpleChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    protected static FixedBiomeSource createBiomeSource(MinecraftServer server, RegistryKey<Biome> biome) {
        var registryManager = server.getRegistryManager();
        return new FixedBiomeSource(registryManager.get(RegistryKeys.BIOME).getEntry(biome).get());
    }

    @Override
    public void addStructureReferences(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {

    }

    @Override
    public void setStructureStarts(DynamicRegistryManager registryManager,
            StructurePlacementCalculator placementCalculator, StructureAccessor structureAccessor, Chunk chunk,
            StructureTemplateManager structureTemplateManager) {

    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig,
            StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getHeight(int x, int z, Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 0;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return VOID_SAMPLE;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 0;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
            StructureAccessor structureAccessor, Chunk chunk, Carver carverStep) {

    }
}
