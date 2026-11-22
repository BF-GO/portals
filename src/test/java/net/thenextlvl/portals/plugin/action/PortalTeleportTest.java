package net.thenextlvl.portals.plugin.action;

import io.papermc.paper.entity.TeleportFlag;
import io.papermc.paper.math.Position;
import net.thenextlvl.nbt.serialization.NBT;
import net.thenextlvl.portals.Portal;
import net.thenextlvl.portals.PortalLike;
import net.thenextlvl.portals.action.ActionType;
import net.thenextlvl.portals.action.EntryAction;
import net.thenextlvl.portals.plugin.PortalsPlugin;
import net.thenextlvl.portals.plugin.adapters.PortalLikeAdapter;
import net.thenextlvl.portals.plugin.model.LazyPortal;
import net.thenextlvl.portals.plugin.particle.PortalParticleService;
import net.thenextlvl.portals.plugin.portal.PaperPortal;
import net.thenextlvl.portals.plugin.portal.PaperPortalProvider;
import net.thenextlvl.portals.shape.BoundingBox;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortalTeleportTest {
    @TempDir
    Path directory;

    private MockedStatic<JavaPlugin> javaPlugin;
    private PortalsPlugin plugin;
    private PaperPortalProvider provider;
    private World world;
    private Entity entity;
    private Portal source;
    private ActionType.Action<PortalLike> action;

    @BeforeEach
    void setUp() {
        plugin = mock(PortalsPlugin.class);
        provider = mock(PaperPortalProvider.class);
        world = mock(World.class);
        entity = mock(Entity.class);
        final var server = mock(Server.class);
        when(plugin.portalProvider()).thenReturn(provider);
        when(plugin.particleService()).thenReturn(mock(PortalParticleService.class));
        when(plugin.getServer()).thenReturn(server);
        when(provider.getDataFolder(any(World.class))).thenReturn(directory);
        when(server.isOwnedByCurrentRegion(any(Location.class))).thenReturn(true);
        when(entity.getLocation()).thenReturn(new Location(world, 0.5, 65, 2, 45, 12));
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.teleportAsync(any(Location.class), eq(PLUGIN), any(TeleportFlag[].class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        javaPlugin = mockStatic(JavaPlugin.class);
        javaPlugin.when(() -> JavaPlugin.getPlugin(PortalsPlugin.class)).thenReturn(plugin);
        action = new SimpleActionTypes().teleportPortal().getAction();
        source = new PaperPortal(plugin, "source", box(world, 0, 64, 0, 1, 68, 4));
    }

    @AfterEach
    void tearDown() {
        if (javaPlugin != null) javaPlugin.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void linkedPortalFollowsRedefine(final boolean changeWorld) {
        final var oldBox = box(world, 10, 64, 20, 11, 68, 24);
        final var target = new PaperPortal(plugin, "target", oldBox);
        when(provider.getPortal("target")).thenReturn(Optional.of(target));
        final var linkedAction = EntryAction.create(new SimpleActionTypes().teleportPortal(), target);
        source.setEntryAction(linkedAction);
        assertTrue(source.getEntryAction().orElseThrow().onEntry(entity, source));

        final var newBox = box(changeWorld ? mock(World.class) : world, -400, 90, 320, -394, 96, 321);
        assertTrue(target.setBoundingBox(newBox));
        assertSame(linkedAction, source.getEntryAction().orElseThrow());
        assertTrue(source.getEntryAction().orElseThrow().onEntry(entity, source));

        final var destination = ArgumentCaptor.forClass(Location.class);
        verify(entity, times(2)).teleportAsync(destination.capture(), eq(PLUGIN), any(TeleportFlag[].class));
        assertInside(oldBox, destination.getAllValues().getFirst());
        assertInside(newBox, destination.getValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothDirectionsUseCurrentPortalBounds(final boolean changeWorld) {
        final var target = new PaperPortal(plugin, "target", box(world, 10, 64, 20, 11, 68, 24));
        when(provider.getPortal("source")).thenReturn(Optional.of(source));
        when(provider.getPortal("target")).thenReturn(Optional.of(target));
        final var type = new SimpleActionTypes().teleportPortal();
        source.setEntryAction(EntryAction.create(type, target));
        target.setEntryAction(EntryAction.create(type, source));
        final var newBox = box(changeWorld ? mock(World.class) : world, -400, 90, 320, -394, 96, 321);
        assertTrue(target.setBoundingBox(newBox));

        assertTrue(source.getEntryAction().orElseThrow().onEntry(entity, source));
        when(entity.getLocation()).thenReturn(newBox.getCenter());
        assertTrue(target.getEntryAction().orElseThrow().onEntry(entity, target));

        final var destination = ArgumentCaptor.forClass(Location.class);
        verify(entity, times(2)).teleportAsync(destination.capture(), eq(PLUGIN), any(TeleportFlag[].class));
        assertInside(newBox, destination.getAllValues().getFirst());
        assertInside(source.getBoundingBox(), destination.getValue());
    }

    @Test
    void savedLinkUsesCurrentPortalAfterWorldReload() {
        final var oldTarget = new PaperPortal(plugin, "target", box(world, 10, 64, 20, 11, 68, 24));
        when(provider.getPortal("target")).thenReturn(Optional.of(oldTarget));
        final var nbt = NBT.builder()
                .registerTypeHierarchyAdapter(PortalLike.class, new PortalLikeAdapter(plugin))
                .build();
        final var savedLink = nbt.serialize(oldTarget, PortalLike.class);
        assertEquals("target", savedLink.getAsString());
        final var link = nbt.deserialize(savedLink, PortalLike.class);
        final var linkedAction = EntryAction.create(new SimpleActionTypes().teleportPortal(), link);
        source.setEntryAction(linkedAction);

        final var reloaded = new PaperPortal(plugin, "target", box(mock(World.class), 500, 80, 600, 501, 84, 604));
        when(provider.getPortal("target")).thenReturn(Optional.of(reloaded));
        final var newBox = box(reloaded.getWorld(), 1000, 90, 2000, 1001, 94, 2004);
        assertTrue(reloaded.setBoundingBox(newBox));

        assertSame(linkedAction, source.getEntryAction().orElseThrow());
        assertTrue(source.getEntryAction().orElseThrow().onEntry(entity, source));

        final var destination = ArgumentCaptor.forClass(Location.class);
        verify(entity).teleportAsync(destination.capture(), eq(PLUGIN), any(TeleportFlag[].class));
        assertInside(newBox, destination.getValue());
    }

    @Test
    void unloadedOrDeletedTargetDoesNotTeleportToOldObject() {
        final var oldTarget = new PaperPortal(plugin, "target", box(world, 10, 64, 20, 11, 68, 24));
        when(provider.getPortal("target")).thenReturn(Optional.empty());

        assertFalse(action.invoke(entity, source, oldTarget));

        verify(entity, never()).teleportAsync(any(Location.class), eq(PLUGIN), any(TeleportFlag[].class));
    }

    @Test
    void lazyReferenceDoesNotKeepUnloadedPortal() {
        final var oldTarget = new PaperPortal(plugin, "target", box(world, 10, 64, 20, 11, 68, 24));
        when(provider.getPortal("target")).thenReturn(Optional.of(oldTarget));
        final var link = new LazyPortal("target");
        assertSame(oldTarget, link.getPortal().orElseThrow());

        when(provider.getPortal("target")).thenReturn(Optional.empty());

        assertTrue(link.getPortal().isEmpty());
    }

    @Test
    void lazyReferenceResolvesReplacementWithSameName() {
        final var oldTarget = new PaperPortal(plugin, "target", box(world, 10, 64, 20, 11, 68, 24));
        when(provider.getPortal("target")).thenReturn(Optional.of(oldTarget));
        final var link = new LazyPortal("target");
        assertSame(oldTarget, link.getPortal().orElseThrow());

        final var replacement = new PaperPortal(plugin, "target", box(world, 500, 80, 600, 501, 84, 604));
        when(provider.getPortal("target")).thenReturn(Optional.of(replacement));

        assertSame(replacement, link.getPortal().orElseThrow());
    }

    private static BoundingBox box(final World world, final double minX, final double minY, final double minZ,
                                   final double maxX, final double maxY, final double maxZ) {
        return BoundingBox.of(world, Position.fine(minX, minY, minZ), Position.fine(maxX, maxY, maxZ));
    }

    private static void assertInside(final BoundingBox box, final Location location) {
        assertSame(box.getWorld(), location.getWorld());
        assertTrue(box.contains(location));
    }
}
