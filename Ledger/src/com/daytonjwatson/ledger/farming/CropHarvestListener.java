package com.daytonjwatson.ledger.farming;

import com.daytonjwatson.ledger.market.MarketItemTag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Item;

public class CropHarvestListener implements Listener {
	private final SoilFatigueService soilFatigueService;

	public CropHarvestListener(SoilFatigueService soilFatigueService) {
		this.soilFatigueService = soilFatigueService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onHarvest(BlockDropItemEvent event) {
		BlockState state = event.getBlockState();
		if (!(state.getBlockData() instanceof Ageable ageable)) {
			return;
		}
		if (ageable.getAge() < ageable.getMaximumAge()) {
			return;
		}
		Block block = state.getBlock();
		Block farmland = block.getRelative(BlockFace.DOWN);
		if (farmland.getType() != Material.FARMLAND) {
			return;
		}
		double multiplier = soilFatigueService.recordHarvest(farmland);
		for (Item item : event.getItems()) {
			ItemStack stack = item.getItemStack();
			if (MarketItemTag.fromMaterial(stack.getType()) != MarketItemTag.CROP) {
				continue;
			}
			soilFatigueService.tagHarvestedItem(stack, multiplier);
			item.setItemStack(stack);
		}
	}
}
