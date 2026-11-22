package net.thenextlvl.portals.plugin.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PortalParticleDataTest {
    private final World world = world("original");
    private final World otherWorld = world("destination");
    private final Location from = new Location(world, 10, 64, -20);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trailKeepsItsOffsetWhenPortalMoves(final boolean changeWorld) {
        final var to = new Location(changeWorld ? otherWorld : world, -200, 90, 300);
        final var target = from.clone().add(2.25, -1.5, 4.75);
        final var data = new Particle.Trail(target, Color.AQUA, 37);

        final var relocated = assertInstanceOf(Particle.Trail.class, PortalParticleData.relocate(data, from, to));

        assertLocation(to.clone().add(2.25, -1.5, 4.75), relocated.getTarget());
        assertEquals(Color.AQUA, relocated.getColor());
        assertEquals(37, relocated.getDuration());
        assertLocation(new Location(world, 12.25, 62.5, -15.25), data.getTarget());
        assertLocation(new Location(world, 10, 64, -20), from);
        assertLocation(new Location(changeWorld ? otherWorld : world, -200, 90, 300), to);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void vibrationKeepsItsOffsetWhenPortalMoves(final boolean changeWorld) {
        final var to = new Location(changeWorld ? otherWorld : world, -200, 90, 300);
        final var target = from.clone().add(-3, 2, 5);
        final var data = new Vibration(new Vibration.Destination.BlockDestination(target), 43);

        final var relocated = assertInstanceOf(Vibration.class, PortalParticleData.relocate(data, from, to));
        final var destination = assertInstanceOf(Vibration.Destination.BlockDestination.class, relocated.getDestination());

        assertLocation(to.clone().add(-3, 2, 5), destination.getLocation());
        assertEquals(43, relocated.getArrivalTime());
        assertLocation(new Location(world, 7, 66, -15),
                assertInstanceOf(Vibration.Destination.BlockDestination.class, data.getDestination()).getLocation());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRAIL", "VIBRATION"})
    void defaultTargetFollowsPortalCenter(final String particle) {
        final var data = PortalParticleData.defaultData(Particle.valueOf(particle), from);
        final var to = new Location(world, 13.5, 67.5, -19.5);

        final var relocated = PortalParticleData.relocate(data, from, to);
        final var target = relocated instanceof Particle.Trail trail ? trail.getTarget()
                : assertInstanceOf(Vibration.Destination.BlockDestination.class,
                        assertInstanceOf(Vibration.class, relocated).getDestination()).getLocation();

        assertLocation(to, target);
    }

    @Test
    void repeatedMovesPreserveTargetOffset() {
        final var data = new Particle.Trail(from.clone().add(1, -2, 3), Color.RED, 20);
        final var intermediate = new Location(otherWorld, 1000, 100, -500);
        final var to = new Location(world, -50, 70, 90);

        final var moved = PortalParticleData.relocate(data, from, intermediate);
        final var relocated = assertInstanceOf(Particle.Trail.class, PortalParticleData.relocate(moved, intermediate, to));

        assertLocation(to.clone().add(1, -2, 3), relocated.getTarget());
    }

    @Test
    void dataWithoutFixedTargetsIsUnchanged() {
        final var to = new Location(otherWorld, 0, 0, 0);
        final var dust = new Particle.DustOptions(Color.RED, 1);

        assertNull(PortalParticleData.relocate(null, from, to));
        assertSame(dust, PortalParticleData.relocate(dust, from, to));
        assertSame(Color.RED, PortalParticleData.relocate(Color.RED, from, to));
    }

    @Test
    void entityVibrationKeepsFollowingItsEntity() {
        final var entity = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
        final var data = new Vibration(new Vibration.Destination.EntityDestination(entity), 20);

        assertSame(data, PortalParticleData.relocate(data, from, new Location(otherWorld, 0, 0, 0)));
    }

    private static void assertLocation(final Location expected, final Location actual) {
        assertSame(expected.getWorld(), actual.getWorld());
        assertEquals(expected.getX(), actual.getX(), 1e-9);
        assertEquals(expected.getY(), actual.getY(), 1e-9);
        assertEquals(expected.getZ(), actual.getZ(), 1e-9);
    }

    private static World world(final String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
