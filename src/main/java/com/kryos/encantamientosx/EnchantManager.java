package com.kryos.encantamientosx;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lee los encantamientos de config.yml y ofrece utilidades (niveles máximos, precios, libros). */
public class EnchantManager {

    public record EnchantInfo(String key, Enchantment enchantment, String name, int max, double basePrice) {
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final EncantamientosX plugin;
    private final Map<String, EnchantInfo> byKey = new LinkedHashMap<>();
    private final Map<Enchantment, EnchantInfo> byEnchant = new HashMap<>();

    public EnchantManager(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    public void load() {
        byKey.clear();
        byEnchant.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("encantamientos");
        if (section == null) {
            plugin.getLogger().warning("No hay sección 'encantamientos' en config.yml");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            int max = Math.min(10, entry.getInt("max", 10));
            if (max <= 0) {
                continue;
            }
            NamespacedKey nsKey = NamespacedKey.minecraft(key.toLowerCase());
            Enchantment enchantment = Registry.ENCHANTMENT.get(nsKey);
            if (enchantment == null) {
                plugin.getLogger().warning("Encantamiento desconocido en esta versión, se ignora: " + key);
                continue;
            }
            EnchantInfo info = new EnchantInfo(
                    key.toLowerCase(),
                    enchantment,
                    entry.getString("nombre", key),
                    max,
                    entry.getDouble("precio", 100));
            byKey.put(info.key(), info);
            byEnchant.put(enchantment, info);
        }
    }

    public Collection<EnchantInfo> all() {
        return byKey.values();
    }

    public EnchantInfo byKey(String key) {
        return key == null ? null : byKey.get(key.toLowerCase());
    }

    public EnchantInfo get(Enchantment enchantment) {
        return byEnchant.get(enchantment);
    }

    /** Nivel máximo configurado, o -1 si el encantamiento no está gestionado por el plugin. */
    public int maxLevel(Enchantment enchantment) {
        EnchantInfo info = byEnchant.get(enchantment);
        return info == null ? -1 : info.max();
    }

    public double price(EnchantInfo info, int level) {
        double multiplier = plugin.getConfig().getDouble("precios.multiplicador", 2.0);
        return info.basePrice() * Math.pow(multiplier, level - 1);
    }

    public ItemStack createBook(EnchantInfo info, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(info.enchantment(), level, true);
        book.setItemMeta(meta);
        return book;
    }

    public static String roman(int level) {
        return level >= 1 && level < ROMAN.length ? ROMAN[level] : String.valueOf(level);
    }
}
