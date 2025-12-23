package com.example.tntwars.model;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

public enum TeamColor {
    RED("Rot", ChatColor.RED, NamedTextColor.RED),
    BLUE("Blau", ChatColor.BLUE, NamedTextColor.BLUE);

    private final String display;
    private final ChatColor chatColor;
    private final NamedTextColor namedColor;

    TeamColor(String display, ChatColor chatColor, NamedTextColor namedColor) {
        this.display = display;
        this.chatColor = chatColor;
        this.namedColor = namedColor;
    }

    public String getDisplay() {
        return display;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public NamedTextColor getNamedColor() {
        return namedColor;
    }

    public static TeamColor fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.startsWith("rot") || normalized.startsWith("red")) {
            return RED;
        }
        if (normalized.startsWith("blau") || normalized.startsWith("blue")) {
            return BLUE;
        }
        return null;
    }
}
