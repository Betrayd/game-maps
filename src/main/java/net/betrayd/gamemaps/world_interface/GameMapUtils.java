package net.betrayd.gamemaps.world_interface;

import java.util.Collection;
import java.util.function.Predicate;

import net.betrayd.gamemaps.GameMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;

/**
 * A set of utility functions regarding game maps.
 */
public class GameMapUtils {

    public static interface Obj2IntFunction<T> {
        public int apply(T obj);
    }

    /**
     * Calculate the maximum int value in a collection.
     * 
     * @param <T>        Collection type.
     * @param collection Subject collection.
     * @param extractor  Function to extract ints from collection entries.
     * @return The maximum value.
     */
    public static <T> int calcMaxValue(Collection<T> collection, Obj2IntFunction<T> extractor) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Collection may not be empty!");
        }
        int max = Integer.MIN_VALUE;

        for (T val : collection) {
            int i = extractor.apply(val);
            if (i > max) max = i;
        }

        return max;
    }

    /**
     * Calculate the minimum int value in a collection.
     * 
     * @param <T>        Collection type.
     * @param collection Subject collection.
     * @param extractor  Function to extract ints from collection entries.
     * @return The minimum value.
     */
    public static <T> int calcMinValue(Collection<T> collection, Obj2IntFunction<T> extractor) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Collection may not be empty!");
        }
        int min = Integer.MAX_VALUE;

        for (T val : collection) {
            int i = extractor.apply(val);
            if (i < min) min = i;
        }

        return min;
    }

    /**
     * Get the minimum X value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Minimum X value, in block coordinates.
     */
    public static int calcMinX(Collection<ChunkSectionPos> chunks) {
        return calcMinValue(chunks, ChunkSectionPos::getMinX);
    }

    /**
     * Get the maximum X value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Maximum X value, in block coordinates.
     */
    public static int calcMaxX(Collection<ChunkSectionPos> chunks) {
        return calcMaxValue(chunks, ChunkSectionPos::getMaxX);
    }

    /**
     * Get the minimum Y value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Minimum Y value, in block coordinates.
     */
    public static int calcMinY(Collection<ChunkSectionPos> chunks) {
        return calcMinValue(chunks, ChunkSectionPos::getMinY);
    }

    /**
     * Get the maximum Y value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Maximum Y value, in block coordinates.
     */
    public static int calcMaxY(Collection<ChunkSectionPos> chunks) {
        return calcMaxValue(chunks, ChunkSectionPos::getMaxY);
    }

    /**
     * Get the minimum Z value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Minimum Z value, in block coordinates.
     */
    public static int calcMinZ(Collection<ChunkSectionPos> chunks) {
        return calcMinValue(chunks, ChunkSectionPos::getMaxZ);
    }

    /**
     * Get the maximum Z value covered by a section of chunks.
     * 
     * @param chunks Chunks to query.
     * @return Maximum Z value, in block coordinates.
     */
    public static int calcMaxZ(Collection<ChunkSectionPos> chunks) {
        return calcMaxValue(chunks, ChunkSectionPos::getMaxZ);
    }

    /**
     * Calculate the world height of a game map.
     * 
     * @param chunks All chunks in game map.
     * @return World height, in blocks.
     */
    public static int getWorldHeight(Collection<ChunkSectionPos> chunks) {
        return calcMaxY(chunks) - calcMinY(chunks);
    }

    /**
     * Determine the top Y value of a particular column in a game map, based on a
     * heightmap type.
     * 
     * @param x         X coordinate.
     * @param z         Z coordinate.
     * @param gameMap   Game map to query.
     * @param minY      Minimum Y value to query.
     * @param maxY      Maximum Y value to query.
     * @param heightmap Heightmap type to use.
     * @return Top Y
     */
    public static int getTopY(int x, int z, GameMap gameMap, int minY, int maxY, Heightmap.Type heightmap) {
        Predicate<BlockState> predicate = heightmap.getBlockPredicate();

        for (int y = maxY; y >= minY; y--) {
            BlockState state = gameMap.getBlock(x, y, z);
            if (predicate.test(state))
                return y;
        }

        return 0;
    }
}
