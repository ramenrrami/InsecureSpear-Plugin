package me.raami.insecurespear;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class SpearCommand implements CommandExecutor {

    private final Insecurespear plugin;

    public SpearCommand(Insecurespear plugin) {
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

        if (args[0].equalsIgnoreCase("give")) {
            Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(getMsg("player-not-found"));
                return true;
            }
            target.getInventory().addItem(plugin.createGodSpear());

            String msg = getMsg("give-success").replace("%player%", target.getName());
            sender.sendMessage(msg);
        }

        else if (args[0].equalsIgnoreCase("check")) {
            Map<String, Integer> counts = new HashMap<>();
            int total = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                int pCount = 0;
                for (ItemStack item : p.getInventory().getContents()) {
                    if (isGodSpear(item)) pCount++;
                }
                if (pCount > 0) {
                    counts.put(p.getName(), pCount);
                    total += pCount;
                }
            }

            String header = getRawMsg("check-header").replace("%amount%", String.valueOf(total));
            sender.sendMessage(header);

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String line = getRawMsg("check-format")
                        .replace("%player%", entry.getKey())
                        .replace("%amount%", String.valueOf(entry.getValue()));
                sender.sendMessage(line);
            }
        }

        else if (args[0].equalsIgnoreCase("removeall")) {
            sender.sendMessage(getMsg("remove-start"));
            int removed = 0;

            for (Player p : Bukkit.getOnlinePlayers()) {
                removed += removeSpears(p.getInventory());
                removed += removeSpears(p.getEnderChest());
                if (isGodSpear(p.getItemOnCursor())) {
                    p.setItemOnCursor(null);
                    removed++;
                }
                p.updateInventory();
            }

            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item) {
                        if (isGodSpear(((Item) entity).getItemStack())) {
                            entity.remove();
                            removed++;
                        }
                    } else if (entity instanceof ItemFrame) {
                        if (isGodSpear(((ItemFrame) entity).getItem())) {
                            ((ItemFrame) entity).setItem(null);
                            removed++;
                        }
                    } else if (entity instanceof LivingEntity) {
                        EntityEquipment equip = ((LivingEntity) entity).getEquipment();
                        if (equip != null) {
                            if (isGodSpear(equip.getItemInMainHand())) {
                                equip.setItemInMainHand(null); removed++;
                            }
                            if (isGodSpear(equip.getItemInOffHand())) {
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
        }

        else if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(getMsg("reload"));
        }

        return true;
    }

    private boolean isGodSpear(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.key, PersistentDataType.STRING);
    }

    private int removeSpears(Inventory inv) {
        int count = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isGodSpear(contents[i])) {
                inv.setItem(i, null);
                count++;
            }
        }
        return count;
    }
}