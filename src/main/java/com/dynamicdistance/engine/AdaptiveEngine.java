package com.dynamicdistance.engine;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.metrics.ServerMetricsCollector;

public class AdaptiveEngine {

    private final DynamicDistance plugin;
    private final ServerMetricsCollector metrics;

    private double smoothedMspt = 20.0;
    private double smoothedTps = 20.0;
    private double smoothedLoadScore = 0.0;

    public AdaptiveEngine(DynamicDistance plugin, ServerMetricsCollector metrics) {
        this.plugin = plugin;
        this.metrics = metrics;
    }

    public synchronized void update() {
        double rawMspt = metrics.getMspt();
        double rawTps = metrics.getTps();
        double cpu = metrics.getCpuUsagePercent();
        double ram = metrics.getMemoryUsagePercent();
        int chunks = metrics.getTotalLoadedChunks();
        int entities = metrics.getTotalEntities();

        double alphaDegrade = Math.max(0.01, Math.min(1.0, plugin.getConfig().getDouble("engine.smoothing_factor_degrade", 0.45)));
        double alphaRecover = Math.max(0.01, Math.min(1.0, plugin.getConfig().getDouble("engine.smoothing_factor_recover", 0.06)));

        double msptAlpha = (rawMspt > smoothedMspt) ? alphaDegrade : alphaRecover;
        smoothedMspt = (msptAlpha * rawMspt) + ((1.0 - msptAlpha) * smoothedMspt);

        double tpsAlpha = (rawTps < smoothedTps) ? alphaDegrade : alphaRecover;
        smoothedTps = (tpsAlpha * rawTps) + ((1.0 - tpsAlpha) * smoothedTps);

        double targetMspt = Math.max(1.0, plugin.getConfig().getDouble("thresholds.target_mspt", 38.0));
        double criticalMspt = Math.max(targetMspt + 1.0, plugin.getConfig().getDouble("thresholds.critical_mspt", 48.0));
        double targetTps = Math.max(1.0, plugin.getConfig().getDouble("thresholds.target_tps", 19.5));
        double criticalTps = Math.max(1.0, Math.min(targetTps - 0.1, plugin.getConfig().getDouble("thresholds.critical_tps", 17.0)));
        double maxRam = Math.max(1.0, plugin.getConfig().getDouble("thresholds.max_memory_percent", 85.0));
        double maxCpu = Math.max(1.0, plugin.getConfig().getDouble("thresholds.max_cpu_percent", 85.0));
        int targetChunks = Math.max(100, plugin.getConfig().getInt("thresholds.target_chunks", 3500));
        int targetEntities = Math.max(100, plugin.getConfig().getInt("thresholds.target_entities", 2000));

        double msptScore = 0.0;
        if (smoothedMspt > targetMspt) {
            double range = criticalMspt - targetMspt;
            msptScore = ((smoothedMspt - targetMspt) / range) * 100.0;
        }

        double tpsScore = 0.0;
        if (smoothedTps < targetTps) {
            double range = targetTps - criticalTps;
            tpsScore = ((targetTps - smoothedTps) / range) * 100.0;
        }

        double ramScore = (ram / maxRam) * 100.0;
        double cpuScore = (cpu / maxCpu) * 100.0;
        double chunkScore = ((double) chunks / targetChunks) * 100.0;
        double entityScore = ((double) entities / targetEntities) * 100.0;

        msptScore = clampScore(msptScore);
        tpsScore = clampScore(tpsScore);
        ramScore = clampScore(ramScore);
        cpuScore = clampScore(cpuScore);
        chunkScore = clampScore(chunkScore);
        entityScore = clampScore(entityScore);

        double rawScore = (msptScore * 0.35) + (tpsScore * 0.25) + (ramScore * 0.10) + (cpuScore * 0.10) + (chunkScore * 0.10) + (entityScore * 0.10);
        double scoreAlpha = (rawScore > smoothedLoadScore) ? alphaDegrade : alphaRecover;
        smoothedLoadScore = (scoreAlpha * rawScore) + ((1.0 - scoreAlpha) * smoothedLoadScore);
    }

    private double clampScore(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    public synchronized double getSmoothedMspt() {
        return Math.round(smoothedMspt * 10.0) / 10.0;
    }

    public synchronized double getSmoothedTps() {
        return Math.round(smoothedTps * 100.0) / 100.0;
    }

    public synchronized double getLoadScore() {
        return Math.round(smoothedLoadScore * 10.0) / 10.0;
    }

    public synchronized double getQualityFactor() {
        double optimal = plugin.getConfig().getDouble("thresholds.optimal_load_score", 35.0);
        double critical = plugin.getConfig().getDouble("thresholds.critical_load_score", 85.0);

        if (critical <= optimal) {
            critical = optimal + 1.0;
        }

        double score = smoothedLoadScore;
        double q;

        if (score <= optimal) {
            q = 1.0;
        } else if (score >= critical) {
            q = 0.0;
        } else {
            q = 1.0 - ((score - optimal) / (critical - optimal));
        }

        return q * q * (3.0 - (2.0 * q));
    }

    public String getHealthState() {
        double score = getLoadScore();
        if (score < 35.0) return "Optimal";
        if (score < 65.0) return "Moderate";
        if (score < 85.0) return "Heavy";
        return "Critical";
    }
}