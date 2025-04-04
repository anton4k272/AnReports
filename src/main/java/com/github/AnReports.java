package com.github;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;

public class AnReports extends JavaPlugin implements Listener {

    private final Map<String, List<Report>> reports = new HashMap<>();
    private int reportCounter = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AnReports has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AnReports has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("report")) {
            if (!sender.hasPermission("anreports.report")) {
                sender.sendMessage(getConfig().getString("messages.no_permission"));
                return true;
            }
            if (args.length == 0) {
                for (String line : getConfig().getStringList("messages.report_help")) {
                    sender.sendMessage(line);
                }
                return true;
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
                if (!sender.hasPermission("anreports.reports")) {
                    sender.sendMessage(getConfig().getString("messages.no_permission"));
                    return true;
                }
                try {
                    int reportId = Integer.parseInt(args[1]);
                    if (deleteReportById(reportId)) {
                        sender.sendMessage(getConfig().getString("messages.report_deleted").replace("{report_id}", String.valueOf(reportId)));
                    } else {
                        sender.sendMessage(getConfig().getString("messages.report_not_found").replace("{report_id}", String.valueOf(reportId)));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(getConfig().getString("messages.invalid_report_id"));
                }
                return true;
            } else if (args.length >= 2) {
                String playerName = args[0];
                String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                int reportId = ++reportCounter;
                Report report = new Report(reportId, sender.getName(), reason, time);

                reports.computeIfAbsent(playerName, k -> new ArrayList<>()).add(report);
                if (reports.get(playerName).size() > 10) {
                    reports.get(playerName).remove(0);
                }

                sender.sendMessage(getConfig().getString("messages.report_submitted")
                        .replace("{player}", playerName)
                        .replace("{reason}", reason));
                updateReportGUI();
                return true;
            }
        } else if (label.equalsIgnoreCase("reports")) {
            if (!sender.hasPermission("anreports.reports")) {
                sender.sendMessage(getConfig().getString("messages.no_permission"));
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openReportGUI(player);
                return true;
            } else {
                sender.sendMessage(getConfig().getString("messages.command_only_players"));
                return true;
            }
        }
        return false;
    }

    private boolean deleteReportById(int reportId) {
        for (List<Report> reportList : reports.values()) {
            Iterator<Report> iterator = reportList.iterator();
            while (iterator.hasNext()) {
                Report report = iterator.next();
                if (report.getId() == reportId) {
                    iterator.remove();
                    updateReportGUI();
                    return true;
                }
            }
        }
        return false;
    }

    private void updateReportGUI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equals(getConfig().getString("gui.title"))) {
                openReportGUI(player);
            }
        }
    }

    private void openReportGUI(Player player) {
        Inventory reportMenu = Bukkit.createInventory(null, getConfig().getInt("gui.size"), getConfig().getString("gui.title"));

        // Add reports to the inventory
        for (Map.Entry<String, List<Report>> entry : reports.entrySet()) {
            String reportedPlayer = entry.getKey();
            for (Report report : entry.getValue()) {
                ItemStack reportItem = new ItemStack(Material.valueOf(getConfig().getString("gui.report_item.material")));
                ItemMeta meta = reportItem.getItemMeta();

                meta.setDisplayName(getConfig().getString("gui.report_item.display_name")
                        .replace("%reporter%", report.getReporter())
                        .replace("%reported%", reportedPlayer));
                List<String> lore = new ArrayList<>();
                lore.add(getConfig().getStringList("gui.report_item.lore").get(0).replace("%reason%", report.getReason()));
                lore.add(getConfig().getStringList("gui.report_item.lore").get(1).replace("%time%", report.getTime()));
                lore.add("Report ID: " + report.getId());
                meta.setLore(lore);

                reportItem.setItemMeta(meta);

                for (int slot : getConfig().getIntegerList("gui.report_item.slots")) {
                    if (reportMenu.getItem(slot) == null || reportMenu.getItem(slot).getType() == Material.AIR) {
                        reportMenu.setItem(slot, reportItem);
                        break;
                    }
                }
            }
        }

        player.openInventory(reportMenu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(getConfig().getString("gui.title"))) {
            event.setCancelled(true);
        }
    }

    private static class Report {
        private final int id;
        private final String reporter;
        private final String reason;
        private final String time;

        public Report(int id, String reporter, String reason, String time) {
            this.id = id;
            this.reporter = reporter;
            this.reason = reason;
            this.time = time;
        }

        public int getId() {
            return id;
        }

        public String getReporter() {
            return reporter;
        }

        public String getReason() {
            return reason;
        }

        public String getTime() {
            return time;
        }
    }
}