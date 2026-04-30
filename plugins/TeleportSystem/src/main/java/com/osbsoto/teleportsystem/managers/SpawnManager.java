package com.osbsoto.teleportsystem.managers;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpawnManager {

    private final TeleportSystem plugin;
    private final File spawnFile;
    private FileConfiguration spawnConfig;

    // Map: worldName -> spawn Location
    private final Map<String, Location> spawnLocations = new HashMap<>();

    public SpawnManager(TeleportSystem plugin) {
        this.plugin = plugin;
        spawnFile = new File(plugin.getDataFolder(), "spawns.yml");
    }

    public void load() {
        if (!spawnFile.exists()) {
            try {
                spawnFile.getParentFile().mkdirs();
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawns.yml: " + e.getMessage());
                return;
            }
        }

        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
        spawnLocations.clear();

        for (String worldName : spawnConfig.getKeys(false)) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found for spawn: " + worldName);
                continue;
            }
            double x = spawnConfig.getDouble(worldName + ".x");
            double y = spawnConfig.getDouble(worldName + ".y");
            double z = spawnConfig.getDouble(worldName + ".z");
            float yaw = (float) spawnConfig.getDouble(worldName + ".yaw");
            float pitch = (float) spawnConfig.getDouble(worldName + ".pitch");
            spawnLocations.put(worldName, new Location(world, x, y, z, yaw, pitch));
        }

        plugin.getLogger().info("Loaded " + spawnLocations.size() + " spawn location(s).");
    }

    public void save() {
        spawnConfig = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : spawnLocations.entrySet()) {
            Location loc = entry.getValue();
            String worldName = entry.getKey();
            spawnConfig.set(worldName + ".x", loc.getX());
            spawnConfig.set(worldName + ".y", loc.getY());
            spawnConfig.set(worldName + ".z", loc.getZ());
            spawnConfig.set(worldName + ".yaw", loc.getYaw());
            spawnConfig.set(worldName + ".pitch", loc.getPitch());
        }

        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawns.yml: " + e.getMessage());
        }
    }

    public void setSpawn(String worldName, Location location) {
        spawnLocations.put(worldName, location);
        save();
    }

    public Location getSpawn(String worldName) {
        return spawnLocations.get(worldName);
    }

    public boolean hasSpawn(String worldName) {
        return spawnLocations.containsKey(worldName);
    }
}
