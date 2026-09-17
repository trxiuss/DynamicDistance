package com.dynamicdistance.util;

import com.dynamicdistance.DynamicDistance;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UpdateChecker {

    private final DynamicDistance plugin;
    private final String currentVersion;

    public UpdateChecker(DynamicDistance plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void check(Consumer<String> onUpdateAvailable) {
        SchedulerUtil.runAsync(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/dynamicdistance/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "DynamicDistance-UpdateChecker/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonElement element = JsonParser.parseReader(reader);
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            if (array.size() > 0) {
                                String latestVersion = array.get(0).getAsJsonObject().get("version_number").getAsString();
                                if (isNewer(latestVersion, currentVersion)) {
                                    onUpdateAvailable.accept(latestVersion);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private boolean isNewer(String latest, String current) {
        try {
            String[] latParts = latest.replace("v", "").split("\\.");
            String[] curParts = current.replace("v", "").split("\\.");
            int length = Math.max(latParts.length, curParts.length);

            for (int i = 0; i < length; i++) {
                int l = i < latParts.length ? Integer.parseInt(latParts[i]) : 0;
                int c = i < curParts.length ? Integer.parseInt(curParts[i]) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}