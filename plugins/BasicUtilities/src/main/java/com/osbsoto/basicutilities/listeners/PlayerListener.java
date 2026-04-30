package com.osbsoto.basicutilities.listeners;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {

    private final BasicUtilities plugin;

    public PlayerListener(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.isGodMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
