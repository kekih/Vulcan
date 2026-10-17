package ru.kekih.vulcan.manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.kekih.vulcan.VulcanPlugin;
import ru.kekih.vulcan.model.VulcanType;
import ru.kekih.vulcan.util.MessageUtil;
import ru.kekih.vulcan.util.StructureBuilder;
import ru.kekih.vulcan.util.TimeUtil;

import java.util.*;

public class VulcanManager {

    private final VulcanPlugin plugin;
    private final ConfigManager config;

    private VulcanType activeType;
    private Location activeLocation;
    private long nextSpawnTime;
    private long eruptionTime;
    private boolean erupting;
    private BukkitTask mainTask;
    private BukkitTask effectTask;
    private BukkitTask particleTask;
    private final List<Location> placedBlocks = new ArrayList<>();
    private final Set<Integer> notifiedPreSpawn = new HashSet<>();

    public VulcanManager(VulcanPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        loadState();
    }

    private void loadState() {
        if (config.isRestoreTimer()) {
            long savedNext = config.getSavedNextSpawn();
            if (savedNext > System.currentTimeMillis()) {
                nextSpawnTime = savedNext;
            } else {
                nextSpawnTime = System.currentTimeMillis() + config.getSpawnInterval() * 1000L;
            }
            String savedType = config.getSavedActiveType();
            long savedEruption = config.getSavedEruptionTime();
            if (savedType != null && savedEruption > System.currentTimeMillis()) {
                VulcanType type = VulcanType.fromString(savedType);
                if (type != null) {
                    activeType = type;
                    activeLocation = config.getLocation(type);
                    eruptionTime = savedEruption;
                    StructureBuilder.build(activeLocation, type, placedBlocks);
                    startPreEruptionEffects();
                }
            }
        } else {
            nextSpawnTime = System.currentTimeMillis() + config.getSpawnInterval() * 1000L;
        }
        saveState();
    }

    public void startScheduler() {
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();

        if (activeType == null && config.isPreSpawnEnabled()) {
            long remaining = (nextSpawnTime - now) / 1000;
            for (int t : config.getPreSpawnTimes()) {
                if (remaining <= t && remaining > t - 2 && !notifiedPreSpawn.contains(t)) {
                    notifiedPreSpawn.add(t);
                    String key = t >= 300 ? "pre-spawn-5min" : "pre-spawn-1min";
                    MessageUtil.broadcast(config.getPrefix() + config.getMessage(key));
                }
            }
        }

        if (activeType == null && now >= nextSpawnTime) {
            spawnRandom();
            notifiedPreSpawn.clear();
        }

        if (activeType != null && !erupting && now >= eruptionTime) {
            startEruption();
        }
    }

    public void spawnRandom() {
        if (config.isOneActive() && activeType != null) {
            return;
        }
        VulcanType type = config.selectRandomType();
        spawn(type, false);
    }

