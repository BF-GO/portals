package net.thenextlvl.portals.particle;

import org.bukkit.Particle;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Defines the particle effect rendered by a portal.
 *
 * @since 1.8.0
 */
public record PortalParticleEffect(
        Particle particle,
        PortalParticlePattern pattern,
        int count,
        int intervalTicks,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        int viewDistance,
        boolean force,
        @Nullable Object data
) {
    public PortalParticleEffect {
        Objects.requireNonNull(particle, "particle");
        Objects.requireNonNull(pattern, "pattern");
        if (count <= 0) throw new IllegalArgumentException("Count must be positive");
        if (intervalTicks <= 0) throw new IllegalArgumentException("Interval ticks must be positive");
        if (viewDistance < 0) throw new IllegalArgumentException("View distance cannot be negative");
    }

    /**
     * Creates a new particle effect without particle data.
     *
     * @param particle      the particle type
     * @param pattern       the particle pattern
     * @param count         the amount of particles per render
     * @param intervalTicks the render interval in ticks
     * @param viewDistance  the receiver distance in blocks
     * @param force         whether to force particles through client settings
     * @return a particle effect
     * @since 1.8.0
     */
    @Contract("_, _, _, _, _, _ -> new")
    public static PortalParticleEffect of(
            final Particle particle,
            final PortalParticlePattern pattern,
            final int count,
            final int intervalTicks,
            final int viewDistance,
            final boolean force
    ) {
        return new PortalParticleEffect(particle, pattern, count, intervalTicks, 0, 0, 0, 1, viewDistance, force, null);
    }

    /**
     * Creates a copy of this effect with new particle data.
     *
     * @param data the particle data
     * @return a particle effect
     * @since 1.8.0
     */
    @Contract("_ -> new")
    public PortalParticleEffect withData(final @Nullable Object data) {
        return new PortalParticleEffect(particle, pattern, count, intervalTicks, offsetX, offsetY, offsetZ, extra, viewDistance, force, data);
    }
}
