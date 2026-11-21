package com.kryos.encantamientosx;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tiendaenc - abre la tienda de libros de encantamientos. */
public class ShopCommand implements CommandExecutor {

    private final EncantamientosX plugin;

    public ShopCommand(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("solo-jugadores"));
            return true;
        }
        if (!player.hasPermission("encantamientosx.tienda")) {
            plugin.send(player, "sin-permiso");
            return true;
        }
        plugin.getShopGUI().openMain(player);
        return true;
    }
}
