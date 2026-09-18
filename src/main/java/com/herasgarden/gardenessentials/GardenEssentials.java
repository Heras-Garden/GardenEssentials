package com.herasgarden.gardenessentials;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardencore.api.integration.IntegrationEventType;
import com.herasgarden.gardenessentials.command.LinkCommand;
import com.herasgarden.gardenessentials.command.ReportCommand;
import com.herasgarden.gardenessentials.command.StaffAlertCommand;
import com.herasgarden.gardenessentials.command.UtilityCommand;
import com.herasgarden.gardenessentials.iris.MinecraftLinkService;
import com.herasgarden.gardenessentials.iris.MinecraftXpService;
import com.herasgarden.gardenessentials.listener.PlayerStateListener;
import com.herasgarden.gardenessentials.location.LocationService;
import com.herasgarden.gardenessentials.social.TeleportRequestService;
import com.herasgarden.gardenessentials.storage.EssentialsSchema;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public final class GardenEssentials extends JavaPlugin {
    private GardenPlatform platform;
    private MinecraftXpService minecraftXp;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<GardenPlatform> registration =
                getServer().getServicesManager().getRegistration(GardenPlatform.class);
        if (registration == null || registration.getProvider() == null) {
            getLogger().severe("GardenCore platform service is unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        platform = registration.getProvider();

        try {
            EssentialsSchema.ensure(platform.storage());
        } catch (SQLException exception) {
            getLogger().severe("GardenEssentials could not prepare storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LocationService locations = new LocationService(platform.storage());
        TeleportRequestService requests = new TeleportRequestService(
                getConfig().getLong("teleport-requests.expire-seconds", 60L));
        UtilityCommand utilities = new UtilityCommand(platform, locations, requests);
        MinecraftLinkService links = new MinecraftLinkService(
                platform,
                getConfig().getLong("iris.account-link.code-expire-minutes", 10L));

        for (String commandName : List.of(
                "balance", "pay", "spawn", "setspawn",
                "warp", "warps", "setwarp", "delwarp",
                "tpa", "tpaccept", "tpdeny",
                "msg", "reply", "back", "afk", "heal", "feed", "fly")) {
            PluginCommand command = getCommand(commandName);
            if (command != null) {
                command.setExecutor(utilities);
                command.setTabCompleter(utilities);
            }
        }

        ReportCommand reports = new ReportCommand(platform);
        PluginCommand report = getCommand("report");
        if (report != null) {
            report.setExecutor(reports);
            report.setTabCompleter(reports);
        }

        PluginCommand staffAlert = getCommand("staffalert");
        if (staffAlert != null) {
            staffAlert.setExecutor(new StaffAlertCommand(platform));
        }

        LinkCommand linkCommand = new LinkCommand(links);
        PluginCommand link = getCommand("link");
        if (link != null) {
            link.setExecutor(linkCommand);
            link.setTabCompleter(linkCommand);
        }

        if (getConfig().getBoolean("iris.minecraft-xp.enabled", true)) {
            minecraftXp = new MinecraftXpService(
                    this,
                    platform,
                    utilities,
                    links,
                    getConfig().getInt("iris.minecraft-xp.amount", 10),
                    getConfig().getLong("iris.minecraft-xp.interval-minutes", 5L));
            minecraftXp.start();
        }

        getServer().getPluginManager().registerEvents(
                new PlayerStateListener(this, platform, utilities), this);

        if (getConfig().getBoolean("iris.server-lifecycle-alerts", true)) {
            publishLifecycle(IntegrationEventType.SERVER_STARTED);
        }

        getLogger().info("GardenEssentials enabled. Utilities, Iris reports, account linking, and Minecraft XP are active.");
    }

    @Override
    public void onDisable() {
        if (minecraftXp != null) {
            minecraftXp.stop();
        }
        if (platform != null && getConfig().getBoolean("iris.server-lifecycle-alerts", true)) {
            publishLifecycle(IntegrationEventType.SERVER_STOPPING);
        }
    }

    private void publishLifecycle(IntegrationEventType type) {
        try {
            platform.integrations().publish(
                    type,
                    "server",
                    "garden-smp",
                    "{\"minecraftVersion\":\"" + json(getServer().getMinecraftVersion())
                            + "\",\"onlinePlayers\":" + getServer().getOnlinePlayers().size() + "}"
            );
        } catch (SQLException exception) {
            getLogger().warning("Could not queue " + type + " event for Iris: " + exception.getMessage());
        }
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
