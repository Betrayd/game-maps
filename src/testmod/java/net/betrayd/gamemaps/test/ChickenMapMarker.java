package net.betrayd.gamemaps.test;

import net.betrayd.gamemaps.map_markers.MapMarker;
import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class ChickenMapMarker extends MapMarker {

    public ChickenMapMarker(MapMarkerType<?> type) {
        super(type);
    }

    private boolean isNamed;

    public boolean isNamed() {
        return isNamed;
    }

    public void setNamed(boolean isNamed) {
        this.isNamed = isNamed;
    }

    @Override
    protected void readCustomNbt(NbtCompound nbt) {
        if (nbt.contains("isNamed", NbtElement.BYTE_TYPE)) {
            isNamed = nbt.getBoolean("isNamed");
        }
    }

    @Override
    protected void writeCustomNbt(NbtCompound nbt) {
        nbt.putBoolean("isNamed", isNamed);
    }
    
}
