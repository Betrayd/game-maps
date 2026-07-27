package net.betrayd.gamemaps.world_interface;

import java.util.Map.Entry;
import net.betrayd.gamemaps.GameChunk;
import net.betrayd.gamemaps.GameMap;
import net.betrayd.gamemaps.GameMapEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Places game maps in the world. Can cause massive hangs with large maps; it's
 * recommended to use {@link SimpleChunkGenerator} to place large maps into empty
 * worlds.
 */
public class GameMapPlacer {

    /**
     * Place a game map in the world as a schematic.
     * @param world World to place into.
     * @param gameMap Game map to place.
     * @param offset Block offset to use.
     */
    public static void placeGameMap(Level world, GameMap gameMap, Vec3i offset) {
        for (var entry : gameMap.getChunks().entrySet()) {
            placeGameChunk(world, entry.getKey(), entry.getValue(), offset);
        }

        for (GameMapEntity ent : gameMap.getEntities()) {
            Vec3 pos = new Vec3(offset.getX(), offset.getY(), offset.getZ());
            ent.createEntities(world, pos, world::addFreshEntity);
        }
    }

    public static void placeGameChunk(Level world, SectionPos chunkPos, GameChunk chunk, Vec3i offset) {
        int xOffset = chunkPos.minBlockX() + offset.getX();
        int yOffset = chunkPos.minBlockY() + offset.getY();
        int zOffset = chunkPos.minBlockZ() + offset.getZ();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = chunk.getBlockState(x, y, z);
                    world.setBlock(new BlockPos(x + xOffset, y + yOffset, z + zOffset), state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
                }
            }
        }

        chunk.getBlockEntities().forEach((pos, nbt) -> {
            int globalX = pos.getX() + xOffset;
            int globalY = pos.getY() + yOffset;
            int globalZ = pos.getZ() + zOffset;
            BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

            BlockState state = chunk.getBlockState(pos);
            BlockEntity ent = BlockEntity.loadStatic(globalPos, state, nbt, world.registryAccess());

            world.setBlockEntity(ent);
        });
    }
}
