package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ArmorEffectsTask extends io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            io.th0rgal.oraxen.OraxenPlugin.get().getScheduler().runEntityTask(player, () -> ArmorEffectsMechanic.addEffects(player), null);
        }
    }
}
