package com.osbsoto.teleportsystem.managers;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final TeleportSystem plugin;

    // Map: playerUUID -> last location (for /back)
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    // Map: requester UUID -> target UUID (pending TPA requests)
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();

    // Map: target UUID -> requester UUID (for tpaccept/tpdeny lookup)
    private final Map<UUID, UUID> incomingRequests = new HashMap<>();

    public TeleportManager(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    public void saveLastLocation(UUID uuid, Location location) {
        lastLocations.put(uuid, location.clone());
    }

    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    public void createTpaRequest(UUID requester, UUID target) {
        // Clean up any previous request from this requester
        UUID previousTarget = tpaRequests.get(requester);
        if (previousTarget != null) {
            incomingRequests.remove(previousTarget);
        }
        tpaRequests.put(requester, target);
        incomingRequests.put(target, requester);

        // Expire request after configured time.
        // We compare the current target for this requester before removing to ensure
        // that a newer request (to a different target) is not accidentally removed
        // by an older expiration task.
        int expireSeconds = plugin.getTpaExpireTime();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            UUID currentTarget = tpaRequests.get(requester);
            if (target.equals(currentTarget)) {
                tpaRequests.remove(requester);
                incomingRequests.remove(target);
            }
        }, expireSeconds * 20L);
    }

    public boolean hasPendingRequest(UUID target) {
        return incomingRequests.containsKey(target);
    }

    public UUID getRequester(UUID target) {
        return incomingRequests.get(target);
    }

    public void clearRequest(UUID target) {
        UUID requester = incomingRequests.remove(target);
        if (requester != null) {
            tpaRequests.remove(requester);
        }
    }
}
