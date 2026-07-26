package net.betrayd.gamemaps.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

@Mixin(VoidChunkGenerator.class)
public interface AccessorVoidChunkGenerator {

    @Accessor("biome")
    Holder<Biome> gamemaps$getBiome();
}
