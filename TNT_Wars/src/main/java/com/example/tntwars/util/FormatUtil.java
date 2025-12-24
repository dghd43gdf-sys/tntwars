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

    public static Component infoBold(String message) {
        return Component.text(message, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD);
    }

    public static Component success(String message) {
        return Component.text(message, NamedTextColor.GREEN);
    }

    public static Component successBold(String message) {
        return Component.text(message, NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
    }

    public static Component error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    public static Component errorBold(String message) {
        return Component.text(message, NamedTextColor.RED).decorate(TextDecoration.BOLD);
    }

    public static Component highlight(String message) {
        return Component.text(message, NamedTextColor.GOLD);
    }

    public static Component highlightBold(String message) {
        return Component.text(message, NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    }

    public static Component gray(String message) {
        return Component.text(message, NamedTextColor.GRAY);
    }

    public static Component teamAssignment(TeamColor team) {
        return Component.text("Du bist in Team ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.YELLOW));
    }

    public static Component teamSwitch(TeamColor team) {
        return Component.text("Du spielst jetzt für Team ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.YELLOW));
    }

    public static Component playerTeamSwitch(String playerName, TeamColor team) {
        return Component.text(playerName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" wurde zu Team ", NamedTextColor.YELLOW))
                .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text(" verschoben.", NamedTextColor.YELLOW));
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
        return Component.text(playerName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" ist gestorben", NamedTextColor.RED));
    }

    public static Component victoryBroadcast(TeamColor winner) {
        return Component.text("Team ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(winner.getDisplay(), winner.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text(" gewinnt!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    }

    public static Component victorySubtitle(TeamColor winner) {
        return Component.text("Team ", winner.getNamedColor())
                .decorate(TextDecoration.BOLD)
                .append(Component.text(winner.getDisplay(), winner.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text(" hat gewonnen!", winner.getNamedColor()).decorate(TextDecoration.BOLD));
    }

    public static Component gameStart() {
        return Component.text("TNT Wars startet jetzt!", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
    }

    public static Component worldReset() {
        return Component.text("Arena wird zurückgesetzt...", NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);
    }
}
