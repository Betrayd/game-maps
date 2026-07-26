package net.betrayd.gamemaps;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
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

    private GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);

    public GameRules getGameRules() {
        return gameRules;
    }

    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules.copy(FeatureFlags.DEFAULT_FLAGS);
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
        world.dimensionTypeRegistration().unwrapKey().ifPresent(this::setDimensionType);
        if (world instanceof ServerLevel serverLevel) {
            setGameRules(serverLevel.getGameRules());
        }
        setDayTime(world.getDefaultClockTime());
    }

    public void writeNbt(CompoundTag nbt) {
        nbt.putString("dimensionType", dimensionType.identifier().toString());
        GameRules.codec(FeatureFlags.DEFAULT_FLAGS).encodeStart(NbtOps.INSTANCE, gameRules)
                .result().ifPresent(tag -> nbt.put("gameRules", tag));
        nbt.putLong("dayTime", dayTime);

        if (customData != null && !customData.isEmpty()) {
            nbt.put("custom", customData);
        }

    }

    public void readNbt(CompoundTag nbt) {
        nbt.getString("dimensionType").ifPresent(id -> {
            dimensionType = ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.parse(id));
        });

        nbt.getCompound("gameRules").ifPresent(gameRulesTag -> {
            GameRules.codec(FeatureFlags.DEFAULT_FLAGS)
                    .parse(new Dynamic<>(NbtOps.INSTANCE, gameRulesTag))
                    .result().ifPresent(gr -> gameRules = gr);
        });

        dayTime = nbt.getLongOr("dayTime", dayTime);

        nbt.getCompound("custom").ifPresent(custom -> customData = custom);
    }
}
