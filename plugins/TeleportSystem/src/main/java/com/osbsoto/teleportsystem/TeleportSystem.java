package com.osbsoto.teleportsystem;

import com.osbsoto.teleportsystem.commands.*;
import com.osbsoto.teleportsystem.listeners.PlayerListener;
import com.osbsoto.teleportsystem.managers.SpawnManager;
import com.osbsoto.teleportsystem.managers.TeleportManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportSystem extends JavaPlugin {

    private SpawnManager spawnManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        spawnManager = new SpawnManager(this);
        spawnManager.load();

        teleportManager = new TeleportManager(this);

        // Register commands
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("TeleportSystem has been enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) {
            spawnManager.save();
        }
        getLogger().info("TeleportSystem has been disabled!");
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public int getTeleportDelay() {
        return getConfig().getInt("teleport-delay", 3);
    }

    public boolean isCancelOnMove() {
        return getConfig().getBoolean("cancel-on-move", true);
    }

    public int getTpaExpireTime() {
        return getConfig().getInt("tpa-expire-time", 60);
    }
}
