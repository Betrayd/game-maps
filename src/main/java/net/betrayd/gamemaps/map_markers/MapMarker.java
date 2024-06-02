package net.betrayd.gamemaps.map_markers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;

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

    public final Vec3d getPos() {
        return new Vec3d(x, y, z);
    }

    public final void setPos(Vec3d pos) {
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
        setPos(entity.getPos());
        setYaw(entity.getYaw());
        setPitch(entity.getPitch());
    }

    protected abstract void readCustomNbt(NbtCompound nbt);
    protected abstract void writeCustomNbt(NbtCompound nbt);

    public final void readNbt(NbtCompound nbt) {
        try {
            NbtList pos = (NbtList) nbt.get("Pos");
            if (pos != null && pos.size() >= 3) {
                setX(doubleFromList(pos, 0));
                setY(doubleFromList(pos, 1));
                setZ(doubleFromList(pos, 2));
            }

        } catch (ClassCastException e) {
            LOGGER.error("Pos element was of the wrong type", e);
        }

        try {
            NbtList rot = (NbtList) nbt.get("Rot");
            if (rot != null && rot.size() >= 2) {
                setYaw(floatFromList(rot, 0));
                setPitch(floatFromList(rot, 1));
            }

        } catch (ClassCastException e) {
            LOGGER.error("Rot element was of the wrong type", e);
        }

        readCustomNbt(nbt);
    }

    public final NbtCompound writeNbt(NbtCompound nbt) {
        writeCustomNbt(nbt);

        NbtList pos = new NbtList();
        pos.add(NbtDouble.of(x));
        pos.add(NbtDouble.of(y));
        pos.add(NbtDouble.of(z));
        nbt.put("Pos", pos);

        NbtList rot = new NbtList();
        rot.add(NbtFloat.of(yaw));
        rot.add(NbtFloat.of(pitch));
        nbt.put("Rot", rot);

        nbt.putString("id", getType().getId().toString());

        return nbt;
    }

    private static double doubleFromList(NbtList list, int index) {
        return ((AbstractNbtNumber) list.get(index)).doubleValue();
    }

    private static float floatFromList(NbtList list, int index) {
        return ((AbstractNbtNumber) list.get(index)).floatValue();
    }
}
