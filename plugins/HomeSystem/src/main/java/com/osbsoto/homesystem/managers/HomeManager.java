package com.osbsoto.homesystem.managers;

import com.osbsoto.homesystem.HomeSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final HomeSystem plugin;
    private final File homesFile;
    private FileConfiguration homesConfig;

    // Map: playerUUID -> (homeName -> Location)
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public HomeManager(HomeSystem plugin) {
        this.plugin = plugin;
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void loadHomes() {
        if (!homesFile.exists()) {
            try {
                homesFile.getParentFile().mkdirs();
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
                return;
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        homes.clear();

        if (homesConfig.getKeys(false).isEmpty()) {
            return;
        }

        for (String uuidStr : homesConfig.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in homes.yml: " + uuidStr);
                continue;
            }

            Map<String, Location> playerHomes = new HashMap<>();
            if (homesConfig.getConfigurationSection(uuidStr) != null) {
                for (String homeName : homesConfig.getConfigurationSection(uuidStr).getKeys(false)) {
                    String path = uuidStr + "." + homeName;
                    String worldName = homesConfig.getString(path + ".world");
                    if (worldName == null) continue;

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("World not found for home '" + homeName + "' of player " + uuidStr + ": " + worldName);
                        continue;
                    }

                    double x = homesConfig.getDouble(path + ".x");
                    double y = homesConfig.getDouble(path + ".y");
                    double z = homesConfig.getDouble(path + ".z");
                    float yaw = (float) homesConfig.getDouble(path + ".yaw");
                    float pitch = (float) homesConfig.getDouble(path + ".pitch");

                    playerHomes.put(homeName, new Location(world, x, y, z, yaw, pitch));
                }
            }
            homes.put(uuid, playerHomes);
        }

        plugin.getLogger().info("Loaded homes for " + homes.size() + " player(s).");
    }

    public void saveHomes() {
        homesConfig = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Location> homeEntry : entry.getValue().entrySet()) {
                String path = uuidStr + "." + homeEntry.getKey();
                Location loc = homeEntry.getValue();
                homesConfig.set(path + ".world", loc.getWorld().getName());
                homesConfig.set(path + ".x", loc.getX());
                homesConfig.set(path + ".y", loc.getY());
                homesConfig.set(path + ".z", loc.getZ());
                homesConfig.set(path + ".yaw", loc.getYaw());
                homesConfig.set(path + ".pitch", loc.getPitch());
            }
        }

        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public boolean setHome(UUID uuid, String name, Location location) {
        homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(name.toLowerCase(), location);
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return null;
        return playerHomes.get(name.toLowerCase());
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        return playerHomes.remove(name.toLowerCase()) != null;
    }

    public Set<String> getHomeNames(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return Collections.emptySet();
        return Collections.unmodifiableSet(playerHomes.keySet());
    }

    public int getHomeCount(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    public boolean hasHome(UUID uuid, String name) {
        return getHome(uuid, name) != null;
    }
}
