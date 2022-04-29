package me.doublenico.bedstom;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.doublenico.bedstom.api.ChatColor;
import me.doublenico.bedstom.api.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.ping.ResponseData;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public final class BedStom {
    private static final List<Player> players = new ArrayList<>();
    public static final List<CustomCommand> commands = new ArrayList<>();
    public static final HashMap<String,CustomRank> customRanks = new HashMap<>();

    public static void main(String[] str) throws IOException {
        final JsonObject properties = StringUtils.readJson("properties.json","{\"maxPlayers\": 1000,\"motd\": \"Line 1\nLine 2\"}");
        final JsonObject ranks = StringUtils.readJson("ranks.json","{\"data\": []}");
        final JsonObject playerData = StringUtils.readJson("playerData.json","{}");
        for (JsonElement je : ranks.get("data").getAsJsonArray()) {
            String id = je.getAsJsonObject().get("id").getAsString();
            if (customRanks.containsKey(id)) throw new IllegalArgumentException("Rank with id: " + id + "Already registered");
            String name = je.getAsJsonObject().get("id").getAsString();
            String rankColor = je.getAsJsonObject().get("rankColor").getAsString();
            String nameColor = je.getAsJsonObject().get("nameColor").getAsString();
            String messageColor = je.getAsJsonObject().get("messageColor").getAsString();
            int tabPriority = je.getAsJsonObject().get("tabPriority").getAsInt();
            List<String> perms = new ArrayList<>();
            je.getAsJsonObject().get("permissions").getAsJsonArray().forEach(perm -> perms.add(perm.getAsString()));
            customRanks.put(id,new CustomRank(name,rankColor,nameColor,messageColor,tabPriority,perms));
        }
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));
        GlobalEventHandler eh = MinecraftServer.getGlobalEventHandler();
        eh.addListener(PlayerLoginEvent.class, e -> {
            final Player p = e.getPlayer();
            e.setSpawningInstance(instanceContainer);
            p.setRespawnPoint(new Pos(0, 42, 0));
            p.setSkin(PlayerSkin.fromUuid(p.getUuid().toString()));
            players.add(p);
            broadcast("&8[&a+&8]&r " + p.getUsername());
        });
        eh.addListener(PlayerDisconnectEvent.class, e -> {
            final Player p = e.getPlayer();
            players.remove(p);
            broadcast("&8[&c-&8]&r " + p.getUsername());
        });
        eh.addListener(ServerListPingEvent.class, e -> {
            ResponseData rd = new ResponseData();
            rd.setMaxPlayer(properties.get("maxPlayers").getAsInt());
            rd.setOnline(players.size());
            rd.setProtocol(758);
            //ChatColor.translateAlternateColorCodes("             &6&LBed&f&lStom &e&lServer").append(ChatColor.translateAlternateColorCodes("\n       &8One of the First Bedwars in Minestom")
            rd.setDescription(ChatColor.translateAlternateColorCodes(properties.get("motd").getAsString()));
            e.setResponseData(rd);
        });

        new CustomCommand("gamemode").addAlias("gm").addSyntax((sender,context) -> {
            if (sender instanceof Player p) {
                switch ((String) context.get("gamemode")) {
                    case "0", "s", "survival" -> p.setGameMode(GameMode.SURVIVAL);
                    case "1", "c", "creative" -> p.setGameMode(GameMode.CREATIVE);
                    case "2", "a", "adventure" -> p.setGameMode(GameMode.ADVENTURE);
                    case "3", "sp", "spectator" -> p.setGameMode(GameMode.SPECTATOR);
                }
                p.sendMessage(ChatColor.translateAlternateColorCodes("&aSuccessfully changed your gamemode to " + p.getGameMode().toString().toLowerCase() + "."));
            }
        }, List.of(ArgumentType.String("gamemode"))).register((sender, context) -> {
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes("&aYour current gamemode is: " + StringUtils.capitaliteFirst(((Player)sender).getGameMode().toString())));
            }
        });

        new CustomCommand("broadcast").addAlias("bc").addSyntax((sender,context) -> Audiences.server().sendMessage(ChatColor.translateAlternateColorCodes("&8[&Broadcast&8]&r " + context.get("message"))),List.of(ArgumentType.String("message"))).register((sender,context) -> {});


        commands.forEach(c -> {
            Command cmd = new Command(c.getName(),c.getAliases().toArray(new String[0]));
            cmd.setDefaultExecutor((sender, context) -> c.getDefaultConsumer().accept(sender, context));
            for (BiConsumer<CommandSender, CommandContext> key : c.getSyntaxes().keySet()) {
                cmd.addSyntax(key::accept,c.getSyntaxes().get(key));
            }
            MinecraftServer.getCommandManager().register(cmd);
        });
        minecraftServer.start("0.0.0.0", 25565);
    }

    public static void broadcast(String msg) {
        players.forEach(p -> p.sendMessage(ChatColor.translateAlternateColorCodes(msg)));
    }
    public static void broadcast(String msg, List<Player> players) {
        players.forEach(p -> p.sendMessage(ChatColor.translateAlternateColorCodes(msg)));
    }

    /**
     * @param title The title string with chatcolors included
     * @param subtitle The subtitle string with chatcolors included
     * @param fadeIn The time in seconds to fade in the title
     * @param stay The time in seconds to stay on the title
     * @param fadeOut The time in seconds to fade out the title
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ChatColor.translateAlternateColorCodes(title);
        Component subtitleComponent = ChatColor.translateAlternateColorCodes(subtitle);
        Title.Times times = Title.Times.times(Duration.ofSeconds(fadeIn), Duration.ofSeconds(stay), Duration.ofSeconds(fadeOut));
        Title mainTitle = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(mainTitle);
    }

    /**
     *
     * @param player The player to send the title to
     * @param title The title string with chatcolors included
     * @param subtitle The subtitle string with chatcolors included
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        Component titleComponent = ChatColor.translateAlternateColorCodes(title);
        Component subtitleComponent = ChatColor.translateAlternateColorCodes(subtitle);
        Title mainTitle = Title.title(titleComponent, subtitleComponent);
        player.showTitle(mainTitle);
    }

    /**
     *
     * @param player The player to send the title to
     * @param message The actionbar message string with chatcolors included
     */
    public static void sendActionBar(Player player, String message) {
        Component messageComponent = ChatColor.translateAlternateColorCodes(message);
        player.sendActionBar(messageComponent);
    }

    public static String formatPlayername(Player p) {

        return "";
    }

    public static List<Player> getOnlinePlayers() {
        return players;
    }
}
