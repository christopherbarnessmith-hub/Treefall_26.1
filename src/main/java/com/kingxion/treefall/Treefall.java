package com.kingxion.treefall;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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

	// Config — loaded from treefall.properties on startup, editable without recompiling
	public static int     MAX_LOGS         = 512;  // abort felling if tree exceeds this many logs
	public static int     XP_PER_LOG       = 1;    // XP points awarded per log felled
	public static boolean DURABILITY_COST  = true; // axe loses 1 durability per log broken
	public static boolean SNEAK_BYPASS     = true; // sneak + break = single block, no felling

	@Override
	public void onInitialize() {
		TreefallConfig.load();
		LOGGER.info("Treefall mod initialized!");

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world.isClientSide()) return;
			if (!isLog(state.getBlock())) return;

			// Only fell if the block below is NOT a log (i.e. this is the base of the trunk)
			if (isLog(world.getBlockState(pos.below()).getBlock())) return;

			// Sneak bypass: player holds sneak → behaves like a normal break, no felling
			if (SNEAK_BYPASS && player.isShiftKeyDown()) return;

			// Only trigger when broken with an axe
			ItemStack tool = player.getMainHandItem();
			if (!isAxe(tool)) return;

			fellTree(world, pos, tool, player);
		});
	}

	private void fellTree(Level world, BlockPos brokenPos, ItemStack tool, Player player) {
		Set<BlockPos>   visited      = new HashSet<>();
		Queue<BlockPos> queue        = new LinkedList<>();
		List<BlockPos>  logsToBreak  = new ArrayList<>();
		List<BlockPos>  leavesToBreak = new ArrayList<>();

		BlockPos startPos = brokenPos.above();
		queue.add(startPos);
		visited.add(startPos);

		// BFS: collect all connected logs and natural leaves
		while (!queue.isEmpty()) {
			BlockPos   current      = queue.poll();
			BlockState currentState = world.getBlockState(current);
			Block      currentBlock = currentState.getBlock();

			if (isLog(currentBlock)) {
				if (logsToBreak.size() >= MAX_LOGS) return; // abort — too large, probably artificial
				logsToBreak.add(current);
				for (BlockPos neighbor : getNeighbors(current)) {
					if (!visited.contains(neighbor)) {
						visited.add(neighbor);
						queue.add(neighbor);
					}
				}
			} else if (isLeaf(currentBlock)) {
				// Only remove natural leaves (persistent=false), never player-placed ones
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
		int totalXp = 0;

		for (BlockPos logPos : logsToBreak) {
			BlockState logState = world.getBlockState(logPos);
			if (logState.isAir()) continue;

			// Spawn drops (respects Fortune/Silk Touch on the axe)
			List<ItemStack> drops = Block.getDrops(logState, serverWorld, logPos, null, null, tool);
			for (ItemStack drop : drops) {
				world.addFreshEntity(new ItemEntity(world,
						logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5, drop));
			}

			// Damage the axe 1 durability per log (Unbreaking enchantment is respected automatically)
			if (DURABILITY_COST) {
				tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				if (tool.isEmpty()) break; // axe broke mid-fell — stop here
			}

			totalXp += XP_PER_LOG;
			world.setBlock(logPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}

		// Spawn a single XP orb at the base worth all accumulated XP
		if (totalXp > 0) {
			world.addFreshEntity(new ExperienceOrb(serverWorld,
					brokenPos.getX() + 0.5, brokenPos.getY() + 0.5, brokenPos.getZ() + 0.5,
					totalXp));
		}

		// Remove leaves instantly with no drops
		for (BlockPos leafPos : leavesToBreak) {
			if (!world.getBlockState(leafPos).isAir()) {
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

	public static boolean isAxe(ItemStack stack)  { return stack.is(ItemTags.AXES); }
	public static boolean isLog(Block block)       { return block.defaultBlockState().is(BlockTags.LOGS); }
	public static boolean isLeaf(Block block)      { return block instanceof LeavesBlock; }
}