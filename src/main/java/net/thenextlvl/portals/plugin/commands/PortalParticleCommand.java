package net.thenextlvl.portals.plugin.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.portals.Portal;
import net.thenextlvl.portals.particle.PortalParticleEffect;
import net.thenextlvl.portals.particle.PortalParticlePattern;
import net.thenextlvl.portals.plugin.PortalsPlugin;
import net.thenextlvl.portals.plugin.commands.arguments.EnumArgumentType;
import net.thenextlvl.portals.plugin.commands.arguments.ParticleArgumentType;
import net.thenextlvl.portals.plugin.commands.brigadier.SimpleCommand;
import net.thenextlvl.portals.plugin.particle.PortalParticleData;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

import static net.thenextlvl.portals.plugin.commands.PortalCommand.portalArgument;

@NullMarked
final class PortalParticleCommand extends SimpleCommand {
    private PortalParticleCommand(final PortalsPlugin plugin) {
        super(plugin, "particle", "portals.command.particle");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(final PortalsPlugin plugin) {
        final var command = new PortalParticleCommand(plugin);

        return command.create().then(portalArgument(plugin)
                .then(Commands.literal("set").then(Commands.argument("particle", new ParticleArgumentType())
                        .then(Commands.argument("pattern", new EnumArgumentType<>(PortalParticlePattern.class))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("intervalTicks", IntegerArgumentType.integer(1)).executes(command::set))))))
                .then(Commands.literal("clear").executes(command::clear))
                .then(Commands.literal("count").then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(command::count)))
                .then(Commands.literal("interval").then(Commands.argument("intervalTicks", IntegerArgumentType.integer(1)).executes(command::interval)))
                .then(Commands.literal("pattern").then(Commands.argument("pattern", new EnumArgumentType<>(PortalParticlePattern.class)).executes(command::pattern)))
                .then(Commands.literal("visibility").then(Commands.argument("radius", IntegerArgumentType.integer(0))
                        .then(Commands.argument("force", BoolArgumentType.bool()).executes(command::visibility))))
                .then(Commands.literal("offset").then(Commands.argument("offsetX", DoubleArgumentType.doubleArg(0))
                        .then(Commands.argument("offsetY", DoubleArgumentType.doubleArg(0))
                                .then(Commands.argument("offsetZ", DoubleArgumentType.doubleArg(0)).executes(command::offset)))))
                .then(Commands.literal("extra").then(Commands.argument("extra", DoubleArgumentType.doubleArg(0)).executes(command::extra)))
                .then(data(command))
                .executes(command));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> data(final PortalParticleCommand command) {
        return Commands.literal("data")
                .then(Commands.literal("block").then(Commands.argument("block", ArgumentTypes.blockState()).executes(command::blockData)))
                .then(Commands.literal("item").then(Commands.argument("item", ArgumentTypes.itemStack()).executes(command::itemData)))
                .then(Commands.literal("color").then(Commands.argument("color", ArgumentTypes.hexColor()).executes(command::colorData)
                        .then(Commands.argument("alpha", IntegerArgumentType.integer(0, 255)).executes(command::colorData))))
                .then(Commands.literal("dust").then(Commands.argument("color", ArgumentTypes.hexColor())
                        .then(Commands.argument("size", FloatArgumentType.floatArg(0)).executes(command::dustData))))
                .then(Commands.literal("transition").then(Commands.argument("from", ArgumentTypes.hexColor())
                        .then(Commands.argument("to", ArgumentTypes.hexColor())
                                .then(Commands.argument("size", FloatArgumentType.floatArg(0)).executes(command::transitionData)))))
                .then(Commands.literal("float").then(Commands.argument("value", FloatArgumentType.floatArg()).executes(command::floatData)))
                .then(Commands.literal("integer").then(Commands.argument("value", IntegerArgumentType.integer()).executes(command::integerData)))
                .then(Commands.literal("spell").then(Commands.argument("color", ArgumentTypes.hexColor())
                        .then(Commands.argument("power", FloatArgumentType.floatArg(0)).executes(command::spellData))))
                .then(Commands.literal("trail").then(Commands.argument("color", ArgumentTypes.hexColor())
                        .then(Commands.argument("durationTicks", IntegerArgumentType.integer(1))
                                .then(Commands.argument("offsetX", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("offsetY", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("offsetZ", DoubleArgumentType.doubleArg()).executes(command::trailData)))))))
                .then(Commands.literal("vibration").then(Commands.argument("arrivalTicks", IntegerArgumentType.integer(1))
                        .then(Commands.argument("offsetX", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("offsetY", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("offsetZ", DoubleArgumentType.doubleArg()).executes(command::vibrationData))))));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var portal = context.getArgument("portal", Portal.class);
        final var effect = portal.getParticleEffect().orElse(null);

        if (effect == null) {
            plugin.bundle().sendMessage(sender, "portal.particle.disabled",
                    Placeholder.parsed("portal", portal.getName()));
            return SINGLE_SUCCESS;
        }

        plugin.bundle().sendMessage(sender, "portal.particle.current",
                Placeholder.parsed("portal", portal.getName()),
                Placeholder.parsed("particle", effect.particle().name()),
                Placeholder.parsed("pattern", effect.pattern().name().toLowerCase()),
                Placeholder.parsed("count", String.valueOf(effect.count())),
                Placeholder.parsed("interval", String.valueOf(effect.intervalTicks())),
                Placeholder.parsed("radius", String.valueOf(effect.viewDistance())),
                Placeholder.parsed("force", String.valueOf(effect.force())),
                Placeholder.parsed("offset_x", String.valueOf(effect.offsetX())),
                Placeholder.parsed("offset_y", String.valueOf(effect.offsetY())),
                Placeholder.parsed("offset_z", String.valueOf(effect.offsetZ())),
                Placeholder.parsed("extra", String.valueOf(effect.extra())),
                Placeholder.parsed("data", PortalParticleData.describe(effect.data())));
        return SINGLE_SUCCESS;
    }

    private int set(final CommandContext<CommandSourceStack> context) {
        final var portal = context.getArgument("portal", Portal.class);
        final var particle = context.getArgument("particle", Particle.class);
        final var pattern = context.getArgument("pattern", PortalParticlePattern.class);
        final var count = context.getArgument("count", Integer.class);
        final var interval = context.getArgument("intervalTicks", Integer.class);
        final var radius = plugin.config().defaultParticleViewDistance();
        if (!validateCount(context, count) || !validateInterval(context, interval) || !validateRadius(context, radius)) return 0;

        final var data = PortalParticleData.defaultData(particle, portal.getBoundingBox().getCenter());
        final var effect = new PortalParticleEffect(particle, pattern, count, interval, 0, 0, 0, 1, radius, plugin.config().defaultParticleForce(), data);
        final var success = portal.setParticleEffect(effect);
        sendChanged(context, success, "portal.particle.set", effect);
        return success ? SINGLE_SUCCESS : 0;
    }

    private int clear(final CommandContext<CommandSourceStack> context) {
        final var portal = context.getArgument("portal", Portal.class);
        final var success = portal.setParticleEffect(null);
        plugin.bundle().sendMessage(context.getSource().getSender(), success ? "portal.particle.cleared" : "nothing.changed",
                Placeholder.parsed("portal", portal.getName()));
        return success ? SINGLE_SUCCESS : 0;
    }

    private int count(final CommandContext<CommandSourceStack> context) {
        final var count = context.getArgument("count", Integer.class);
        if (!validateCount(context, count)) return 0;
        return update(context, effect -> copy(effect, count, effect.intervalTicks(), effect.pattern(), effect.viewDistance(), effect.force(),
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.extra(), effect.data()), "portal.particle.count");
    }

    private int interval(final CommandContext<CommandSourceStack> context) {
        final var interval = context.getArgument("intervalTicks", Integer.class);
        if (!validateInterval(context, interval)) return 0;
        return update(context, effect -> copy(effect, effect.count(), interval, effect.pattern(), effect.viewDistance(), effect.force(),
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.extra(), effect.data()), "portal.particle.interval");
    }

    private int pattern(final CommandContext<CommandSourceStack> context) {
        final var pattern = context.getArgument("pattern", PortalParticlePattern.class);
        return update(context, effect -> copy(effect, effect.count(), effect.intervalTicks(), pattern, effect.viewDistance(), effect.force(),
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.extra(), effect.data()), "portal.particle.pattern");
    }

    private int visibility(final CommandContext<CommandSourceStack> context) {
        final var radius = context.getArgument("radius", Integer.class);
        final var force = context.getArgument("force", Boolean.class);
        if (!validateRadius(context, radius)) return 0;
        return update(context, effect -> copy(effect, effect.count(), effect.intervalTicks(), effect.pattern(), radius, force,
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.extra(), effect.data()), "portal.particle.visibility");
    }

    private int offset(final CommandContext<CommandSourceStack> context) {
        final var x = context.getArgument("offsetX", Double.class);
        final var y = context.getArgument("offsetY", Double.class);
        final var z = context.getArgument("offsetZ", Double.class);
        return update(context, effect -> copy(effect, effect.count(), effect.intervalTicks(), effect.pattern(), effect.viewDistance(), effect.force(),
                x, y, z, effect.extra(), effect.data()), "portal.particle.offset");
    }

    private int extra(final CommandContext<CommandSourceStack> context) {
        final var extra = context.getArgument("extra", Double.class);
        return update(context, effect -> copy(effect, effect.count(), effect.intervalTicks(), effect.pattern(), effect.viewDistance(), effect.force(),
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), extra, effect.data()), "portal.particle.extra");
    }

    private int blockData(final CommandContext<CommandSourceStack> context) {
        return setData(context, context.getArgument("block", BlockState.class).getBlockData());
    }

    private int itemData(final CommandContext<CommandSourceStack> context) {
        return setData(context, context.getArgument("item", ItemStack.class));
    }

    private int colorData(final CommandContext<CommandSourceStack> context) {
        final var color = color(context.getArgument("color", TextColor.class),
                tryGetArgument(context, "alpha", Integer.class).orElse(255));
        return setData(context, color);
    }

    private int dustData(final CommandContext<CommandSourceStack> context) {
        return setData(context, new Particle.DustOptions(color(context.getArgument("color", TextColor.class), 255),
                context.getArgument("size", Float.class)));
    }

    private int transitionData(final CommandContext<CommandSourceStack> context) {
        return setData(context, new Particle.DustTransition(
                color(context.getArgument("from", TextColor.class), 255),
                color(context.getArgument("to", TextColor.class), 255),
                context.getArgument("size", Float.class)));
    }

    private int floatData(final CommandContext<CommandSourceStack> context) {
        return setData(context, context.getArgument("value", Float.class));
    }

    private int integerData(final CommandContext<CommandSourceStack> context) {
        return setData(context, context.getArgument("value", Integer.class));
    }

    private int spellData(final CommandContext<CommandSourceStack> context) {
        return setData(context, new Particle.Spell(color(context.getArgument("color", TextColor.class), 255),
                context.getArgument("power", Float.class)));
    }

    private int trailData(final CommandContext<CommandSourceStack> context) {
        final var portal = context.getArgument("portal", Portal.class);
        final var target = portal.getBoundingBox().getCenter().add(
                context.getArgument("offsetX", Double.class),
                context.getArgument("offsetY", Double.class),
                context.getArgument("offsetZ", Double.class));
        return setData(context, new Particle.Trail(target,
                color(context.getArgument("color", TextColor.class), 255),
                context.getArgument("durationTicks", Integer.class)));
    }

    private int vibrationData(final CommandContext<CommandSourceStack> context) {
        final var portal = context.getArgument("portal", Portal.class);
        final var target = portal.getBoundingBox().getCenter().add(
                context.getArgument("offsetX", Double.class),
                context.getArgument("offsetY", Double.class),
                context.getArgument("offsetZ", Double.class));
        return setData(context, new Vibration(new Vibration.Destination.BlockDestination(target),
                context.getArgument("arrivalTicks", Integer.class)));
    }

    private int setData(final CommandContext<CommandSourceStack> context, final Object data) {
        final var portal = context.getArgument("portal", Portal.class);
        final var effect = portal.getParticleEffect().orElse(null);
        if (effect == null) return missing(context, portal);
        if (!PortalParticleData.isValid(effect.particle(), data)) {
            plugin.bundle().sendMessage(context.getSource().getSender(), "portal.particle.data.invalid",
                    Placeholder.parsed("portal", portal.getName()),
                    Placeholder.parsed("expected", PortalParticleData.typeName(effect.particle())));
            return 0;
        }
        return update(context, ignored -> effect.withData(data), "portal.particle.data");
    }

    private int update(
            final CommandContext<CommandSourceStack> context,
            final Function<PortalParticleEffect, PortalParticleEffect> update,
            final String message
    ) {
        final var portal = context.getArgument("portal", Portal.class);
        final var current = portal.getParticleEffect().orElse(null);
        if (current == null) return missing(context, portal);
        final var effect = update.apply(current);
        final var success = portal.setParticleEffect(effect);
        sendChanged(context, success, message, effect);
        return success ? SINGLE_SUCCESS : 0;
    }

    private int missing(final CommandContext<CommandSourceStack> context, final Portal portal) {
        plugin.bundle().sendMessage(context.getSource().getSender(), "portal.particle.disabled",
                Placeholder.parsed("portal", portal.getName()));
        return 0;
    }

    private void sendChanged(final CommandContext<CommandSourceStack> context, final boolean success, final String message, final PortalParticleEffect effect) {
        final var portal = context.getArgument("portal", Portal.class);
        plugin.bundle().sendMessage(context.getSource().getSender(), success ? message : "nothing.changed",
                Placeholder.parsed("portal", portal.getName()),
                Placeholder.parsed("particle", effect.particle().name()),
                Placeholder.parsed("pattern", effect.pattern().name().toLowerCase()),
                Placeholder.parsed("count", String.valueOf(effect.count())),
                Placeholder.parsed("interval", String.valueOf(effect.intervalTicks())),
                Placeholder.parsed("radius", String.valueOf(effect.viewDistance())),
                Placeholder.parsed("force", String.valueOf(effect.force())),
                Placeholder.parsed("offset_x", String.valueOf(effect.offsetX())),
                Placeholder.parsed("offset_y", String.valueOf(effect.offsetY())),
                Placeholder.parsed("offset_z", String.valueOf(effect.offsetZ())),
                Placeholder.parsed("extra", String.valueOf(effect.extra())),
                Placeholder.parsed("data", PortalParticleData.describe(effect.data())));
    }

    private boolean validateCount(final CommandContext<CommandSourceStack> context, final int count) {
        return validateLimit(context, count <= plugin.config().maxParticleCount(), "count", plugin.config().maxParticleCount());
    }

    private boolean validateInterval(final CommandContext<CommandSourceStack> context, final int interval) {
        return validateLimit(context, interval >= plugin.config().minParticleIntervalTicks(), "intervalTicks", plugin.config().minParticleIntervalTicks());
    }

    private boolean validateRadius(final CommandContext<CommandSourceStack> context, final int radius) {
        return validateLimit(context, radius <= plugin.config().maxParticleViewDistance(), "radius", plugin.config().maxParticleViewDistance());
    }

    private boolean validateLimit(final CommandContext<CommandSourceStack> context, final boolean valid, final String setting, final int limit) {
        if (valid) return true;
        plugin.bundle().sendMessage(context.getSource().getSender(), "portal.particle.limit",
                Placeholder.parsed("setting", setting),
                Placeholder.parsed("limit", String.valueOf(limit)));
        return false;
    }

    private static PortalParticleEffect copy(
            final PortalParticleEffect effect,
            final int count,
            final int intervalTicks,
            final PortalParticlePattern pattern,
            final int viewDistance,
            final boolean force,
            final double offsetX,
            final double offsetY,
            final double offsetZ,
            final double extra,
            final @Nullable Object data
    ) {
        return new PortalParticleEffect(effect.particle(), pattern, count, intervalTicks, offsetX, offsetY, offsetZ, extra, viewDistance, force, data);
    }

    private static Color color(final TextColor color, final int alpha) {
        return Color.fromARGB(alpha, color.red(), color.green(), color.blue());
    }
}
