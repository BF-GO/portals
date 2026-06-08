package net.thenextlvl.portals.view;

import net.thenextlvl.binder.StaticBinder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;

/**
 * Configuration for the portal plugin.
 *
 * @since 0.2.0
 */
@ApiStatus.NonExtendable
public interface PortalConfig {
    /**
     * Gets the portal configuration.
     *
     * @return the portal configuration
     * @since 0.2.0
     */
    static @CheckReturnValue PortalConfig config() {
        return StaticBinder.getInstance(PortalConfig.class.getClassLoader()).find(PortalConfig.class);
    }

    /**
     * Whether to allow random teleports to caves.
     *
     * @return {@code true} if cave and spawns are allowed, {@code false} otherwise
     * @since 0.2.0
     */
    boolean allowCaveSpawns();

    /**
     * Whether to use economy for entry costs.
     *
     * @return {@code true} if economy is used for entry costs, {@code false} otherwise
     * @since 0.2.0
     */
    boolean entryCosts();

    /**
     * Whether to ignore entity movement.
     *
     * @return {@code true} if entity movement is ignored, {@code false} otherwise
     * @since 0.2.0
     */
    boolean ignoreEntityMovement();

    /**
     * Whether to show a message when the warmup starts.
     *
     * @return {@code true} if a message is shown, {@code false} otherwise
     * @since 1.2.0
     */
    boolean warmupMessage();

    /**
     * Speed at which entities are pushed back.
     * <p>
     * A value of 0 and below means this feature is disabled.
     *
     * @return the pushback speed
     * @since 0.2.0
     */
    double pushbackSpeed();

    /**
     * Whether portal particles are globally enabled.
     *
     * @return {@code true} if portal particles are enabled, {@code false} otherwise
     * @since 1.8.0
     */
    default boolean particlesEnabled() {
        return true;
    }

    /**
     * The minimum interval in ticks between particle renders.
     *
     * @return the minimum particle interval in ticks
     * @since 1.8.0
     */
    default int minParticleIntervalTicks() {
        return 5;
    }

    /**
     * The maximum amount of particles rendered per portal operation.
     *
     * @return the maximum particle count
     * @since 1.8.0
     */
    default int maxParticleCount() {
        return 64;
    }

    /**
     * The default particle receiver distance in blocks.
     *
     * @return the default particle view distance
     * @since 1.8.0
     */
    default int defaultParticleViewDistance() {
        return 32;
    }

    /**
     * Whether newly created particle effects should force particles through client particle settings.
     *
     * @return {@code true} if new particle effects should force particles, {@code false} otherwise
     * @since 1.8.0
     */
    default boolean defaultParticleForce() {
        return true;
    }

    /**
     * The maximum particle receiver distance in blocks.
     *
     * @return the maximum particle view distance
     * @since 1.8.0
     */
    default int maxParticleViewDistance() {
        return 64;
    }
}
