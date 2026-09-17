package com.dynamicdistance.modules;

import com.dynamicdistance.DynamicDistance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {

    private final DynamicDistance plugin;
    private final Map<String, GroupSettings> groupMap = new ConcurrentHashMap<>();
    private final Map<UUID, CachedGroup> cache = new ConcurrentHashMap<>();
    private final GroupSettings bypassGroup = new GroupSettings("bypass", 9999, 4, 2, true, true, true);

    private GroupSettings defaultGroup;
    private Class<?> luckPermsProviderClass;

    public PermissionManager(DynamicDistance plugin) {
        this.plugin = plugin;
        try {
            this.luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
        } catch (Throwable ignored) {}
        loadGroups();
    }

    public void reload() {
        groupMap.clear();
        cache.clear();
        loadGroups();
    }

    public void loadGroups() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("groups");
        if (section == null) {
            defaultGroup = new GroupSettings("default", 0, 0, 0, false, false, false);
            groupMap.put("default", defaultGroup);
            return;
        }

        for (String groupName : section.getKeys(false)) {
            int priority = section.getInt(groupName + ".priority", 0);
            int bonusVd = section.getInt(groupName + ".bonus_view_distance", 0);
            int bonusSd = section.getInt(groupName + ".bonus_sim_distance", 0);
            boolean bypassDynamic = section.getBoolean(groupName + ".bypass_dynamic", false);
            boolean bypassAfk = section.getBoolean(groupName + ".bypass_afk", false);
            boolean bypassSpeed = section.getBoolean(groupName + ".bypass_speed", false);

            GroupSettings settings = new GroupSettings(groupName, priority, bonusVd, bonusSd, bypassDynamic, bypassAfk, bypassSpeed);
            groupMap.put(groupName.toLowerCase(), settings);

            if (groupName.equalsIgnoreCase("default")) {
                defaultGroup = settings;
            }
        }

        if (defaultGroup == null) {
            defaultGroup = new GroupSettings("default", 0, 0, 0, false, false, false);
        }
    }

    public GroupSettings getPlayerGroupSettings(Player player) {
        if (player == null) return defaultGroup;

        long ttl = Math.max(1L, plugin.getConfig().getLong("engine.permission_cache_seconds", 10)) * 1000L;
        long now = System.currentTimeMillis();

        CachedGroup cached = cache.get(player.getUniqueId());
        if (cached != null && cached.expires > now) {
            return cached.settings;
        }

        GroupSettings settings = resolve(player);
        cache.put(player.getUniqueId(), new CachedGroup(settings, now + ttl));
        return settings;
    }

    private GroupSettings resolve(Player player) {
        if (player.isPermissionSet("dynamicdistance.bypass") && player.hasPermission("dynamicdistance.bypass")) {
            return bypassGroup;
        }

        List<GroupSettings> matchingGroups = new ArrayList<>();
        Set<String> added = new HashSet<>();

        if (luckPermsProviderClass != null) {
            try {
                Object lp = luckPermsProviderClass.getMethod("get").invoke(null);
                Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
                Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());

                if (user != null) {
                    String primaryGroup = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
                    if (primaryGroup != null && groupMap.containsKey(primaryGroup.toLowerCase())) {
                        addGroup(matchingGroups, added, groupMap.get(primaryGroup.toLowerCase()));
                    }

                    Method getNodes = user.getClass().getMethod("getNodes");
                    Collection<?> nodes = (Collection<?>) getNodes.invoke(user);

                    for (Object node : nodes) {
                        Method getKey = node.getClass().getMethod("getKey");
                        String key = (String) getKey.invoke(node);
                        if (key.startsWith("group.")) {
                            String groupName = key.substring(6).toLowerCase();
                            if (groupMap.containsKey(groupName)) {
                                addGroup(matchingGroups, added, groupMap.get(groupName));
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        for (Map.Entry<String, GroupSettings> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();
            if (player.hasPermission("group." + groupName) ||
                player.hasPermission("dynamicdistance.group." + groupName) ||
                player.hasPermission("dynamicdistance." + groupName)) {
                addGroup(matchingGroups, added, entry.getValue());
            }
        }

        if (!matchingGroups.isEmpty()) {
            matchingGroups.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            return matchingGroups.get(0);
        }

        return defaultGroup;
    }

    private void addGroup(List<GroupSettings> matchingGroups, Set<String> added, GroupSettings settings) {
        if (settings != null && added.add(settings.getName().toLowerCase())) {
            matchingGroups.add(settings);
        }
    }

    public void removeCache(UUID uuid) {
        cache.remove(uuid);
    }

    public static class GroupSettings {
        private final String name;
        private final int priority;
        private final int bonusViewDistance;
        private final int bonusSimDistance;
        private final boolean bypassDynamic;
        private final boolean bypassAfk;
        private final boolean bypassSpeed;

        public GroupSettings(String name, int priority, int bonusViewDistance, int bonusSimDistance, boolean bypassDynamic, boolean bypassAfk, boolean bypassSpeed) {
            this.name = name;
            this.priority = priority;
            this.bonusViewDistance = bonusViewDistance;
            this.bonusSimDistance = bonusSimDistance;
            this.bypassDynamic = bypassDynamic;
            this.bypassAfk = bypassAfk;
            this.bypassSpeed = bypassSpeed;
        }

        public String getName() { return name; }
        public int getPriority() { return priority; }
        public int getBonusViewDistance() { return bonusViewDistance; }
        public int getBonusSimDistance() { return bonusSimDistance; }
        public boolean isBypassDynamic() { return bypassDynamic; }
        public boolean isBypassAfk() { return bypassAfk; }
        public boolean isBypassSpeed() { return bypassSpeed; }
    }

    private static class CachedGroup {
        private final GroupSettings settings;
        private final long expires;

        private CachedGroup(GroupSettings settings, long expires) {
            this.settings = settings;
            this.expires = expires;
        }
    }
}