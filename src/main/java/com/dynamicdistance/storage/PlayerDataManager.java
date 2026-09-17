package com.dynamicdistance.storage;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.util.SchedulerUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final DynamicDistance plugin;
    private final File dataFile;
    private final Gson gson;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(DynamicDistance plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadData();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public Map<UUID, PlayerData> getAllCachedData() {
        return cache;
    }

    public void setOverride(UUID uuid, int viewDistance, Integer simDistance, Long expiryTimestamp) {
        PlayerData data = getPlayerData(uuid);
        data.setCustomViewDistance(viewDistance);
        data.setCustomSimDistance(simDistance);
        data.setExpiryTimestamp(expiryTimestamp);
        saveDataAsync();
    }

    public void clearOverride(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.clearOverride();
            saveDataAsync();
        }
    }

    public void setStatusBarState(UUID uuid, boolean enabled) {
        PlayerData data = getPlayerData(uuid);
        data.setStatusBarEnabled(enabled);
        saveDataAsync();
    }

    public void removeIfNoOverride(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null && !data.hasPersistedData()) {
            cache.remove(uuid);
        }
    }

    public List<UUID> cleanupExpiredOverrides() {
        List<UUID> cleaned = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            PlayerData data = entry.getValue();
            if (data != null && data.isOverrideExpired()) {
                data.clearOverride();
                cleaned.add(entry.getKey());
            }
        }
        if (!cleaned.isEmpty()) {
            saveDataAsync();
        }
        return cleaned;
    }

    public void loadData() {
        if (!dataFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, StorageWrapper>>() {}.getType();
            Map<String, StorageWrapper> rawMap = gson.fromJson(reader, type);

            if (rawMap != null) {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, StorageWrapper> entry : rawMap.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        StorageWrapper wrapper = entry.getValue();

                        if (wrapper.expiry != null && now > wrapper.expiry) {
                            wrapper.view = null;
                            wrapper.sim = null;
                            wrapper.expiry = null;
                        }

                        PlayerData data = new PlayerData(uuid);
                        data.setCustomViewDistance(wrapper.view);
                        data.setCustomSimDistance(wrapper.sim);
                        data.setExpiryTimestamp(wrapper.expiry);
                        data.setStatusBarEnabled(wrapper.statusBar != null && wrapper.statusBar);

                        if (data.hasPersistedData()) {
                            cache.put(uuid, data);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load playerdata.json: " + e.getMessage());
        }
    }

    public void saveDataAsync() {
        SchedulerUtil.runAsync(this::saveDataSync);
    }

    public synchronized void saveDataSync() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Map<String, StorageWrapper> exportMap = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
                PlayerData data = entry.getValue();
                if (data.hasPersistedData()) {
                    StorageWrapper wrapper = new StorageWrapper();
                    wrapper.view = data.getCustomViewDistance();
                    wrapper.sim = data.getCustomSimDistance();
                    wrapper.expiry = data.getExpiryTimestamp();
                    wrapper.statusBar = data.isStatusBarEnabled();
                    exportMap.put(entry.getKey().toString(), wrapper);
                }
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(exportMap, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save playerdata.json: " + e.getMessage());
        }
    }

    private static class StorageWrapper {
        Integer view;
        Integer sim;
        Long expiry;
        Boolean statusBar;
    }
}