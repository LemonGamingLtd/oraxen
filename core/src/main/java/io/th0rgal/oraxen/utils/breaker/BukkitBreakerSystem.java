package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BukkitBreakerSystem extends BreakerSystem implements Listener {

    @Override
    public void registerListener() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    @Override
    protected void sendBlockBreak(Player player, Location location, int stage) {
        // how bout no
    }

    @Override
    protected void handleEvent(Player player, Block block, Location location, BlockFace blockFace, World world, Runnable cancel, boolean startedDigging) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final ItemStack item = player.getInventory().getItemInMainHand();

        NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
        StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (block.getType() == Material.NOTE_BLOCK && noteMechanic == null) return;
        if (block.getType() == Material.TRIPWIRE && stringMechanic == null) return;
        if (block.getType() == Material.BARRIER && furnitureMechanic == null) return;

        cancel.run();

        final Drop drop;
        if (furnitureMechanic != null)
            drop = furnitureMechanic.getDrop() != null ? furnitureMechanic.getDrop() : Drop.emptyDrop();
        else if (noteMechanic != null)
            drop = noteMechanic.getDrop() != null ? noteMechanic.getDrop() : Drop.emptyDrop();
        else if (stringMechanic != null)
            drop = stringMechanic.getDrop() != null ? stringMechanic.getDrop() : Drop.emptyDrop();
        else drop = null;

        if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
            // Damage item with properties identified earlier
            ItemUtils.damageItem(player, drop, item);
            block.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onBlockDamageEvent(@NotNull BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final World world = location.getWorld();
        final BlockFace blockFace = event.getBlockFace();

        handleEvent(player, block, location, blockFace, world, () -> event.setCancelled(true), true);
    }
}
