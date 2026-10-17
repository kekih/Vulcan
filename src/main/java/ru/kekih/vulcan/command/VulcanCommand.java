package ru.kekih.vulcan.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.kekih.vulcan.VulcanPlugin;
import ru.kekih.vulcan.manager.ConfigManager;
import ru.kekih.vulcan.manager.VulcanManager;
import ru.kekih.vulcan.model.RewardItem;
import ru.kekih.vulcan.model.VulcanType;
import ru.kekih.vulcan.util.TimeUtil;

import java.util.*;

public class VulcanCommand implements CommandExecutor, TabCompleter {

    private final VulcanPlugin plugin;
    private final ConfigManager config;
    private final VulcanManager manager;

    public VulcanCommand(VulcanPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.manager = plugin.getVulcanManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("vulcan.info")) {
                sender.sendMessage(manager.getInfoMessage());
            } else {
                sender.sendMessage(config.getPrefix() + config.getMessage("no-permission"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("info")) {
            if (!sender.hasPermission("vulcan.info")) {
                sender.sendMessage(config.getPrefix() + config.getMessage("no-permission"));
                return true;
            }
            sender.sendMessage(manager.getInfoMessage());
            return true;
        }

        if (!sender.hasPermission("vulcan.admin")) {
            sender.sendMessage(config.getPrefix() + config.getMessage("no-permission"));
            return true;
        }

        switch (sub) {
            case "reload":
                plugin.reload();
                sender.sendMessage(config.getPrefix() + config.getMessage("reload"));
                break;

            case "timer":
                long remaining = Math.max(0, (manager.getNextSpawnTime() - System.currentTimeMillis()) / 1000);
                if (manager.getActiveType() != null) {
                    long erup = Math.max(0, (manager.getEruptionTime() - System.currentTimeMillis()) / 1000);
                    sender.sendMessage(config.getPrefix() + "§fАктивный: " + config.getDisplayName(manager.getActiveType())
                            + " §7| До извержения: §e" + TimeUtil.formatTime(erup));
                } else {
                    sender.sendMessage(config.getPrefix() + config.format("timer", Collections.singletonMap("time", TimeUtil.formatTime(remaining))));
                }
                break;

            case "list":
                sender.sendMessage(config.getPrefix() + "§fТипы вулканов:");
                for (VulcanType t : VulcanType.values()) {
                    sender.sendMessage("§7- " + config.getColor(t) + config.getDisplayName(t)
                            + " §8(шанс: " + config.getSpawnChance(t) + "%)");
                }
                break;

            case "spawn":
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan spawn <тип>");
                    return true;
                }
                VulcanType spawnType = VulcanType.fromString(args[1]);
                if (spawnType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                if (manager.getActiveType() != null && config.isOneActive()) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("already-active"));
                    return true;
                }
                manager.spawn(spawnType, true);
                Map<String, String> ph = new HashMap<>();
                ph.put("type", config.getDisplayName(spawnType));
                sender.sendMessage(config.getPrefix() + config.format("spawn-manual", ph));
                break;

            case "despawn":
                if (manager.getActiveType() == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("no-active"));
                    return true;
                }
                manager.despawn();
                sender.sendMessage(config.getPrefix() + config.getMessage("despawn"));
                break;

