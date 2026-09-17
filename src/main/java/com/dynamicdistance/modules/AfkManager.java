package com.dynamicdistance.modules;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.engine.DistanceController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkManager implements Listener {

    private final DynamicDistance plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkState = new ConcurrentHashMap<>();
    private DistanceController controller;

    public AfkManager(DynamicDistance plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setController(DistanceController controller) {
        this.controller = controller;
    }

    public void updateActivity(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        Boolean wasAfk = afkState.put(uuid, false);
        if (wasAfk != null && wasAfk) {
            if (controller != null) {
                controller.applyToPlayer(player);
            }
        }
    }

    public boolean isAfk(Player player) {
        if (!plugin.getConfig().getBoolean("modules.afk_limiter.enabled", true)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        long afkThresholdMs = plugin.getConfig().getLong("modules.afk_limiter.afk_seconds", 60) * 1000L;
        Long last = lastActivity.get(uuid);

        if (last == null) {
            lastActivity.put(uuid, System.currentTimeMillis());
            afkState.put(uuid, false);
            return false;
        }

        boolean nowAfk = (System.currentTimeMillis() - last) > afkThresholdMs;
        Boolean previousState = afkState.put(uuid, nowAfk);

        if (previousState != null && previousState != nowAfk) {
            if (controller != null) {
                controller.applyToPlayer(player);
            }
        }

        return nowAfk;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY()) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastActivity.remove(uuid);
        afkState.remove(uuid);
    }
}