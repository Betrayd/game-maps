package net.betrayd.gamemaps.world_interface;

import org.jetbrains.annotations.Nullable;

import net.betrayd.gamemaps.EntityFilter;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Reads an mc world and creates a game map from it. This capture implementation
 * reads the world block-by-block. While this allows game maps of any size and
 * offset to be captured, it's much slower than
 * {@link WorldAlignedMapCapture}.
 */
public class GameMapCapture {
    public static GameMap read(Level world, BlockPos pos1, BlockPos pos2) {
        return read(world, pos1, pos2, null);
    }
    
    public static GameMap read(Level world, BlockPos pos1, BlockPos pos2, @Nullable EntityFilter entityFilter) {
        BlockPos minPos = min(pos1, pos2);
        BlockPos maxPos = max(pos1, pos2);

        GameMap map = new GameMap(world.registryAccess().registryOrThrow(Registries.BIOME));

        for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
            for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos globalPos = new BlockPos(x, y, z);
                    map.setBlock(globalPos.subtract(pos1), world.getBlockState(globalPos));
                    map.setBiome(globalPos.subtract(pos1), world.getBiome(globalPos));

                    BlockEntity blockEntity = world.getBlockEntity(globalPos);
                    if (blockEntity != null) {
                        map.putBlockEntity(globalPos.subtract(pos1), blockEntity);
                    }
                }
            }
        }

        if (world instanceof ServerLevel serverWorld) {
            for (Entity ent : serverWorld.getAllEntities()) {
                if (boxContains(minPos, maxPos, ent.blockPosition())) {
                    if (entityFilter != null)
                        ent = entityFilter.apply(ent, map);
                    
                    if (ent == null)
                        continue;

                    map.addEntity(GameMapEntity.fromEntity(ent.position().subtract(Vec3.atLowerCornerOf(pos1)), ent));
                }
            }
        }

        map.getMeta().setFromWorld(world);

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

    private static boolean boxContains(BlockPos minPos, BlockPos maxPos, BlockPos pos) {
        return minPos.getX() <= pos.getX() && pos.getX() <= maxPos.getX()
            && minPos.getY() <= pos.getY() && pos.getY() <= maxPos.getY()
            && minPos.getZ() <= pos.getZ() && pos.getZ() <= maxPos.getZ();
    }
}
