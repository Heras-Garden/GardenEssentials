package com.herasgarden.gardenessentials.iris;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardencore.api.integration.IntegrationEventType;
import com.herasgarden.gardenessentials.command.UtilityCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MinecraftXpService {
    private final JavaPlugin plugin;
    private final GardenPlatform platform;
    private final UtilityCommand utilities;
    private final MinecraftLinkService links;
    private final int amount;
    private final long intervalTicks;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private BukkitTask task;

    public MinecraftXpService(
            JavaPlugin plugin,
            GardenPlatform platform,
            UtilityCommand utilities,
            MinecraftLinkService links,
            int amount,
            long intervalMinutes
    ) {
        this.plugin = plugin;
        this.platform = platform;
        this.utilities = utilities;
        this.links = links;
        this.amount = Math.max(1, amount);
        this.intervalTicks = Math.max(1L, intervalMinutes) * 60L * 20L;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(
                plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void tick() {
        if (!running.compareAndSet(false, true)) return;

        List<PlayerSnapshot> eligible = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !utilities.isAfk(player.getUniqueId()))
                .map(player -> new PlayerSnapshot(player.getUniqueId(), player.getName()))
                .toList();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                for (PlayerSnapshot player : eligible) {
                    publishIfLinked(player);
                }
            } finally {
                running.set(false);
            }
        });
    }

    private void publishIfLinked(PlayerSnapshot player) {
        try {
            if (!links.isLinked(player.id())) return;
            platform.integrations().publish(
                    IntegrationEventType.MINECRAFT_XP_EARNED,
                    "player",
                    player.id().toString(),
                    "{\"playerUuid\":\"" + player.id()
                            + "\",\"playerName\":\"" + json(player.name())
                            + "\",\"amount\":" + amount + "}"
            );
        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Could not queue Minecraft XP for " + player.name() + ": "
                            + exception.getMessage());
        }
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record PlayerSnapshot(UUID id, String name) {
    }
}
