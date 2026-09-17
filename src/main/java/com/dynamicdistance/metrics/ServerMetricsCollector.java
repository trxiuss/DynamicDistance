package com.dynamicdistance.metrics;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

public class ServerMetricsCollector {

    private final DynamicDistance plugin;
    private final TickTracker tickTracker;

    private Method getTpsMethod;
    private Method getAverageTickTimeMethod;
    private Method getTickTimesMethod;

    private OperatingSystemMXBean osBean;
    private Object sunOsBean;
    private Method getProcessCpuLoadMethod;
    private Method getCpuLoadMethod;
    private Method getSystemCpuLoadMethod;

    private volatile int cachedLoadedChunks = 0;
    private volatile int cachedEntities = 0;

    public ServerMetricsCollector(DynamicDistance plugin, TickTracker tickTracker) {
        this.plugin = plugin;
        this.tickTracker = tickTracker;

        try {
            Class<?> serverClass = Bukkit.getServer().getClass();
            try { this.getTpsMethod = serverClass.getMethod("getTPS"); } catch (Throwable ignored) {}
            try { this.getAverageTickTimeMethod = serverClass.getMethod("getAverageTickTime"); } catch (Throwable ignored) {}
            try { this.getTickTimesMethod = serverClass.getMethod("getTickTimes"); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        try {
            this.osBean = ManagementFactory.getOperatingSystemMXBean();
            Class<?> sunOsClass = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (sunOsClass.isInstance(this.osBean)) {
                this.sunOsBean = this.osBean;
                try { this.getProcessCpuLoadMethod = sunOsClass.getMethod("getProcessCpuLoad"); } catch (Throwable ignored) {}
                try { this.getCpuLoadMethod = sunOsClass.getMethod("getCpuLoad"); } catch (Throwable ignored) {}
                try { this.getSystemCpuLoadMethod = sunOsClass.getMethod("getSystemCpuLoad"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public double getTps() {
        try {
            if (getTpsMethod != null) {
                Object result = getTpsMethod.invoke(Bukkit.getServer());
                if (result instanceof double[]) {
                    double[] tps = (double[]) result;
                    if (tps.length > 0) return clampTps(tps[0]);
                }
            }
        } catch (Throwable ignored) {}

        double mspt = getMspt();
        if (mspt <= 50.0) return 20.0;
        return clampTps(1000.0 / mspt);
    }

    public double getMspt() {
        try {
            if (getAverageTickTimeMethod != null) {
                Object result = getAverageTickTimeMethod.invoke(Bukkit.getServer());
                if (result instanceof Number) return Math.round(((Number) result).doubleValue() * 10.0) / 10.0;
            }
        } catch (Throwable ignored) {}

        try {
            if (getTickTimesMethod != null) {
                Object result = getTickTimesMethod.invoke(Bukkit.getServer());
                if (result instanceof long[]) {
                    long[] times = (long[]) result;
                    if (times.length > 0) {
                        long sum = 0;
                        for (long time : times) sum += time;
                        return Math.round(((sum / (double) times.length) / 1_000_000.0) * 10.0) / 10.0;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return tickTracker != null ? Math.round(tickTracker.getAverageTickTimeMs() * 10.0) / 10.0 : 20.0;
    }

    public double getCpuUsagePercent() {
        try {
            if (getProcessCpuLoadMethod != null && sunOsBean != null) {
                Object result = getProcessCpuLoadMethod.invoke(sunOsBean);
                if (result instanceof Number) {
                    double load = ((Number) result).doubleValue();
                    if (load >= 0.0) return Math.round(load * 1000.0) / 10.0;
                }
            }
            if (getCpuLoadMethod != null && sunOsBean != null) {
                Object result = getCpuLoadMethod.invoke(sunOsBean);
                if (result instanceof Number) {
                    double load = ((Number) result).doubleValue();
                    if (load >= 0.0) return Math.round(load * 1000.0) / 10.0;
                }
            }
            if (getSystemCpuLoadMethod != null && sunOsBean != null) {
                Object result = getSystemCpuLoadMethod.invoke(sunOsBean);
                if (result instanceof Number) {
                    double load = ((Number) result).doubleValue();
                    if (load >= 0.0) return Math.round(load * 1000.0) / 10.0;
                }
            }
        } catch (Throwable ignored) {}
        return 0.0;
    }

    public double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        if (max <= 0) return 0.0;
        return Math.round(((double) used / (double) max) * 1000.0) / 10.0;
    }

    public long getUsedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    public long getMaxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public int getAvailableCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public void refreshWorldCounts() {
        int chunks = 0;
        int entities = 0;
        try {
            for (World world : Bukkit.getWorlds()) {
                chunks += world.getLoadedChunks().length;
                entities += world.getEntities().size();
            }
        } catch (Throwable ignored) {}
        cachedLoadedChunks = chunks;
        cachedEntities = entities;
    }

    public int getTotalLoadedChunks() {
        return cachedLoadedChunks;
    }

    public int getTotalEntities() {
        return cachedEntities;
    }

    private double clampTps(double tps) {
        return Math.min(20.0, Math.max(0.0, Math.round(tps * 100.0) / 100.0));
    }
}
