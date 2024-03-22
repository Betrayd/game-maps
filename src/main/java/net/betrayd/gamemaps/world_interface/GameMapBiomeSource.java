package net.betrayd.gamemaps.world_interface;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.betrayd.gamemaps.GameMap;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

/**
 * A <code>BiomeSource</code> based on the biomes from a game map, mainly for
 * use in {@link GameMapChunkGenerator}.
 */
public class GameMapBiomeSource extends BiomeSource {

    /**
     * We never need to deserialize this biome source, so a bullshit codec provides
     * a fixed biome source with a random biome upon deserialization.
     */
    public static final Codec<BiomeSource> CODEC = (Biome.REGISTRY_CODEC.fieldOf("biome"))
            .<BiomeSource>xmap(FixedBiomeSource::new, biomeSource -> {
                return biomeSource.getBiomes().iterator().next();
            }).stable().codec();

    private final GameMap gameMap;

    public GameMapBiomeSource(GameMap gameMap) {
        this.gameMap = gameMap;
    }
    
    @Nullable
    public GameMap getGameMap() {
        return gameMap;
    }
    
    public Codec<BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        if (gameMap == null) {
        }

        Registry<Biome> biomes = gameMap.getBiomeRegistry();
        return gameMap.getBiomeRegistry().streamEntries().map(b -> biomes.getEntry(b.value()));
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler var4) {
        int blockX = BiomeCoords.toBlock(x);
        int blockY = BiomeCoords.toBlock(y);
        int blockZ = BiomeCoords.toBlock(z);

        return gameMap.getBiome(blockX, blockY, blockZ);
    }
    
}
