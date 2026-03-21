/*
 * Copyright (C) 2026 ramenrrami
 *
 * This work is licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nd/4.0/
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You are not allowed to remove the credits when using this plugin.
 */
package me.raami.insecureSpears;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpearCommand implements CommandExecutor, TabCompleter {

    private final InsecureSpears plugin;

    public SpearCommand(InsecureSpears plugin) {
        this.plugin = plugin;
    }

    private String getMsg(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bGodSpear&8] ");
        String msg = plugin.getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private String getRawMsg(String path) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + path, ""));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("credits")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n&6Plugin Made By: ue_rami\n"));
            return true;
        }

        if (!sender.hasPermission("insecurespear.adminuse")) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(getMsg("player-not-found"));
                return true;
            }
            target.getInventory().addItem(plugin.createGodSpear(target.getName(), target.getUniqueId().toString()));

            String msg = getMsg("give-success").replace("%player%", target.getName());
            sender.sendMessage(msg);
        }

        else if (args[0].equalsIgnoreCase("check")) {
            plugin.getDatabase().getActiveSpearsCountsAsync((counts) -> {
                int total = counts.values().stream().mapToInt(Integer::intValue).sum();
                String header = getRawMsg("check-header").replace("%amount%", String.valueOf(total));
                sender.sendMessage(header);

                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String line = getRawMsg("check-format")
                            .replace("%player%", entry.getKey())
                            .replace("%amount%", String.valueOf(entry.getValue()));
                    sender.sendMessage(line);
                }
            });
        }

        else if (args[0].equalsIgnoreCase("removeall")) {
            sender.sendMessage(getMsg("remove-start"));
            
            plugin.getDatabase().removeAllAsync(() -> {
                int removed = 0;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    removed += removeSpears(p.getInventory());
                    removed += removeSpears(p.getEnderChest());
                    if (plugin.isGodSpearValid(p.getItemOnCursor())) {
                        p.setItemOnCursor(null);
                        removed++;
                    }
                    p.updateInventory();
                }

                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Item) {
                            if (plugin.isGodSpearValid(((Item) entity).getItemStack())) {
                                entity.remove();
                                removed++;
                            }
                        } else if (entity instanceof ItemFrame) {
                            if (plugin.isGodSpearValid(((ItemFrame) entity).getItem())) {
                                ((ItemFrame) entity).setItem(null);
                                removed++;
                            }
                        } else if (entity instanceof LivingEntity) {
                            EntityEquipment equip = ((LivingEntity) entity).getEquipment();
                            if (equip != null) {
                                if (plugin.isGodSpearValid(equip.getItemInMainHand())) {
                                    equip.setItemInMainHand(null); removed++;
                                }
                                if (plugin.isGodSpearValid(equip.getItemInOffHand())) {
                                    equip.setItemInOffHand(null); removed++;
                                }
                            }
                        }
                    }

                    for (Chunk chunk : world.getLoadedChunks()) {
                        for (BlockState state : chunk.getTileEntities()) {
                            if (state instanceof Container) {
                                removed += removeSpears(((Container) state).getInventory());
                            }
                        }
                    }
                }

                String msg = getMsg("remove-success").replace("%amount%", String.valueOf(removed));
                sender.sendMessage(msg);
            });
        }

        else if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getDatabase().disconnect();
            plugin.getDatabase().connect();
            plugin.getDatabase().createTable();
            sender.sendMessage(getMsg("reload"));
        }

        return true;
    }

    private int removeSpears(Inventory inv) {
        int count = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (plugin.isGodSpearValid(item)) {
                inv.setItem(i, null);
                count++;
            } else if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BundleMeta) {
                BundleMeta bundleMeta = (BundleMeta) item.getItemMeta();
                List<ItemStack> bundleItems = new ArrayList<>(bundleMeta.getItems());
                boolean changed = false;
                for (int j = bundleItems.size() - 1; j >= 0; j--) {
                    if (plugin.isGodSpearValid(bundleItems.get(j))) {
                        bundleItems.remove(j);
                        count++;
                        changed = true;
                    }
                }
                if (changed) {
                    bundleMeta.setItems(bundleItems);
                    item.setItemMeta(bundleMeta);
                }
            }
        }
        return count;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = Arrays.asList("give", "check", "removeall", "reload", "credits");
            completions = options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            if (!sender.hasPermission("insecurespear.adminuse")) {
                completions.removeIf(s -> !s.equals("credits"));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("insecurespear.adminuse")) {
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}