package net.betrayd.gamemaps.test;

import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.util.Identifier;

public class TestMapMarkers {
    public static final MapMarkerType<ChickenMapMarker> CHICKEN = MapMarkerType
            .register(new Identifier("betrayd:chicken"), new MapMarkerType<>(ChickenMapMarker::new));
}
