package com.kryos.encantamientosx;

import com.kryos.encantamientosx.EnchantManager.EnchantInfo;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {

    private final EncantamientosX plugin;

    public ShopListener(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopGUI.Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGUI.Holder holder)) {
            return;
        }
        // Nada se puede mover dentro de la tienda (ni con shift-clic desde el inventario)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        EnchantManager manager = plugin.getEnchantManager();

        if (holder.isMain()) {
            String key = holder.enchantBySlot.get(slot);
            if (key == null) {
                return;
            }
            EnchantInfo info = manager.byKey(key);
            if (info == null) {
                return;
            }
            // Se abre en el siguiente tick para no tocar el inventario dentro del evento de clic
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopGUI().openLevels(player, info));
            return;
        }

        if (slot == ShopGUI.BACK_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopGUI().openMain(player));
            return;
        }

        Integer level = holder.levelBySlot.get(slot);
        EnchantInfo info = manager.byKey(holder.enchantKey);
        if (level == null || info == null) {
            return;
        }
        buy(player, info, level);
    }

    private void buy(Player player, EnchantInfo info, int level) {
        EnchantManager manager = plugin.getEnchantManager();
        double price = manager.price(info, level);
        String priceText = plugin.usesEconomy() ? plugin.money(price) : "Gratis";

        if (plugin.usesEconomy()) {
            Economy economy = plugin.getEconomy();
            if (economy == null) {
                plugin.send(player, "sin-economia");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            if (!economy.has(player, price)) {
                plugin.send(player, "sin-dinero", "precio", priceText);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            EconomyResponse response = economy.withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                plugin.send(player, "sin-dinero", "precio", priceText);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

        ItemStack book = manager.createBook(info, level);
        boolean fit = plugin.giveItem(player, book);

        plugin.send(player, "compra-exitosa",
                "encantamiento", info.name(),
                "nivel", EnchantManager.roman(level),
                "precio", priceText);
        if (!fit) {
            plugin.send(player, "inventario-lleno");
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }
}
