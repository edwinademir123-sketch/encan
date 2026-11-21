package com.kryos.encantamientosx;

import com.kryos.encantamientosx.EnchantManager.EnchantInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Menús de la tienda: lista de encantamientos y selector de nivel. */
public class ShopGUI {

    public static final int[] LEVEL_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
    public static final int BACK_SLOT = 31;

    /** Holder propio para reconocer nuestros inventarios. */
    public static class Holder implements InventoryHolder {
        public final String enchantKey; // null en el menú principal
        public final Map<Integer, String> enchantBySlot = new HashMap<>();
        public final Map<Integer, Integer> levelBySlot = new HashMap<>();
        private Inventory inventory;

        public Holder(String enchantKey) {
            this.enchantKey = enchantKey;
        }

        public boolean isMain() {
            return enchantKey == null;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final EncantamientosX plugin;

    public ShopGUI(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        EnchantManager manager = plugin.getEnchantManager();

        Holder holder = new Holder(null);
        Inventory inv = Bukkit.createInventory(holder, 54,
                plugin.parse(cfg.getString("menu.titulo-principal", "Tienda de Encantamientos")));
        holder.setInventory(inv);

        int slot = 0;
        for (EnchantInfo info : manager.all()) {
            if (slot >= 45) {
                break;
            }
            String name = cfg.getString("menu.nombre-encantamiento", "{encantamiento}");
            List<Component> lore = new ArrayList<>();
            for (String line : cfg.getStringList("menu.lore-principal")) {
                lore.add(plugin.parse(line,
                        "encantamiento", info.name(),
                        "max", EnchantManager.roman(info.max()),
                        "precio_minimo", plugin.usesEconomy() ? plugin.money(manager.price(info, 1)) : "Gratis"));
            }
            inv.setItem(slot, icon(plugin.parse(name, "encantamiento", info.name()), lore, 1));
            holder.enchantBySlot.put(slot, info.key());
            slot++;
        }

        fillRow(inv, 45, 53);
        player.openInventory(inv);
    }

    public void openLevels(Player player, EnchantInfo info) {
        FileConfiguration cfg = plugin.getConfig();
        EnchantManager manager = plugin.getEnchantManager();

        Holder holder = new Holder(info.key());
        Inventory inv = Bukkit.createInventory(holder, 36,
                plugin.parse(cfg.getString("menu.titulo-niveles", "{encantamiento}"),
                        "encantamiento", info.name()));
        holder.setInventory(inv);

        for (int level = 1; level <= info.max() && level <= LEVEL_SLOTS.length; level++) {
            int slot = LEVEL_SLOTS[level - 1];
            String price = plugin.usesEconomy() ? plugin.money(manager.price(info, level)) : "Gratis";

            List<Component> lore = new ArrayList<>();
            for (String line : cfg.getStringList("menu.lore-nivel")) {
                lore.add(plugin.parse(line,
                        "encantamiento", info.name(),
                        "nivel", EnchantManager.roman(level),
                        "precio", price));
            }
            Component name = plugin.parse(cfg.getString("menu.nombre-nivel", "{encantamiento} {nivel}"),
                    "encantamiento", info.name(),
                    "nivel", EnchantManager.roman(level));

            inv.setItem(slot, icon(name, lore, level));
            holder.levelBySlot.put(slot, level);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(plugin.parse(cfg.getString("menu.boton-volver", "Volver")));
        back.setItemMeta(backMeta);
        inv.setItem(BACK_SLOT, back);

        fillRow(inv, 27, 35, BACK_SLOT);
        player.openInventory(inv);
    }

    private ItemStack icon(Component name, List<Component> lore, int amount) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Rellena un rango de casillas vacías con paneles de cristal (saltando la casilla indicada). */
    private void fillRow(Inventory inv, int from, int to, int... skip) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = from; i <= to; i++) {
            boolean skipped = false;
            for (int s : skip) {
                if (s == i) {
                    skipped = true;
                    break;
                }
            }
            if (!skipped) {
                inv.setItem(i, pane);
            }
        }
    }
}
