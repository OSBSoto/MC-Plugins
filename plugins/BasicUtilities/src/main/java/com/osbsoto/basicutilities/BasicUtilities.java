package com.osbsoto.basicutilities;

import com.osbsoto.basicutilities.commands.*;
import com.osbsoto.basicutilities.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BasicUtilities extends JavaPlugin {

    private final Set<UUID> godModePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Register commands
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("gamemode").setExecutor(new GamemodeCommand(this));
        getCommand("speed").setExecutor(new SpeedCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("BasicUtilities has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BasicUtilities has been disabled!");
    }

    public Set<UUID> getGodModePlayers() {
        return Collections.unmodifiableSet(godModePlayers);
    }

    public boolean isGodMode(UUID uuid) {
        return godModePlayers.contains(uuid);
    }

    public void toggleGodMode(UUID uuid) {
        if (godModePlayers.contains(uuid)) {
            godModePlayers.remove(uuid);
        } else {
            godModePlayers.add(uuid);
        }
    }
}
