package com.herasgarden.gardenessentials.ui;

import com.herasgarden.gardencore.api.ui.GardenMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public final class EssentialsMessages {
    private EssentialsMessages() {}

    public static Component prefix() {
        return GardenMessages.prefix();
    }

    public static void send(CommandSender sender, String message) {
        GardenMessages.send(sender, message);
    }
}
