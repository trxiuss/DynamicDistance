package com.dynamicdistance.commands;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.engine.AdaptiveEngine;
import com.dynamicdistance.engine.DistanceController;
import com.dynamicdistance.locale.LanguageManager;
import com.dynamicdistance.metrics.ServerMetricsCollector;
import com.dynamicdistance.modules.AfkManager;
import com.dynamicdistance.modules.PermissionManager;
import com.dynamicdistance.modules.PermissionManager.GroupSettings;
import com.dynamicdistance.modules.StatusBarManager;
import com.dynamicdistance.platform.PlatformAdapter;
import com.dynamicdistance.storage.PlayerData;
import com.dynamicdistance.storage.PlayerDataManager;
import com.dynamicdistance.util.SchedulerUtil;
import com.dynamicdistance.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DynamicDistanceCommand implements TabExecutor {

    private final DynamicDistance plugin;
    private final LanguageManager lang;
    private final ServerMetricsCollector metrics;
    private final AdaptiveEngine engine;
    private final DistanceController controller;
    private final PlatformAdapter adapter;
    private final PlayerDataManager playerDataManager;
    private final AfkManager afkManager;
    private final PermissionManager permissionManager;
    private final StatusBarManager statusBarManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList("status", "statusbar", "reload", "setoverride", "clearoverride", "info");
    private static final List<String> DURATION_SUGGESTIONS = Arrays.asList("30s", "1m", "5m", "15m", "30m", "1h", "12h", "1d");

    public DynamicDistanceCommand(
            DynamicDistance plugin,
            LanguageManager lang,
            ServerMetricsCollector metrics,
            AdaptiveEngine engine,
            DistanceController controller,
            PlatformAdapter adapter,
            PlayerDataManager dataManager,
            AfkManager afk,
            PermissionManager perm,
            StatusBarManager statusBarManager
    ) {
        this.plugin = plugin;
        this.lang = lang;
        this.metrics = metrics;
        this.engine = engine;
        this.controller = controller;
        this.adapter = adapter;
        this.playerDataManager = dataManager;
        this.afkManager = afk;
        this.permissionManager = perm;
        this.statusBarManager = statusBarManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dynamicdistance.admin")) {
            sender.sendMessage(lang.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status":
                handleStatus(sender);
                break;
            case "statusbar":
            case "bar":
            case "hud":
                handleStatusBar(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "setoverride":
                handleSetOverride(sender, args);
                break;
            case "clearoverride":
                handleClearOverride(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        SchedulerUtil.runAsync(() -> {
            plugin.reloadConfig();
            plugin.validateAndFixConfig();
            permissionManager.reload();
            lang.loadLanguage(plugin.getConfig().getString("language", "tr"));
            controller.reload();
            sender.sendMessage(lang.getMessage("command-reload"));
        });
    }

    private void handleStatusBar(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", args[1]);
                sender.sendMessage(lang.getMessage("player-not-found", ph));
                return;
            }

            boolean enabled = statusBarManager.toggle(target);
            Map<String, String> ph = new HashMap<>();
            ph.put("player", target.getName());

            if (enabled) {
                sender.sendMessage(lang.getMessage("command-statusbar-other-enabled", ph));
                target.sendMessage(lang.getMessage("command-statusbar-enabled"));
            } else {
                sender.sendMessage(lang.getMessage("command-statusbar-other-disabled", ph));
                target.sendMessage(lang.getMessage("command-statusbar-disabled"));
            }
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("player-only-command"));
            return;
        }

        Player player = (Player) sender;
        boolean enabled = statusBarManager.toggle(player);

        if (enabled) {
            player.sendMessage(lang.getMessage("command-statusbar-enabled"));
        } else {
            player.sendMessage(lang.getMessage("command-statusbar-disabled"));
        }
    }

    private void handleStatus(CommandSender sender) {
        Map<String, String> ph = new HashMap<>();
        ph.put("platform", adapter.getPlatformName());
        ph.put("tps", String.valueOf(engine.getSmoothedTps()));
        ph.put("mspt", String.valueOf(engine.getSmoothedMspt()));
        ph.put("cpu", String.valueOf(metrics.getCpuUsagePercent()));
        ph.put("cores", String.valueOf(metrics.getAvailableCores()));
        ph.put("ram", String.valueOf(metrics.getMemoryUsagePercent()));
        ph.put("ram_mb", String.valueOf(metrics.getUsedMemoryMb()));
        ph.put("ram_max", String.valueOf(metrics.getMaxMemoryMb()));
        ph.put("chunks", String.valueOf(metrics.getTotalLoadedChunks()));
        ph.put("entities", String.valueOf(metrics.getTotalEntities()));
        ph.put("vd", String.valueOf(controller.getCurrentTargetViewDistance()));
        ph.put("sd", adapter.supportsSimulationDistance() ? String.valueOf(controller.getCurrentTargetSimDistance()) : "N/A");
        ph.put("load", String.valueOf(engine.getLoadScore()));
        ph.put("state", engine.getHealthState());

        sender.sendMessage(lang.getMessage("command-status-header", ph));
        sender.sendMessage(lang.getMessage("command-status-platform", ph));
        sender.sendMessage(lang.getMessage("command-status-tps", ph));
        sender.sendMessage(lang.getMessage("command-status-cpu", ph));
        sender.sendMessage(lang.getMessage("command-status-ram", ph));
        sender.sendMessage(lang.getMessage("command-status-world-stats", ph));
        sender.sendMessage(lang.getMessage("command-status-current-vd", ph));

        if (adapter.supportsSimulationDistance()) {
            sender.sendMessage(lang.getMessage("command-status-current-sd", ph));
        }

        sender.sendMessage(lang.getMessage("command-status-load", ph));
        sender.sendMessage(lang.getMessage("command-status-footer", ph));
    }

    private void handleSetOverride(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.getMessage("command-help-override"));
            return;
        }

        Integer view;
        Integer sim = null;
        Long expiry = null;

        try {
            view = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(lang.getMessage("invalid-number"));
            return;
        }

        if (args.length >= 4) {
            try {
                sim = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                Long duration = TimeUtil.parseDurationToMillis(args[3]);
                if (duration != null) {
                    expiry = System.currentTimeMillis() + duration;
                } else {
                    sender.sendMessage(lang.getMessage("invalid-duration"));
                    return;
                }
            }
        }

        if (args.length >= 5 && expiry == null) {
            Long duration = TimeUtil.parseDurationToMillis(args[4]);
            if (duration != null) {
                expiry = System.currentTimeMillis() + duration;
            } else {
                sender.sendMessage(lang.getMessage("invalid-duration"));
                return;
            }
        }

        if (!adapter.supportsSimulationDistance()) {
            sim = null;
        }

        int minVd = controller.getGlobalViewMin();
        int maxVd = controller.getGlobalViewMax();
        view = Math.max(minVd, Math.min(maxVd, view));

        if (sim != null) {
            int minSd = controller.getGlobalSimMin();
            int maxSd = controller.getGlobalSimMax();
            sim = Math.max(minSd, Math.min(maxSd, sim));
        }

        String durationStr = expiry != null ? TimeUtil.formatRemainingTime(expiry - System.currentTimeMillis()) : lang.getMessage("duration-permanent");

        if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerDataManager.setOverride(p.getUniqueId(), view, sim, expiry);
                controller.applyToPlayer(p);
            }

            Map<String, String> ph = new HashMap<>();
            ph.put("view", String.valueOf(view));
            ph.put("sim", sim != null ? String.valueOf(sim) : "Oto");
            ph.put("duration", durationStr);
            sender.sendMessage(lang.getMessage("command-override-set-all", ph));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", args[1]);
            sender.sendMessage(lang.getMessage("player-not-found", ph));
            return;
        }

        playerDataManager.setOverride(target.getUniqueId(), view, sim, expiry);
        controller.applyToPlayer(target);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());
        ph.put("view", String.valueOf(view));
        ph.put("sim", sim != null ? String.valueOf(sim) : "Oto");
        ph.put("duration", durationStr);
        sender.sendMessage(lang.getMessage("command-override-set", ph));
    }

    private void handleClearOverride(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("command-help-clear"));
            return;
        }

        if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) {
            for (UUID uuid : new ArrayList<>(playerDataManager.getAllCachedData().keySet())) {
                playerDataManager.clearOverride(uuid);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                controller.applyToPlayer(p);
            }
            sender.sendMessage(lang.getMessage("command-override-cleared-all"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            playerDataManager.clearOverride(target.getUniqueId());
            controller.applyToPlayer(target);
            Map<String, String> ph = new HashMap<>();
            ph.put("player", target.getName());
            sender.sendMessage(lang.getMessage("command-override-cleared", ph));
            return;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            playerDataManager.clearOverride(offline.getUniqueId());
            Map<String, String> ph = new HashMap<>();
            ph.put("player", offline.getName() != null ? offline.getName() : args[1]);
            sender.sendMessage(lang.getMessage("command-override-cleared", ph));
            return;
        }

        Map<String, String> ph = new HashMap<>();
        ph.put("player", args[1]);
        sender.sendMessage(lang.getMessage("player-not-found", ph));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("command-help-info"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", args[1]);
            sender.sendMessage(lang.getMessage("player-not-found", ph));
            return;
        }

        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        GroupSettings group = permissionManager.getPlayerGroupSettings(target);

        String afkStatus = afkManager.isAfk(target) ? lang.getMessage("status-yes") : lang.getMessage("status-no");
        String overrideStatus;
        if (data.hasOverride()) {
            String timePart = data.isTimedOverride() ? " &8(&6" + data.getFormattedRemainingTime() + "&8)" : "";
            overrideStatus = "VD: " + data.getCustomViewDistance() + " | SD: " + data.getCustomSimDistance() + timePart;
        } else {
            overrideStatus = lang.getMessage("status-none");
        }

        String reason = controller.getAppliedReason(target);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());
        ph.put("view", String.valueOf(adapter.getViewDistance(target)));
        ph.put("sim", String.valueOf(adapter.getSimulationDistance(target)));
        ph.put("reason", reason);
        ph.put("afk", afkStatus);
        ph.put("group", group.getName().toUpperCase() + " &8(+&b" + group.getBonusViewDistance() + " VD&8, +&3" + group.getBonusSimDistance() + " SD&8)");
        ph.put("override", overrideStatus);

        sender.sendMessage(lang.getMessage("command-info-header", ph));
        sender.sendMessage(lang.getMessage("command-info-view", ph));

        if (adapter.supportsSimulationDistance()) {
            sender.sendMessage(lang.getMessage("command-info-sim", ph));
        }

        sender.sendMessage(lang.getMessage("command-info-reason", ph));
        sender.sendMessage(lang.getMessage("command-info-afk", ph));
        sender.sendMessage(lang.getMessage("command-info-group", ph));
        sender.sendMessage(lang.getMessage("command-info-override", ph));
        sender.sendMessage(lang.getMessage("command-info-footer", ph));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lang.getMessage("command-help-header"));
        sender.sendMessage(lang.getMessage("command-help-status"));
        sender.sendMessage(lang.getMessage("command-help-statusbar"));
        sender.sendMessage(lang.getMessage("command-help-reload"));
        sender.sendMessage(lang.getMessage("command-help-override"));
        sender.sendMessage(lang.getMessage("command-help-clear"));
        sender.sendMessage(lang.getMessage("command-help-info"));
        sender.sendMessage(lang.getMessage("command-help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dynamicdistance.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("statusbar") || sub.equals("bar") || sub.equals("hud") || sub.equals("info")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            }

            if (sub.equals("setoverride") || sub.equals("clearoverride")) {
                List<String> playerNames = new ArrayList<>();
                playerNames.add("all");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setoverride")) {
            int min = controller.getGlobalViewMin();
            int max = controller.getGlobalViewMax();
            List<String> views = new ArrayList<>();
            for (int i = min; i <= max; i++) views.add(String.valueOf(i));
            StringUtil.copyPartialMatches(args[2], views, completions);
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("setoverride")) {
            List<String> suggestions = new ArrayList<>();
            if (adapter.supportsSimulationDistance()) {
                int min = controller.getGlobalSimMin();
                int max = controller.getGlobalSimMax();
                for (int i = min; i <= max; i++) suggestions.add(String.valueOf(i));
            }
            suggestions.addAll(DURATION_SUGGESTIONS);
            StringUtil.copyPartialMatches(args[3], suggestions, completions);
            return completions;
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("setoverride")) {
            StringUtil.copyPartialMatches(args[4], DURATION_SUGGESTIONS, completions);
            return completions;
        }

        return Collections.emptyList();
    }
}