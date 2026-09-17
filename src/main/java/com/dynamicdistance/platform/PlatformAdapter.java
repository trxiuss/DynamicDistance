package com.dynamicdistance.platform;

import org.bukkit.World;
import org.bukkit.entity.Player;

public interface PlatformAdapter {

    String getPlatformName();

    void setViewDistance(Player player, int distance);

    void setSimulationDistance(Player player, int distance);

    void setWorldViewDistance(World world, int distance);

    void setWorldSimulationDistance(World world, int distance);

    int getViewDistance(Player player);

    int getSimulationDistance(Player player);

    boolean supportsSimulationDistance();

    boolean supportsPerPlayerDistance();
}