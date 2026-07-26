package net.betrayd.gamemaps.map_markers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fake "entity" notating a position on the map.
 */
public abstract class MapMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapMarker.class);

    private final MapMarkerType<?> type;

    public MapMarker(MapMarkerType<?> type) {
        this.type = type;
    }

    public final MapMarkerType<?> getType() {
        return type;
    }

    private double x;
    private double y;
    private double z;

    public final double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }
    
    public final double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public final double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public final Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    public final void setPos(Vec3 pos) {
        setX(pos.x);
        setY(pos.y);
        setZ(pos.z);
    }

    private float yaw;
    private float pitch;

    public final float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public final float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public final void setPositionFromEntity(Entity entity) {
        setPos(entity.position());
        setYaw(entity.getYRot());
        setPitch(entity.getXRot());
    }

    protected abstract void readCustomNbt(CompoundTag nbt);
    protected abstract void writeCustomNbt(CompoundTag nbt);

    public final void readNbt(CompoundTag nbt) {
        try {
            ListTag pos = (ListTag) nbt.get("Pos");
            if (pos != null && pos.size() >= 3) {
                setX(doubleFromList(pos, 0));
                setY(doubleFromList(pos, 1));
                setZ(doubleFromList(pos, 2));
            }

        } catch (ClassCastException e) {
            LOGGER.error("Pos element was of the wrong type", e);
        }

        try {
            ListTag rot = (ListTag) nbt.get("Rot");
            if (rot != null && rot.size() >= 2) {
                setYaw(floatFromList(rot, 0));
                setPitch(floatFromList(rot, 1));
            }

        } catch (ClassCastException e) {
            LOGGER.error("Rot element was of the wrong type", e);
        }

        readCustomNbt(nbt);
    }

    public final CompoundTag writeNbt(CompoundTag nbt) {
        writeCustomNbt(nbt);

        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(x));
        pos.add(DoubleTag.valueOf(y));
        pos.add(DoubleTag.valueOf(z));
        nbt.put("Pos", pos);

        ListTag rot = new ListTag();
        rot.add(FloatTag.valueOf(yaw));
        rot.add(FloatTag.valueOf(pitch));
        nbt.put("Rot", rot);

        nbt.putString("id", getType().getId().toString());

        return nbt;
    }

    private static double doubleFromList(ListTag list, int index) {
        return ((NumericTag) list.get(index)).getAsDouble();
    }

    private static float floatFromList(ListTag list, int index) {
        return ((NumericTag) list.get(index)).getAsFloat();
    }
}
