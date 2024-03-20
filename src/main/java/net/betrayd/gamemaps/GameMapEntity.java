package net.betrayd.gamemaps;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record GameMapEntity(Vec3d pos, NbtCompound nbt) {

    @Nullable
    public static GameMapEntity fromEntity(Entity entity) {
        return fromEntity(entity.getPos(), entity);
    }

    public static GameMapEntity fromEntity(Vec3d position, Entity entity) {
        var nbt = new NbtCompound();
        if (!entity.saveNbt(nbt)) return null;

        nbt.remove("UUID");;

        return new GameMapEntity(position, nbt);
    }

    public static GameMapEntity fromNbt(NbtCompound nbt) {
        NbtList posList = nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
        Vec3d pos = posList != null ? listToPos(posList) : Vec3d.ZERO;

        return new GameMapEntity(pos, nbt);
    }

    public GameMapEntity withPos(Vec3d pos) {
        return new GameMapEntity(pos, this.nbt);
    }

    /**
     * Turn this MapEntity into a set of real entities (parent + children).
     * 
     * @param world    World to put in.
     * @param consumer Entity consumer.
     */
    public void createEntities(World world, Consumer<Entity> consumer) {
        NbtCompound nbt = createEntityNbt();
        EntityType.loadEntityWithPassengers(nbt, world, ent -> {
            consumer.accept(ent);
            return ent;
        });
    }

    /**
     * Create entity NBT data with its proper position embedded.
     * @return World-loadable entity NBT.
     */
    public NbtCompound createEntityNbt() {
        NbtCompound nbt = this.nbt.copy();

        nbt.put("Pos", posToList(pos));

        nbt.remove("UUID");

        // AbstractDecorationEntity has special position handling with an attachment position.
        if (nbt.contains("TileX", NbtElement.INT_TYPE)) {
            BlockPos blockPos = BlockPos.ofFloored(pos);
            nbt.putInt("TileX", blockPos.getX());
            nbt.putInt("TileY", blockPos.getY());
            nbt.putInt("TileZ", blockPos.getZ());
        }

        return nbt;
    }

    private static NbtList posToList(Vec3d pos) {
        NbtList list = new NbtList();
        list.add(NbtDouble.of(pos.getX()));
        list.add(NbtDouble.of(pos.getY()));
        list.add(NbtDouble.of(pos.getZ()));
        return list;
    }

    private static Vec3d listToPos(NbtList list) {
        return new Vec3d(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }
}
