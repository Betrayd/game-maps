package net.betrayd.gamemaps;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

public class GameMapMeta {
    private RegistryKey<DimensionType> dimensionType = DimensionTypes.OVERWORLD;

    public RegistryKey<DimensionType> getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(RegistryKey<DimensionType> dimensionType) {
        this.dimensionType = dimensionType;
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putString("dimensionType", dimensionType.getValue().toString());
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("dimensionType", NbtElement.STRING_TYPE)) {
            Identifier dimensionId = new Identifier(nbt.getString("dimensionType"));
            dimensionType = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, dimensionId);
        }
    }
}
