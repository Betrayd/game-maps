package net.betrayd.gamemaps.map_markers;

import java.util.function.Function;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * A type declaration of map markers
 */
public final class MapMarkerType<T extends MapMarker> {
    private final Function<MapMarkerType<T>, T> factory;

    public MapMarkerType(Function<MapMarkerType<T>, T> factory) {
        this.factory = factory;
    }

    public T create() {
        return factory.apply(this);
    }

    public Identifier getId() {
        return REGISTRY.inverse().get(this);
    }

    public static final BiMap<Identifier, MapMarkerType<?>> REGISTRY = HashBiMap.create();

    /**
     * Register a map marker.
     * 
     * @param <T>  Map marker class type.
     * @param id   ID to use.
     * @param type Map marker type instance.
     * @return <code>type</code>
     */
    public static <T extends MapMarker> MapMarkerType<T> register(Identifier id, MapMarkerType<T> type) {
        REGISTRY.put(id, type);
        return type;
    }

    /**
     * Deserialize a map marker from NBT.
     * 
     * @param nbt NBT to read.
     * @return Parsed map marker. <code>null</code> if the map marker type was not
     *         found.
     */
    public static MapMarker deserialize(NbtCompound nbt) {
        Identifier id = Identifier.tryParse(nbt.getString("id"));
        if (id == null)
            return null;
        
        MapMarkerType<?> type = REGISTRY.get(id);
        if (type == null)
            return null;
        
        MapMarker marker = type.create();
        marker.readNbt(nbt);
        return marker;
    }
}
