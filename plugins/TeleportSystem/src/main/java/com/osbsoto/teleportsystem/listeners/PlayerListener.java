package com.osbsoto.teleportsystem.listeners;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final TeleportSystem plugin;

    public PlayerListener(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Save death location so /back can return player there
        plugin.getTeleportManager().saveLastLocation(
                event.getEntity().getUniqueId(),
                event.getEntity().getLocation()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Save pre-teleport location for /back (except for plugin-initiated teleports)
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            plugin.getTeleportManager().saveLastLocation(
                    event.getPlayer().getUniqueId(),
                    event.getFrom()
            );
        }
    }
}
