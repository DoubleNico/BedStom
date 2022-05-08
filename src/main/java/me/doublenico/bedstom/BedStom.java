package me.doublenico.bedstom;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.reflect.jvm.internal.pcollections.HashPMap;
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
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.permission.Permission;
import net.minestom.server.permission.PermissionHandler;
import net.minestom.server.ping.ResponseData;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class BedStom {
    private static final List<Player> players = new ArrayList<>();
    public static final List<CustomCommand> commands = new ArrayList<>();
    public static final HashMap<String,CustomRank> customRanks = new HashMap<>();
    private static JsonObject playerData;

    public static void main(String[] str) throws IOException {
        final JsonObject properties = StringUtils.readJson("properties.json","{\"maxPlayers\": 1000,\"motd\": \"Line 1\nLine 2\", \"noPermMessage\": \"&cYou don't have permission to use this command.\"}");
        final JsonObject ranks = StringUtils.readJson("ranks.json","{\"data\": []}");
        playerData = StringUtils.readJson("playerData.json","{}");
        for (JsonElement je : ranks.get("data").getAsJsonArray()) {
            String id = je.getAsJsonObject().get("id").getAsString();
            if (customRanks.containsKey(id)) throw new IllegalArgumentException("Rank with id: " + id + "Already registered");
            String name = je.getAsJsonObject().get("name").getAsString();
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
            p.setDisplayName(ChatColor.translateAlternateColorCodes("&#" + String.format("%06d", getRankById(getRankOfPlayer(p.getUuid())).tabPriority())).append(formatPlayername(p)));
            e.setSpawningInstance(instanceContainer);

            p.setRespawnPoint(new Pos(0, 42, 0));
            p.setSkin(PlayerSkin.fromUuid(p.getUuid().toString()));
            players.add(p);
            Audiences.server().sendMessage(ChatColor.translateGradient("<#2AFF00;#327334;#1C491D;#052506>This is a multicolor gradient oh my god love it so much"));
            Audiences.server().sendMessage(ChatColor.translateAlternateColorCodes("&8[&a+&8]&r ").append(formatPlayername(p)));
            for (String perm : getRankById(getRankOfPlayer(p.getUuid())).permissions()) {
                p.addPermission(new Permission(perm));
            }
        });
        eh.addListener(PlayerDisconnectEvent.class, e -> {
            final Player p = e.getPlayer();
            players.remove(p);
            Audiences.server().sendMessage(ChatColor.translateAlternateColorCodes("&8[&c-&8]&r ").append(formatPlayername(p)));
        });
        eh.addListener(PlayerChatEvent.class, e -> {
            if (ChatColor.trimColors(e.getMessage()).equalsIgnoreCase("")) {
                e.setCancelled(true);
                return;
            }
            e.setChatFormat(ev -> {
                if (!hasPermission(ev.getPlayer(),"chat.colors")) ev.setMessage(ChatColor.trimColors(ev.getMessage()));
                Component msg = ChatColor.translateAlternateColorCodes(getRankById(getRankOfPlayer(ev.getPlayer().getUuid())).messageColor() + "" + ev.getMessage());
                return formatPlayername(ev.getPlayer()).append(Component.text("» ").append(msg));
            });
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

        new CustomCommand("gamemode").addAlias("gm").setDefaultPermission("commands.creative").addSyntax((sender,context) -> {
            if (sender instanceof Player p) {
                switch ((String) context.get("gamemode")) {
                    case "0", "s", "survival" -> p.setGameMode(GameMode.SURVIVAL);
                    case "1", "c", "creative" -> p.setGameMode(GameMode.CREATIVE);
                    case "2", "a", "adventure" -> p.setGameMode(GameMode.ADVENTURE);
                    case "3", "sp", "spectator" -> p.setGameMode(GameMode.SPECTATOR);
                    default -> {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes("&cInvalid gamemode: " + context.get("gamemode")));
                        return;
                    }
                }

                p.sendMessage(ChatColor.translateAlternateColorCodes("&aSuccessfully changed your gamemode to " + p.getGameMode().toString().toLowerCase() + "."));
            }
        }, List.of(ArgumentType.String("gamemode"))).addSyntax((sender,context) -> {
            Player p = getPlayer((String)context.get("player"));
            if (p == null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes("&cInvalid player: " + context.get("player")));
                return;
            }
            switch ((String) context.get("gamemode")) {
                case "0", "s", "survival" -> p.setGameMode(GameMode.SURVIVAL);
                case "1", "c", "creative" -> p.setGameMode(GameMode.CREATIVE);
                case "2", "a", "adventure" -> p.setGameMode(GameMode.ADVENTURE);
                case "3", "sp", "spectator" -> p.setGameMode(GameMode.SPECTATOR);
                default -> sender.sendMessage(ChatColor.translateAlternateColorCodes("&cInvalid gamemode: " + context.get("gamemode")));
            }
        },List.of(ArgumentType.String("gamemode"),ArgumentType.String("player")), "commands.gamemode.other").register((sender, context) -> {
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes("&aYour current gamemode is: " + StringUtils.capitaliteFirst(((Player)sender).getGameMode().toString())));
            }
        });

        new CustomCommand("broadcast").addAlias("bc").setDefaultPermission("commands.broadcast").addSyntax((sender,context) -> Audiences.server().sendMessage(ChatColor.translateAlternateColorCodes("&8[&cBroadcast&8]&r " + String.join(" ", context.get(ArgumentType.StringArray("message"))))),List.of(ArgumentType.StringArray("message"))).register((sender,context) -> {});

        HashMap<Player,InstanceContainer> currentlyEditing = new HashMap<>();

        new CustomCommand("createWorld").addAlias("cw").setDefaultPermission("commands.createworld").addSyntax((sender,context) -> {
            InstanceContainer ic = instanceManager.createInstanceContainer();
            ic.setGenerator(unit -> {});
            ic.setBlock(0,0,0,Block.STONE);
            ic.setChunkLoader(new AnvilLoader("./maps/" + context.get("name")));
            currentlyEditing.put((Player)sender,ic);
            ((Player)sender).setInstance(ic,new Pos(0.5f,1f,0.5f));
            ((Player)sender).setGameMode(GameMode.CREATIVE);
        }, List.of(ArgumentType.String("name"))).register((sender,context) -> {

        });

        new CustomCommand("saveWorld").addAlias("sw").setDefaultPermission("commands.saveworld").addSyntax((sender,context) -> {
            if (currentlyEditing.containsKey((Player)sender)) {
                currentlyEditing.get((Player)sender).saveChunksToStorage();
                sender.sendMessage("Done");
            }
        }, List.of()).register((sender,context) -> {

        });

        commands.forEach(c -> {
            Command cmd = new Command(c.getName(),c.getAliases().toArray(new String[0]));
            cmd.setDefaultExecutor((sender, context) -> {
                if (hasPermission(sender,c.getPermission()) || c.getPermission().equalsIgnoreCase("")) {
                    c.getDefaultConsumer().accept(sender, context);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes(properties.get("noPermMessage").getAsString()));
                }
            });
            c.getSyntaxes().keySet().forEach(key -> cmd.addSyntax((sender, context) -> {
                String perm = c.getSyntaxes().get(key).getValue();
                if (perm.equalsIgnoreCase("") && hasPermission(sender,c.getPermission()) || hasPermission(sender,perm)) {
                    key.accept(sender,context);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes(properties.get("noPermMessage").getAsString()));
                }
            },c.getSyntaxes().get(key).getKey()));
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
    public static Player getPlayer(String name) {
        for (Player p : getOnlinePlayers()) if (p.getUsername().equalsIgnoreCase(name)) return p;
        return null;
    }
    public static Player getPlayer(UUID uuid) {
        for (Player p : getOnlinePlayers()) if (p.getUuid() == uuid) return p;
        return null;
    }

    /**
     *
     * @param handler The handler to check for.
     * @param permission The permission to check for.
     * @return If the handler has said permission or its *
     */
    public static boolean hasPermission(PermissionHandler handler, String permission) { return handler.hasPermission("*") || handler.hasPermission(permission); }

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

    /**
     *
     * @param p the player to format the name of.
     * @return the name of the player with the prefix.
     */
    public static Component formatPlayername(Player p) {
        CustomRank r = getRankById(getRankOfPlayer(p.getUuid()));
        return ChatColor.translateAlternateColorCodes("&8[" + r.rankColor() + r.name() + "&8] " + r.nameColor() + p.getUsername());
    }

    public static CustomRank getRankById(String id) {
        return customRanks.getOrDefault(id,null);
    }

    public static String getRankOfPlayer(UUID id) {
        return getPlayerData(id).get("rank").getAsString();
    }

    private static JsonObject getPlayerData(UUID id) {
        if (playerData.has(id.toString())) {
            return playerData.get(id.toString()).getAsJsonObject();
        } else {
            JsonObject pd = new JsonObject();
            pd.addProperty("rank","owner");
            playerData.add(id.toString(),pd);
            return pd;
        }
    }

    public static List<Player> getOnlinePlayers() {
        return players;
    }
}
