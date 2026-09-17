package com.dynamicdistance.platform;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlatformDetector {

    public static PlatformAdapter detectAdapter(DynamicDistance plugin) {
        if (DynamicDistance.isFolia()) {
            return new FoliaAdapter(plugin);
        }

        try {
            Player.class.getMethod("setSimulationDistance", int.class);
            World.class.getMethod("setSimulationDistance", int.class);
            return new ModernSimAdapter(plugin);
        } catch (Throwable ignored) {}

        return new ModernAdapter(plugin);
    }
}