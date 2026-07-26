package net.betrayd.gamemaps;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

public class GameMapMeta {
    private ResourceKey<DimensionType> dimensionType = BuiltinDimensionTypes.OVERWORLD;


    public ResourceKey<DimensionType> getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(ResourceKey<DimensionType> dimensionType) {
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
    private CompoundTag customData;

    public CompoundTag getCustomData() {
        if (customData == null)
            customData = new CompoundTag();
        return customData;
    }

    public void removeCustomData() {
        customData = null;
    }

    public void setFromWorld(Level world) {
        setDimensionType(world.dimensionTypeId());
        setGameRules(world.getGameRules());
        setDayTime(world.getDayTime());
    }

    public void writeNbt(CompoundTag nbt) {
        nbt.putString("dimensionType", dimensionType.location().toString());
        nbt.put("gameRules", gameRules.createTag());
        nbt.putLong("dayTime", dayTime);

        if (customData != null && !customData.isEmpty()) {
            nbt.put("custom", customData);
        }

    }

    public void readNbt(CompoundTag nbt) {
        if (nbt.contains("dimensionType", Tag.TAG_STRING)) {
            ResourceLocation dimensionId = new ResourceLocation(nbt.getString("dimensionType"));
            dimensionType = ResourceKey.create(Registries.DIMENSION_TYPE, dimensionId);
        }

        if (nbt.contains("gameRules", Tag.TAG_COMPOUND)) {
            gameRules = new GameRules(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("gameRules")));
        }

        if (nbt.contains("dayTime", Tag.TAG_LONG)) {
            dayTime = nbt.getLong("dayTime");
        }

        if (nbt.contains("custom", Tag.TAG_COMPOUND)) {
            customData = nbt.getCompound("custom");
        }

    }
}
