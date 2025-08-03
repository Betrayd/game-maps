package net.betrayd.gamemaps;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;

public class GameMapMeta {
    private RegistryKey<DimensionType> dimensionType = DimensionTypes.OVERWORLD;


    public RegistryKey<DimensionType> getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(RegistryKey<DimensionType> dimensionType) {
        this.dimensionType = dimensionType;
    }

    private GameRules gameRules = new GameRules();

    public GameRules getGameRules() {
        return gameRules;
    }

    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules.copy();
    }

    private long dayTime;

    public long getDayTime() {
        return dayTime;
    }

    public void setDayTime(long dayTime) {
        this.dayTime = dayTime;
    }

    @Nullable
    private NbtCompound customData;

    public NbtCompound getCustomData() {
        if (customData == null)
            customData = new NbtCompound();
        return customData;
    }

    public void removeCustomData() {
        customData = null;
    }

    public void setFromWorld(World world) {
        setDimensionType(world.getDimensionKey());
        setGameRules(world.getGameRules());
        setDayTime(world.getTimeOfDay());
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putString("dimensionType", dimensionType.getValue().toString());
        nbt.put("gameRules", gameRules.toNbt());
        nbt.putLong("dayTime", dayTime);

        if (customData != null && !customData.isEmpty()) {
            nbt.put("custom", customData);
        }

    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("dimensionType", NbtElement.STRING_TYPE)) {
            Identifier dimensionId = new Identifier(nbt.getString("dimensionType"));
            dimensionType = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, dimensionId);
        }

        if (nbt.contains("gameRules", NbtElement.COMPOUND_TYPE)) {
            gameRules = new GameRules(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("gameRules")));
        }

        if (nbt.contains("dayTime", NbtElement.LONG_TYPE)) {
            dayTime = nbt.getLong("dayTime");
        }

        if (nbt.contains("custom", NbtElement.COMPOUND_TYPE)) {
            customData = nbt.getCompound("custom");
        }

    }
}
