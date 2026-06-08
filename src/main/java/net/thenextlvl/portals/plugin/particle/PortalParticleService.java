package net.thenextlvl.portals.plugin.particle;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.thenextlvl.portals.Portal;
import net.thenextlvl.portals.particle.PortalParticleEffect;
import net.thenextlvl.portals.particle.PortalParticlePattern;
import net.thenextlvl.portals.plugin.PortalsPlugin;
import net.thenextlvl.portals.plugin.portal.PaperPortal;
import net.thenextlvl.portals.shape.BoundingBox;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class PortalParticleService {
    private static final double SURFACE_OFFSET = 0.08;

    private final Map<PaperPortal, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Set<PaperPortal> logged = ConcurrentHashMap.newKeySet();
    private final PortalsPlugin plugin;

    public PortalParticleService(final PortalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh(final Portal portal) {
        if (!(portal instanceof final PaperPortal paperPortal)) return;
        cancel(paperPortal);

        final var effect = paperPortal.getParticleEffect().orElse(null);
        if (effect == null || !plugin.config().particlesEnabled()) return;

        final var center = paperPortal.getBoundingBox().getCenter();
        final var chunkX = center.getBlockX() >> 4;
        final var chunkZ = center.getBlockZ() >> 4;
        final var task = plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, paperPortal.getWorld(), chunkX, chunkZ, ignored -> {
            render(paperPortal);
        }, 1, Math.max(1, effect.intervalTicks()));
        tasks.put(paperPortal, task);
    }

    public void cancel(final Portal portal) {
        if (portal instanceof final PaperPortal paperPortal) cancel(paperPortal);
    }

    public void cancelAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        logged.clear();
    }

    private void cancel(final PaperPortal portal) {
        final var task = tasks.remove(portal);
        if (task != null) task.cancel();
        logged.remove(portal);
    }

    private void render(final PaperPortal portal) {
        final var effect = portal.getParticleEffect().orElse(null);
        if (effect == null || !plugin.config().particlesEnabled()) {
            cancel(portal);
            return;
        }
        if (!PortalParticleData.isValid(effect.particle(), effect.data())) {
            plugin.getComponentLogger().warn("Skipping invalid particle data for portal {}", portal.getName());
            return;
        }

        final var receivers = receivers(portal, effect);
        if (logged.add(portal)) logFirstRender(portal, effect, receivers.size());
        if (receivers.isEmpty()) return;

        final var points = effect.pattern() == PortalParticlePattern.OUTLINE
                ? outlinePoints(portal, effect)
                : fillPoints(portal, effect);
        points.forEach(point -> spawn(point, effect, receivers));
    }

    private List<Player> receivers(final PaperPortal portal, final PortalParticleEffect effect) {
        final var box = portal.getBoundingBox();
        final var center = box.getCenter();
        final var radius = effect.viewDistance();
        final var nearby = portal.getWorld().getNearbyPlayers(center,
                (box.getMaxX() - box.getMinX()) / 2 + radius,
                (box.getMaxY() - box.getMinY()) / 2 + radius,
                (box.getMaxZ() - box.getMinZ()) / 2 + radius);
        final var receivers = new ArrayList<Player>();
        nearby.stream()
                .filter(player -> containsExpanded(box, player.getLocation(), radius))
                .forEach(receivers::add);
        return receivers;
    }

    private void spawn(final Location location, final PortalParticleEffect effect, final List<Player> receivers) {
        if (plugin.getServer().isOwnedByCurrentRegion(location)) spawnOwned(location, effect, receivers);
        else plugin.getServer().getRegionScheduler().run(plugin, location, ignored -> spawnOwned(location, effect, receivers));
    }

    private void spawnOwned(final Location location, final PortalParticleEffect effect, final List<Player> receivers) {
        location.getWorld().spawnParticle(effect.particle(), receivers, null,
                location.getX(), location.getY(), location.getZ(), 1,
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.extra(), effect.data(), effect.force());
    }

    private List<Location> fillPoints(final PaperPortal portal, final PortalParticleEffect effect) {
        final var box = portal.getBoundingBox();
        final var cells = new Grid(cells(box.getMinX(), box.getMaxX()), cells(box.getMinY(), box.getMaxY()), cells(box.getMinZ(), box.getMaxZ()));
        final var grid = cells.count() <= effect.count() ? cells : fitGrid(cells, effect.count());
        final var points = new ArrayList<Location>((int) grid.count());

        for (var x = 0; x < grid.x(); x++) {
            for (var y = 0; y < grid.y(); y++) {
                for (var z = 0; z < grid.z(); z++) {
                    points.add(point(portal, box, x, y, z, grid));
                }
            }
        }
        return points;
    }

    private List<Location> outlinePoints(final PaperPortal portal, final PortalParticleEffect effect) {
        final var points = new ArrayList<Location>();
        final var box = portal.getBoundingBox();
        final var axes = axes(box);
        final var counts = split(effect.count(), 2);

        for (var side = 0; side < 2; side++) {
            final var count = counts[side];
            final var face = side == 0 ? Face.MIN : Face.MAX;
            for (var index = 0; index < count; index++) {
                final var edge = index % 4;
                final var progress = ((index / 4) + 0.5) / Math.max(1, Math.ceil(count / 4d));
                points.add(switch (edge) {
                    case 0 -> point(portal, box, axes, progress, 0, face);
                    case 1 -> point(portal, box, axes, progress, 1, face);
                    case 2 -> point(portal, box, axes, 0, progress, face);
                    case 3 -> point(portal, box, axes, 1, progress, face);
                    default -> throw new IllegalStateException("Unexpected edge: " + edge);
                });
            }
        }
        return points;
    }

    private static Location point(final PaperPortal portal, final BoundingBox box, final Axes axes, final double u, final double v, final Face face) {
        final var min = new double[]{box.getMinX(), box.getMinY(), box.getMinZ()};
        final var max = new double[]{box.getMaxX(), box.getMaxY(), box.getMaxZ()};
        final var point = new double[]{
                (min[0] + max[0]) / 2,
                (min[1] + max[1]) / 2,
                (min[2] + max[2]) / 2
        };
        point[axes.u()] = lerp(min[axes.u()], max[axes.u()], u);
        point[axes.v()] = lerp(min[axes.v()], max[axes.v()], v);
        point[axes.w()] = face == Face.MIN ? min[axes.w()] - SURFACE_OFFSET : max[axes.w()] + SURFACE_OFFSET;
        return new Location(portal.getWorld(), point[0], point[1], point[2]);
    }

    private static Location point(final PaperPortal portal, final BoundingBox box, final int x, final int y, final int z, final Grid grid) {
        return new Location(portal.getWorld(),
                coordinate(box.getMinX(), box.getMaxX(), x, grid.x()),
                coordinate(box.getMinY(), box.getMaxY(), y, grid.y()),
                coordinate(box.getMinZ(), box.getMaxZ(), z, grid.z()));
    }

    private static boolean containsExpanded(final BoundingBox box, final Location location, final double expansion) {
        return location.getWorld().equals(box.getWorld())
                && location.getX() >= box.getMinX() - expansion
                && location.getX() <= box.getMaxX() + expansion
                && location.getY() >= box.getMinY() - expansion
                && location.getY() <= box.getMaxY() + expansion
                && location.getZ() >= box.getMinZ() - expansion
                && location.getZ() <= box.getMaxZ() + expansion;
    }

    private void logFirstRender(final PaperPortal portal, final PortalParticleEffect effect, final int receivers) {
        final var box = portal.getBoundingBox();
        final var center = box.getCenter();
        plugin.getComponentLogger().info(
                "Rendering particles for portal {} in world {} at {}, {}, {} (bounds {}, {}, {} -> {}, {}, {}, interval {} ticks, receivers {})",
                portal.getName(), portal.getWorld().getName(),
                center.getBlockX(), center.getBlockY(), center.getBlockZ(),
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ(),
                effect.intervalTicks(), receivers
        );
    }

    private static double lerp(final double min, final double max, final double progress) {
        return min + (max - min) * progress;
    }

    private static int[] split(final int count, final int parts) {
        final var result = new int[parts];
        final var base = count / parts;
        final var remainder = count % parts;
        for (var i = 0; i < parts; i++) result[i] = base + (i < remainder ? 1 : 0);
        return result;
    }

    private static Axes axes(final BoundingBox box) {
        final var x = box.getMaxX() - box.getMinX();
        final var y = box.getMaxY() - box.getMinY();
        final var z = box.getMaxZ() - box.getMinZ();
        var w = 0;
        if (y < component(x, y, z, w)) w = 1;
        if (z < component(x, y, z, w)) w = 2;
        final var u = w == 0 ? 1 : 0;
        final var v = w == 2 ? 1 : 2;
        return new Axes(w, u, v);
    }

    private static double component(final double x, final double y, final double z, final int axis) {
        return switch (axis) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static int cells(final double min, final double max) {
        return Math.max(1, (int) Math.ceil(max - min));
    }

    private static Grid fitGrid(final Grid cells, final int limit) {
        final var scale = Math.cbrt(limit / (double) cells.count());
        final var maxX = Math.min(cells.x(), limit);
        final var maxY = Math.min(cells.y(), limit);
        final var maxZ = Math.min(cells.z(), limit);
        var best = new Grid(1, 1, 1);
        var bestScore = Double.MAX_VALUE;

        for (var x = 1; x <= maxX; x++) {
            for (var y = 1; y <= maxY; y++) {
                final var maxGridZ = Math.min(maxZ, limit / (x * y));
                for (var z = 1; z <= maxGridZ; z++) {
                    final var grid = new Grid(x, y, z);
                    final var score = score(cells, grid, limit, scale);
                    if (score < bestScore || score == bestScore && grid.count() > best.count()) {
                        best = grid;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    private static double score(final Grid cells, final Grid grid, final int limit, final double scale) {
        final var shape = axisScore(cells.x(), grid.x(), scale)
                + axisScore(cells.y(), grid.y(), scale)
                + axisScore(cells.z(), grid.z(), scale);
        final var unused = (limit - grid.count()) / (double) limit;
        return shape + unused * 0.1;
    }

    private static double axisScore(final int cells, final int grid, final double scale) {
        final var target = Math.max(1, cells * scale);
        final var error = Math.log(grid / target);
        return error * error;
    }

    private static double coordinate(final double min, final double max, final int index, final int cells) {
        final var size = max - min;
        if (size <= 0) return min;
        return min + (index + 0.5) * size / cells;
    }

    private record Axes(int w, int u, int v) {
    }

    private record Grid(int x, int y, int z) {
        private long count() {
            return (long) x * y * z;
        }
    }

    private enum Face {
        MIN,
        MAX
    }
}
