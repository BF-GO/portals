package net.thenextlvl.portals.plugin.model;

import net.thenextlvl.portals.Portal;
import net.thenextlvl.portals.PortalLike;
import net.thenextlvl.portals.plugin.PortalsPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public final class LazyPortal implements PortalLike {
    private final String name;

    public LazyPortal(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<Portal> getPortal() {
        final var plugin = JavaPlugin.getPlugin(PortalsPlugin.class);
        return plugin.portalProvider().getPortal(name);
    }
}
