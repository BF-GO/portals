package net.thenextlvl.portals.plugin.adapters;

import net.thenextlvl.nbt.serialization.ParserException;
import net.thenextlvl.nbt.serialization.TagAdapter;
import net.thenextlvl.nbt.serialization.TagDeserializationContext;
import net.thenextlvl.nbt.serialization.TagSerializationContext;
import net.thenextlvl.nbt.tag.CompoundTag;
import net.thenextlvl.nbt.tag.Tag;
import net.thenextlvl.portals.particle.PortalParticleEffect;
import net.thenextlvl.portals.particle.PortalParticlePattern;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Base64;

@NullMarked
public final class PortalParticleEffectAdapter implements TagAdapter<PortalParticleEffect> {
    private final World world;

    public PortalParticleEffectAdapter(final World world) {
        this.world = world;
    }

    @Override
    public PortalParticleEffect deserialize(final Tag tag, final TagDeserializationContext context) throws ParserException {
        final var root = tag.getAsCompound();
        final var particle = Particle.valueOf(root.get("particle").getAsString());
        final var pattern = PortalParticlePattern.valueOf(root.get("pattern").getAsString());
        final var count = root.get("count").getAsInt();
        final var intervalTicks = root.get("intervalTicks").getAsInt();
        final var offsetX = root.optional("offsetX").map(Tag::getAsDouble).orElse(0d);
        final var offsetY = root.optional("offsetY").map(Tag::getAsDouble).orElse(0d);
        final var offsetZ = root.optional("offsetZ").map(Tag::getAsDouble).orElse(0d);
        final var extra = root.optional("extra").map(Tag::getAsDouble).orElse(1d);
        final var viewDistance = root.get("viewDistance").getAsInt();
        final var force = root.optional("force").map(Tag::getAsByte).map(value -> value != 0).orElse(false);
        final var data = root.optional("data").map(value -> deserializeData(value.getAsCompound())).orElse(null);
        return new PortalParticleEffect(particle, pattern, count, intervalTicks, offsetX, offsetY, offsetZ, extra, viewDistance, force, data);
    }

    @Override
    public Tag serialize(final PortalParticleEffect effect, final TagSerializationContext context) throws ParserException {
        final var tag = CompoundTag.builder()
                .put("particle", effect.particle().name())
                .put("pattern", effect.pattern().name())
                .put("count", effect.count())
                .put("intervalTicks", effect.intervalTicks())
                .put("offsetX", effect.offsetX())
                .put("offsetY", effect.offsetY())
                .put("offsetZ", effect.offsetZ())
                .put("extra", effect.extra())
                .put("viewDistance", effect.viewDistance())
                .put("force", effect.force());

        final var data = effect.data();
        if (data != null) tag.put("data", serializeData(data));
        return tag.build();
    }

    private Tag serializeData(final Object data) throws ParserException {
        if (data instanceof Particle.DustTransition transition) return CompoundTag.builder()
                .put("type", "transition")
                .put("from", serializeColor(transition.getColor()))
                .put("to", serializeColor(transition.getToColor()))
                .put("size", transition.getSize())
                .build();
        if (data instanceof Particle.DustOptions dust) return CompoundTag.builder()
                .put("type", "dust")
                .put("color", serializeColor(dust.getColor()))
                .put("size", dust.getSize())
                .build();
        if (data instanceof Particle.Spell spell) return CompoundTag.builder()
                .put("type", "spell")
                .put("color", serializeColor(spell.getColor()))
                .put("power", spell.getPower())
                .build();
        if (data instanceof Particle.Trail trail) return CompoundTag.builder()
                .put("type", "trail")
                .put("target", serializeLocation(trail.getTarget()))
                .put("color", serializeColor(trail.getColor()))
                .put("duration", trail.getDuration())
                .build();
        if (data instanceof Vibration vibration) return CompoundTag.builder()
                .put("type", "vibration")
                .put("target", serializeVibrationTarget(vibration))
                .put("arrivalTime", vibration.getArrivalTime())
                .build();
        if (data instanceof BlockData blockData) return CompoundTag.builder()
                .put("type", "block")
                .put("block", blockData.getAsString())
                .build();
        if (data instanceof ItemStack itemStack) return CompoundTag.builder()
                .put("type", "item")
                .put("item", Base64.getEncoder().encodeToString(itemStack.serializeAsBytes()))
                .build();
        if (data instanceof Color color) return CompoundTag.builder()
                .put("type", "color")
                .put("color", serializeColor(color))
                .build();
        if (data instanceof Float value) return CompoundTag.builder()
                .put("type", "float")
                .put("value", value)
                .build();
        if (data instanceof Integer value) return CompoundTag.builder()
                .put("type", "integer")
                .put("value", value)
                .build();
        throw new ParserException("Unsupported particle data type: " + data.getClass().getName());
    }

