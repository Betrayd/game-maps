package net.betrayd.gamemaps.world_interface;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;

/**
 * A chunk generator with empty implementations for many boilerplate methods.
 */
public abstract class SimpleChunkGenerator extends ChunkGenerator {

    public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.stable().fieldOf("biome").forGetter(VoidChunkGenerator::getBiome)
    ).apply(instance, instance.stable(VoidChunkGenerator::new)));

    public static final NoiseColumn VOID_SAMPLE = new NoiseColumn(0, new BlockState[0]);

    public SimpleChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    protected static FixedBiomeSource createBiomeSource(MinecraftServer server, ResourceKey<Biome> biome) {
        var registryManager = server.registryAccess();
//        return new FixedBiomeSource(registryManager.getOrThrow(Registries.BIOME).getHolder(biome).get());
        return new FixedBiomeSource(registryManager.getOrThrow(Registries.BIOME).value().getOrThrow(biome));
    }

    @Override
    public void createReferences(WorldGenLevel world, StructureManager structureAccessor, ChunkAccess chunk) {

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
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

}
