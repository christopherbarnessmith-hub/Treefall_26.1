package com.kingxion.treefall.fabric;

import com.kingxion.treefall.Treefall;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class TreefallFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Treefall.init();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                Treefall.onBlockBroken(world, player, pos, state)
        );
    }
}
