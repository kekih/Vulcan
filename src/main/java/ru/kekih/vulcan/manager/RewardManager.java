package ru.kekih.vulcan.manager;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import ru.kekih.vulcan.VulcanPlugin;
import ru.kekih.vulcan.model.RewardItem;
import ru.kekih.vulcan.model.VulcanType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RewardManager {

    private final VulcanPlugin plugin;
    private final Random random = new Random();

    public RewardManager(VulcanPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // no-op, rewards loaded from config on demand
    }

    public List<ItemStack> rollRewards(VulcanType type) {
        List<RewardItem> rewards = plugin.getConfigManager().getRewards(type);
        List<ItemStack> result = new ArrayList<>();
        if (rewards.isEmpty()) {
            result.add(new ItemStack(org.bukkit.Material.IRON_INGOT, 4 + random.nextInt(8)));
            result.add(new ItemStack(org.bukkit.Material.GOLD_INGOT, 2 + random.nextInt(4)));
            if (random.nextDouble() < 0.3) {
                result.add(new ItemStack(org.bukkit.Material.DIAMOND, 1 + random.nextInt(2)));
            }
            return result;
        }

        int dropsMin = plugin.getConfigManager().getDropsMin();
        int dropsMax = plugin.getConfigManager().getDropsMax();
        int count = dropsMin + random.nextInt(Math.max(1, dropsMax - dropsMin + 1));

        for (int i = 0; i < count; i++) {
            RewardItem chosen = selectWeighted(rewards);
            if (chosen != null) {
                result.add(chosen.toItemStack());
            }
        }
        return result;
    }

    private RewardItem selectWeighted(List<RewardItem> rewards) {
        double total = 0;
        for (RewardItem r : rewards) total += r.getChance();
        if (total <= 0) return rewards.get(random.nextInt(rewards.size()));
        double roll = random.nextDouble() * total;
        double current = 0;
        for (RewardItem r : rewards) {
            current += r.getChance();
            if (roll <= current) return r;
        }
        return rewards.get(rewards.size() - 1);
    }

    public void dropRewards(Location center, VulcanType type) {
        List<ItemStack> items = rollRewards(type);
        ConfigManager cfg = plugin.getConfigManager();
        double radius = cfg.getDropRadius();
        double height = cfg.getDropHeight();
        double velocity = cfg.getDropVelocity();

        Location dropLoc = center.clone().add(0, height, 0);

        for (ItemStack stack : items) {
            Item item = dropLoc.getWorld().dropItem(dropLoc, stack);
            item.setPickupDelay(40);

            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double speed = velocity * (0.6 + ThreadLocalRandom.current().nextDouble() * 0.8);
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.5;
            item.setVelocity(new Vector(vx, vy, vz));
        }
    }
}
