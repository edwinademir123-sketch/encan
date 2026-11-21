package com.kryos.encantamientosx;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Permite que dos encantamientos del mismo nivel se combinen en el yunque
 * y den el siguiente nivel (IV + IV = V ... IX + IX = X), por encima del máximo vanilla.
 */
public class AnvilListener implements Listener {

    private final EncantamientosX plugin;

    public AnvilListener(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getFirstItem();
        ItemStack second = inv.getSecondItem();
        if (first == null || second == null) {
            return;
        }
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("encantamientosx.yunque")) {
            return;
        }

        boolean firstIsBook = first.getType() == Material.ENCHANTED_BOOK;
        boolean secondIsBook = second.getType() == Material.ENCHANTED_BOOK;
        // Solo libro + ítem, libro + libro o dos ítems del mismo tipo
        if (!secondIsBook && second.getType() != first.getType()) {
            return;
        }

        Map<Enchantment, Integer> firstEnchants = enchantsOf(first);
        Map<Enchantment, Integer> secondEnchants = enchantsOf(second);
        if (secondEnchants.isEmpty()) {
            return;
        }

        EnchantManager manager = plugin.getEnchantManager();

        // Partimos del resultado vanilla (si existe) para conservar renombre, reparación, etc.
        ItemStack vanilla = event.getResult();
        ItemStack base = (vanilla != null && !vanilla.getType().isAir()) ? vanilla.clone() : first.clone();
        Map<Enchantment, Integer> result = new HashMap<>(enchantsOf(base));

        boolean touched = false;
        int levelSum = 0;

        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int max = manager.maxLevel(ench);
            if (max < 0) {
                continue; // no gestionado por el plugin: lo deja a vanilla
            }

            int levelA = firstEnchants.getOrDefault(ench, 0);
            int levelB = entry.getValue();

            int target;
            if (levelA > 0 && levelA == levelB) {
                target = Math.min(levelA + 1, Math.max(max, levelA));
            } else {
                target = Math.max(levelA, levelB);
            }

            // Si es un encantamiento nuevo que vanilla descartó, comprobamos compatibilidad
            if (levelA == 0 && !result.containsKey(ench)) {
                if (!firstIsBook && !ench.canEnchantItem(first)) {
                    continue;
                }
                boolean conflict = false;
                for (Enchantment other : result.keySet()) {
                    if (!other.equals(ench) && other.conflictsWith(ench)) {
                        conflict = true;
                        break;
                    }
                }
                if (conflict) {
                    continue;
                }
            }

            int current = result.getOrDefault(ench, 0);
            if (target > current) {
                result.put(ench, target);
                touched = true;
            }
            int finalLevel = result.getOrDefault(ench, 0);
            if (finalLevel > levelA) {
                levelSum += finalLevel;
            }
        }

        if (!touched) {
            return; // vanilla ya resuelve este caso
        }

        ItemMeta meta = base.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Enchantment e : new ArrayList<>(storage.getStoredEnchants().keySet())) {
                storage.removeStoredEnchant(e);
            }
            for (Map.Entry<Enchantment, Integer> e : result.entrySet()) {
                storage.addStoredEnchant(e.getKey(), e.getValue(), true);
            }
        } else {
            for (Enchantment e : new ArrayList<>(meta.getEnchants().keySet())) {
                meta.removeEnchant(e);
            }
            for (Map.Entry<Enchantment, Integer> e : result.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
        }
        base.setItemMeta(meta);
        event.setResult(base);

        // Costo en niveles de XP
        AnvilView view = event.getView();
        int perLevel = plugin.getConfig().getInt("yunque.costo-por-nivel", 2);
        int maxCost = Math.max(1, plugin.getConfig().getInt("yunque.costo-maximo", 39));
        int cost = Math.min(Math.max(view.getRepairCost(), levelSum * perLevel), maxCost);
        view.setMaximumRepairCost(Math.max(40, maxCost + 1));
        view.setRepairCost(cost);
    }

    private Map<Enchantment, Integer> enchantsOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storage) {
            return new HashMap<>(storage.getStoredEnchants());
        }
        return new HashMap<>(item.getEnchantments());
    }
}
