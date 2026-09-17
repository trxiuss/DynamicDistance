package com.dynamicdistance.hook;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.engine.AdaptiveEngine;
import com.dynamicdistance.engine.DistanceController;
import com.dynamicdistance.metrics.ServerMetricsCollector;
import com.dynamicdistance.platform.PlatformAdapter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private final DynamicDistance plugin;
    private final AdaptiveEngine engine;
    private final DistanceController controller;
    private final ServerMetricsCollector metrics;
    private final PlatformAdapter adapter;

    public PlaceholderAPIExpansion(
            DynamicDistance plugin,
            AdaptiveEngine engine,
            DistanceController controller,
            ServerMetricsCollector metrics,
            PlatformAdapter adapter
    ) {
        this.plugin = plugin;
        this.engine = engine;
        this.controller = controller;
        this.metrics = metrics;
        this.adapter = adapter;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dynamicdistance";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        String param = params.toLowerCase();

        switch (param) {
            case "view":
            case "view_distance":
                return player != null ? String.valueOf(adapter.getViewDistance(player)) : String.valueOf(controller.getCurrentTargetViewDistance());

            case "sim":
            case "sim_distance":
            case "simulation_distance":
                return player != null ? String.valueOf(adapter.getSimulationDistance(player)) : String.valueOf(controller.getCurrentTargetSimDistance());

            case "target_view":
                return String.valueOf(controller.getCurrentTargetViewDistance());

            case "target_sim":
                return String.valueOf(controller.getCurrentTargetSimDistance());

            case "tps":
                return String.format("%.2f", engine.getSmoothedTps());

            case "mspt":
                return String.format("%.1f", engine.getSmoothedMspt());

            case "cpu":
                return String.format("%.1f", metrics.getCpuUsagePercent());

            case "ram":
                return String.format("%.1f", metrics.getMemoryUsagePercent());

            case "ram_mb":
                return String.valueOf(metrics.getUsedMemoryMb());

            case "chunks":
                return String.valueOf(metrics.getTotalLoadedChunks());

            case "entities":
                return String.valueOf(metrics.getTotalEntities());

            case "load":
            case "load_score":
                return String.format("%.1f", engine.getLoadScore());

            case "state":
                return engine.getHealthState();

            case "reason":
                return player != null ? controller.getAppliedReason(player) : "N/A";

            default:
                return null;
        }
    }
}