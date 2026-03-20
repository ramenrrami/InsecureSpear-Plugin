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
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InsecureSpears extends JavaPlugin {

    public static InsecureSpears instance;
    public final NamespacedKey key = new NamespacedKey(this, "godspear_id");
    public final NamespacedKey uuidKey = new NamespacedKey(this, "godspear_uuid");
    private Database database;
    private final Set<String> removedSpears = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        database = new Database(this);
        database.connect();
        database.createTable();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            database.fetchRemovedSpears(removedSpears);
        }, 0L, 100L);

        getCommand("insecurespear").setExecutor(new SpearCommand(this));
        getCommand("insecurespear").setTabCompleter(new SpearCommand(this));
        getServer().getPluginManager().registerEvents(new SpearListener(this), this);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.disconnect();
        }
    }

    public Database getDatabase() {
        return database;
    }

    public boolean isGodSpearValid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return false;
        
        if (meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
            if (removedSpears.contains(uuid)) {
                item.setAmount(0);
                return false;
            }
        }
        return true;
    }

    public ItemStack createGodSpear(String ownerName, String ownerUuid) {
        Material spearMat;
        try {
            spearMat = Material.valueOf("TRIDENT");
        } catch (IllegalArgumentException e) {
            spearMat = Material.NETHERITE_SWORD;
        }

        ItemStack spear = new ItemStack(spearMat);
        ItemMeta meta = spear.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§bOP Spear");
            meta.setLore(Collections.singletonList("§cℹ ᴍᴀᴅᴇ ʙʏ ᴜᴇ_ʀᴀᴍɪ"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "godspear");
            
            String spearUuid = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, spearUuid);

            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 3, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.FROST_WALKER, 3, true);

            addCustomEnchant(meta, "wind_burst", 3);
            addCustomEnchant(meta, "lunge", 4);

            spear.setItemMeta(meta);
            
            database.insertSpearAsync(spearUuid, ownerName, ownerUuid);
        }
        return spear;
    }

    private void addCustomEnchant(ItemMeta meta, String keyName, int level) {
        try {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(keyName));
            if (ench != null) {
                meta.addEnchant(ench, level, true);
            }
        } catch (Exception ignored) {}
    }
}