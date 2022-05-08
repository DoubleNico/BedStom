package me.doublenico.bedstom.api;

import me.doublenico.bedstom.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.awt.*;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatColor {
        private static final String colorCharacter = "&";
        public static String deprecatedHex = "("+ colorCharacter + "<(#[A-Fa-f0-9]{6})>)";
        public static String gradient = "(<(#[A-Fa-f0-9]{6});(#[A-Fa-f0-9]{6})>)";
        public static String chatColor = "&([0-9a-fk-or])";
        public static String hex = "&(#[A-Fa-f0-9]{6})";
        public static String multipleGradient = "<((?:#[a-fA-F0-9]{6};#[a-fA-F0-9]{6}[^>]?)+)>";

    /**
     * @param message The message to send with chatcolors included
     * @return A component with the message and chatcolors
     */
    public static Component translateAlternateColorCodes(String message) {
        Component out = Component.text(message);
        for (String s : message.split(colorCharacter)) {
            if (s.equalsIgnoreCase("")) continue;
            Component replaced = Component.text("");
            final String s2 = colorCharacter + Pattern.compile("[{}()\\[\\].+*?^$\\\\|]").matcher(s).replaceAll("\\\\$0");
            Matcher g = Pattern.compile(gradient).matcher(s);
            boolean found = false;
            while (g.find() && !found) {
                s = s.replaceFirst(g.group(1),"");
                int looped = 0;
                int curSegment = 0;
                List<String> segments = new ArrayList<>();
                for (char c : s.toCharArray()) {
                    if (segments.size() > curSegment) {
                        segments.set(curSegment,(segments.get(curSegment) + c));
                    } else {
                        segments.add(c + "");
                    }
                    looped++;
                    if (looped % 33 == 0) { //Whatever value you want +1
                        curSegment++;
                    }
                }
                Color color1 = Color.decode(g.group(2));
                Color color2 = Color.decode(g.group(3));
                for (String segment : segments) {
                    for (int i = 0; i < segment.length(); i++) {
                        float ratio = (float) i / (float) segment.length();
                        int red = (int) (color2.getRed() * ratio + color1.getRed() * (1 - ratio));
                        int green = (int) (color2.getGreen() * ratio + color1.getGreen() * (1 - ratio));
                        int blue = (int) (color2.getBlue() * ratio + color1.getBlue() * (1 - ratio));
                        replaced = replaced.append(Component.text(segment.toCharArray()[i]).color(TextColor.color(red, green, blue)));
                    }
                    Color old1 = color1;
                    color1 = color2;
                    color2 = old1;
                }
                found = true;
                System.out.println(GsonComponentSerializer.gson().serialize(replaced));
            }
            Matcher c = Pattern.compile(chatColor).matcher(colorCharacter + s);
            while (c.find() &&!found) {
                s = s.replaceFirst(c.group(1),"");
                replaced = replaced.append(LegacyComponentSerializer.legacyAmpersand().deserialize(colorCharacter + c.group(1) + s));
                found = true;
            }
            Matcher h = Pattern.compile(hex).matcher(colorCharacter + s);
            while (h.find() && !found) {
                s = s.replaceFirst(h.group(1),"");
                replaced = replaced.append(LegacyComponentSerializer.legacyAmpersand().deserialize(colorCharacter + h.group(1) + s));
                found = true;
            }
            Component finalReplaced = replaced;
            if (!replaced.equals(Component.empty())) {
                out = out.replaceText(b -> {
                    b.match(s2).replacement(finalReplaced);
                });
            }
        }
        return out;
    }


    //TODO: Add support for other color codes
    // Multi Color gradient
    public static Component translateGradient(String message) {
        Pattern pattern = Pattern.compile(multipleGradient);
        Matcher matcher = pattern.matcher(message);
        StringBuilder sb = new StringBuilder();
        Component text = Component.empty();
        while (matcher.find()) {
            String gradient = matcher.group(1);
            List<Color> hexes = new ArrayList<>();
            for (String color : gradient.split(";")) hexes.add(fromHex(color));
            message = message.replace(matcher.group(0), "");
            List<Color> convertedGradient = createGradient(message.length(), hexes);
            int i = 0;
            for (char c : message.toCharArray()) {
                if (convertedGradient.size() > i) {
                    text = text.append(Component.text(c).color(TextColor.color(convertedGradient.get(i).getRGB())));
                    i++;
                }
            }
        }
        return text;
    }

    // Create a gradient from a list of colors
    public static List<Color> createGradient(int length, List<Color> colors) {
        if (colors.size() < 1) return null;
        List<Color> gradient = new ArrayList<>();
        Color start = colors.get(0);
        length /= colors.size();
        for(Color color : colors) {
            for(int i = 0; i < length; i++) {
                float ratio = (float) i / (float) length;
                int red = (int) (color.getRed() * ratio + start.getRed() * (1 - ratio));
                int green = (int) (color.getGreen() * ratio + start.getGreen() * (1 - ratio));
                int blue = (int) (color.getBlue() * ratio + start.getBlue() * (1 - ratio));
                if (!gradient.contains(new Color(red, green, blue))) gradient.add(new Color(red, green, blue));
            }
            start = color;
        }
        return gradient;
    }

    public static Color fromHex(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        return new Color(Integer.parseInt(hex, 16));
    }

    /**
     * @param component The message in a component, if it contains a color code it will be converted to a chatcolor
     * @return A string format of the component with chatcolors included
     */
    public static String translateAlternateColorCodes(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    public static String trimColors(String message) {
        Matcher g = Pattern.compile(gradient).matcher(message);
        while (g.find()) {
            message = message.replaceAll(colorCharacter + g.group(1),"");
        }
        Matcher h = Pattern.compile(hex).matcher(message);
        while (h.find()) {
            message = message.replaceAll(colorCharacter + h.group(1),"");
        }
        Matcher c = Pattern.compile(chatColor).matcher(message);
        while (c.find()) {
            message = message.replaceAll(colorCharacter + c.group(1),"");
        }
        return message;
    }
}
