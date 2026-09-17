package com.dynamicdistance.platform;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ModernSimAdapter implements PlatformAdapter {

    private final DynamicDistance plugin;

    public ModernSimAdapter(DynamicDistance plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Paper/Purpur 1.18.2+ Dual Distance Engine";
    }

    @Override
    public void setViewDistance(Player player, int distance) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            if (player.getViewDistance() != distance) {
                player.setViewDistance(distance);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setSimulationDistance(Player player, int distance) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            if (player.getSimulationDistance() != distance) {
                player.setSimulationDistance(distance);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setWorldViewDistance(World world, int distance) {
        if (world == null) {
            return;
        }

        try {
            world.setViewDistance(distance);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setWorldSimulationDistance(World world, int distance) {
        if (world == null) {
            return;
        }

        try {
            world.setSimulationDistance(distance);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int getViewDistance(Player player) {
        try {
            return player.getViewDistance();
        } catch (Throwable e) {
            return 8;
        }
    }

    @Override
    public int getSimulationDistance(Player player) {
        try {
            return player.getSimulationDistance();
        } catch (Throwable e) {
            return getViewDistance(player);
        }
    }

    @Override
    public boolean supportsSimulationDistance() {
        return true;
    }

    @Override
    public boolean supportsPerPlayerDistance() {
        return true;
    }
}
