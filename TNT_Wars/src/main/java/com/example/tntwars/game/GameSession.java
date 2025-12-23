package com.example.tntwars.game;

import com.example.tntwars.model.MapArena;
import com.example.tntwars.model.TeamColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameSession {
    public enum State {
        LOBBY,
        RUNNING,
        FINISHED
    }

    private final MapArena arena;
    private final Map<TeamColor, Set<UUID>> teamMembers = new EnumMap<>(TeamColor.class);
    private final Map<TeamColor, Integer> spawnRotation = new EnumMap<>(TeamColor.class);
    private State state = State.LOBBY;

    public GameSession(MapArena arena) {
        this.arena = arena;
        for (TeamColor color : TeamColor.values()) {
            teamMembers.put(color, new HashSet<>());
            spawnRotation.put(color, 0);
        }
    }

    public MapArena getArena() {
        return arena;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void assignPlayers(List<Player> players) {
        for (Set<UUID> members : teamMembers.values()) {
            members.clear();
        }
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        Deque<Player> deque = new ArrayDeque<>(shuffled);
        boolean toggle = false;
        while (!deque.isEmpty()) {
            Player player = deque.poll();
            TeamColor team = toggle ? TeamColor.BLUE : TeamColor.RED;
            toggle = !toggle;
            teamMembers.get(team).add(player.getUniqueId());
        }
        resetSpawnRotation();
    }

    public void resetSpawnRotation() {
        for (TeamColor color : TeamColor.values()) {
            spawnRotation.put(color, 0);
        }
    }

    public Set<UUID> getTeamMembers(TeamColor team) {
        return teamMembers.get(team);
    }

    public TeamColor getPlayerTeam(Player player) {
        UUID uuid = player.getUniqueId();
        for (Map.Entry<TeamColor, Set<UUID>> entry : teamMembers.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isParticipating(Player player) {
        return getPlayerTeam(player) != null;
    }

    public Location nextSpawn(TeamColor team) {
        List<Location> spawns = new ArrayList<>(arena.getValidSpawns(team));
        if (spawns.isEmpty()) {
            World world = getWorld();
            if (world != null) {
                return world.getSpawnLocation();
            }
            return null;
        }
        int index = spawnRotation.getOrDefault(team, 0);
        Location location = spawns.get(index % spawns.size());
        spawnRotation.put(team, (index + 1) % spawns.size());
        return location;
    }

    public World getWorld() {
        Location beacon = arena.getFirstAvailableBeacon();
        if (beacon != null) {
            return beacon.getWorld();
        }
        List<Location> redSpawns = arena.getValidSpawns(TeamColor.RED);
        if (!redSpawns.isEmpty()) {
            return redSpawns.get(0).getWorld();
        }
        List<Location> blueSpawns = arena.getValidSpawns(TeamColor.BLUE);
        if (!blueSpawns.isEmpty()) {
            return blueSpawns.get(0).getWorld();
        }
        return null;
    }

    public TeamColor removePlayer(UUID uuid) {
        for (Map.Entry<TeamColor, Set<UUID>> entry : teamMembers.entrySet()) {
            if (entry.getValue().remove(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean hasPlayers(TeamColor team) {
        return !teamMembers.get(team).isEmpty();
    }

    public boolean switchPlayerTeam(UUID uuid, TeamColor newTeam) {
        TeamColor oldTeam = null;
        for (Map.Entry<TeamColor, Set<UUID>> entry : teamMembers.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                oldTeam = entry.getKey();
                entry.getValue().remove(uuid);
                break;
            }
        }
        if (oldTeam == null) {
            return false;
        }
        teamMembers.get(newTeam).add(uuid);
        return true;
    }
}
