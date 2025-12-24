package com.example.tntwars.game;

import com.example.tntwars.model.MapArena;
import com.example.tntwars.model.TeamColor;
import com.example.tntwars.storage.MapStorage;
import com.example.tntwars.util.FormatUtil;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GameManager implements Listener {
    private final Plugin plugin;
    private final MapStorage storage;
    private final Map<String, MapArena> arenas;
    private final Map<String, GameSession> sessions = new HashMap<>();
    private final ScoreboardService scoreboardService = new ScoreboardService();

    public GameManager(Plugin plugin, MapStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.arenas = new HashMap<>(storage.loadArenas());
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public MapArena getOrCreateArena(String worldName) {
        return arenas.computeIfAbsent(worldName.toLowerCase(), name -> new MapArena(worldName));
    }

    public Optional<MapArena> getArena(String worldName) {
        return Optional.ofNullable(arenas.get(worldName.toLowerCase()));
    }

    public boolean hasArena(String worldName) {
        return arenas.containsKey(worldName.toLowerCase());
    }

    public List<String> getArenaNames() {
        return arenas.values().stream()
                .map(MapArena::getWorldName)
                .toList();
    }

    public boolean worldFolderExists(String worldName) {
        File folder = new File(Bukkit.getWorldContainer(), worldName);
        if (!folder.isDirectory()) {
            return false;
        }
        File levelDat = new File(folder, "level.dat");
        return levelDat.exists();
    }

    public boolean registerArena(String worldName) {
        String key = worldName.toLowerCase();
        if (arenas.containsKey(key)) {
            return false;
        }
        MapArena arena = new MapArena(worldName);
        arenas.put(key, arena);
        saveArena(arena);
        return true;
    }

    public void saveArena(MapArena arena) {
        storage.saveArena(arena);
    }

    public boolean startGame(Player initiator) {
        World playerWorld = initiator.getWorld();
        MapArena arena = arenas.get(playerWorld.getName().toLowerCase());
        if (arena == null) {
            initiator.sendMessage(FormatUtil.error("Für diese Welt ist keine TNT-Wars-Arena gespeichert."));
            return false;
        }
        String key = arena.getWorldName().toLowerCase();
        if (sessions.containsKey(key)) {
            initiator.sendMessage(FormatUtil.error("In dieser Welt läuft bereits ein Spiel."));
            return false;
        }
        if (!arena.hasSpawns(TeamColor.RED) || !arena.hasSpawns(TeamColor.BLUE)) {
            initiator.sendMessage(FormatUtil.error("Es müssen für beide Teams mindestens ein Spawn gesetzt sein."));
            return false;
        }
        if (!arena.hasBeacon(TeamColor.RED) || !arena.hasBeacon(TeamColor.BLUE)) {
            initiator.sendMessage(FormatUtil.error("Es müssen beide Beacon-Positionen gesetzt sein."));
            return false;
        }
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            world = Bukkit.createWorld(org.bukkit.WorldCreator.name(arena.getWorldName()));
        }
        if (world == null) {
            initiator.sendMessage(FormatUtil.error("Diese Welt konnte nicht geladen werden."));
            return false;
        }
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && !player.isDead()) {
                players.add(player);
            }
        }
        if (players.size() < 2) {
            initiator.sendMessage(FormatUtil.error("Mindestens zwei Spieler werden benötigt."));
            return false;
        }
        GameSession session = new GameSession(arena);
        session.assignPlayers(players);
        session.setState(GameSession.State.RUNNING);
        sessions.put(key, session);
        ensureBackup(arena.getWorldName());
        clearInventories(players);
        distributePlayers(session, world);
        updateScoreboard(session);
        broadcast(session, FormatUtil.gameStart());
        return true;
    }

    private void clearInventories(List<Player> players) {
        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            inventory.clear();
            inventory.setArmorContents(null);
            inventory.setItemInOffHand(null);
        }
    }

    private void distributePlayers(GameSession session, World world) {
        for (TeamColor color : TeamColor.values()) {
            for (UUID uuid : session.getTeamMembers(color)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                Location target = session.nextSpawn(color);
                if (target == null || target.getWorld() == null) {
                    target = world.getSpawnLocation();
                }
                player.teleport(target);
                preparePlayer(player);
                player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP.getKey(), Sound.Source.PLAYER, 0.8f, 1f), Sound.Emitter.self());
                player.sendMessage(FormatUtil.teamAssignment(color));
            }
        }
    }

    private void preparePlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            player.setHealth(healthAttribute.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        PlayerInventory inventory = player.getInventory();
        inventory.setHeldItemSlot(0);
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void broadcast(GameSession session, Component component) {
        for (Player participant : getParticipants(session)) {
            participant.sendMessage(component);
        }
    }

    private List<Player> getParticipants(GameSession session) {
        List<Player> participants = new ArrayList<>();
        for (TeamColor color : TeamColor.values()) {
            for (UUID uuid : session.getTeamMembers(color)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    participants.add(player);
                }
            }
        }
        return participants;
    }

    public void handleBeaconDestruction(World world, TeamColor destroyedTeam, Player breaker) {
        GameSession session = sessions.get(world.getName().toLowerCase());
        if (session == null || session.getState() != GameSession.State.RUNNING) {
            return;
        }
        TeamColor winner = destroyedTeam == TeamColor.RED ? TeamColor.BLUE : TeamColor.RED;
        broadcast(session, FormatUtil.victoryBroadcast(winner));
        endGame(session, winner);
    }

    public void endGame(GameSession session, TeamColor winner) {
        if (session.getState() == GameSession.State.FINISHED) {
            return;
        }
        session.setState(GameSession.State.FINISHED);
        sessions.remove(session.getArena().getWorldName().toLowerCase());
        List<Player> participants = getParticipants(session);
        scoreboardService.clear(session, participants);
        for (Player player : participants) {
            player.playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE.getKey(), Sound.Source.MASTER, 0.7f, 1f), Sound.Emitter.self());
            player.showTitle(Title.title(FormatUtil.tntTitleComponent("TNT Wars"), FormatUtil.victorySubtitle(winner)));
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> resetWorld(session.getArena().getWorldName()), 200L);
    }

    public void resetWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        broadcastToWorld(world, FormatUtil.worldReset());
        for (Player player : new ArrayList<>(world.getPlayers())) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.unloadWorld(world, false);
            Path worldPath = Bukkit.getWorldContainer().toPath().resolve(worldName);
            Path backupPath = plugin.getDataFolder().toPath().resolve("backups").resolve(worldName);
            try {
                deleteDirectory(worldPath);
                copyDirectory(backupPath, worldPath);
            } catch (IOException exception) {
                plugin.getLogger().severe("Fehler beim Wiederherstellen der Welt " + worldName + ": " + exception.getMessage());
            }
            Bukkit.createWorld(org.bukkit.WorldCreator.name(worldName));
        }, 40L);
    }

    private void ensureBackup(String worldName) {
        Path backupPath = plugin.getDataFolder().toPath().resolve("backups").resolve(worldName);
        if (Files.exists(backupPath)) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        Path worldPath = Bukkit.getWorldContainer().toPath().resolve(worldName);
        try {
            copyDirectory(worldPath, backupPath);
        } catch (IOException exception) {
            plugin.getLogger().severe("Konnte Backup für " + worldName + " nicht erstellen: " + exception.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            var paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).iterator();
            while (paths.hasNext()) {
                Path current = paths.next();
                Files.deleteIfExists(current);
            }
        }
    }

    public boolean isProtectedWall(Location location) {
        MapArena arena = arenas.get(location.getWorld().getName().toLowerCase());
        if (arena == null) {
            return false;
        }
        return arena.getWalls().stream().anyMatch(cuboid -> cuboid.contains(location));
    }

    public boolean switchPlayerTeam(Player requester, Player target, TeamColor newTeam) {
        World targetWorld = target.getWorld();
        GameSession session = sessions.get(targetWorld.getName().toLowerCase());
        if (session == null) {
            requester.sendMessage(FormatUtil.error("In dieser Welt läuft kein Spiel."));
            return false;
        }
        if (session.getState() != GameSession.State.RUNNING) {
            requester.sendMessage(FormatUtil.error("Das Spiel muss laufen um Spieler zu verschieben."));
            return false;
        }
        TeamColor oldTeam = session.getPlayerTeam(target);
        if (oldTeam == null) {
            requester.sendMessage(FormatUtil.error(target.getName() + " nimmt nicht am Spiel teil."));
            return false;
        }
        if (oldTeam == newTeam) {
            requester.sendMessage(FormatUtil.error(target.getName() + " ist bereits in Team " + newTeam.getDisplay() + "."));
            return false;
        }
        if (!session.switchPlayerTeam(target.getUniqueId(), newTeam)) {
            requester.sendMessage(FormatUtil.error("Fehler beim Verschieben des Spielers."));
            return false;
        }
        Location spawn = session.nextSpawn(newTeam);
        if (spawn != null) {
            target.teleport(spawn);
        }
        preparePlayer(target);
        updateScoreboard(session);
        broadcast(session, FormatUtil.playerTeamSwitch(target.getName(), newTeam));
        target.sendMessage(FormatUtil.teamSwitch(newTeam));
        return true;
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (isProtectedWall(location)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(FormatUtil.error("Diese Wand kann nur durch TNT zerstört werden."));
            return;
        }
        if (event.getBlock().getType() != Material.BEACON) {
            return;
        }
        MapArena arena = arenas.get(location.getWorld().getName().toLowerCase());
        if (arena == null) {
            return;
        }
        for (TeamColor color : TeamColor.values()) {
            for (Location beacon : arena.getValidBeacons(color)) {
                if (isSameBlock(beacon, location)) {
                    GameSession session = sessions.get(location.getWorld().getName().toLowerCase());
                    if (session != null && session.getState() == GameSession.State.RUNNING) {
                        event.setCancelled(true);
                        location.getBlock().setType(Material.AIR);
                        handleBeaconDestruction(location.getWorld(), color, event.getPlayer());
                    }
                    return;
                }
            }
        }
    }

    private boolean isSameBlock(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    public void handleExplosion(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        if (world != null) {
            MapArena arena = arenas.get(world.getName().toLowerCase());
            if (arena != null) {
                List<Location> toCheck = new ArrayList<>();
                for (org.bukkit.block.Block block : event.blockList()) {
                    if (block.getType() == Material.BEACON) {
                        toCheck.add(block.getLocation());
                    }
                }
                boolean handled = false;
                for (Location location : toCheck) {
                    for (TeamColor color : TeamColor.values()) {
                        for (Location beacon : arena.getValidBeacons(color)) {
                            if (isSameBlock(beacon, location)) {
                                handleBeaconDestruction(world, color, null);
                                handled = true;
                                break;
                            }
                        }
                        if (handled) {
                            break;
                        }
                    }
                    if (handled) {
                        break;
                    }
                }
            }
        }
        if (!(event.getEntity() instanceof org.bukkit.entity.TNTPrimed
                || event.getEntity() instanceof org.bukkit.entity.minecart.ExplosiveMinecart)) {
            event.blockList().removeIf(block -> isProtectedWall(block.getLocation()));
        }
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(FormatUtil.deathMessage(event.getPlayer().getName()));
    }

    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        for (GameSession session : sessions.values()) {
            if (session.getState() != GameSession.State.RUNNING) {
                continue;
            }
            TeamColor team = session.getPlayerTeam(player);
            if (team == null) {
                continue;
            }
            Location spawn = session.nextSpawn(team);
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            Bukkit.getScheduler().runTask(plugin, () -> preparePlayer(player));
            break;
        }
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (GameSession session : new ArrayList<>(sessions.values())) {
            TeamColor team = session.removePlayer(player.getUniqueId());
            if (team != null) {
                updateScoreboard(session);
                checkTeamStatus(session);
                break;
            }
        }
    }

    private void checkTeamStatus(GameSession session) {
        if (session.getState() != GameSession.State.RUNNING) {
            return;
        }
        boolean redHasPlayers = session.hasPlayers(TeamColor.RED);
        boolean blueHasPlayers = session.hasPlayers(TeamColor.BLUE);
        if (redHasPlayers && blueHasPlayers) {
            return;
        }
        if (!redHasPlayers && blueHasPlayers) {
            broadcast(session, FormatUtil.victoryBroadcast(TeamColor.BLUE));
            endGame(session, TeamColor.BLUE);
        } else if (redHasPlayers && !blueHasPlayers) {
            broadcast(session, FormatUtil.victoryBroadcast(TeamColor.RED));
            endGame(session, TeamColor.RED);
        }
    }

    private void updateScoreboard(GameSession session) {
        if (session.getState() != GameSession.State.RUNNING) {
            return;
        }
        scoreboardService.update(session, getParticipants(session));
    }

    private void broadcastToWorld(World world, Component component) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(component);
        }
    }
}
