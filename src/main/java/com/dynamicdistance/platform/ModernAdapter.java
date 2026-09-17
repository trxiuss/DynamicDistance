package com.dynamicdistance.platform;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ModernAdapter implements PlatformAdapter {

    private final DynamicDistance plugin;

    public ModernAdapter(DynamicDistance plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Paper/Spigot Modern";
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
        return getViewDistance(player);
    }

    @Override
    public boolean supportsSimulationDistance() {
        return false;
    }

    @Override
    public boolean supportsPerPlayerDistance() {
        return true;
    }
}
