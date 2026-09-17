package com.dynamicdistance.modules;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.engine.AdaptiveEngine;
import com.dynamicdistance.engine.DistanceController;
import com.dynamicdistance.metrics.ServerMetricsCollector;
import com.dynamicdistance.platform.PlatformAdapter;
import com.dynamicdistance.storage.PlayerData;
import com.dynamicdistance.storage.PlayerDataManager;
import com.dynamicdistance.util.ChatUtil;
import com.dynamicdistance.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatusBarManager {

    private final DynamicDistance plugin;
    private final AdaptiveEngine engine;
    private final ServerMetricsCollector metrics;
    private final DistanceController controller;
    private final PlatformAdapter adapter;
    private final PlayerDataManager playerDataManager;

    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    public StatusBarManager(
            DynamicDistance plugin,
            AdaptiveEngine engine,
            ServerMetricsCollector metrics,
            DistanceController controller,
            PlatformAdapter adapter,
            PlayerDataManager playerDataManager
    ) {
        this.plugin = plugin;
        this.engine = engine;
        this.metrics = metrics;
        this.controller = controller;
        this.adapter = adapter;
        this.playerDataManager = playerDataManager;
    }

    public void start() {
        this.running = true;
        SchedulerUtil.runAsyncTimer(this::updateBars, 20L, 20L);
    }

    public void stop() {
        this.running = false;
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerDataManager.getPlayerData(uuid);

        if (activeBars.containsKey(uuid)) {
            BossBar bar = activeBars.remove(uuid);
            if (bar != null) {
                bar.removePlayer(player);
            }
            data.setStatusBarEnabled(false);
            playerDataManager.saveDataAsync();
            return false;
        }

        BossBar bar = Bukkit.createBossBar(renderTitle(), BarColor.GREEN, BarStyle.SEGMENTED_10);
        bar.addPlayer(player);
        bar.setVisible(true);
        activeBars.put(uuid, bar);

        data.setStatusBarEnabled(true);
        playerDataManager.saveDataAsync();
        updateBars();
        return true;
    }

    public void onPlayerJoin(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());

        if (data.isStatusBarEnabled() && !activeBars.containsKey(player.getUniqueId())) {
            BossBar bar = Bukkit.createBossBar(renderTitle(), BarColor.GREEN, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            bar.setVisible(true);
            activeBars.put(player.getUniqueId(), bar);
        }
    }

    public void removePlayer(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    private void updateBars() {
        if (!running || activeBars.isEmpty()) return;

        String title = renderTitle();
        double qualityFactor = engine.getQualityFactor();
        double progress = Math.max(0.0, Math.min(1.0, qualityFactor));
        BarColor color = getBarColor(qualityFactor);

        for (Map.Entry<UUID, BossBar> entry : activeBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            BossBar bar = entry.getValue();

            if (player == null || !player.isOnline()) {
                activeBars.remove(entry.getKey());
                if (bar != null) bar.removeAll();
                continue;
            }

            SchedulerUtil.runEntityTask(player, () -> {
                if (bar != null && player.isOnline()) {
                    bar.setTitle(title);
                    bar.setProgress(progress);
                    bar.setColor(color);
                }
            });
        }
    }

    private String renderTitle() {
        double tps = engine.getSmoothedTps();
        double mspt = engine.getSmoothedMspt();
        double cpu = metrics.getCpuUsagePercent();
        double ram = metrics.getMemoryUsagePercent();
        int vd = controller.getCurrentTargetViewDistance();
        int sd = controller.getCurrentTargetSimDistance();

        String tpsColor = tps >= 19.0 ? "&a" : (tps >= 16.0 ? "&e" : "&c");
        String msptColor = mspt <= 35.0 ? "&a" : (mspt <= 45.0 ? "&e" : "&c");
        String cpuColor = cpu <= 50.0 ? "&a" : (cpu <= 80.0 ? "&e" : "&c");
        String ramColor = ram <= 70.0 ? "&a" : (ram <= 85.0 ? "&e" : "&c");

        return ChatUtil.color(
                "&7TPS: " + tpsColor + String.format("%.1f", tps) + " &8| " +
                "&7MSPT: " + msptColor + String.format("%.1f", mspt) + " &8| " +
                "&7CPU: " + cpuColor + String.format("%.1f", cpu) + "% &8| " +
                "&7RAM: " + ramColor + String.format("%.1f", ram) + "% &8| " +
                "&7VD: &#00E5FF" + vd + "c" +
                (adapter.supportsSimulationDistance() ? " &8| &7SD: &#00C6FF" + sd + "c" : "")
        );
    }

    private BarColor getBarColor(double qualityFactor) {
        if (qualityFactor >= 0.75) return BarColor.GREEN;
        if (qualityFactor >= 0.40) return BarColor.YELLOW;
        return BarColor.RED;
    }
}