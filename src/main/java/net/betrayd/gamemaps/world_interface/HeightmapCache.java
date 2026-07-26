package net.betrayd.gamemaps.world_interface;

import java.util.EnumMap;
import java.util.function.IntSupplier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;;

class HeightmapCache {
    private final EnumMap<Types, Long2IntMap> cache = new EnumMap<>(Types.class);

    public int getOrDefault(int x, int z, Types type, int fallback) {
        Long2IntMap cache = this.cache.get(type);
        if (cache == null) return fallback;

        long key = getKey(x, z);
        return cache.getOrDefault(key, fallback);
    }

    public int computeIfAbsent(int x, int z, Types type, IntSupplier factory) {
        Long2IntMap cache = this.cache.computeIfAbsent(type, t -> new Long2IntArrayMap());
        long key = getKey(x, z);
        
        return cache.computeIfAbsent(key, k -> factory.getAsInt());
    }

    private static long getKey(int x, int z) {
        return ChunkPos.asLong(x, z); // This isn't technically a chunk pos, but I don't care
    }
}
