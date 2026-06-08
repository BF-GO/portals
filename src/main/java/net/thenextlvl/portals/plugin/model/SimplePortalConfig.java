package net.thenextlvl.portals.plugin.model;

import net.thenextlvl.portals.view.PortalConfig;

public final class SimplePortalConfig implements PortalConfig {
    private final boolean allowCaveSpawns;
    private final boolean entryCosts;
    private final boolean ignoreEntityMovement;
    private final boolean warmupMessage;
    private final double pushbackSpeed;

    private final Boolean particlesEnabled;
    private final Integer minParticleIntervalTicks;
    private final Integer maxParticleCount;
    private final Integer defaultParticleViewDistance;
    private final Boolean defaultParticleForce;
    private final Integer maxParticleViewDistance;

    public SimplePortalConfig(
            final boolean allowCaveSpawns,
            final boolean entryCosts,
            final boolean ignoreEntityMovement,
            final boolean warmupMessage,
            final double pushbackSpeed,
            final boolean particlesEnabled,
            final int minParticleIntervalTicks,
            final int maxParticleCount,
            final int defaultParticleViewDistance,
            final boolean defaultParticleForce,
            final int maxParticleViewDistance
    ) {
        this.allowCaveSpawns = allowCaveSpawns;
        this.entryCosts = entryCosts;
        this.ignoreEntityMovement = ignoreEntityMovement;
        this.warmupMessage = warmupMessage;
        this.pushbackSpeed = pushbackSpeed;
        this.particlesEnabled = particlesEnabled;
        this.minParticleIntervalTicks = minParticleIntervalTicks;
        this.maxParticleCount = maxParticleCount;
        this.defaultParticleViewDistance = defaultParticleViewDistance;
        this.defaultParticleForce = defaultParticleForce;
        this.maxParticleViewDistance = maxParticleViewDistance;
    }

    @Override
    public boolean allowCaveSpawns() {
        return allowCaveSpawns;
    }

    @Override
    public boolean entryCosts() {
        return entryCosts;
    }

    @Override
    public boolean ignoreEntityMovement() {
        return ignoreEntityMovement;
    }

    @Override
    public boolean warmupMessage() {
        return warmupMessage;
    }

    @Override
    public double pushbackSpeed() {
        return pushbackSpeed;
    }

    @Override
    public boolean particlesEnabled() {
        return particlesEnabled == null || particlesEnabled;
    }

    @Override
    public int minParticleIntervalTicks() {
        return positiveOrDefault(minParticleIntervalTicks, 5);
    }

    @Override
    public int maxParticleCount() {
        return positiveOrDefault(maxParticleCount, 64);
    }

    @Override
    public int defaultParticleViewDistance() {
        return positiveOrDefault(defaultParticleViewDistance, 32);
    }

    @Override
    public boolean defaultParticleForce() {
        return defaultParticleForce == null || defaultParticleForce;
    }

    @Override
    public int maxParticleViewDistance() {
        return Math.max(defaultParticleViewDistance(), positiveOrDefault(maxParticleViewDistance, 64));
    }

    private static int positiveOrDefault(final Integer value, final int fallback) {
        return value != null && value > 0 ? value : fallback;
    }
}
