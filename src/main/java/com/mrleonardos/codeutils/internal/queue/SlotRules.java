package com.mrleonardos.codeutils.internal.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mrleonardos.codeutils.internal.Rights;
import com.mrleonardos.codeutils.internal.queue.QueueFile.Tier;

public final class SlotRules {

    public static final String NO_TIER = "";

    private final int base;
    private final List<Step> steps;

    private SlotRules(int base, List<Step> steps) {
        this.base = base;
        this.steps = Collections.unmodifiableList(steps);
    }

    public static SlotRules of(QueueFile file) {
        List<Step> steps = new ArrayList<>();
        for (Map.Entry<String, Tier> entry : file.slots.tiers.entrySet()) {
            Tier tier = entry.getValue();
            if (tier != null && tier.slots > 0) {
                steps.add(new Step(entry.getKey(), tier.node == null ? "" : tier.node.trim(), tier.slots));
            }
        }
        Collections.sort(
            steps,
            Comparator.comparingInt((Step step) -> step.slots)
                .reversed());
        return new SlotRules(Math.max(0, file.slots.base), steps);
    }

    public int base() {
        return base;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Step step : steps) {
            names.add(step.name);
        }
        return names;
    }

    public int slotsOf(String tier) {
        Step step = step(tier);
        return step == null ? 0 : step.slots;
    }

    public String nodeOf(String tier) {
        Step step = step(tier);
        return step == null ? "" : step.node;
    }

    public int rankOf(String tier) {
        for (int index = 0; index < steps.size(); index++) {
            if (steps.get(index).name.equals(tier)) {
                return steps.size() - index;
            }
        }
        return 0;
    }

    public int topSlots() {
        return steps.isEmpty() ? base : Math.max(base, steps.get(0).slots);
    }

    public String tierOf(UUID player, Rights rights) {
        for (Step step : steps) {
            if (!step.node.isEmpty() && rights.allowed(player, step.node)) {
                return step.name;
            }
        }
        return NO_TIER;
    }

    public int capOf(String tier) {
        Step step = step(tier);
        return step == null ? base : step.slots;
    }

    private Step step(String tier) {
        for (Step step : steps) {
            if (step.name.equals(tier)) {
                return step;
            }
        }
        return null;
    }

    private static final class Step {

        private final String name;
        private final String node;
        private final int slots;

        private Step(String name, String node, int slots) {
            this.name = name;
            this.node = node;
            this.slots = slots;
        }
    }
}
