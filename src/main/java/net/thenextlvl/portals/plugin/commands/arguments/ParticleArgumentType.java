package net.thenextlvl.portals.plugin.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@NullMarked
public final class ParticleArgumentType implements CustomArgumentType.Converted<Particle, String> {
    @Override
    public Particle convert(final String nativeType) throws CommandSyntaxException {
        final var normalized = nativeType.toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Particle.valueOf(normalized);
        } catch (final IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(
                    MessageComponentSerializer.message().serialize(Component.text("Unknown particle: " + nativeType))
            ).create();
        }
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        Arrays.stream(Particle.values())
                .map(Particle::name)
                .map(String::toLowerCase)
                .map(name -> name.replace('_', '-'))
                .filter(name -> name.contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
