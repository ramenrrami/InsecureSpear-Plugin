/*
 * Copyright (C) 2026 ramenrrami
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * You are not allowed to remove the credits when using this plugin.
 */
package me.raami.insecureSpears;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpearListener implements Listener {

    private final InsecureSpears plugin;

    public SpearListener(InsecureSpears plugin) {
        this.plugin = plugin;
        startPassiveEffectsTask();
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack main = p.getInventory().getItemInMainHand();
                ItemStack off = p.getInventory().getItemInOffHand();

                boolean hasSpear = plugin.isGodSpearValid(main) || plugin.isGodSpearValid(off);

                if (hasSpear) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 2, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));
                    p.setFoodLevel(20);
                    p.setSaturation(20f);
                }

                if (plugin.isGodSpearValid(off) && main != null && main.getType().toString().contains("PICKAXE")) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 29, false, false));
                }
            }
        }, 0L, 10L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();

        if (plugin.isGodSpearValid(main) || plugin.isGodSpearValid(off)) {
            Block blockUnder = p.getLocation().getBlock().getRelative(0, -1, 0);
            if (blockUnder.getType() == Material.WATER && p.isOnGround()) {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Block target = blockUnder.getRelative(x, 0, z);
                        if (target.getType() == Material.WATER && target.getRelative(0, 1, 0).getType() == Material.AIR) {
                            target.setType(Material.FROSTED_ICE);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHungerDeplete(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            ItemStack main = p.getInventory().getItemInMainHand();
            ItemStack off = p.getInventory().getItemInOffHand();

            if (plugin.isGodSpearValid(main) || plugin.isGodSpearValid(off)) {
                event.setCancelled(true);
                p.setFoodLevel(20);
                p.setSaturation(20f);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            
            checkAndUpdateOwner(currentItem, p);
            checkAndUpdateOwner(cursorItem, p);
        }
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            ItemStack item = event.getItem().getItemStack();
            checkAndUpdateOwner(item, p);
        }
    }

    private void checkAndUpdateOwner(ItemStack item, Player p) {
        if (plugin.isGodSpearValid(item)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(plugin.uuidKey, PersistentDataType.STRING)) {
                String uuid = meta.getPersistentDataContainer().get(plugin.uuidKey, PersistentDataType.STRING);
                plugin.getDatabase().updateOwnerAsync(uuid, p.getName(), p.getUniqueId().toString());
            }
        }
    }
}