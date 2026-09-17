package com.dynamicdistance.metrics;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicLong;

public class TickTracker {

    private static final int HISTORY_SIZE = 100;

    private final long[] tickDurations = new long[HISTORY_SIZE];
    private int tickIndex = 0;
    private volatile boolean first = true;
    private final AtomicLong lastTickTime = new AtomicLong(System.nanoTime());

    public void start(DynamicDistance plugin) {
        if (DynamicDistance.isFolia()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.nanoTime();
                long last = lastTickTime.getAndSet(now);

                if (first) {
                    first = false;
                    return;
                }

                long elapsed = now - last;

                synchronized (tickDurations) {
                    tickDurations[tickIndex % HISTORY_SIZE] = elapsed;
                    tickIndex++;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public double getAverageTickTimeMs() {
        long total = 0;
        int count;

        synchronized (tickDurations) {
            count = Math.min(tickIndex, HISTORY_SIZE);
            if (count == 0) {
                return 20.0;
            }

            for (int i = 0; i < count; i++) {
                total += tickDurations[i];
            }
        }

        return (total / (double) count) / 1_000_000.0;
    }
}