package com.example.tntwars.util;

import com.example.tntwars.model.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;

public final class FormatUtil {
    private static final ChatColor[] BLANK_COLORS = {
            ChatColor.BLACK,
            ChatColor.DARK_BLUE,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_RED,
            ChatColor.DARK_PURPLE,
            ChatColor.GOLD
    };

    private FormatUtil() {
    }

    public static Component info(String message) {
        return Component.text(message, NamedTextColor.YELLOW);
    }

    public static Component success(String message) {
        return Component.text(message, NamedTextColor.GREEN);
    }

    public static Component error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    public static Component highlight(String message) {
        return Component.text(message, NamedTextColor.GOLD);
    }

    public static String coloredTeamName(String base, ChatColor chatColor) {
        return chatColor + base + ChatColor.RESET;
    }

    public static Component tntTitleComponent(String text) {
        TextComponent.Builder builder = Component.text();
        boolean redTurn = true;
        for (char ch : text.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                builder.append(Component.space());
                continue;
            }
            NamedTextColor color = redTurn ? NamedTextColor.RED : NamedTextColor.WHITE;
            builder.append(Component.text(String.valueOf(ch), color).decorate(TextDecoration.BOLD));
            redTurn = !redTurn;
        }
        return builder.build();
    }

    public static String tntTitleLegacy(String text) {
        StringBuilder builder = new StringBuilder();
        boolean redTurn = true;
        for (char ch : text.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                builder.append(ChatColor.RESET).append(' ');
                continue;
            }
            ChatColor color = redTurn ? ChatColor.RED : ChatColor.WHITE;
            builder.append(color).append(ChatColor.BOLD).append(ch);
            redTurn = !redTurn;
        }
        builder.append(ChatColor.RESET);
        return builder.toString();
    }

    public static String teamCountLine(TeamColor team, int count) {
        return team.getChatColor() + "" + ChatColor.BOLD + team.getDisplay() + ChatColor.RESET
                + ChatColor.WHITE + ": " + ChatColor.GOLD + count;
    }

    public static String blankLine(int index) {
        ChatColor color = BLANK_COLORS[index % BLANK_COLORS.length];
        return ChatColor.RESET.toString() + color;
    }

    public static Component deathMessage(String playerName) {
        return Component.text(playerName + " ist gestorben", NamedTextColor.RED);
    }

    public static Component victoryBroadcast(TeamColor winner) {
        return Component.text("Team ")
                .append(Component.text(winner.getDisplay(), winner.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text(" gewinnt ", NamedTextColor.GOLD))
                .append(tntTitleComponent("TNT Wars"))
                .append(Component.text("!", winner.getNamedColor()));
    }

    public static Component victorySubtitle(TeamColor winner) {
        return Component.text("Team " + winner.getDisplay() + " hat gewonnen!", winner.getNamedColor())
                .decorate(TextDecoration.BOLD);
    }
}
