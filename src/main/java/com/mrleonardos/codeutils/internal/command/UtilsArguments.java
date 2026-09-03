package com.mrleonardos.codeutils.internal.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.mrleonardos.codecore.api.command.ArgumentType;
import com.mrleonardos.codecore.api.command.CommandSender;

public final class UtilsArguments {

    private final Supplier<List<String>> sets;
    private final Supplier<List<String>> jobs;

    public UtilsArguments(Supplier<List<String>> sets, Supplier<List<String>> jobs) {
        this.sets = sets;
        this.jobs = jobs;
    }

    public ArgumentType<String> setName() {
        return named(sets);
    }

    public ArgumentType<String> jobName() {
        return named(jobs);
    }

    public ArgumentType<String> restartWhen() {
        return named(() -> Arrays.asList("cancel", "now"));
    }

    private static ArgumentType<String> named(Supplier<List<String>> names) {
        return new ArgumentType<String>() {

            @Override
            public String parse(String raw) {
                return raw;
            }

            @Override
            public List<String> suggestions(CommandSender sender, String partial) {
                return startingWith(names.get(), partial);
            }
        };
    }

    private static List<String> startingWith(List<String> candidates, String partial) {
        String prefix = partial.toLowerCase(Locale.ROOT);
        List<String> matching = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT)
                .startsWith(prefix)) {
                matching.add(candidate);
            }
        }
        return matching;
    }
}
