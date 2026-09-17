package com.dynamicdistance;

import com.dynamicdistance.commands.DynamicDistanceCommand;
import com.dynamicdistance.engine.AdaptiveEngine;
import com.dynamicdistance.engine.DistanceController;
import com.dynamicdistance.hook.PlaceholderAPIExpansion;
import com.dynamicdistance.locale.LanguageManager;
import com.dynamicdistance.metrics.ServerMetricsCollector;
import com.dynamicdistance.metrics.TickTracker;
import com.dynamicdistance.modules.AfkManager;
import com.dynamicdistance.modules.PermissionManager;
import com.dynamicdistance.modules.SpeedManager;
import com.dynamicdistance.modules.StatusBarManager;
import com.dynamicdistance.platform.PlatformAdapter;
import com.dynamicdistance.platform.PlatformDetector;
import com.dynamicdistance.storage.PlayerDataManager;
import com.dynamicdistance.util.SchedulerUtil;
import com.dynamicdistance.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicDistance extends JavaPlugin implements Listener {

    private static DynamicDistance instance;
    private static boolean isFoliaServer = false;

    private LanguageManager languageManager;
    private PlayerDataManager playerDataManager;
    private TickTracker tickTracker;
    private ServerMetricsCollector metricsCollector;
    private PlatformAdapter platformAdapter;
    private AdaptiveEngine adaptiveEngine;
    private DistanceController distanceController;
    private AfkManager afkManager;
    private SpeedManager speedManager;
    private PermissionManager permissionManager;
    private StatusBarManager statusBarManager;
    private String latestFoundVersion = null;

    @Override
    public void onLoad() {
        instance = this;
        checkFolia();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        validateAndFixConfig();

        this.languageManager = new LanguageManager(this);
        this.languageManager.loadLanguage(getConfig().getString("language", "tr"));

        this.playerDataManager = new PlayerDataManager(this);
        this.tickTracker = new TickTracker();
        this.tickTracker.start(this);
        this.metricsCollector = new ServerMetricsCollector(this, tickTracker);
        this.platformAdapter = PlatformDetector.detectAdapter(this);
        this.permissionManager = new PermissionManager(this);
        this.speedManager = new SpeedManager(this);
        this.adaptiveEngine = new AdaptiveEngine(this, metricsCollector);
        this.afkManager = new AfkManager(this);

        this.distanceController = new DistanceController(
                this,
                adaptiveEngine,
                metricsCollector,
                platformAdapter,
                playerDataManager,
                afkManager,
                speedManager,
                permissionManager
        );

        this.afkManager.setController(this.distanceController);

        this.statusBarManager = new StatusBarManager(
                this,
                adaptiveEngine,
                metricsCollector,
                distanceController,
                platformAdapter,
                playerDataManager
        );
        this.statusBarManager.start();

        this.distanceController.start();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(this, adaptiveEngine, distanceController, metricsCollector, platformAdapter).register();
        }

        if (getConfig().getBoolean("metrics", true)) {
            int pluginId = 33461;
            Metrics bStats = new Metrics(this, pluginId);
            bStats.addCustomChart(new SimplePie("plugin_language", () -> getConfig().getString("language", "tr")));
            bStats.addCustomChart(new SimplePie("platform_engine", () -> platformAdapter.getPlatformName()));
            bStats.addCustomChart(new SimplePie("dual_distance_mode", () -> platformAdapter.supportsSimulationDistance() ? "Dual (View+Sim)" : "View Only"));
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        long autoSaveMinutes = Math.max(1L, getConfig().getLong("storage.auto_save_interval_minutes", 5));
        long autoSaveTicks = autoSaveMinutes * 60L * 20L;
        SchedulerUtil.runAsyncTimer(() -> playerDataManager.saveDataSync(), autoSaveTicks, autoSaveTicks);

        DynamicDistanceCommand command = new DynamicDistanceCommand(
                this,
                languageManager,
                metricsCollector,
                adaptiveEngine,
                distanceController,
                platformAdapter,
                playerDataManager,
                afkManager,
                permissionManager,
                statusBarManager
        );

        PluginCommand pluginCommand = getCommand("dynamicdistance");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        if (getConfig().getBoolean("update_checker", true)) {
            new UpdateChecker(this).check(latest -> {
                this.latestFoundVersion = latest;
                Map<String, String> ph = new HashMap<>();
                ph.put("latest", latest);
                ph.put("current", getDescription().getVersion());
                getLogger().info("Yeni guncelleme mevcut: v" + latest + " (Mevcut: v" + getDescription().getVersion() + ")");
            });
        }

        getLogger().info("DynamicDistance aktif edildi -> " + platformAdapter.getPlatformName());
    }

    @Override
    public void onDisable() {
        if (statusBarManager != null) {
            statusBarManager.stop();
        }
        if (playerDataManager != null) {
            playerDataManager.saveDataSync();
        }
        getLogger().info("DynamicDistance devre disi birakildi.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SchedulerUtil.runEntityTask(player, () -> {
            if (distanceController != null) {
                distanceController.applyToPlayer(player);
            }
            if (statusBarManager != null) {
                statusBarManager.onPlayerJoin(player);
            }
            if (latestFoundVersion != null && player.hasPermission("dynamicdistance.admin")) {
                Map<String, String> ph = new HashMap<>();
                ph.put("latest", latestFoundVersion);
                ph.put("current", getDescription().getVersion());
                player.sendMessage(languageManager.getMessage("update-available", ph));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        SchedulerUtil.runEntityTask(player, () -> {
            if (distanceController != null) {
                distanceController.applyToPlayer(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (speedManager != null) speedManager.remove(uuid);
        if (permissionManager != null) permissionManager.removeCache(uuid);
        if (statusBarManager != null) statusBarManager.removePlayer(player);
        if (distanceController != null) distanceController.removePlayer(uuid);
        if (playerDataManager != null) playerDataManager.removeIfNoOverride(uuid);
    }

    public void validateAndFixConfig() {
        boolean changed = false;
        int viewMin = getConfig().getInt("distance_limits.view_distance.global_min", 3);
        int viewMax = getConfig().getInt("distance_limits.view_distance.global_max", 14);
        if (viewMin < 2) {
            getConfig().set("distance_limits.view_distance.global_min", 2);
            viewMin = 2;
            changed = true;
        }
        if (viewMax < viewMin) {
            getConfig().set("distance_limits.view_distance.global_max", viewMin);
            changed = true;
            getLogger().warning("Config fix: view_distance.global_max was less than global_min, corrected.");
        }
        int simMin = getConfig().getInt("distance_limits.simulation_distance.global_min", 2);
        int simMax = getConfig().getInt("distance_limits.simulation_distance.global_max", 10);
        if (simMin < 2) {
            getConfig().set("distance_limits.simulation_distance.global_min", 2);
            simMin = 2;
            changed = true;
        }
        if (simMax < simMin) {
            getConfig().set("distance_limits.simulation_distance.global_max", simMin);
            changed = true;
            getLogger().warning("Config fix: simulation_distance.global_max was less than global_min, corrected.");
        }
        long interval = getConfig().getLong("engine.check_interval", 2);
        if (interval < 1) {
            getConfig().set("engine.check_interval", 1);
            changed = true;
            getLogger().warning("Config fix: engine.check_interval was less than 1, set to 1.");
        }
        long cooldown = getConfig().getLong("engine.recovery_cooldown", 15);
        if (cooldown < 0) {
            getConfig().set("engine.recovery_cooldown", 0);
            changed = true;
        }
        if (getConfig().isConfigurationSection("worlds")) {
            for (String worldName : getConfig().getConfigurationSection("worlds").getKeys(false)) {
                String basePath = "worlds." + worldName;
                int wViewMin = getConfig().getInt(basePath + ".view_min", viewMin);
                int wViewMax = getConfig().getInt(basePath + ".view_max", viewMax);
                if (wViewMin < 2) {
                    getConfig().set(basePath + ".view_min", 2);
                    wViewMin = 2;
                    changed = true;
                }
                if (wViewMax < wViewMin) {
                    getConfig().set(basePath + ".view_max", wViewMin);
                    changed = true;
                    getLogger().warning("Config fix: worlds." + worldName + ".view_max was less than view_min, corrected.");
                }
                int wSimMin = getConfig().getInt(basePath + ".sim_min", simMin);
                int wSimMax = getConfig().getInt(basePath + ".sim_max", simMax);
                if (wSimMin < 2) {
                    getConfig().set(basePath + ".sim_min", 2);
                    wSimMin = 2;
                    changed = true;
                }
                if (wSimMax < wSimMin) {
                    getConfig().set(basePath + ".sim_max", wSimMin);
                    changed = true;
                    getLogger().warning("Config fix: worlds." + worldName + ".sim_max was less than sim_min, corrected.");
                }
            }
        }
        if (changed) {
            saveConfig();
            getLogger().info("Config validation applied corrections.");
        }
    }

    private static void checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFoliaServer = true;
        } catch (ClassNotFoundException e) {
            isFoliaServer = false;
        }
    }

    public static DynamicDistance getInstance() {
        return instance;
    }

    public static boolean isFolia() {
        return isFoliaServer;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }

    public DistanceController getDistanceController() {
        return distanceController;
    }

    public StatusBarManager getStatusBarManager() {
        return statusBarManager;
    }
}