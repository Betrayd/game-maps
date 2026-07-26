package net.betrayd.gamemaps.test;

import net.betrayd.gamemaps.map_markers.MapMarkerType;
import net.minecraft.resources.ResourceLocation;

public class TestMapMarkers {
    public static final MapMarkerType<ChickenMapMarker> CHICKEN = MapMarkerType
            .register(new ResourceLocation("betrayd:chicken"), new MapMarkerType<>(ChickenMapMarker::new));
}
