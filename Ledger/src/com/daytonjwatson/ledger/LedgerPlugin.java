package com.daytonjwatson.ledger;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.BankService;
import com.daytonjwatson.ledger.economy.DeathPenaltyListener;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.items.InventoryScanScheduler;
import com.daytonjwatson.ledger.items.LoreValueService;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.MarketState;
import com.daytonjwatson.ledger.market.MarketStorageYaml;
import com.daytonjwatson.ledger.mobs.MobKillListener;
import com.daytonjwatson.ledger.mobs.MobPayoutService;
import com.daytonjwatson.ledger.spawn.SpawnInteractionListener;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import com.daytonjwatson.ledger.tools.RepairService;
import com.daytonjwatson.ledger.tools.ToolMetaService;
import com.daytonjwatson.ledger.tools.EnchantBlockListener;
import com.daytonjwatson.ledger.tools.ToolVendorCommand;
import com.daytonjwatson.ledger.tools.ToolVendorService;
import com.daytonjwatson.ledger.util.AtomicFileWriter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LedgerPlugin extends JavaPlugin {

	private ConfigManager configManager;
	private MarketState marketState;
	private MarketStorageYaml marketStorage;
	private MarketService marketService;
	private MoneyService moneyService;
	private SpawnRegionService spawnRegionService;
	private BankService bankService;
	private LoreValueService loreValueService;
	private ToolVendorService toolVendorService;
	private RepairService repairService;
	private MobPayoutService mobPayoutService;
	
	@Override
	public void onEnable() {
		AtomicFileWriter.ensureDirectory(getDataFolder());
		this.configManager = new ConfigManager(this);
		configManager.loadAll();

		this.spawnRegionService = new SpawnRegionService(configManager);
		this.marketState = new MarketState();
		this.marketStorage = new MarketStorageYaml(this, marketState, configManager);
		marketStorage.load();

		this.moneyService = new MoneyService(this, configManager);
		moneyService.load();

		this.marketService = new MarketService(configManager, marketState);
		this.bankService = new BankService(spawnRegionService, moneyService);
		this.toolVendorService = new ToolVendorService(configManager, moneyService);
		this.repairService = new RepairService(configManager, moneyService, new ToolMetaService(this));
		this.mobPayoutService = new MobPayoutService(configManager, marketState);
		this.loreValueService = new LoreValueService(this, configManager, marketService);

		Bukkit.getPluginManager().registerEvents(new SpawnInteractionListener(spawnRegionService), this);
		Bukkit.getPluginManager().registerEvents(new DeathPenaltyListener(configManager, moneyService), this);
		Bukkit.getPluginManager().registerEvents(new MobKillListener(mobPayoutService, moneyService), this);
		Bukkit.getPluginManager().registerEvents(loreValueService, this);
		Bukkit.getPluginManager().registerEvents(new EnchantBlockListener(), this);

		getCommand("sell").setExecutor(new com.daytonjwatson.ledger.spawn.SellCommand(spawnRegionService, marketService, moneyService));
		getCommand("bank").setExecutor(new com.daytonjwatson.ledger.economy.BankCommand(bankService));
		getCommand("balance").setExecutor(new com.daytonjwatson.ledger.economy.BalanceCommand(moneyService));
		getCommand("tool").setExecutor(new ToolVendorCommand(spawnRegionService, toolVendorService, repairService));

		new InventoryScanScheduler(this, loreValueService).start();

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			moneyService.save();
			marketStorage.save();
		}, 20L * 60, 20L * 60);
	}

	@Override
	public void onDisable() {
		if (moneyService != null) {
			moneyService.save();
		}
		if (marketStorage != null) {
			marketStorage.save();
		}
	}
}
