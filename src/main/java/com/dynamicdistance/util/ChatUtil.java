package com.dynamicdistance.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String message) {
        if (message == null) {
            return "";
        }

        try {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String color = matcher.group(1);
                StringBuilder replacement = new StringBuilder("§x");
                for (char c : color.toCharArray()) {
                    replacement.append('§').append(c);
                }
                matcher.appendReplacement(buffer, replacement.toString());
            }

            message = matcher.appendTail(buffer).toString();
        } catch (Throwable ignored) {
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}