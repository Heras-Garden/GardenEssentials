package com.herasgarden.gardenessentials.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class DeleteItemCommand implements CommandExecutor {
    private static final int MAX_DISTANCE = 10;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be used in-game.");
            return true;
        }
        if (!player.hasPermission("gardenessentials.deleteitem")) {
            player.sendMessage("You do not have permission to use /deleteitem.");
            return true;
        }

        Entity target = player.getTargetEntity(MAX_DISTANCE);
        if (target == null) {
            player.sendMessage("Look directly at an entity within " + MAX_DISTANCE + " blocks.");
            return true;
        }
        if (target instanceof Player) {
            player.sendMessage("/deleteitem cannot delete players.");
            return true;
        }

        String type = target.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        target.remove();
        player.sendMessage("Deleted " + type + ".");
        return true;
    }
}