    public void spawn(VulcanType type, boolean manual) {
        if (config.isOneActive() && activeType != null) {
            return;
        }
        activeType = type;
        activeLocation = config.getLocation(type).clone();
        erupting = false;
        eruptionTime = System.currentTimeMillis() + config.getEruptionDelay() * 1000L;

        StructureBuilder.build(activeLocation, type, placedBlocks);

        String msgKey = "spawned-" + type.getKey();
        String announce = config.getPrefix() + config.getMessage(msgKey);
        MessageUtil.broadcast(announce);

        if (activeLocation.getWorld() != null) {
            activeLocation.getWorld().playSound(activeLocation, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
            activeLocation.getWorld().playSound(activeLocation, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.8f);
        }

        startPreEruptionEffects();
        saveState();

        if (manual) {
            plugin.getLogger().info("Вулкан " + type.getKey() + " вызван вручную.");
        }
    }

    private void startPreEruptionEffects() {
        if (particleTask != null) particleTask.cancel();
        particleTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (activeType == null || erupting) {
                    cancel();
                    return;
                }
                tick++;
                Location loc = activeLocation.clone().add(0.5, 1.5, 0.5);
                World world = loc.getWorld();
                if (world == null) return;

                double intensity = config.getEffectIntensity(activeType);
                int maxP = config.getMaxParticles();

                int count = (int) Math.min(maxP, 5 + (tick / 10.0) * intensity);
                world.spawnParticle(Particle.SMOKE_LARGE, loc, count, 1.2, 1.5, 1.2, 0.02);
                world.spawnParticle(Particle.LAVA, loc, Math.max(1, count / 3), 0.8, 0.5, 0.8, 0);
                world.spawnParticle(Particle.FLAME, loc, Math.max(1, count / 4), 0.6, 0.8, 0.6, 0.01);

                if (tick % 40 == 0) {
                    world.playSound(loc, Sound.BLOCK_LAVA_POP, 0.8f, 0.7f + (float)(Math.random() * 0.3));
                }
                if (tick % 80 == 0) {
                    world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void startEruption() {
        if (activeType == null || erupting) return;
        erupting = true;

        MessageUtil.broadcast(config.getPrefix() + config.getMessage("eruption-start"));

        Location center = activeLocation.clone().add(0.5, 2, 0.5);
        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            world.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1.2f, 0.4f);
        }

        plugin.getRewardManager().dropRewards(activeLocation, activeType);

        if (effectTask != null) effectTask.cancel();
        final VulcanType type = activeType;
        final double intensity = config.getEffectIntensity(type);
        final int duration = config.getEruptionEffectDuration();

        effectTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= duration) {
                    finishEruption();
                    cancel();
                    return;
                }
                tick++;
                if (world == null) return;

                int maxP = config.getMaxParticles();
                int count = (int) Math.min(maxP, 15 * intensity);

                world.spawnParticle(Particle.EXPLOSION_LARGE, center, 1, 0.5, 0.5, 0.5, 0);
                world.spawnParticle(Particle.SMOKE_LARGE, center, count, 2.0, 2.5, 2.0, 0.05);
                world.spawnParticle(Particle.LAVA, center, (int)(count * 0.6), 1.5, 1.0, 1.5, 0);
                world.spawnParticle(Particle.FLAME, center, (int)(count * 0.8), 1.2, 2.0, 1.2, 0.02);
                world.spawnParticle(Particle.ASH, center, count, 2.5, 3.0, 2.5, 0.01);

                if (tick % 10 == 0) {
                    world.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.6f + (float)Math.random() * 0.4f);
                }
                if (tick % 20 == 0) {
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.7f);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void finishEruption() {
        MessageUtil.broadcast(config.getPrefix() + config.getMessage("eruption-end"));
        cleanupStructure();
        activeType = null;
        activeLocation = null;
        erupting = false;
        eruptionTime = 0;

        nextSpawnTime = System.currentTimeMillis() + config.getSpawnInterval() * 1000L;
        config.clearActiveData();
        saveState();

        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public void despawn() {
        if (activeType == null) return;
        cleanupStructure();
        if (particleTask != null) particleTask.cancel();
        if (effectTask != null) effectTask.cancel();
        activeType = null;
        activeLocation = null;
        erupting = false;
        eruptionTime = 0;
        config.clearActiveData();
        nextSpawnTime = System.currentTimeMillis() + config.getSpawnInterval() * 1000L;
        saveState();
    }

    private void cleanupStructure() {
        for (Location loc : placedBlocks) {
            Block b = loc.getBlock();
            if (isVolcanoBlock(b.getType())) {
                b.setType(Material.AIR, false);
            }
        }
        placedBlocks.clear();
    }

    private boolean isVolcanoBlock(Material m) {
        return m == Material.MAGMA_BLOCK || m == Material.BLACKSTONE || m == Material.BASALT
                || m == Material.BLACKSTONE_SLAB || m == Material.BLACKSTONE_STAIRS
                || m == Material.NETHERRACK || m == Material.OBSIDIAN
                || m == Material.LAVA || m == Material.FIRE;
    }

    private void saveState() {
        config.setSavedNextSpawn(nextSpawnTime);
        if (activeType != null) {
            config.setSavedActiveType(activeType.getKey());
            config.setSavedEruptionTime(eruptionTime);
        } else {
            config.clearActiveData();
        }
    }

    public void reload() {
    }

    public void shutdown() {
        if (mainTask != null) mainTask.cancel();
        if (particleTask != null) particleTask.cancel();
        if (effectTask != null) effectTask.cancel();
        saveState();
    }

    public VulcanType getActiveType() {
        return activeType;
    }

    public Location getActiveLocation() {
        return activeLocation;
    }

    public long getNextSpawnTime() {
        return nextSpawnTime;
    }

    public long getEruptionTime() {
        return eruptionTime;
    }

    public boolean isErupting() {
        return erupting;
    }

    public String getInfoMessage() {
        if (activeType != null) {
            long remaining = Math.max(0, (eruptionTime - System.currentTimeMillis()) / 1000);
            String time = TimeUtil.formatTime(remaining);
            Location loc = activeLocation;
            return config.getPrefix() + "§fАктивный вулкан: " + config.getColor(activeType) + config.getDisplayName(activeType) + "\n"
                    + "§7Координаты: §f" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + "\n"
                    + "§7Мир: §f" + loc.getWorld().getName() + "\n"
                    + "§7До извержения: §e" + time;
        } else {
            long remaining = Math.max(0, (nextSpawnTime - System.currentTimeMillis()) / 1000);
            String time = TimeUtil.formatTime(remaining);
            return config.getPrefix() + "§fСледующий вулкан появится через §e" + time;
        }
    }
}
