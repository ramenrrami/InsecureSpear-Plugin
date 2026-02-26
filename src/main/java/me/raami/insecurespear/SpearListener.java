package me.raami.insecurespear;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpearListener implements Listener {

    private final Insecurespear plugin;

    public SpearListener(Insecurespear plugin) {
        this.plugin = plugin;
        startPassiveEffectsTask();
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack main = p.getInventory().getItemInMainHand();
                ItemStack off = p.getInventory().getItemInOffHand();

                boolean hasSpear = isGodSpear(main) || isGodSpear(off);

                if (hasSpear) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 2, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));

                    p.setFoodLevel(20);
                    p.setSaturation(20f);
                }

                if (isGodSpear(off) && main.getType().toString().contains("PICKAXE")) {
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

        if (isGodSpear(main) || isGodSpear(off)) {
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

            if (isGodSpear(main) || isGodSpear(off)) {
                event.setCancelled(true);
                p.setFoodLevel(20);
                p.setSaturation(20f);
            }
        }
    }

    private boolean isGodSpear(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.key, PersistentDataType.STRING);
    }
}