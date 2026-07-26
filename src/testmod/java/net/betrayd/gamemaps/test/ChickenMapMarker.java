package net.betrayd.gamemaps.test;

import net.betrayd.gamemaps.map_markers.MapMarker;
import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

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
    protected void readCustomNbt(CompoundTag nbt) {
        if (nbt.contains("isNamed", Tag.TAG_BYTE)) {
            isNamed = nbt.getBoolean("isNamed");
        }
    }

    @Override
    protected void writeCustomNbt(CompoundTag nbt) {
        nbt.putBoolean("isNamed", isNamed);
    }
    
}