            case "setlocation":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("player-only"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan setlocation <тип>");
                    return true;
                }
                VulcanType locType = VulcanType.fromString(args[1]);
                if (locType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                Player p = (Player) sender;
                config.setLocation(locType, p.getLocation());
                Map<String, String> locPh = new HashMap<>();
                locPh.put("type", config.getDisplayName(locType));
                locPh.put("world", p.getWorld().getName());
                locPh.put("x", String.valueOf((int) p.getLocation().getX()));
                locPh.put("y", String.valueOf((int) p.getLocation().getY()));
                locPh.put("z", String.valueOf((int) p.getLocation().getZ()));
                sender.sendMessage(config.getPrefix() + config.format("location-set", locPh));
                break;

            case "setinterval":
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan setinterval <секунды>");
                    return true;
                }
                try {
                    int sec = Integer.parseInt(args[1]);
                    if (sec < 60) {
                        sender.sendMessage(config.getPrefix() + "§cМинимум 60 секунд.");
                        return true;
                    }
                    config.setSpawnInterval(sec);
                    Map<String, String> iPh = Collections.singletonMap("seconds", String.valueOf(sec));
                    sender.sendMessage(config.getPrefix() + config.format("interval-set", iPh));
                } catch (NumberFormatException e) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-number"));
                }
                break;

            case "setduration":
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan setduration <секунды>");
                    return true;
                }
                try {
                    int sec = Integer.parseInt(args[1]);
                    if (sec < 10) {
                        sender.sendMessage(config.getPrefix() + "§cМинимум 10 секунд.");
                        return true;
                    }
                    config.setEruptionDelay(sec);
                    Map<String, String> dPh = Collections.singletonMap("seconds", String.valueOf(sec));
                    sender.sendMessage(config.getPrefix() + config.format("duration-set", dPh));
                } catch (NumberFormatException e) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-number"));
                }
                break;

            case "add":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("player-only"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan add <тип>");
                    return true;
                }
                VulcanType addType = VulcanType.fromString(args[1]);
                if (addType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("no-item-in-hand"));
                    return true;
                }
                RewardItem reward = RewardItem.fromItemStack(hand.clone(), 10.0);
                config.addReward(addType, reward);
                Map<String, String> aPh = Collections.singletonMap("type", config.getDisplayName(addType));
                sender.sendMessage(config.getPrefix() + config.format("reward-added", aPh));
                break;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan remove <тип> <номер>");
                    return true;
                }
                VulcanType remType = VulcanType.fromString(args[1]);
                if (remType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                try {
                    int idx = Integer.parseInt(args[2]);
                    if (config.removeReward(remType, idx)) {
                        Map<String, String> rPh = new HashMap<>();
                        rPh.put("index", String.valueOf(idx));
                        rPh.put("type", config.getDisplayName(remType));
                        sender.sendMessage(config.getPrefix() + config.format("reward-removed", rPh));
                    } else {
                        sender.sendMessage(config.getPrefix() + "§cНаграда с таким номером не найдена.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-number"));
                }
                break;

            case "setchance":
                if (args.length < 4) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan setchance <тип> <номер> <шанс>");
                    return true;
                }
                VulcanType chType = VulcanType.fromString(args[1]);
                if (chType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                try {
                    int idx = Integer.parseInt(args[2]);
                    double chance = Double.parseDouble(args[3]);
                    if (config.setRewardChance(chType, idx, chance)) {
                        Map<String, String> cPh = new HashMap<>();
                        cPh.put("index", String.valueOf(idx));
                        cPh.put("type", config.getDisplayName(chType));
                        cPh.put("chance", String.valueOf(chance));
                        sender.sendMessage(config.getPrefix() + config.format("chance-set", cPh));
                    } else {
                        sender.sendMessage(config.getPrefix() + "§cНаграда с таким номером не найдена.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-number"));
                }
                break;

            case "check":
                if (args.length < 2) {
                    sender.sendMessage(config.getPrefix() + "§cИспользование: /vulcan check <тип>");
                    return true;
                }
                VulcanType checkType = VulcanType.fromString(args[1]);
                if (checkType == null) {
                    sender.sendMessage(config.getPrefix() + config.getMessage("invalid-type"));
                    return true;
                }
                List<RewardItem> rewards = config.getRewards(checkType);
                sender.sendMessage(config.getPrefix() + "§fНаграды " + config.getColor(checkType) + config.getDisplayName(checkType) + "§f:");
                if (rewards.isEmpty()) {
                    sender.sendMessage("§7Список пуст. Добавьте предметы командой /vulcan add " + checkType.getKey());
                } else {
                    for (int i = 0; i < rewards.size(); i++) {
                        RewardItem r = rewards.get(i);
                        String name = r.getName() != null ? r.getName() : r.getMaterial().name();
                        sender.sendMessage("§7#" + i + " §f" + name + " §7x" + r.getAmount() + " §8(шанс: §e" + r.getChance() + "%§8)");
                    }
                }
                break;

            case "create":
            case "delete":
                sender.sendMessage(config.getPrefix() + "§7Типы уже созданы в конфигурации. Используйте config.yml для тонкой настройки.");
                break;

            case "admin":
                sender.sendMessage(config.getPrefix() + "§7GUI-меню пока не реализовано. Используйте команды.");
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(config.getPrefix() + config.getMessage("unknown-subcommand"));
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== Vulcan — Помощь ===");
        sender.sendMessage("§e/vulcan info §7— информация о вулкане");
        if (sender.hasPermission("vulcan.admin")) {
            sender.sendMessage("§e/vulcan spawn <тип> §7— вызвать вулкан");
            sender.sendMessage("§e/vulcan despawn §7— удалить активный");
            sender.sendMessage("§e/vulcan setlocation <тип> §7— сохранить локацию");
            sender.sendMessage("§e/vulcan setinterval <сек> §7— интервал появления");
            sender.sendMessage("§e/vulcan setduration <сек> §7— время до извержения");
            sender.sendMessage("§e/vulcan add <тип> §7— добавить предмет из руки");
            sender.sendMessage("§e/vulcan remove <тип> <номер> §7— удалить награду");
            sender.sendMessage("§e/vulcan setchance <тип> <номер> <шанс> §7— шанс награды");
            sender.sendMessage("§e/vulcan check <тип> §7— список наград");
            sender.sendMessage("§e/vulcan list §7— список типов");
            sender.sendMessage("§e/vulcan timer §7— таймер");
            sender.sendMessage("§e/vulcan reload §7— перезагрузка конфига");
            sender.sendMessage("§7Типы: normal, legendary, mythical");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("vulcan.info")) subs.add("info");
            if (sender.hasPermission("vulcan.admin")) {
                subs.addAll(Arrays.asList("spawn", "despawn", "setlocation", "setinterval", "setduration",
                        "add", "remove", "setchance", "check", "list", "timer", "reload", "help"));
            }
            String input = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(input)) completions.add(s);
            }
        } else if (args.length == 2 && sender.hasPermission("vulcan.admin")) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("spawn", "setlocation", "add", "remove", "setchance", "check").contains(sub)) {
                String input = args[1].toLowerCase();
                for (VulcanType t : VulcanType.values()) {
                    if (t.getKey().startsWith(input)) completions.add(t.getKey());
                }
            }
        }
        return completions;
    }
}
