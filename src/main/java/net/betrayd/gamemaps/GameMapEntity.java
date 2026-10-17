package net.betrayd.gamemaps;

import java.util.function.Consumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GameMapEntity(CompoundTag nbt) {

    private static final Logger LOGGER = LoggerFactory.getLogger("GameMaps/GameMapEntity");

    public static GameMapEntity fromEntity(Entity entity) {
        try (var reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.save(output);
            var nbt = output.buildResult();
            return new GameMapEntity(nbt);
        }
    }

    public static GameMapEntity fromEntity(Vec3 pos, Entity entity) {
        GameMapEntity ent = fromEntity(entity);
        ent.nbt().put("Pos", posToList(pos));
        return ent;
    }

    public static GameMapEntity fromNbt(CompoundTag nbt) {
        return new GameMapEntity(nbt);
    }


    @SuppressWarnings("OptionalIsPresent") // Makes it impossible to fucking read
    public Vec3 pos() {
        var poseOpt = nbt.getList("Pos");
        return poseOpt.isPresent() ? listToPos(poseOpt.get()) : Vec3.ZERO;
    }

    /**
     * Turn this MapEntity into a set of real entities (parent + children).
     * 
     * @param world    World to put in.
     * @param offset   The offset to use creating the entity
     * @param consumer Entity consumer.
     */
    public void createEntities(Level world, Vec3 offset, Consumer<Entity> consumer) {
        try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
            ValueInput input = TagValueInput.create(reporter, world.registryAccess(), createEntityNbt());
            EntityType.loadEntityRecursive(input, world, EntitySpawnReason.LOAD, ent -> {
                ent.setPos(ent.position().add(offset));
                consumer.accept(ent);
                return ent;
            });
        }
    }

    /**
     * Turn this MapEntity into a set of real entities (parent + children).
     *
     * @param world    World to put in.
     * @param consumer Entity consumer.
     */
    public void createEntities(Level world, Consumer<Entity> consumer) {
        createEntities(world, Vec3.ZERO, consumer);
    }

    /**
     * Create entity NBT data with its proper position embedded.
     * @return World-loadable entity NBT.
     */
    public CompoundTag createEntityNbt() {
        return this.nbt.copy();
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
