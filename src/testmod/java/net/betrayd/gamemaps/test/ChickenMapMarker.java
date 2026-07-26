package net.betrayd.gamemaps.test;

import net.betrayd.gamemaps.map_markers.MapMarker;
import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.nbt.CompoundTag;

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
        isNamed = nbt.getBooleanOr("isNamed", isNamed);
    }

    @Override
    protected void writeCustomNbt(CompoundTag nbt) {
        nbt.putBoolean("isNamed", isNamed);
    }
    
}
