package com.herasgarden.gardenessentials.listener;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardencore.api.integration.IntegrationEventType;
import com.herasgarden.gardenessentials.command.UtilityCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class PlayerStateListener implements Listener {
    private final JavaPlugin plugin;
    private final GardenPlatform platform;
    private final UtilityCommand utilities;

    public PlayerStateListener(JavaPlugin plugin, GardenPlatform platform, UtilityCommand utilities) {
        this.plugin = plugin;
        this.platform = platform;
        this.utilities = utilities;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        utilities.rememberDeath(event.getEntity(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("iris.player-join-leave-alerts", false)) {
            publish(IntegrationEventType.PLAYER_JOINED, event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        utilities.clearPlayer(event.getPlayer().getUniqueId());
        if (plugin.getConfig().getBoolean("iris.player-join-leave-alerts", false)) {
            publish(IntegrationEventType.PLAYER_LEFT, event.getPlayer());
        }
    }

    private void publish(IntegrationEventType type, Player player) {
        try {
            platform.integrations().publish(
                    type,
                    "player",
                    player.getUniqueId().toString(),
                    "{\"playerUuid\":\"" + player.getUniqueId()
                            + "\",\"playerName\":\"" + json(player.getName()) + "\"}"
            );
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not queue " + type + " alert: " + exception.getMessage());
        }
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
