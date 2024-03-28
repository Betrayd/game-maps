package net.betrayd.gamemaps;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

import com.mojang.logging.LogUtils;

import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serialization;
import net.minecraft.util.Identifier;
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

    public Identifier getId() {
        return new Identifier(nbt.getString("id"));
    }

    public void setId(Identifier id) {
        nbt.putString("id", id.toString());
    }

    public Vector2f getRotation() {
        NbtList rotation = nbt.getList("Rotation", NbtElement.FLOAT_TYPE);
        if (rotation == null)
            return new Vector2f(0, 0);
        
        return new Vector2f(rotation.getFloat(0), rotation.getFloat(1));
    }
    
    public void setRotation(Vector2fc rot) {
        NbtList rotation = new NbtList();
        rotation.add(NbtFloat.of(rot.x()));
        rotation.add(NbtFloat.of(rot.y()));

        nbt.put("Rotation", rotation);
    }

    public Text getCustomName() {
        String json = nbt.getString("CustomName");
        if (json == null || json.isEmpty()) return Text.empty();

        try {
            return Serialization.fromJson(json);
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to parse entity custom name {}", json, e);
            return Text.empty();
        }
    }

    public void setCustomName(@Nullable Text name) {
        if (name == null) {
            nbt.remove("CustomName");
        } else {
            nbt.putString("CustomName", Serialization.toJsonString(name));
        }
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
