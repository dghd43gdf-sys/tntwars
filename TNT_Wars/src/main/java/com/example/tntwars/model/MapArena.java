package com.example.tntwars.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MapArena {
    private final String worldName;
    private final Map<TeamColor, List<StoredLocation>> spawns = new EnumMap<>(TeamColor.class);
    private final Map<TeamColor, List<StoredLocation>> beacons = new EnumMap<>(TeamColor.class);
    private final List<Cuboid> walls = new ArrayList<>();

    public MapArena(String worldName) {
        this.worldName = worldName;
        for (TeamColor color : TeamColor.values()) {
            spawns.put(color, new ArrayList<>());
            beacons.put(color, new ArrayList<>());
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public List<StoredLocation> getSpawnSlots(TeamColor color) {
        return spawns.get(color);
    }

    public List<Location> getValidSpawns(TeamColor color) {
        List<Location> result = new ArrayList<>();
        for (StoredLocation location : spawns.get(color)) {
            if (location != null) {
                Location resolved = location.resolve();
                if (resolved != null) {
                    result.add(resolved);
                }
            }
        }
        return result;
    }

    public void addSpawn(TeamColor color, Location location, Integer index) {
        addSpawn(color, StoredLocation.from(location), index);
    }

    public void addSpawn(TeamColor color, StoredLocation location, Integer index) {
        List<StoredLocation> list = spawns.get(color);
        if (index != null && index >= 0 && index < 4) {
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, location);
            return;
        }
        if (list.size() < 4) {
            list.add(location);
        } else {
            list.set(list.size() - 1, location);
        }
    }

    public List<StoredLocation> getBeaconSlots(TeamColor color) {
        return beacons.get(color);
    }

    public List<Location> getValidBeacons(TeamColor color) {
        List<Location> result = new ArrayList<>();
        for (StoredLocation location : beacons.get(color)) {
            if (location != null) {
                Location resolved = location.resolve();
                if (resolved != null) {
                    result.add(resolved);
                }
            }
        }
        return result;
    }

    public void addBeacon(TeamColor color, Location location, Integer index) {
        addBeacon(color, StoredLocation.from(location), index);
    }

    public void addBeacon(TeamColor color, StoredLocation location, Integer index) {
        List<StoredLocation> list = beacons.get(color);
        if (index != null && index >= 0 && index < 4) {
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, location);
            return;
        }
        if (list.size() < 4) {
            list.add(location);
        } else {
            list.set(list.size() - 1, location);
        }
    }

    public boolean hasSpawns(TeamColor color) {
        return !getValidSpawns(color).isEmpty();
    }

    public boolean hasBeacon(TeamColor color) {
        return !getValidBeacons(color).isEmpty();
    }

    public Location getFirstAvailableBeacon() {
        for (TeamColor color : TeamColor.values()) {
            for (Location location : getValidBeacons(color)) {
                return location;
            }
        }
        return null;
    }

    public List<Cuboid> getWalls() {
        return walls;
    }

    public void addWall(Cuboid cuboid) {
        walls.add(cuboid);
    }
}
