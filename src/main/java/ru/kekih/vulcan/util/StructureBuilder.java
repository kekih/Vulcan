package ru.kekih.vulcan.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import ru.kekih.vulcan.model.VulcanType;

import java.util.List;
import java.util.Random;

/**
 * Строит физическую структуру вулкана в мире.
 * Стиль вдохновлён событиями FunTime: конус из чёрного камня/базальта/магмы с кратером.
 */
public class StructureBuilder {

    private static final Random RANDOM = new Random();

    public static void build(Location center, VulcanType type, List<Location> placed) {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int baseRadius = type == VulcanType.NORMAL ? 5 : (type == VulcanType.LEGENDARY ? 6 : 7);
        int height = type == VulcanType.NORMAL ? 6 : (type == VulcanType.LEGENDARY ? 7 : 8);

        for (int y = 0; y < height; y++) {
            double progress = (double) y / height;
            int radius = (int) Math.max(1, baseRadius * (1.0 - progress * 0.85));
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= radius + 0.5) {
                        int bx = cx + x;
                        int by = cy + y;
                        int bz = cz + z;
                        Material mat = chooseMaterial(dist, radius, y, height, type);
                        setBlock(world, bx, by, bz, mat, placed);
                    }
                }
            }
        }

        int craterY = cy + height;
        int craterR = Math.max(1, baseRadius / 3);
        for (int x = -craterR; x <= craterR; x++) {
            for (int z = -craterR; z <= craterR; z++) {
                if (x * x + z * z <= craterR * craterR) {
                    setBlock(world, cx + x, craterY, cz + z, Material.AIR, placed);
                    if (x * x + z * z <= (craterR - 1) * (craterR - 1) && craterR > 1) {
                        setBlock(world, cx + x, craterY - 1, cz + z, Material.MAGMA_BLOCK, placed);
                    }
                }
            }
        }

        setBlock(world, cx, craterY - 1, cz, Material.LAVA, placed);
        if (type != VulcanType.NORMAL) {
            setBlock(world, cx, craterY, cz, Material.LAVA, placed);
        }

        if (type == VulcanType.LEGENDARY || type == VulcanType.MYTHICAL) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int ox = (int) Math.round(Math.cos(angle) * (craterR + 1));
                int oz = (int) Math.round(Math.sin(angle) * (craterR + 1));
                setBlock(world, cx + ox, craterY, cz + oz, Material.OBSIDIAN, placed);
            }
        }

        if (type == VulcanType.MYTHICAL) {
            for (int i = 0; i < 6; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                int ox = (int) Math.round(Math.cos(angle) * (baseRadius - 1));
                int oz = (int) Math.round(Math.sin(angle) * (baseRadius - 1));
                Block b = world.getBlockAt(cx + ox, cy + 1, cz + oz);
                if (b.getType().isAir() || isVolcanoMat(b.getType())) {
                    setBlock(world, cx + ox, cy + 1, cz + oz, Material.FIRE, placed);
                }
            }
        }
    }

    private static Material chooseMaterial(double dist, int radius, int y, int height, VulcanType type) {
        double edge = radius - dist;
        if (edge < 1.2) {
            if (RANDOM.nextDouble() < 0.4) return Material.BLACKSTONE;
            if (RANDOM.nextDouble() < 0.3) return Material.BASALT;
            return Material.BLACKSTONE;
        }
        if (y > height * 0.6 && RANDOM.nextDouble() < 0.5) {
            return Material.MAGMA_BLOCK;
        }
        if (RANDOM.nextDouble() < 0.25) return Material.MAGMA_BLOCK;
        if (RANDOM.nextDouble() < 0.3) return Material.NETHERRACK;
        if (RANDOM.nextDouble() < 0.2) return Material.BASALT;
        return Material.BLACKSTONE;
    }

    private static void setBlock(World world, int x, int y, int z, Material mat, List<Location> placed) {
        Block b = world.getBlockAt(x, y, z);
        if (b.getType() == Material.BEDROCK) return;
        b.setType(mat, false);
        if (mat == Material.LAVA) {
            if (b.getBlockData() instanceof Levelled) {
                Levelled levelled = (Levelled) b.getBlockData();
                levelled.setLevel(0);
                b.setBlockData(levelled, false);
            }
        }
        placed.add(b.getLocation());
    }

    private static boolean isVolcanoMat(Material m) {
        return m == Material.MAGMA_BLOCK || m == Material.BLACKSTONE || m == Material.BASALT
                || m == Material.NETHERRACK || m == Material.OBSIDIAN;
    }
}
