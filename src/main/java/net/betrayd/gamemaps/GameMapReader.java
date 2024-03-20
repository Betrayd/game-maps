package net.betrayd.gamemaps;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/**
 * Reads an mc world and creates a game map from it.
 */
public class GameMapReader {
    public static GameMap read(World world, BlockPos pos1, BlockPos pos2) {
        BlockPos minPos = min(pos1, pos2);
        BlockPos maxPos = max(pos1, pos2);

        GameMap map = new GameMap(world.getRegistryManager().get(RegistryKeys.BIOME));

        for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
            for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos globalPos = new BlockPos(x, y, z);
                    map.setBlock(globalPos.subtract(pos1), world.getBlockState(globalPos));
                }
            }
        }

        return map;
    }


    private static interface TriIntFunction<T> {
        public T apply(int a, int b, int c);
    }

    private static <T> T min(Vec3i a, Vec3i b, TriIntFunction<T> factory) {
        return factory.apply(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return min(a, b, BlockPos::new);
    }

    private static <T> T max(Vec3i a, Vec3i b, TriIntFunction<T> factory) {
        return factory.apply(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return max(a, b, BlockPos::new);
    }
}
