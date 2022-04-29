package me.doublenico.bedstom.api;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatColor {

       public static String hex = "(<(#[A-Fa-f0-9]{6})>)";

    /**
     * @param message The message to send with chatcolors included
     * @return A component with the message and chatcolors
     */
    public static Component translateAlternateColorCodes(String message) {
        // This is a &6message <#123456>Another Message &4Moree messages
        Component component = Component.text(message);
        Matcher matcher = Pattern.compile(hex).matcher(message);
        while (matcher.find()){
            System.out.println(matcher.group(2)); // prints the exact color code
            if (hex.isEmpty()) continue;
            String hex = matcher.group(2); // The hex color code
            component = component.color(TextColor.fromHexString(hex));
            component = component.replaceText(TextReplacementConfig.builder().match(matcher.group(2)).replacement("").build());
        }
        component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        return component;
    }

    /**
     * @param component The message in a component, if it contains a color code it will be converted to a chatcolor
     * @return A string format of the component with chatcolors included
     */
    public static String translateAlternateColorCodes(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }
}
