package com.kingxion.treefall.neoforge;

import com.kingxion.treefall.Treefall;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.minecraft.world.entity.player.Player;

@Mod(Treefall.MOD_ID)
public final class TreefallNeoForge {
    public TreefallNeoForge(IEventBus modBus) {
        Treefall.init();
        NeoForge.EVENT_BUS.addListener(this::onBlockDrops);
    }

    private void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (event.getBreaker() instanceof Player player) {
            Treefall.onBlockBroken(event.getLevel(), player, event.getPos(), event.getState(), event.getTool());
        }
    }
}
