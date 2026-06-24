package com.kingxion.treefall;

import com.ionapi.IonPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class Treefall {
    public static final String MOD_ID = "treefall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static int MAX_LOGS = 512;
    public static int XP_PER_LOG = 1;
    public static boolean DURABILITY_COST = true;
    public static boolean SNEAK_BYPASS = true;
    public static boolean ALLOW_BUILT_LOGS = false;

    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        TreefallConfig.load();
        LOGGER.info("Treefall initialized on {}", IonPlatform.loaderName());
    }

    public static void onBlockBroken(Level world, Player player, BlockPos pos, BlockState state) {
        onBlockBroken(world, player, pos, state, player.getMainHandItem());
    }

    public static void onBlockBroken(Level world, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        if (world.isClientSide()) {
            return;
        }

        if (!isLog(state.getBlock())) {
            return;
        }

        if (isLog(world.getBlockState(pos.below()).getBlock())) {
            return;
        }

        if (SNEAK_BYPASS && player.isShiftKeyDown()) {
            return;
        }

        if (!isAxe(tool)) {
            return;
        }

        fellTree(world, pos, tool, player);
    }

    private static void fellTree(Level world, BlockPos brokenPos, ItemStack tool, Player player) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logsToBreak = new ArrayList<>();
        List<BlockPos> leavesToBreak = new ArrayList<>();

        BlockPos startPos = brokenPos.above();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState currentState = world.getBlockState(current);
            Block currentBlock = currentState.getBlock();

            if (isLog(currentBlock)) {
                if (logsToBreak.size() >= MAX_LOGS) {
                    return;
                }

                logsToBreak.add(current);
                for (BlockPos neighbor : getLogNeighbors(current)) {
                    if (!visited.contains(neighbor) && shouldVisitLogNeighbor(world, current, neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            } else if (isLeaf(currentBlock) && !currentState.getValue(LeavesBlock.PERSISTENT)) {
                leavesToBreak.add(current);
                for (BlockPos neighbor : getFaceNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (!ALLOW_BUILT_LOGS && leavesToBreak.isEmpty()) {
            return;
        }

        ServerLevel serverWorld = (ServerLevel) world;
        int totalXp = 0;
        Set<BlockPos> replantPositions = findReplantPositions(world, brokenPos, logsToBreak);

        for (BlockPos logPos : logsToBreak) {
            BlockState logState = world.getBlockState(logPos);
            if (logState.isAir()) {
                continue;
            }

            List<ItemStack> drops = Block.getDrops(logState, serverWorld, logPos, null, null, tool);
            for (ItemStack drop : drops) {
                world.addFreshEntity(new ItemEntity(world, logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5, drop));
            }

            if (DURABILITY_COST) {
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (tool.isEmpty()) {
                    break;
                }
            }

            totalXp += XP_PER_LOG;
            world.setBlock(logPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (totalXp > 0) {
            world.addFreshEntity(new ExperienceOrb(serverWorld, brokenPos.getX() + 0.5, brokenPos.getY() + 0.5, brokenPos.getZ() + 0.5, totalXp));
        }

        for (BlockPos leafPos : leavesToBreak) {
            BlockState leafState = world.getBlockState(leafPos);
            if (leafState.isAir()) {
                continue;
            }

            List<ItemStack> drops = Block.getDrops(leafState, serverWorld, leafPos, null, null, ItemStack.EMPTY);
            for (ItemStack drop : drops) {
                world.addFreshEntity(new ItemEntity(world, leafPos.getX() + 0.5, leafPos.getY() + 0.5, leafPos.getZ() + 0.5, drop));
            }
            world.setBlock(leafPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        plantSaplingsFromInventory(world, replantPositions, player);
    }

    private static Set<BlockPos> findReplantPositions(Level world, BlockPos brokenPos, List<BlockPos> logsToBreak) {
        Set<BlockPos> logPositions = new HashSet<>(logsToBreak);
        Set<BlockPos> replantPositions = new LinkedHashSet<>();
        replantPositions.add(brokenPos);

        for (BlockPos logPos : logsToBreak) {
            boolean isBottomLog = !logPositions.contains(logPos.below()) && !isLog(world.getBlockState(logPos.below()).getBlock());
            boolean hasTrunkAbove = logPositions.contains(logPos.above());
            if (isBottomLog && hasTrunkAbove) {
                replantPositions.add(logPos);
            }
        }

        return replantPositions;
    }

    private static boolean shouldVisitLogNeighbor(Level world, BlockPos current, BlockPos neighbor) {
        if (ALLOW_BUILT_LOGS) {
            return true;
        }

        BlockState neighborState = world.getBlockState(neighbor);
        if (!isLog(neighborState.getBlock())) {
            return true;
        }

        return neighbor.getY() != current.getY() || hasNaturalLeafNearby(world, neighbor);
    }

    private static boolean hasNaturalLeafNearby(Level world, BlockPos pos) {
        int radius = 2;
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
            BlockState state = world.getBlockState(checkPos);
            if (isLeaf(state.getBlock()) && !state.getValue(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }

    private static void plantSaplingsFromInventory(Level world, Set<BlockPos> positions, Player player) {
        for (BlockPos pos : positions) {
            plantSaplingFromInventory(world, pos, player);
        }
    }

    private static void plantSaplingFromInventory(Level world, BlockPos pos, Player player) {
        if (!world.getBlockState(pos).isAir()) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }

            if (!(blockItem.getBlock() instanceof SaplingBlock)) {
                continue;
            }

            BlockState saplingState = blockItem.getBlock().defaultBlockState();
            if (!saplingState.canSurvive(world, pos)) {
                continue;
            }

            world.setBlock(pos, saplingState, Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return;
        }
    }

    private static List<BlockPos> getFaceNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(6);
        neighbors.add(pos.above());
        neighbors.add(pos.below());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());
        return neighbors;
    }

    private static List<BlockPos> getLogNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    neighbors.add(pos.offset(dx, dy, dz));
                }
            }
        }
        return neighbors;
    }

    public static boolean isAxe(ItemStack stack) {
        return stack.is(ItemTags.AXES);
    }

    public static boolean isLog(Block block) {
        return block.defaultBlockState().is(BlockTags.LOGS);
    }

    public static boolean isLeaf(Block block) {
        return block instanceof LeavesBlock;
    }

    private Treefall() {
    }
}
