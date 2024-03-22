package net.betrayd.gamemaps.world_interface;

import java.util.EnumMap;
import java.util.function.IntSupplier;

import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap.Type;;

class HeightmapCache {
    private final EnumMap<Type, Long2IntMap> cache = new EnumMap<>(Type.class);

    public int getOrDefault(int x, int z, Type type, int fallback) {
        Long2IntMap cache = this.cache.get(type);
        if (cache == null) return fallback;

        long key = getKey(x, z);
        return cache.getOrDefault(key, fallback);
    }

    public int computeIfAbsent(int x, int z, Type type, IntSupplier factory) {
        Long2IntMap cache = this.cache.computeIfAbsent(type, t -> new Long2IntArrayMap());
        long key = getKey(x, z);
        
        return cache.computeIfAbsent(key, k -> factory.getAsInt());
    }

    private static long getKey(int x, int z) {
        return ChunkPos.toLong(x, z); // This isn't technically a chunk pos, but I don't care
    }
}
