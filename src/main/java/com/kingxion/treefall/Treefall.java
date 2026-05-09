package com.kingxion.treefall;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Treefall implements ModInitializer {

	public static final String MOD_ID = "treefall";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Safety cap to prevent lag on huge/artificial structures
	private static final int MAX_LOGS = 512;

	@Override
	public void onInitialize() {
		LOGGER.info("Treefall mod initialized!");

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world.isClientSide()) return;

			if (!isLog(state.getBlock())) return;

			// Only fell if the block below is NOT a log (i.e. this is the base)
			BlockPos belowPos = pos.below();
			BlockState belowState = world.getBlockState(belowPos);
			if (isLog(belowState.getBlock())) return;

			// Only trigger when broken with an axe
			ItemStack tool = player.getMainHandItem();
			if (!isAxe(tool)) return;

			fellTree(world, pos, tool);
		});
	}

	private void fellTree(Level world, BlockPos brokenPos, ItemStack tool) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new LinkedList<>();

		BlockPos startPos = brokenPos.above();
		queue.add(startPos);
		visited.add(startPos);

		List<BlockPos> logsToBreak = new ArrayList<>();
		List<BlockPos> leavesToBreak = new ArrayList<>();

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();
			BlockState currentState = world.getBlockState(current);
			Block currentBlock = currentState.getBlock();

			if (isLog(currentBlock)) {
				// Abort if the tree is unnaturally large
				if (logsToBreak.size() >= MAX_LOGS) return;
				logsToBreak.add(current);
				for (BlockPos neighbor : getNeighbors(current)) {
					if (!visited.contains(neighbor)) {
						visited.add(neighbor);
						queue.add(neighbor);
					}
				}
			} else if (isLeaf(currentBlock)) {
				// Only break persistent=false leaves (natural, not player-placed)
				if (!currentState.getValue(LeavesBlock.PERSISTENT)) {
					leavesToBreak.add(current);
					for (BlockPos neighbor : getNeighbors(current)) {
						if (!visited.contains(neighbor)) {
							visited.add(neighbor);
							queue.add(neighbor);
						}
					}
				}
			}
		}

		ServerLevel serverWorld = (ServerLevel) world;

		for (BlockPos logPos : logsToBreak) {
			BlockState logState = world.getBlockState(logPos);
			if (!logState.isAir()) {
				List<ItemStack> drops = Block.getDrops(logState, serverWorld, logPos, null, null, tool);
				for (ItemStack drop : drops) {
					ItemEntity itemEntity = new ItemEntity(world,
							logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5, drop);
					world.addFreshEntity(itemEntity);
				}
				world.setBlock(logPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}

		for (BlockPos leafPos : leavesToBreak) {
			BlockState leafState = world.getBlockState(leafPos);
			if (!leafState.isAir()) {
				// Remove instantly with no drops — leaves vanish immediately
				world.setBlock(leafPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private List<BlockPos> getNeighbors(BlockPos pos) {
		return Arrays.asList(
				pos.above(), pos.below(),
				pos.north(), pos.south(),
				pos.east(), pos.west()
		);
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
}