    private @Nullable Object deserializeData(final CompoundTag root) {
        return switch (root.get("type").getAsString()) {
            case "block" -> Bukkit.createBlockData(root.get("block").getAsString());
            case "item" -> ItemStack.deserializeBytes(Base64.getDecoder().decode(root.get("item").getAsString()));
            case "color" -> deserializeColor(root.get("color").getAsCompound());
            case "dust" -> new Particle.DustOptions(
                    deserializeColor(root.get("color").getAsCompound()),
                    root.get("size").getAsFloat()
            );
            case "transition" -> new Particle.DustTransition(
                    deserializeColor(root.get("from").getAsCompound()),
                    deserializeColor(root.get("to").getAsCompound()),
                    root.get("size").getAsFloat()
            );
            case "float" -> root.get("value").getAsFloat();
            case "integer" -> root.get("value").getAsInt();
            case "spell" -> new Particle.Spell(
                    deserializeColor(root.get("color").getAsCompound()),
                    root.get("power").getAsFloat()
            );
            case "trail" -> new Particle.Trail(
                    deserializeLocation(root.get("target").getAsCompound()),
                    deserializeColor(root.get("color").getAsCompound()),
                    root.get("duration").getAsInt()
            );
            case "vibration" -> new Vibration(
                    new Vibration.Destination.BlockDestination(deserializeLocation(root.get("target").getAsCompound())),
                    root.get("arrivalTime").getAsInt()
            );
            default -> null;
        };
    }

    private Tag serializeColor(final Color color) {
        return CompoundTag.builder()
                .put("alpha", color.getAlpha())
                .put("red", color.getRed())
                .put("green", color.getGreen())
                .put("blue", color.getBlue())
                .build();
    }

    private Color deserializeColor(final CompoundTag root) {
        final var alpha = root.optional("alpha").map(Tag::getAsInt).orElse(255);
        final var red = root.get("red").getAsInt();
        final var green = root.get("green").getAsInt();
        final var blue = root.get("blue").getAsInt();
        return Color.fromARGB(alpha, red, green, blue);
    }

    private Tag serializeLocation(final Location location) {
        return CompoundTag.builder()
                .put("x", location.getX())
                .put("y", location.getY())
                .put("z", location.getZ())
                .build();
    }

    private Location deserializeLocation(final CompoundTag root) {
        return new Location(world, root.get("x").getAsDouble(), root.get("y").getAsDouble(), root.get("z").getAsDouble());
    }

    private Tag serializeVibrationTarget(final Vibration vibration) {
        if (vibration.getDestination() instanceof final Vibration.Destination.BlockDestination destination) {
            return serializeLocation(destination.getLocation());
        }
        if (vibration.getDestination() instanceof final Vibration.Destination.EntityDestination destination) {
            return serializeLocation(destination.getEntity().getLocation());
        }
        return serializeLocation(new Location(world, 0, 0, 0));
    }
}
