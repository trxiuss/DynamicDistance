package com.dynamicdistance.modules;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedManager implements Listener {

    private final DynamicDistance plugin;
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Double> playerSpeeds = new ConcurrentHashMap<>();

    public SpeedManager(DynamicDistance plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isEnabled()) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        update(event.getPlayer().getUniqueId(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        reset(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    private void update(UUID uuid, Location to) {
        long now = System.currentTimeMillis();

        Location last = lastLocations.get(uuid);
        Long lastTime = lastUpdateTimes.get(uuid);

        if (last == null || lastTime == null || last.getWorld() == null || to.getWorld() == null || !last.getWorld().equals(to.getWorld())) {
            reset(uuid);
            lastLocations.put(uuid, to.clone());
            lastUpdateTimes.put(uuid, now);
            playerSpeeds.put(uuid, 0.0);
            return;
        }

        long elapsed = now - lastTime;
        if (elapsed < 200L) {
            return;
        }

        double distance = last.distance(to);

        if (distance > 100.0) {
            reset(uuid);
            lastLocations.put(uuid, to.clone());
            lastUpdateTimes.put(uuid, now);
            playerSpeeds.put(uuid, 0.0);
            return;
        }

        double speed = distance / (elapsed / 1000.0);
        playerSpeeds.put(uuid, speed);
        lastLocations.put(uuid, to.clone());
        lastUpdateTimes.put(uuid, now);
    }

    public boolean isFastMoving(Player player) {
        if (!isEnabled()) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Double speed = playerSpeeds.get(uuid);
        Long lastUpdate = lastUpdateTimes.get(uuid);

        if (speed == null || lastUpdate == null) {
            return false;
        }

        if (System.currentTimeMillis() - lastUpdate > 1000L) {
            return false;
        }

        double threshold = plugin.getConfig().getDouble("modules.speed_limiter.speed_threshold", 18.0);
        return speed >= threshold;
    }

    public void remove(UUID uuid) {
        lastLocations.remove(uuid);
        lastUpdateTimes.remove(uuid);
        playerSpeeds.remove(uuid);
    }

    public void reset(UUID uuid) {
        lastLocations.remove(uuid);
        lastUpdateTimes.remove(uuid);
        playerSpeeds.put(uuid, 0.0);
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("modules.speed_limiter.enabled", true);
    }
}