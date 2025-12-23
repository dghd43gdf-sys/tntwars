package com.example.tntwars.storage;

import com.example.tntwars.model.Cuboid;
import com.example.tntwars.model.MapArena;
import com.example.tntwars.model.StoredLocation;
import com.example.tntwars.model.TeamColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MapStorage {
    private final File file;
    private final Logger logger;
    private FileConfiguration configuration;

    public MapStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "data.yml");
        this.logger = logger;
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                logger.severe("Konnte data.yml nicht erstellen: " + exception.getMessage());
            }
        }
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            logger.severe("Konnte data.yml nicht speichern: " + exception.getMessage());
        }
    }

    public Map<String, MapArena> loadArenas() {
        Map<String, MapArena> arenas = new HashMap<>();
        ConfigurationSection root = configuration.getConfigurationSection("arenas");
        if (root == null) {
            return arenas;
        }
        for (String worldName : root.getKeys(false)) {
            MapArena arena = new MapArena(worldName);
            ConfigurationSection section = root.getConfigurationSection(worldName);
            if (section == null) {
                continue;
            }
            for (TeamColor color : TeamColor.values()) {
                ConfigurationSection spawnSection = section.getConfigurationSection("spawns." + color.name().toLowerCase());
                if (spawnSection != null) {
                    for (String key : spawnSection.getKeys(false)) {
                        StoredLocation location = deserializeLocation(spawnSection.getConfigurationSection(key));
                        if (location != null) {
                            int index = Integer.parseInt(key);
                            arena.addSpawn(color, location, index);
                        }
                    }
                }
                ConfigurationSection beaconSection = section.getConfigurationSection("beacons." + color.name().toLowerCase());
                if (beaconSection != null) {
                    for (String key : beaconSection.getKeys(false)) {
                        StoredLocation location = deserializeLocation(beaconSection.getConfigurationSection(key));
                        if (location != null) {
                            int index = Integer.parseInt(key);
                            arena.addBeacon(color, location, index);
                        }
                    }
                }
            }
            ConfigurationSection wallsSection = section.getConfigurationSection("walls");
            if (wallsSection != null) {
                for (String key : wallsSection.getKeys(false)) {
                    ConfigurationSection wall = wallsSection.getConfigurationSection(key);
                    if (wall == null) {
                        continue;
                    }
                    int x1 = wall.getInt("x1");
                    int y1 = wall.getInt("y1");
                    int z1 = wall.getInt("z1");
                    int x2 = wall.getInt("x2");
                    int y2 = wall.getInt("y2");
                    int z2 = wall.getInt("z2");
                    arena.addWall(new Cuboid(worldName, x1, y1, z1, x2, y2, z2));
                }
            }
            arenas.put(worldName.toLowerCase(), arena);
        }
        return arenas;
    }

    public void saveArena(MapArena arena) {
        ConfigurationSection root = configuration.getConfigurationSection("arenas");
        if (root == null) {
            root = configuration.createSection("arenas");
        }
        root.set(arena.getWorldName(), null);
        ConfigurationSection section = root.createSection(arena.getWorldName());
        for (TeamColor color : TeamColor.values()) {
            List<StoredLocation> spawns = arena.getSpawnSlots(color);
            ConfigurationSection spawnSection = section.createSection("spawns." + color.name().toLowerCase());
            for (int i = 0; i < spawns.size(); i++) {
                StoredLocation location = spawns.get(i);
                if (location != null) {
                    serializeLocation(spawnSection.createSection(String.valueOf(i)), location);
                }
            }
            List<StoredLocation> beacons = arena.getBeaconSlots(color);
            ConfigurationSection beaconSection = section.createSection("beacons." + color.name().toLowerCase());
            for (int i = 0; i < beacons.size(); i++) {
                StoredLocation location = beacons.get(i);
                if (location != null) {
                    serializeLocation(beaconSection.createSection(String.valueOf(i)), location);
                }
            }
        }
        ConfigurationSection wallsSection = section.createSection("walls");
        List<Cuboid> walls = arena.getWalls();
        for (int i = 0; i < walls.size(); i++) {
            Cuboid cuboid = walls.get(i);
            ConfigurationSection wall = wallsSection.createSection(String.valueOf(i));
            wall.set("x1", cuboid.getMinX());
            wall.set("y1", cuboid.getMinY());
            wall.set("z1", cuboid.getMinZ());
            wall.set("x2", cuboid.getMaxX());
            wall.set("y2", cuboid.getMaxY());
            wall.set("z2", cuboid.getMaxZ());
        }
        save();
    }

    private StoredLocation deserializeLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new StoredLocation(worldName, x, y, z, yaw, pitch);
    }

    private void serializeLocation(ConfigurationSection section, StoredLocation location) {
        section.set("world", location.getWorldName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
}
