package net.betrayd.gamemaps;


import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;

/**
 * A filter that can be applied to entities for processing during export.
 */
public interface EntityFilter {

    /**
     * Apply this filter to an entity.
     * 
     * @param entity    The subject entity.
     * @param customNbt The game map being written
     * @return The filtered entity. <code>null</code> if the entity should be
     *         removed.
     */
    @Nullable
    public Entity apply(Entity entity, GameMap customNbt);
}
