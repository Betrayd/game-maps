package net.betrayd.gamemaps.world_interface;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A chunk generator with empty implementations for many boilerplate methods.
 */
public abstract class SimpleChunkGenerator extends ChunkGenerator {

    public static final Codec<? extends ChunkGenerator> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return Biome.CODEC.decode(ops, ops.createString(Biomes.THE_VOID.location().toString()))
                    .map(pair -> pair.mapFirst(VoidChunkGenerator::new));
        }

        @Override
        public <T> DataResult<T> encode(ChunkGenerator input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(prefix);
        }
    };

    public static final NoiseColumn VOID_SAMPLE = new NoiseColumn(0, new BlockState[0]);

    public SimpleChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    protected static FixedBiomeSource createBiomeSource(MinecraftServer server, ResourceKey<Biome> biome) {
        var registryManager = server.registryAccess();
        return new FixedBiomeSource(registryManager.registryOrThrow(Registries.BIOME).getHolder(biome).get());
    }

    @Override
    public void createReferences(WorldGenLevel world, StructureManager structureAccessor, ChunkAccess chunk) {

    }

    @Override
    public void createStructures(RegistryAccess registryManager,
            ChunkGeneratorStructureState placementCalculator, StructureManager structureAccessor, ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager) {

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState noiseConfig,
            StructureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {

    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {

    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        return VOID_SAMPLE;
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {

    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {

    }

    @Override
    public int getGenDepth() {
        return 0;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess,
            StructureManager structureAccessor, ChunkAccess chunk, Carving carverStep) {

    }
}
