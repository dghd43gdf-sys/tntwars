package com.example.tntwars.command;

import com.example.tntwars.game.GameManager;
import com.example.tntwars.model.Cuboid;
import com.example.tntwars.model.MapArena;
import com.example.tntwars.model.TeamColor;
import com.example.tntwars.model.StoredLocation;
import com.example.tntwars.util.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class TNTWarsCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;

    public TNTWarsCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern benutzt werden.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Verwendung: /tntwars <add|info|welt|setspawn|setbeacon|wall|setteam>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> handleAdd(player, Arrays.copyOfRange(args, 1, args.length));
            case "info" -> handleInfo(player, Arrays.copyOfRange(args, 1, args.length));
            case "setspawn" -> handleSetSpawn(player, Arrays.copyOfRange(args, 1, args.length));
            case "setbeacon" -> handleSetBeacon(player, Arrays.copyOfRange(args, 1, args.length));
            case "wall" -> handleWall(player, Arrays.copyOfRange(args, 1, args.length));
            case "setteam" -> handleSetTeam(player, Arrays.copyOfRange(args, 1, args.length));
            default -> handleTeleport(player, sub);
        }
        return true;
    }

    private void handleTeleport(Player player, String worldName) {
        MapArena arena = gameManager.getArena(worldName)
                .orElse(null);
        if (arena == null) {
            player.sendMessage(FormatUtil.error("Diese Welt ist nicht für TNT Wars registriert. Nutze /tntwars add <welt>."));
            return;
        }

        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            world = Bukkit.createWorld(new org.bukkit.WorldCreator(arena.getWorldName()));
        }
        if (world == null) {
            player.sendMessage(FormatUtil.error("Diese Welt konnte nicht geladen werden."));
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(FormatUtil.success("Du wurdest in die Welt ")
                .append(Component.text(world.getName(), NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text(" teleportiert.", NamedTextColor.GREEN)));
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(FormatUtil.error("Verwendung: /tntwars add <welt>"));
            return;
        }
        String worldName = args[0];
        if (gameManager.hasArena(worldName)) {
            player.sendMessage(FormatUtil.error("Diese Welt ist bereits für TNT Wars registriert."));
            return;
        }
        if (!gameManager.worldFolderExists(worldName)) {
            player.sendMessage(FormatUtil.error("Es wurde kein Weltordner mit diesem Namen gefunden."));
            return;
        }
        if (!gameManager.registerArena(worldName)) {
            player.sendMessage(FormatUtil.error("Diese Welt ist bereits für TNT Wars registriert."));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
        }
        if (world == null) {
            player.sendMessage(FormatUtil.error("Die Welt konnte nicht geladen werden, obwohl der Ordner existiert."));
            return;
        }

        player.sendMessage(FormatUtil.success("Arena ")
                .append(Component.text(world.getName(), NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text(" wurde hinzugefügt.", NamedTextColor.GREEN)));
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(FormatUtil.error("Verwendung: /tntwars setspawn <rot|blau> [index]").append(Component.text(" (1-4)", NamedTextColor.GRAY)));
            return;
        }
        TeamColor team = TeamColor.fromString(args[0]);
        if (team == null) {
            player.sendMessage(FormatUtil.error("Unbekanntes Team. Nutze rot oder blau."));
            return;
        }
        Integer index = null;
        if (args.length >= 2) {
            try {
                int value = Integer.parseInt(args[1]) - 1;
                if (value < 0 || value > 3) {
                    player.sendMessage(FormatUtil.error("Index muss zwischen 1 und 4 liegen."));
                    return;
                }
                index = value;
            } catch (NumberFormatException exception) {
                player.sendMessage(FormatUtil.error("Der Index muss eine Zahl sein."));
                return;
            }
        }
        MapArena arena = gameManager.getOrCreateArena(player.getWorld().getName());
        arena.addSpawn(team, player.getLocation(), index);
        gameManager.saveArena(arena);
        player.sendMessage(FormatUtil.success("Spawn für Team ")
                .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                .append(Component.text(" gespeichert.", NamedTextColor.GREEN)));
    }

    private void handleSetBeacon(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(FormatUtil.error("Verwendung: /tntwars setbeacon <rot|blau> [index]"));
            return;
        }
        TeamColor team = TeamColor.fromString(args[0]);
        if (team == null) {
            player.sendMessage(FormatUtil.error("Unbekanntes Team. Nutze rot oder blau."));
            return;
        }
        Integer index = null;
        if (args.length >= 2) {
            try {
                int value = Integer.parseInt(args[1]) - 1;
                if (value < 0 || value > 3) {
                    player.sendMessage(FormatUtil.error("Index muss zwischen 1 und 4 liegen."));
                    return;
                }
                index = value;
            } catch (NumberFormatException exception) {
                player.sendMessage(FormatUtil.error("Der Index muss eine Zahl sein."));
                return;
            }
        }
        MapArena arena = gameManager.getOrCreateArena(player.getWorld().getName());
        Block beaconBlock = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (beaconBlock.getType() != Material.BEACON) {
            player.sendMessage(FormatUtil.error("Du musst direkt auf einem Beacon stehen, um ihn zu speichern."));
            return;
        }
        Location beaconLocation = beaconBlock.getLocation();
        arena.addBeacon(team, beaconLocation, index);
        gameManager.saveArena(arena);
        if (index != null) {
            player.sendMessage(FormatUtil.success("Beacon ")
                    .append(Component.text((index + 1), NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                    .append(Component.text(" für Team ", NamedTextColor.GREEN))
                    .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                    .append(Component.text(" gespeichert.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(FormatUtil.success("Beacon für Team ")
                    .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD))
                    .append(Component.text(" gespeichert.", NamedTextColor.GREEN)));
        }
    }

    private void handleWall(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(FormatUtil.error("Verwendung: /tntwars wall <x1,y1,z1> <x2,y2,z2>"));
            return;
        }
        Location first = parseCoordinate(player.getWorld(), args[0]);
        Location second = parseCoordinate(player.getWorld(), args[1]);
        if (first == null || second == null) {
            player.sendMessage(FormatUtil.error("Koordinaten konnten nicht gelesen werden."));
            return;
        }
        MapArena arena = gameManager.getOrCreateArena(player.getWorld().getName());
        arena.addWall(new Cuboid(player.getWorld().getName(), first.getBlockX(), first.getBlockY(), first.getBlockZ(), second.getBlockX(), second.getBlockY(), second.getBlockZ()));
        gameManager.saveArena(arena);
        player.sendMessage(FormatUtil.successBold("Schutzwand gespeichert."));
    }

    private void handleSetTeam(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(FormatUtil.error("Verwendung: /tntwars setteam <spieler> <rot|blau>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(FormatUtil.error("Spieler ")
                    .append(Component.text(args[0], NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                    .append(Component.text(" ist nicht online.", NamedTextColor.RED)));
            return;
        }
        TeamColor team = TeamColor.fromString(args[1]);
        if (team == null) {
            player.sendMessage(FormatUtil.error("Unbekanntes Team. Nutze rot oder blau."));
            return;
        }
        gameManager.switchPlayerTeam(player, target, team);
    }

    private void handleInfo(Player player, String[] args) {
        String worldName;
        if (args.length >= 1 && !args[0].isBlank()) {
            worldName = args[0];
        } else {
            worldName = player.getWorld().getName();
        }
        MapArena arena = gameManager.getArena(worldName).orElse(null);
        if (arena == null) {
            player.sendMessage(FormatUtil.error("Keine gespeicherten Daten für diese Welt gefunden."));
            return;
        }

        TeamColor filter = null;
        if (args.length >= 2) {
            filter = TeamColor.fromString(args[1]);
            if (filter == null) {
                player.sendMessage(FormatUtil.error("Unbekanntes Team. Nutze rot oder blau."));
                return;
            }
        }

        player.sendMessage(Component.text("=== TNT Wars Daten: ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(arena.getWorldName(), NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text(" ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
        if (filter != null) {
            sendTeamOverview(player, arena, filter);
        } else {
            sendTeamOverview(player, arena, TeamColor.RED);
            sendTeamOverview(player, arena, TeamColor.BLUE);
        }

        List<Cuboid> walls = arena.getWalls();
        player.sendMessage(Component.text("Wände: ", NamedTextColor.YELLOW)
                .append(Component.text(walls.isEmpty() ? "Keine gespeichert" : "", NamedTextColor.GRAY)));
        for (int i = 0; i < walls.size(); i++) {
            Cuboid wall = walls.get(i);
            String text = String.format(Locale.GERMAN, "  %d) %s: (%d,%d,%d) -> (%d,%d,%d)",
                    i + 1,
                    wall.getWorldName(),
                    wall.getMinX(), wall.getMinY(), wall.getMinZ(),
                    wall.getMaxX(), wall.getMaxY(), wall.getMaxZ());
            player.sendMessage(Component.text(text, NamedTextColor.GRAY));
        }
    }

    private void sendTeamOverview(Player player, MapArena arena, TeamColor team) {
        player.sendMessage(Component.text("Team ", team.getNamedColor())
                .decorate(TextDecoration.BOLD)
                .append(Component.text(team.getDisplay(), team.getNamedColor()).decorate(TextDecoration.BOLD)));
        List<StoredLocation> spawns = arena.getSpawnSlots(team);
        if (spawns.isEmpty()) {
            player.sendMessage(Component.text("  Keine Spawns gespeichert.", NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < Math.max(spawns.size(), 4); i++) {
                StoredLocation location = i < spawns.size() ? spawns.get(i) : null;
                player.sendMessage(describeSlot("Spawn", i, location));
            }
        }

        List<StoredLocation> beacons = arena.getBeaconSlots(team);
        if (beacons.isEmpty()) {
            player.sendMessage(Component.text("  Keine Beacons gespeichert.", NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < Math.max(beacons.size(), 4); i++) {
                StoredLocation location = i < beacons.size() ? beacons.get(i) : null;
                player.sendMessage(describeSlot("Beacon", i, location));
            }
        }
    }

    private Component describeSlot(String label, int index, StoredLocation location) {
        Component prefix = Component.text("  " + label + " ", NamedTextColor.YELLOW)
                .append(Component.text((index + 1), NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.YELLOW));
        if (location == null) {
            return prefix.append(Component.text("-", NamedTextColor.GRAY));
        }
        boolean loaded = Bukkit.getWorld(location.getWorldName()) != null;
        Component info = Component.text(location.toDisplayString(), NamedTextColor.GRAY);
        if (!loaded) {
            info = info.append(Component.text(" (Welt nicht geladen)", NamedTextColor.RED));
        }
        return prefix.append(info);
    }

    private Location parseCoordinate(World world, String coordinate) {
        String[] split = coordinate.split(",");
        if (split.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("add");
            sub.add("info");
            sub.add("setspawn");
            sub.add("setbeacon");
            sub.add("wall");
            sub.add("setteam");
            sub.addAll(gameManager.getArenaNames());
            return filter(sub, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(Arrays.asList("rot", "blau"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(Arrays.asList("1", "2", "3", "4"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setbeacon")) {
            return filter(Arrays.asList("rot", "blau"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setbeacon")) {
            return filter(Arrays.asList("1", "2", "3", "4"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return filter(getWorldFolderSuggestions(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> names = new ArrayList<>(gameManager.getArenaNames());
            String currentWorld = playerWorldName(sender);
            if (!currentWorld.isEmpty()) {
                names.add(currentWorld);
            }
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("info")) {
            return filter(Arrays.asList("rot", "blau"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setteam")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setteam")) {
            return filter(Arrays.asList("rot", "blau"), args[2]);
        }
        return Collections.emptyList();
    }

    private String playerWorldName(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getWorld().getName();
        }
        return "";
    }

    private List<String> filter(List<String> input, String token) {
        return input.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private List<String> getWorldFolderSuggestions() {
        File container = Bukkit.getWorldContainer();
        File[] files = container.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        Set<String> registered = new HashSet<>();
        for (String name : gameManager.getArenaNames()) {
            registered.add(name.toLowerCase(Locale.ROOT));
        }
        List<String> suggestions = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            if (!new File(file, "level.dat").exists()) {
                continue;
            }
            String name = file.getName();
            if (!registered.contains(name.toLowerCase(Locale.ROOT))) {
                suggestions.add(name);
            }
        }
        suggestions.sort(String::compareToIgnoreCase);
        return suggestions;
    }
}
