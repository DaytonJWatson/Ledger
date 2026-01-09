package com.daytonjwatson.ledger;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.BankService;
import com.daytonjwatson.ledger.economy.DeathPenaltyListener;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.farming.CropHarvestListener;
import com.daytonjwatson.ledger.farming.SoilFatigueService;
import com.daytonjwatson.ledger.gui.HubCommand;
import com.daytonjwatson.ledger.gui.GuiListener;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.MenuCommand;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.gui.menus.BankMenu;
import com.daytonjwatson.ledger.gui.menus.HubMenu;
import com.daytonjwatson.ledger.gui.menus.RepairMenu;
import com.daytonjwatson.ledger.gui.menus.SellMenu;
import com.daytonjwatson.ledger.gui.menus.ToolsMenu;
import com.daytonjwatson.ledger.gui.menus.UpgradesMenu;
import com.daytonjwatson.ledger.items.InventoryScanScheduler;
import com.daytonjwatson.ledger.items.LoreValueService;
import com.daytonjwatson.ledger.market.DepletionListener;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.MarketState;
import com.daytonjwatson.ledger.market.MarketStorageYaml;
import com.daytonjwatson.ledger.market.ScarcityWindowService;
import com.daytonjwatson.ledger.market.SellValidator;
import com.daytonjwatson.ledger.mobs.MobKillListener;
import com.daytonjwatson.ledger.mobs.MobPayoutService;
import com.daytonjwatson.ledger.mining.AutoSmeltListener;
import com.daytonjwatson.ledger.spawn.AnimalSellListener;
import com.daytonjwatson.ledger.spawn.AnimalSellService;
import com.daytonjwatson.ledger.spawn.SellService;
import com.daytonjwatson.ledger.spawn.SpawnInteractionListener;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import com.daytonjwatson.ledger.tools.RepairService;
import com.daytonjwatson.ledger.tools.SilkTouchMarkListener;
import com.daytonjwatson.ledger.tools.SilkTouchMarkService;
import com.daytonjwatson.ledger.tools.ToolMetaService;
import com.daytonjwatson.ledger.tools.EnchantBlockListener;
import com.daytonjwatson.ledger.tools.ToolVendorCommand;
import com.daytonjwatson.ledger.tools.ToolVendorService;
import com.daytonjwatson.ledger.util.AtomicFileWriter;
import com.daytonjwatson.ledger.upgrades.UpgradeCommand;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LedgerPlugin extends JavaPlugin {

	private ConfigManager configManager;
	private MarketState marketState;
	private MarketStorageYaml marketStorage;
	private MarketService marketService;
	private MoneyService moneyService;
	private UpgradeService upgradeService;
	private SpawnRegionService spawnRegionService;
	private BankService bankService;
	private LoreValueService loreValueService;
	private ToolVendorService toolVendorService;
	private RepairService repairService;
	private ToolMetaService toolMetaService;
	private MobPayoutService mobPayoutService;
	private SilkTouchMarkService silkTouchMarkService;
	private ScarcityWindowService scarcityWindowService;
	private SoilFatigueService soilFatigueService;
	private AnimalSellService animalSellService;
	private SellService sellService;
	private SellValidator sellValidator;
	private GuiManager guiManager;
	
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

		this.upgradeService = new UpgradeService(configManager, moneyService, spawnRegionService);
		this.silkTouchMarkService = new SilkTouchMarkService(this);
		this.scarcityWindowService = new ScarcityWindowService(configManager);
		this.soilFatigueService = new SoilFatigueService(this, configManager);
		soilFatigueService.load();
		this.toolMetaService = new ToolMetaService(this);
		this.marketService = new MarketService(configManager, marketState, upgradeService, silkTouchMarkService, scarcityWindowService, soilFatigueService);
		marketService.validateCoverage();
		this.bankService = new BankService(spawnRegionService, moneyService);
		this.toolVendorService = new ToolVendorService(configManager, moneyService, spawnRegionService, upgradeService, toolMetaService);
		this.repairService = new RepairService(configManager, moneyService, toolMetaService, spawnRegionService, toolVendorService);
		this.mobPayoutService = new MobPayoutService(configManager, marketState, scarcityWindowService, upgradeService);
		this.animalSellService = new AnimalSellService(configManager, mobPayoutService, moneyService);
		this.sellValidator = new SellValidator(marketService, toolMetaService);
		this.loreValueService = new LoreValueService(this, configManager, marketService, sellValidator);
		this.sellService = new SellService(marketService, moneyService, sellValidator);
		this.guiManager = new GuiManager(spawnRegionService);
		guiManager.register(new HubMenu(guiManager));
		guiManager.register(new SellMenu(guiManager, marketService, sellService, sellValidator));
		guiManager.register(new BankMenu(guiManager, moneyService, bankService, spawnRegionService));
		guiManager.register(new ToolsMenu(guiManager, moneyService, toolVendorService, spawnRegionService));
		guiManager.register(new RepairMenu(guiManager, moneyService, repairService, toolMetaService, toolVendorService, this));
		guiManager.register(new UpgradesMenu(guiManager, upgradeService, moneyService));

		Bukkit.getPluginManager().registerEvents(new SpawnInteractionListener(spawnRegionService, guiManager, configManager), this);
		Bukkit.getPluginManager().registerEvents(new DeathPenaltyListener(configManager, moneyService, upgradeService), this);
		Bukkit.getPluginManager().registerEvents(new MobKillListener(configManager, mobPayoutService, moneyService), this);
		Bukkit.getPluginManager().registerEvents(new DepletionListener(marketService), this);
		Bukkit.getPluginManager().registerEvents(new CropHarvestListener(soilFatigueService), this);
		Bukkit.getPluginManager().registerEvents(new AnimalSellListener(animalSellService), this);
		Bukkit.getPluginManager().registerEvents(loreValueService, this);
		Bukkit.getPluginManager().registerEvents(new EnchantBlockListener(), this);
		Bukkit.getPluginManager().registerEvents(new SilkTouchMarkListener(silkTouchMarkService), this);
		Bukkit.getPluginManager().registerEvents(new AutoSmeltListener(upgradeService), this);
		Bukkit.getPluginManager().registerEvents(new GuiListener(guiManager), this);

		getCommand("sell").setExecutor(new MenuCommand(guiManager, MenuId.SELL));
		getCommand("bank").setExecutor(new MenuCommand(guiManager, MenuId.BANK));
		getCommand("balance").setExecutor(new com.daytonjwatson.ledger.economy.BalanceCommand(moneyService));
		getCommand("tool").setExecutor(new ToolVendorCommand(spawnRegionService, toolVendorService, repairService));
		getCommand("tools").setExecutor(new MenuCommand(guiManager, MenuId.TOOLS));
		getCommand("repair").setExecutor(new MenuCommand(guiManager, MenuId.REPAIR));
		getCommand("ledger").setExecutor(new HubCommand(guiManager, configManager, marketService, mobPayoutService, upgradeService, moneyService));
		UpgradeCommand upgradeCommand = new UpgradeCommand(upgradeService);
		getCommand("upgrade").setExecutor(upgradeCommand);
		getCommand("upgrades").setExecutor(new MenuCommand(guiManager, MenuId.UPGRADES));

		new InventoryScanScheduler(this, loreValueService).start();

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			moneyService.save();
			marketStorage.save();
			soilFatigueService.save();
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
		if (soilFatigueService != null) {
			soilFatigueService.save();
		}
	}
}
