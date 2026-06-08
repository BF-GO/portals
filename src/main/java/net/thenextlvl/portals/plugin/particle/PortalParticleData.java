package net.thenextlvl.portals.plugin.particle;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PortalParticleData {
    private PortalParticleData() {
    }

    public static @Nullable Object defaultData(final Particle particle, final Location origin) {
        final var type = particle.getDataType();
        if (type.equals(Void.class)) return null;
        if (type.equals(BlockData.class)) return Bukkit.createBlockData(Material.STONE);
        if (type.equals(ItemStack.class)) return ItemStack.of(Material.STONE);
        if (type.equals(Color.class)) return Color.WHITE;
        if (type.equals(Particle.DustOptions.class)) return new Particle.DustOptions(Color.WHITE, 1);
        if (type.equals(Particle.DustTransition.class)) return new Particle.DustTransition(Color.WHITE, Color.WHITE, 1);
        if (type.equals(Float.class)) return 1f;
        if (type.equals(Integer.class)) return 0;
        if (type.equals(Vibration.class)) return new Vibration(new Vibration.Destination.BlockDestination(origin), 20);
        if (type.equals(Particle.Trail.class)) return new Particle.Trail(origin, Color.WHITE, 20);
        if (type.equals(Particle.Spell.class)) return new Particle.Spell(Color.WHITE, 1);
        return null;
    }

    public static boolean isValid(final Particle particle, final @Nullable Object data) {
        final var type = particle.getDataType();
        if (type.equals(Void.class)) return data == null;
        return data != null && type.isInstance(data);
    }

    public static String typeName(final Particle particle) {
        return simpleTypeName(particle.getDataType());
    }

    public static String describe(final @Nullable Object data) {
        if (data == null) return "none";
        if (data instanceof BlockData blockData) return blockData.getAsString();
        if (data instanceof ItemStack itemStack) return itemStack.getType().key().asString();
        if (data instanceof Color color) return color(color);
        if (data instanceof Particle.DustTransition transition) {
            return color(transition.getColor()) + " -> " + color(transition.getToColor()) + " size " + transition.getSize();
        }
        if (data instanceof Particle.DustOptions dust) return color(dust.getColor()) + " size " + dust.getSize();
        if (data instanceof Particle.Spell spell) return color(spell.getColor()) + " power " + spell.getPower();
        if (data instanceof Particle.Trail trail) return color(trail.getColor()) + " duration " + trail.getDuration();
        if (data instanceof Vibration vibration) return "arrival " + vibration.getArrivalTime();
        return String.valueOf(data);
    }

    private static String simpleTypeName(final Class<?> type) {
        if (type.equals(Void.class)) return "none";
        if (type.equals(BlockData.class)) return "block";
        if (type.equals(ItemStack.class)) return "item";
        if (type.equals(Color.class)) return "color";
        if (type.equals(Particle.DustOptions.class)) return "dust";
        if (type.equals(Particle.DustTransition.class)) return "transition";
        if (type.equals(Float.class)) return "float";
        if (type.equals(Integer.class)) return "integer";
        if (type.equals(Vibration.class)) return "vibration";
        if (type.equals(Particle.Trail.class)) return "trail";
        if (type.equals(Particle.Spell.class)) return "spell";
        return type.getSimpleName();
    }

    private static String color(final Color color) {
        return "#" + hex(color.getRed()) + hex(color.getGreen()) + hex(color.getBlue()) + "/" + color.getAlpha();
    }

    private static String hex(final int value) {
        return String.format("%02x", value);
    }
}
