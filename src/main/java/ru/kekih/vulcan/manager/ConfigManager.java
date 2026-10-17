package ru.kekih.vulcan.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.kekih.vulcan.VulcanPlugin;
import ru.kekih.vulcan.model.RewardItem;
import ru.kekih.vulcan.model.VulcanType;

import java.util.*;

public class ConfigManager {

    private final VulcanPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(VulcanPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getSpawnInterval() {
        return config.getInt("settings.spawn-interval", 14400);
    }

    public void setSpawnInterval(int seconds) {
        config.set("settings.spawn-interval", seconds);
        plugin.saveConfig();
    }

    public int getEruptionDelay() {
        return config.getInt("settings.eruption-delay", 360);
    }

    public void setEruptionDelay(int seconds) {
        config.set("settings.eruption-delay", seconds);
        plugin.saveConfig();
    }

    public boolean isOneActive() {
        return config.getBoolean("settings.one-active-vulcan", true);
    }

    public boolean isRestoreTimer() {
        return config.getBoolean("settings.restore-timer-after-restart", true);
    }

    public boolean isPreSpawnEnabled() {
        return config.getBoolean("settings.pre-spawn-notifications.enabled", true);
    }

    public List<Integer> getPreSpawnTimes() {
        return config.getIntegerList("settings.pre-spawn-notifications.times");
    }

    public int getDropRadius() {
        return config.getInt("settings.drop-radius", 8);
    }

    public int getDropsMin() {
        return config.getInt("settings.drops-min", 8);
    }

    public int getDropsMax() {
        return config.getInt("settings.drops-max", 18);
    }

    public double getDropHeight() {
        return config.getDouble("settings.drop-height", 2.5);
    }

    public double getDropVelocity() {
        return config.getDouble("settings.drop-velocity", 0.6);
    }

    public int getEruptionEffectDuration() {
        return config.getInt("settings.eruption-effect-duration", 100);
    }

    public int getMaxParticles() {
        return config.getInt("settings.max-particles-per-tick", 40);
    }

    public String getMessage(String key) {
        String msg = config.getString("messages." + key, "&cСообщение не найдено: " + key);
        return colorize(msg);
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public String format(String key, Map<String, String> placeholders) {
        String msg = getMessage(key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        return msg;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }

    public String getDisplayName(VulcanType type) {
        return colorize(config.getString("vulcans." + type.getKey() + ".display-name", type.getDefaultName()));
    }

    public String getColor(VulcanType type) {
        return colorize(config.getString("vulcans." + type.getKey() + ".color", "&f"));
    }

    public double getEffectIntensity(VulcanType type) {
        return config.getDouble("vulcans." + type.getKey() + ".effect-intensity", 1.0);
    }

    public int getSpawnChance(VulcanType type) {
        return config.getInt("vulcans." + type.getKey() + ".spawn-chance", 0);
    }

    public Location getLocation(VulcanType type) {
        String path = "vulcans." + type.getKey();
        String worldName = config.getString(path + ".world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        double x = config.getDouble(path + ".x", 0);
        double y = config.getDouble(path + ".y", 64);
        double z = config.getDouble(path + ".z", 0);
        return new Location(world, x, y, z);
    }

    public void setLocation(VulcanType type, Location loc) {
        String path = "vulcans." + type.getKey();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        plugin.saveConfig();
    }

    public List<RewardItem> getRewards(VulcanType type) {
        List<RewardItem> list = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("vulcans." + type.getKey() + ".rewards");
        if (section == null) {
            return list;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec != null) {
                RewardItem item = RewardItem.fromConfig(itemSec);
                if (item != null) list.add(item);
            }
        }
        return list;
    }

    public void saveRewards(VulcanType type, List<RewardItem> rewards) {
        String path = "vulcans." + type.getKey() + ".rewards";
        config.set(path, null);
        int i = 0;
        for (RewardItem reward : rewards) {
            ConfigurationSection sec = config.createSection(path + "." + i);
            reward.saveToConfig(sec);
            i++;
        }
        plugin.saveConfig();
    }

    public void addReward(VulcanType type, RewardItem item) {
        List<RewardItem> current = getRewards(type);
        current.add(item);
        saveRewards(type, current);
    }

    public boolean removeReward(VulcanType type, int index) {
        List<RewardItem> current = getRewards(type);
        if (index < 0 || index >= current.size()) return false;
        current.remove(index);
        saveRewards(type, current);
        return true;
    }

    public boolean setRewardChance(VulcanType type, int index, double chance) {
        List<RewardItem> current = getRewards(type);
        if (index < 0 || index >= current.size()) return false;
        current.set(index, current.get(index).withChance(chance));
        saveRewards(type, current);
        return true;
    }

    public VulcanType selectRandomType() {
        List<VulcanType> types = Arrays.asList(VulcanType.values());
        int total = 0;
        for (VulcanType t : types) {
            total += getSpawnChance(t);
        }
        if (total <= 0) return VulcanType.NORMAL;
        int roll = new Random().nextInt(total);
        int current = 0;
        for (VulcanType t : types) {
            current += getSpawnChance(t);
            if (roll < current) return t;
        }
        return VulcanType.NORMAL;
    }

    public long getSavedNextSpawn() {
        return config.getLong("data.next-spawn", 0);
    }

    public void setSavedNextSpawn(long timestamp) {
        config.set("data.next-spawn", timestamp);
        plugin.saveConfig();
    }

    public String getSavedActiveType() {
        return config.getString("data.active-type", null);
    }

    public void setSavedActiveType(String type) {
        config.set("data.active-type", type);
        plugin.saveConfig();
    }

    public long getSavedEruptionTime() {
        return config.getLong("data.eruption-time", 0);
    }

    public void setSavedEruptionTime(long timestamp) {
        config.set("data.eruption-time", timestamp);
        plugin.saveConfig();
    }

    public void clearActiveData() {
        config.set("data.active-type", null);
        config.set("data.eruption-time", 0);
        plugin.saveConfig();
    }
}
