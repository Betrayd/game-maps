package net.betrayd.gamemaps;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/**
 * Places game maps in the world.
 */
public class GameMapPlacer {

    /**
     * Place a game map in the world as a schematic.
     * @param world World to place into.
     * @param gameMap Game map to place.
     * @param offset Block offset to use.
     */
    public static void placeGameMap(World world, GameMap gameMap, Vec3i offset) {
        for (var entry : gameMap.getChunks().entrySet()) {
            placeGameChunk(world, entry.getKey(), entry.getValue(), offset);
        }
    }

    public static void placeGameChunk(World world, ChunkSectionPos chunkPos, GameChunk chunk, Vec3i offset) {
        int xOffset = chunkPos.getMinX() + offset.getX();
        int yOffset = chunkPos.getMinY() + offset.getY();
        int zOffset = chunkPos.getMinZ() + offset.getZ();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = chunk.getBlockState(x, y, z);
                    world.setBlockState(new BlockPos(x + xOffset, y + yOffset, z + zOffset), state, Block.FORCE_STATE);
                }
            }
        }
    }
}
