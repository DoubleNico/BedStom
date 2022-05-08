package me.doublenico.bedstom;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomCommand {
    private String name;
    private String permission;
    private List<String> aliases;
    private BiConsumer<CommandSender,CommandContext> defaultLambda;
    private HashMap<BiConsumer<CommandSender,CommandContext>, Map.Entry<Argument<?>[],String>> syntaxes;
    private boolean registered;
    public CustomCommand(String name) {
        this.name = name;
        this.permission = "";
        this.aliases = new ArrayList<>();
        this.syntaxes = new HashMap<>();
    }
    public CustomCommand addAlias(String alias) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        aliases.add(alias);
        return this;
    }
    public CustomCommand addSyntax(BiConsumer<CommandSender, CommandContext> consumer, List<Argument<?>> arguments) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        syntaxes.put(consumer,new AbstractMap.SimpleEntry<>(arguments.toArray(new Argument<?>[0]),""));
        return this;
    }
    public CustomCommand addSyntax(BiConsumer<CommandSender, CommandContext> consumer, List<Argument<?>> arguments, String permission) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        syntaxes.put(consumer,new AbstractMap.SimpleEntry<>(arguments.toArray(new Argument<?>[0]),permission));
        return this;
    }
    public CustomCommand setDefaultPermission(String permission) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        this.permission = permission;
        return this;
    }
    public void register(BiConsumer<CommandSender,CommandContext> defaultLambda) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        registered = true;
        this.defaultLambda = defaultLambda;
        BedStom.commands.add(this);
    }
    public String getName() { return name; }
    public List<String> getAliases() { return aliases; }
    public String getPermission() { return permission; }
    public BiConsumer<CommandSender,CommandContext> getDefaultConsumer() { return defaultLambda; }
    public HashMap<BiConsumer<CommandSender,CommandContext>, Map.Entry<Argument<?>[],String>> getSyntaxes() { return syntaxes; }
}
