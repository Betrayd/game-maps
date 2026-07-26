package net.betrayd.gamemaps.world_interface;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.betrayd.gamemaps.GameMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.FixedBiomeSource;

/**
 * A <code>BiomeSource</code> based on the biomes from a game map, mainly for
 * use in {@link GameMapChunkGenerator}.
 */
public class GameMapBiomeSource extends BiomeSource {

    /**
     * We never need to deserialize this biome source, so a bullshit codec provides
     * a fixed biome source with a random biome upon deserialization.
     */
    public static final Codec<BiomeSource> CODEC = (Biome.CODEC.fieldOf("biome"))
            .<BiomeSource>xmap(FixedBiomeSource::new, biomeSource -> {
                return biomeSource.possibleBiomes().iterator().next();
            }).stable().codec();

    private final GameMap gameMap;

    public GameMapBiomeSource(GameMap gameMap) {
        this.gameMap = gameMap;
    }
    
    @Nullable
    public GameMap getGameMap() {
        return gameMap;
    }
    
    public Codec<BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        if (gameMap == null) {
        }

        Registry<Biome> biomes = gameMap.getBiomeRegistry();
        return gameMap.getBiomeRegistry().holders().map(b -> biomes.wrapAsHolder(b.value()));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler var4) {
        int blockX = QuartPos.toBlock(x);
        int blockY = QuartPos.toBlock(y);
        int blockZ = QuartPos.toBlock(z);

        return gameMap.getBiome(blockX, blockY, blockZ);
    }
    
}
