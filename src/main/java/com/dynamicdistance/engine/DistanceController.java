package com.dynamicdistance.engine;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.locale.LanguageManager;
import com.dynamicdistance.metrics.ServerMetricsCollector;
import com.dynamicdistance.modules.AfkManager;
import com.dynamicdistance.modules.PermissionManager;
import com.dynamicdistance.modules.PermissionManager.GroupSettings;
import com.dynamicdistance.modules.SpeedManager;
import com.dynamicdistance.platform.PlatformAdapter;
import com.dynamicdistance.storage.PlayerData;
import com.dynamicdistance.storage.PlayerDataManager;
import com.dynamicdistance.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DistanceController {

    private final DynamicDistance plugin;
    private final AdaptiveEngine engine;
    private final ServerMetricsCollector metricsCollector;
    private final PlatformAdapter adapter;
    private final PlayerDataManager playerDataManager;
    private final AfkManager afkManager;
    private final SpeedManager speedManager;
    private final PermissionManager permissionManager;

    private final Map<String, Integer> lastWorldView = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastWorldSim = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastPlayerView = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastPlayerSim = new ConcurrentHashMap<>();

    private volatile int currentTargetViewDistance;
    private volatile int currentTargetSimDistance;
    private volatile long lastCriticalTime = 0L;
    private volatile String lastHealthState = "Optimal";
    private volatile int lastLoggedView = -1;
    private volatile int lastLoggedSim = -1;
    private long lastExpireCleanup = 0L;

    public DistanceController(
            DynamicDistance plugin,
            AdaptiveEngine engine,
            ServerMetricsCollector metricsCollector,
            PlatformAdapter adapter,
            PlayerDataManager dataManager,
            AfkManager afk,
            SpeedManager speed,
            PermissionManager perm
    ) {
        this.plugin = plugin;
        this.engine = engine;
        this.metricsCollector = metricsCollector;
        this.adapter = adapter;
        this.playerDataManager = dataManager;
        this.afkManager = afk;
        this.speedManager = speed;
        this.permissionManager = perm;
        this.currentTargetViewDistance = validateConfigBound("distance_limits.view_distance.default_start", 10, 3, 32);
        this.currentTargetSimDistance = validateConfigBound("distance_limits.simulation_distance.default_start", 8, 2, 32);
    }

    public void start() {
        long intervalTicks = Math.max(1L, plugin.getConfig().getLong("engine.check_interval", 2)) * 20L;
        SchedulerUtil.runAsyncTimer(this::tick, 40L, intervalTicks);
    }

    private void tick() {
        SchedulerUtil.runGlobalRegionTask(() -> {
            try {
                metricsCollector.refreshWorldCounts();
                engine.update();
                evaluateTargetDistances();
                applyDistances();
                long now = System.currentTimeMillis();
                if (now - lastExpireCleanup >= 30000L) {
                    lastExpireCleanup = now;
                    java.util.List<java.util.UUID> cleaned = playerDataManager.cleanupExpiredOverrides();
                    if (!cleaned.isEmpty()) {
                        plugin.getLogger().info("Expired overrides cleaned: " + cleaned.size());
                        for (java.util.UUID uuid : cleaned) {
                            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(uuid);
                            if (online != null && online.isOnline()) {
                                applyToPlayer(online);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Performance evaluation tick failed", t);
            }
        });
    }

    private void evaluateTargetDistances() {
        double score = engine.getLoadScore();
        double criticalScore = plugin.getConfig().getDouble("thresholds.critical_load_score", 85.0);
        double qualityFactor = engine.getQualityFactor();

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(1L, plugin.getConfig().getLong("engine.recovery_cooldown", 15)) * 1000L;

        if (score >= criticalScore) {
            lastCriticalTime = now;
        }

        long sinceCritical = now - lastCriticalTime;
        if (sinceCritical < cooldownMs) {
            double rampCeiling = sinceCritical / (double) cooldownMs;
            if (qualityFactor > rampCeiling) {
                qualityFactor = rampCeiling;
            }
        }

        int minVd = getGlobalViewMin();
        int maxVd = getGlobalViewMax();
        int minSd = getGlobalSimMin();
        int maxSd = getGlobalSimMax();

        double rawView = minVd + (qualityFactor * (maxVd - minVd));
        double rawSim = minSd + (qualityFactor * (maxSd - minSd));

        int newView = clamp((int) Math.round(rawView), minVd, maxVd);
        int newSim = clamp((int) Math.round(rawSim), minSd, maxSd);

        String health = engine.getHealthState();
        if (!health.equals(lastHealthState)) {
            if ("Critical".equals(health)) {
                plugin.getLogger().warning("Server entered Critical load state (score=" + score + ")");
            } else if ("Critical".equals(lastHealthState)) {
                plugin.getLogger().info("Server left Critical load state -> " + health + " (score=" + score + ")");
            } else if (plugin.getConfig().getBoolean("engine.debug", false)) {
                plugin.getLogger().info("Health state: " + lastHealthState + " -> " + health + " (score=" + score + ")");
            }
            lastHealthState = health;
        }

        if (newView != lastLoggedView || newSim != lastLoggedSim) {
            if (plugin.getConfig().getBoolean("engine.debug", false)) {
                plugin.getLogger().info("Target distances updated: view=" + newView + " sim=" + newSim + " score=" + score);
            }
            lastLoggedView = newView;
            lastLoggedSim = newSim;
        }

        currentTargetViewDistance = newView;
        currentTargetSimDistance = newSim;
    }

    private void applyDistances() {
        for (World world : Bukkit.getWorlds()) {
            if (world != null) {
                applyToWorld(world);
            }
        }
        if (adapter.supportsPerPlayerDistance()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != null && player.isOnline()) {
                    applyToPlayer(player);
                }
            }
        }
    }

    public void applyToWorld(World world) {
        if (world == null) return;
        String worldName = world.getName();
        int targetVd = clamp(currentTargetViewDistance, getViewMin(worldName), getViewMax(worldName));
        Integer lastVd = lastWorldView.get(worldName);

        if (lastVd == null || lastVd != targetVd) {
            lastWorldView.put(worldName, targetVd);
            SchedulerUtil.runWorldTask(world, () -> {
                adapter.setWorldViewDistance(world, targetVd);
            });
        }

        if (!adapter.supportsSimulationDistance()) return;

        int targetSd = clamp(currentTargetSimDistance, getSimMin(worldName), getSimMax(worldName));
        Integer lastSd = lastWorldSim.get(worldName);

        if (lastSd == null || lastSd != targetSd) {
            lastWorldSim.put(worldName, targetSd);
            SchedulerUtil.runWorldTask(world, () -> {
                adapter.setWorldSimulationDistance(world, targetSd);
            });
        }
    }

    public void applyToPlayer(Player player) {
        if (player == null || !player.isOnline() || !adapter.supportsPerPlayerDistance()) {
            return;
        }
        SchedulerUtil.runEntityTask(player, () -> applyToPlayerNow(player));
    }

    private void applyToPlayerNow(Player player) {
        if (player == null || !player.isOnline() || player.getWorld() == null) return;

        String worldName = player.getWorld().getName();
        int minVd = getViewMin(worldName);
        int maxVd = getViewMax(worldName);
        int minSd = getSimMin(worldName);
        int maxSd = getSimMax(worldName);

        UUID uuid = player.getUniqueId();
        boolean applySim = adapter.supportsSimulationDistance();
        PlayerData data = playerDataManager.getPlayerData(uuid);

        if (data.hasOverride()) {
            int view = data.getCustomViewDistance() != null ? clamp(data.getCustomViewDistance(), minVd, maxVd) : clamp(currentTargetViewDistance, minVd, maxVd);
            int sim = applySim ? (data.getCustomSimDistance() != null ? clamp(data.getCustomSimDistance(), minSd, maxSd) : clamp(currentTargetSimDistance, minSd, maxSd)) : -1;
            applyIfChanged(player, uuid, view, sim, applySim);
            return;
        }

        GroupSettings group = permissionManager.getPlayerGroupSettings(player);

        if (group.isBypassDynamic()) {
            int sim = applySim ? maxSd : -1;
            applyIfChanged(player, uuid, maxVd, sim, applySim);
            return;
        }

        if (afkManager.isAfk(player) && !group.isBypassAfk()) {
            int afkVd = clamp(plugin.getConfig().getInt("modules.afk_limiter.afk_view_distance", 3), minVd, maxVd);
            int afkSd = applySim ? clamp(plugin.getConfig().getInt("modules.afk_limiter.afk_sim_distance", 2), minSd, maxSd) : -1;
            applyIfChanged(player, uuid, afkVd, afkSd, applySim);
            return;
        }

        int simTarget = clamp(currentTargetSimDistance + group.getBonusSimDistance(), minSd, maxSd);

        if (speedManager.isFastMoving(player) && !group.isBypassSpeed()) {
            int speedSd = clamp(plugin.getConfig().getInt("modules.speed_limiter.speed_sim_distance", 3), minSd, maxSd);
            simTarget = Math.min(simTarget, speedSd);
        }

        int viewTarget = clamp(currentTargetViewDistance + group.getBonusViewDistance(), minVd, maxVd);

        applyIfChanged(player, uuid, viewTarget, applySim ? simTarget : -1, applySim);
    }

    private void applyIfChanged(Player player, UUID uuid, int view, int sim, boolean applySim) {
        Integer lastView = lastPlayerView.get(uuid);
        if (lastView == null || lastView != view) {
            adapter.setViewDistance(player, view);
            lastPlayerView.put(uuid, view);
        }

        if (applySim) {
            Integer lastSim = lastPlayerSim.get(uuid);
            if (lastSim == null || lastSim != sim) {
                adapter.setSimulationDistance(player, sim);
                lastPlayerSim.put(uuid, sim);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid == null) return;
        lastPlayerView.remove(uuid);
        lastPlayerSim.remove(uuid);
    }

    public String getAppliedReason(Player player) {
        if (player == null || !player.isOnline()) return "N/A";
        LanguageManager lang = plugin.getLanguageManager();
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());

        if (data.hasOverride()) {
            if (data.isTimedOverride()) {
                Map<String, String> ph = new HashMap<>();
                ph.put("time", data.getFormattedRemainingTime());
                return lang.getMessage("reason-override-timed", ph);
            }
            return lang.getMessage("reason-override-permanent");
        }

        GroupSettings group = permissionManager.getPlayerGroupSettings(player);
        if (group.isBypassDynamic()) {
            return lang.getMessage("reason-bypass");
        }

        if (afkManager.isAfk(player) && !group.isBypassAfk()) {
            return lang.getMessage("reason-afk");
        }

        if (speedManager.isFastMoving(player) && !group.isBypassSpeed()) {
            return lang.getMessage("reason-speed");
        }

        if (group.getBonusViewDistance() > 0 || group.getBonusSimDistance() > 0) {
            Map<String, String> ph = new HashMap<>();
            ph.put("group", group.getName().toUpperCase());
            return lang.getMessage("reason-group-bonus", ph);
        }

        return lang.getMessage("reason-dynamic");
    }

    public void reload() {
        int minVd = getGlobalViewMin();
        int maxVd = getGlobalViewMax();
        int minSd = getGlobalSimMin();
        int maxSd = getGlobalSimMax();

        currentTargetViewDistance = clamp(currentTargetViewDistance, minVd, maxVd);
        currentTargetSimDistance = clamp(currentTargetSimDistance, minSd, maxSd);

        lastWorldView.clear();
        lastWorldSim.clear();
        lastPlayerView.clear();
        lastPlayerSim.clear();
        applyDistances();
    }

    public int getCurrentTargetViewDistance() {
        return currentTargetViewDistance;
    }

    public int getCurrentTargetSimDistance() {
        return currentTargetSimDistance;
    }

    public int getGlobalViewMin() {
        int min = plugin.getConfig().getInt("distance_limits.view_distance.global_min", 3);
        int max = plugin.getConfig().getInt("distance_limits.view_distance.global_max", 14);
        return Math.max(2, Math.min(min, max));
    }

    public int getGlobalViewMax() {
        int min = plugin.getConfig().getInt("distance_limits.view_distance.global_min", 3);
        int max = plugin.getConfig().getInt("distance_limits.view_distance.global_max", 14);
        return Math.max(getGlobalViewMin(), Math.max(min, max));
    }

    public int getGlobalSimMin() {
        int min = plugin.getConfig().getInt("distance_limits.simulation_distance.global_min", 2);
        int max = plugin.getConfig().getInt("distance_limits.simulation_distance.global_max", 10);
        return Math.max(2, Math.min(min, max));
    }

    public int getGlobalSimMax() {
        int min = plugin.getConfig().getInt("distance_limits.simulation_distance.global_min", 2);
        int max = plugin.getConfig().getInt("distance_limits.simulation_distance.global_max", 10);
        return Math.max(getGlobalSimMin(), Math.max(min, max));
    }

    private int getViewMin(String worldName) {
        int def = getGlobalViewMin();
        int min = plugin.getConfig().getInt("worlds." + worldName + ".view_min", def);
        int max = plugin.getConfig().getInt("worlds." + worldName + ".view_max", getGlobalViewMax());
        return Math.max(2, Math.min(min, max));
    }

    private int getViewMax(String worldName) {
        int min = getViewMin(worldName);
        int max = plugin.getConfig().getInt("worlds." + worldName + ".view_max", getGlobalViewMax());
        return Math.max(min, max);
    }

    private int getSimMin(String worldName) {
        int def = getGlobalSimMin();
        int min = plugin.getConfig().getInt("worlds." + worldName + ".sim_min", def);
        int max = plugin.getConfig().getInt("worlds." + worldName + ".sim_max", getGlobalSimMax());
        return Math.max(2, Math.min(min, max));
    }

    private int getSimMax(String worldName) {
        int min = getSimMin(worldName);
        int max = plugin.getConfig().getInt("worlds." + worldName + ".sim_max", getGlobalSimMax());
        return Math.max(min, max);
    }

    private int validateConfigBound(String path, int def, int minLimit, int maxLimit) {
        int val = plugin.getConfig().getInt(path, def);
        return clamp(val, minLimit, maxLimit);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
