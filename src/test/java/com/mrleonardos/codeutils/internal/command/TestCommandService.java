package com.mrleonardos.codeutils.internal.command;

import java.util.ArrayList;
import java.util.List;

import com.mrleonardos.codecore.api.command.CommandNode;
import com.mrleonardos.codecore.api.command.CommandService;

final class TestCommandService implements CommandService {

    private final List<CommandNode> roots = new ArrayList<>();

    @Override
    public void register(CommandNode root) {
        roots.add(root);
    }

    List<CommandNode> roots() {
        return roots;
    }

    List<String> names() {
        List<String> names = new ArrayList<>();
        for (CommandNode root : roots) {
            names.add(root.name());
        }
        return names;
    }

    CommandNode root(String name) {
        for (CommandNode root : roots) {
            if (root.name()
                .equals(name)) {
                return root;
            }
        }
        return null;
    }

    static CommandNode child(CommandNode parent, String name) {
        for (CommandNode child : parent.children()) {
            if (child.name()
                .equals(name)) {
                return child;
            }
        }
        return null;
    }

    static List<String> childNames(CommandNode parent) {
        List<String> names = new ArrayList<>();
        for (CommandNode child : parent.children()) {
            names.add(child.name());
        }
        return names;
    }
}
