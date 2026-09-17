package com.dynamicdistance.util;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchedulerUtil {

    private static final Class<?> PLUGIN_CLASS = Plugin.class;

    public static boolean isFolia() {
        return DynamicDistance.isFolia();
    }

    public static void runAsync(Runnable runnable) {
        Plugin plugin = DynamicDistance.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        if (isFolia()) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNow = asyncScheduler.getClass().getMethod("runNow", PLUGIN_CLASS, Consumer.class);
                runNow.invoke(asyncScheduler, plugin, (Consumer<Object>) o -> runnable.run());
                return;
            } catch (Throwable ignored) {}
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.start();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runAsyncTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
        Plugin plugin = DynamicDistance.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        long initialDelayMs = Math.max(0L, initialDelayTicks * 50L);
        long periodMs = Math.max(1L, periodTicks * 50L);

        if (isFolia()) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runAtFixedRate = asyncScheduler.getClass().getMethod("runAtFixedRate", PLUGIN_CLASS, Consumer.class, long.class, long.class, TimeUnit.class);
                runAtFixedRate.invoke(asyncScheduler, plugin, (Consumer<Object>) o -> runnable.run(), initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Throwable ignored) {}
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelayTicks, periodTicks);
    }

    public static boolean runEntityTask(Player player, Runnable runnable) {
        Plugin plugin = DynamicDistance.getInstance();
        if (plugin == null || !plugin.isEnabled() || player == null || !player.isOnline()) return false;

        if (isFolia()) {
            try {
                Object entityScheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method run = entityScheduler.getClass().getMethod("run", PLUGIN_CLASS, Consumer.class, Runnable.class);
                run.invoke(entityScheduler, plugin, (Consumer<Object>) o -> runnable.run(), null);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return true;
    }

    public static boolean runWorldTask(World world, Runnable runnable) {
        Plugin plugin = DynamicDistance.getInstance();
        if (plugin == null || !plugin.isEnabled() || world == null) return false;

        if (isFolia()) {
            try {
                Method getScheduler = world.getClass().getMethod("getScheduler");
                Object worldScheduler = getScheduler.invoke(world);
                Method run = worldScheduler.getClass().getMethod("run", PLUGIN_CLASS, Consumer.class);
                run.invoke(worldScheduler, plugin, (Consumer<Object>) o -> runnable.run());
                return true;
            } catch (Throwable ignored) {}
            return runGlobalRegionTask(runnable);
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return true;
    }

    public static boolean runGlobalRegionTask(Runnable runnable) {
        Plugin plugin = DynamicDistance.getInstance();
        if (plugin == null || !plugin.isEnabled()) return false;

        if (isFolia()) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run = globalScheduler.getClass().getMethod("run", PLUGIN_CLASS, Consumer.class);
                run.invoke(globalScheduler, plugin, (Consumer<Object>) o -> runnable.run());
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return true;
    }
}