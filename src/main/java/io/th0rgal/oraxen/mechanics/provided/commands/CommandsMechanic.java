package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CommandsMechanic extends Mechanic {

    private CommandsParser commandsParser;
    private boolean oneUsage;
    private String permission;
    private boolean hasTimer = false;
    private TimersFactory timersFactory;

    public CommandsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        this.commandsParser = new CommandsParser(section);

        if (section.isBoolean("one_usage"))
            this.oneUsage = section.getBoolean("one_usage");

        if (section.isString("permission"))
            this.permission = section.getString("permission");

        if (section.isLong("delay")) {
            this.hasTimer = true;
            this.timersFactory = new TimersFactory(section.getLong("delay"));
        }

    }

    public boolean isOneUsage() {
        return this.oneUsage;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean hasPermission(Player player) {
        return permission == null || player.hasPermission(this.permission);
    }

    public boolean hasTimer() {
        return  this.hasTimer;
    }

    public Timer getTimer(Player player) {
        return  timersFactory.getTimer(player);
    }

    public CommandsParser getCommands() {
        return commandsParser;
    }

}
