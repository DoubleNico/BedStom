package me.doublenico.bedstom;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomCommand {
    private String name;
    private List<String> aliases;
    private BiConsumer<CommandSender,CommandContext> defaultLambda;
    private HashMap<BiConsumer<CommandSender,CommandContext>,Argument<?>[]> syntaxes;
    private List<Argument<?>> arguments;

    private boolean registered;
    public CustomCommand(String name) {
        this.name = name;
        this.arguments = new ArrayList<>();
        this.aliases = new ArrayList<>();
        this.syntaxes = new HashMap<>();
    }
    public CustomCommand addAlias(String alias) {
        if (registered) throw new IllegalCallerException("This command is already registered");
        aliases.add(alias);
        return this;
    }
    public CustomCommand addSyntax(BiConsumer<CommandSender, CommandContext> consumer, List<Argument<?>> arguments) {
        syntaxes.put(consumer,arguments.toArray(new Argument<?>[0]));
        return this;
    }
    public void register(BiConsumer<CommandSender,CommandContext> defaultLambda) {
        registered = true;
        this.defaultLambda = defaultLambda;
        BedStom.commands.add(this);
    }
    public String getName() { return name; }
    public List<String> getAliases() {return aliases; }
    public BiConsumer<CommandSender,CommandContext> getDefaultConsumer() { return defaultLambda; }
    public HashMap<BiConsumer<CommandSender,CommandContext>,Argument<?>[]> getSyntaxes() { return syntaxes; }
}
