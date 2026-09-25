package com.herasgarden.gardenessentials.command;

import com.herasgarden.gardencore.api.calendar.GardenCalendar;
import com.herasgarden.gardenessentials.ui.EssentialsMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class CalendarCommand implements CommandExecutor, TabCompleter {
    private final GardenCalendar calendar;

    public CalendarCommand(GardenCalendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            EssentialsMessages.send(sender, calendar.snapshot().formattedTime());
            return true;
        }
        if (!player.hasPermission("gardenessentials.calendar")) {
            EssentialsMessages.send(player, "You do not have permission to use the Garden calendar.");
            return true;
        }
        if (args.length == 0) {
            show(player);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hud")) {
            boolean enabled;
            if (args[1].equalsIgnoreCase("on")) enabled = true;
            else if (args[1].equalsIgnoreCase("off")) enabled = false;
            else {
                EssentialsMessages.send(player, "Use /calendar hud <on|off>.");
                return true;
            }
            try {
                calendar.setHudEnabled(player.getUniqueId(), enabled);
                EssentialsMessages.send(player, "Calendar HUD " + (enabled ? "enabled." : "disabled."));
            } catch (SQLException exception) {
                EssentialsMessages.send(player, "Your calendar preference could not be saved.");
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("hud")) {
            try {
                EssentialsMessages.send(player, "Calendar HUD is "
                        + (calendar.hudEnabled(player.getUniqueId()) ? "on." : "off."));
            } catch (SQLException exception) {
                EssentialsMessages.send(player, "Your calendar preference could not be loaded.");
            }
            return true;
        }
        EssentialsMessages.send(player, "Use /calendar or /calendar hud <on|off>.");
        return true;
    }

    private void show(Player player) {
        EssentialsMessages.send(player, calendar.snapshot().formattedTime());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return match(args[0], List.of("hud"));
        if (args.length == 2 && args[0].equalsIgnoreCase("hud")) return match(args[1], List.of("on", "off"));
        return List.of();
    }

    private List<String> match(String raw, List<String> values) {
        String prefix = raw.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
