package ru.kekih.vulcan;

import org.bukkit.plugin.java.JavaPlugin;
import ru.kekih.vulcan.command.VulcanCommand;
import ru.kekih.vulcan.manager.ConfigManager;
import ru.kekih.vulcan.manager.RewardManager;
import ru.kekih.vulcan.manager.VulcanManager;

public class VulcanPlugin extends JavaPlugin {

    private static VulcanPlugin instance;
    private ConfigManager configManager;
    private VulcanManager vulcanManager;
    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.rewardManager = new RewardManager(this);
        this.vulcanManager = new VulcanManager(this);

        VulcanCommand command = new VulcanCommand(this);
        getCommand("vulcan").setExecutor(command);
        getCommand("vulcan").setTabCompleter(command);

        vulcanManager.startScheduler();
        getLogger().info("Vulcan успешно загружен!");
    }

    @Override
    public void onDisable() {
        if (vulcanManager != null) {
            vulcanManager.shutdown();
        }
        getLogger().info("Vulcan выключен.");
    }

    public static VulcanPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public VulcanManager getVulcanManager() {
        return vulcanManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        rewardManager.reload();
        vulcanManager.reload();
    }
}
