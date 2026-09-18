package com.herasgarden.gardenessentials.command;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardencore.api.integration.IntegrationEventType;
import com.herasgarden.gardenessentials.ui.EssentialsMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.Arrays;

public final class StaffAlertCommand implements CommandExecutor {
    private final GardenPlatform platform;

    public StaffAlertCommand(GardenPlatform platform) {
        this.platform = platform;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gardenessentials.staffalert")) {
            EssentialsMessages.send(sender, "You do not have permission to send staff alerts.");
            return true;
        }
        if (args.length == 0) {
            EssentialsMessages.send(sender, "Use /staffalert <message>.");
            return true;
        }

        String message = String.join(" ", Arrays.asList(args)).trim();
        if (message.length() > 1800) message = message.substring(0, 1800);
        try {
            platform.integrations().publish(
                    IntegrationEventType.STAFF_ALERT,
                    "server",
                    "garden-smp",
                    "{\"sender\":\"" + json(sender.getName()) + "\",\"message\":\"" + json(message) + "\"}"
            );
            EssentialsMessages.send(sender, "Staff alert sent to Iris.");
        } catch (SQLException exception) {
            EssentialsMessages.send(sender, "The alert could not be saved right now.");
        }
        return true;
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
