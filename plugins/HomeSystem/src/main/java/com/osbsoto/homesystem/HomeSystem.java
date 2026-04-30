package com.osbsoto.homesystem;

import com.osbsoto.homesystem.commands.*;
import com.osbsoto.homesystem.managers.HomeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HomeSystem extends JavaPlugin {

    private HomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        homeManager = new HomeManager(this);
        homeManager.loadHomes();

        // Register commands
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));

        getLogger().info("HomeSystem has been enabled!");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.saveHomes();
        }
        getLogger().info("HomeSystem has been disabled!");
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public int getMaxHomes() {
        return getConfig().getInt("max-homes", 3);
    }

    public int getTeleportDelay() {
        return getConfig().getInt("teleport-delay", 3);
    }

    public boolean isCancelOnMove() {
        return getConfig().getBoolean("cancel-on-move", true);
    }
}
