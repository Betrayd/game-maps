package net.betrayd.gamemaps.world_interface;

import java.util.stream.Stream;

import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.betrayd.gamemaps.GameMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.FixedBiomeSource;
import org.jspecify.annotations.NonNull;

/**
 * A <code>BiomeSource</code> based on the biomes from a game map, mainly for
 * use in {@link GameMapChunkGenerator}.
 */
public class GameMapBiomeSource extends BiomeSource {

    /**
     * We never need to deserialize this biome source, so a bullshit codec provides
     * a fixed biome source with a random biome upon deserialization.
     */
    public static final MapCodec<BiomeSource> CODEC = (Biome.CODEC.fieldOf("biome"))
            .<BiomeSource>xmap(FixedBiomeSource::new, biomeSource ->
                    biomeSource.possibleBiomes().iterator().next()).stable();

    private final GameMap gameMap;

    public GameMapBiomeSource(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    @Nullable
    public GameMap getGameMap() {
        return gameMap;
    }

    @Override
    protected @NonNull MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NonNull Stream<Holder<Biome>> collectPossibleBiomes() {
        Registry<Biome> biomes = gameMap.getRegistryAccess().lookupOrThrow(Registries.BIOME);
        return biomes.entrySet().stream()
                .map(e -> biomes.wrapAsHolder(e.getValue()));
    }

    @Override
    public @NonNull Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler var4) {
        int blockX = QuartPos.toBlock(x);
        int blockY = QuartPos.toBlock(y);
        int blockZ = QuartPos.toBlock(z);

        return gameMap.getBiome(blockX, blockY, blockZ);
    }

}
