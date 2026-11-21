package com.kryos.encantamientosx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

public class EncantamientosX extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private EnchantManager enchantManager;
    private ShopGUI shopGUI;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        enchantManager = new EnchantManager(this);
        enchantManager.load();

        shopGUI = new ShopGUI(this);

        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getCommand("tiendaenc").setExecutor(new ShopCommand(this));
        AdminCommand admin = new AdminCommand(this);
        getCommand("encx").setExecutor(admin);
        getCommand("encx").setTabCompleter(admin);

        getLogger().info("EncantamientosX activado con " + enchantManager.all().size() + " encantamientos.");
    }

    public void reload() {
        reloadConfig();
        enchantManager.load();
        economy = null; // se vuelve a buscar cuando haga falta
    }

    // ---------- Accesos ----------

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    // ---------- Economía (Vault) ----------

    public boolean usesEconomy() {
        return getConfig().getBoolean("economia.usar-vault", true);
    }

    /** Devuelve la economía de Vault, o null si no está disponible. */
    public Economy getEconomy() {
        if (economy != null) {
            return economy;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
        return economy;
    }

    // ---------- Textos ----------

    /** Reemplaza {clave} por valor. Se pasa como pares: "clave", "valor", "clave2", "valor2". */
    public String fill(String template, String... kv) {
        String result = template;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            result = result.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return result;
    }

    /** Convierte un texto MiniMessage a Component (sin cursiva). */
    public Component parse(String template, String... kv) {
        return MM.deserialize(fill(template, kv)).decoration(TextDecoration.ITALIC, false);
    }

    /** Mensaje de config.yml (mensajes.<clave>) con prefijo. */
    public Component message(String key, String... kv) {
        String prefix = getConfig().getString("mensajes.prefijo", "");
        String text = getConfig().getString("mensajes." + key, key);
        return parse(prefix + fill(text, kv));
    }

    public void send(Player player, String key, String... kv) {
        player.sendMessage(message(key, kv));
    }

    public String money(double amount) {
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return getConfig().getString("economia.simbolo", "$") + df.format(amount);
    }

    // ---------- Libros ----------

    /** Entrega los libros al jugador; lo que no quepa cae al suelo. Devuelve true si todo cupo. */
    public boolean giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        return leftover.isEmpty();
    }
}
