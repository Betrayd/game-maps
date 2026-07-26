package net.betrayd.gamemaps;

import java.util.function.Consumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

public record GameMapEntity(Vec3 pos, CompoundTag nbt) {

    @Nullable
    public static GameMapEntity fromEntity(Entity entity) {
        return fromEntity(entity.position(), entity);
    }

    public static GameMapEntity fromEntity(Vec3 position, Entity entity) {
        var nbt = new CompoundTag();
        if (!entity.save(nbt)) return null;

        nbt.remove("UUID");;

        return new GameMapEntity(position, nbt);
    }

    public static GameMapEntity fromNbt(CompoundTag nbt) {
        Vec3 pos = nbt.getList("Pos").map(GameMapEntity::listToPos).orElse(Vec3.ZERO);

        return new GameMapEntity(pos, nbt);
    }

    public GameMapEntity withPos(Vec3 pos) {
        return new GameMapEntity(pos, this.nbt);
    }

    /**
     * Turn this MapEntity into a set of real entities (parent + children).
     * 
     * @param world    World to put in.
     * @param consumer Entity consumer.
     */
    public void createEntities(Level world, Consumer<Entity> consumer) {
        CompoundTag nbt = createEntityNbt();
        EntityType.loadEntityRecursive(nbt, world, ent -> {
            consumer.accept(ent);
            return ent;
        });
    }

    /**
     * Create entity NBT data with its proper position embedded.
     * @return World-loadable entity NBT.
     */
    public CompoundTag createEntityNbt() {
        CompoundTag nbt = this.nbt.copy();

        nbt.put("Pos", posToList(pos));

        nbt.remove("UUID");

        // AbstractDecorationEntity has special position handling with an attachment position.
        if (nbt.contains("TileX")) {
            BlockPos blockPos = BlockPos.containing(pos);
            nbt.putInt("TileX", blockPos.getX());
            nbt.putInt("TileY", blockPos.getY());
            nbt.putInt("TileZ", blockPos.getZ());
        }

        return nbt;
    }

    public Identifier getId() {
        return Identifier.parse(nbt.getStringOr("id", ""));
    }

    public void setId(Identifier id) {
        nbt.putString("id", id.toString());
    }

    public UVPair getRotation() {
        ListTag rotation = nbt.getListOrEmpty("Rotation");
        return new UVPair(rotation.getFloatOr(0, 0), rotation.getFloatOr(1, 0));
    }
    
    public void setRotation(Vector2fc rot) {
        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(rot.x()));
        rotation.add(FloatTag.valueOf(rot.y()));

        nbt.put("Rotation", rotation);
    }

    public Component getCustomName() {
        String json = nbt.getStringOr("CustomName", "");
        if (json.isEmpty()) return Component.empty();

        try {
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                    .result().orElseThrow();
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to parse entity custom name {}", json, e);
            return Component.empty();
        }
    }

    public void setCustomName(@Nullable Component name) {
        if (name == null) {
            nbt.remove("CustomName");
        } else {
            nbt.putString("CustomName", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, name)
                    .result().orElseThrow().toString());
        }
    }

    private static ListTag posToList(Vec3 pos) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(pos.x()));
        list.add(DoubleTag.valueOf(pos.y()));
        list.add(DoubleTag.valueOf(pos.z()));
        return list;
    }

    private static Vec3 listToPos(ListTag list) {
        return new Vec3(list.getDoubleOr(0, 0), list.getDoubleOr(1, 0), list.getDoubleOr(2, 0));
    }
}
