package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.aura;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class Aura {

    protected final AuraMechanic mechanic;
    private io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable runnable;

    protected Aura(AuraMechanic mechanic) {
        this.mechanic = mechanic;
    }

    io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable getRunnable() {
        return new io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable() {
            @Override
            public void run() {
                mechanic.players.forEach(Aura.this::spawnParticles);
            }
        };
    }

    protected abstract void spawnParticles(Player player);

    protected abstract long getDelay();

    public void start() {
        runnable = getRunnable();
        io.th0rgal.oraxen.api.scheduler.AdaptedTask task = runnable.runTaskTimerAsynchronously(0L, getDelay());
        MechanicsManager.registerTask(mechanic.getFactory().getMechanicID(), task);
    }

    public void stop() {
        runnable.getAdaptedTask().cancel();
    }


}
