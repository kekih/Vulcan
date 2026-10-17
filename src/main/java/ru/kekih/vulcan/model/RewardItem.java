package ru.kekih.vulcan.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardItem {

    private final Material material;
    private final int amount;
    private final double chance;
    private final String name;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final int customModelData;

    public RewardItem(Material material, int amount, double chance, String name,
                      List<String> lore, Map<Enchantment, Integer> enchantments, int customModelData) {
        this.material = material;
        this.amount = Math.max(1, amount);
        this.chance = Math.max(0, Math.min(100, chance));
        this.name = name;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.customModelData = customModelData;
    }

    public static RewardItem fromItemStack(ItemStack stack, double chance) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        ItemMeta meta = stack.getItemMeta();
        String name = null;
        List<String> lore = new ArrayList<>();
        Map<Enchantment, Integer> enchants = new HashMap<>();
        int cmd = 0;
        if (meta != null) {
            if (meta.hasDisplayName()) name = meta.getDisplayName();
            if (meta.hasLore()) lore = meta.getLore();
            if (meta.hasEnchants()) enchants.putAll(meta.getEnchants());
            if (meta.hasCustomModelData()) cmd = meta.getCustomModelData();
        }
        return new RewardItem(stack.getType(), stack.getAmount(), chance, name, lore, enchants, cmd);
    }

    public static RewardItem fromConfig(ConfigurationSection section) {
        if (section == null) return null;
        Material mat;
        try {
            mat = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        } catch (Exception e) {
            return null;
        }
        int amount = section.getInt("amount", 1);
        double chance = section.getDouble("chance", 10.0);
        String name = section.getString("name", null);
        List<String> lore = section.getStringList("lore");
        Map<Enchantment, Integer> enchants = new HashMap<>();
        if (section.isConfigurationSection("enchantments")) {
            ConfigurationSection enchSec = section.getConfigurationSection("enchantments");
            for (String key : enchSec.getKeys(false)) {
                try {
                    Enchantment ench = Enchantment.getByName(key.toUpperCase());
                    if (ench != null) {
                        enchants.put(ench, enchSec.getInt(key));
                    }
                } catch (Exception ignored) {}
            }
        }
        List<String> enchList = section.getStringList("enchantments");
        for (String s : enchList) {
            String[] parts = s.split(":");
            if (parts.length == 2) {
                try {
                    Enchantment ench = Enchantment.getByName(parts[0].toUpperCase());
                    if (ench != null) {
                        enchants.put(ench, Integer.parseInt(parts[1]));
                    }
                } catch (Exception ignored) {}
            }
        }
        int cmd = section.getInt("custom-model-data", 0);
        return new RewardItem(mat, amount, chance, name, lore, enchants, cmd);
    }

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(name.replace('&', '§'));
            }
            if (!lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(line.replace('&', '§'));
                }
                meta.setLore(colored);
            }
            for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("material", material.name());
        section.set("amount", amount);
        section.set("chance", chance);
        if (name != null) section.set("name", name);
        if (!lore.isEmpty()) section.set("lore", lore);
        if (!enchantments.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
                list.add(e.getKey().getName() + ":" + e.getValue());
            }
            section.set("enchantments", list);
        }
        if (customModelData > 0) section.set("custom-model-data", customModelData);
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public double getChance() { return chance; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public int getCustomModelData() { return customModelData; }

    public RewardItem withChance(double newChance) {
        return new RewardItem(material, amount, newChance, name, lore, enchantments, customModelData);
    }
}
