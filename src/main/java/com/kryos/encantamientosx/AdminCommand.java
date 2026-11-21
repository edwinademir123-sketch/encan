package com.kryos.encantamientosx;

import com.kryos.encantamientosx.EnchantManager.EnchantInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /encx dar <jugador> <encantamiento> <nivel> [cantidad] | lista | recargar */
public class AdminCommand implements TabExecutor {

    private final EncantamientosX plugin;

    public AdminCommand(EncantamientosX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("encantamientosx.admin")) {
            sender.sendMessage(plugin.message("sin-permiso"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.message("uso-dar"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "recargar", "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.message("recargado",
                        "cantidad", String.valueOf(plugin.getEnchantManager().all().size())));
            }
            case "lista", "list" -> {
                sender.sendMessage(Component.text("Encantamientos disponibles:"));
                for (EnchantInfo info : plugin.getEnchantManager().all()) {
                    sender.sendMessage(Component.text(" - " + info.key() + " (" + info.name()
                            + ") máx " + EnchantManager.roman(info.max())));
                }
            }
            case "dar", "give" -> give(sender, args);
            default -> sender.sendMessage(plugin.message("uso-dar"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.message("uso-dar"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.message("jugador-no-encontrado"));
            return;
        }
        EnchantInfo info = plugin.getEnchantManager().byKey(args[2]);
        if (info == null) {
            sender.sendMessage(plugin.message("encantamiento-no-encontrado"));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            level = -1;
        }
        if (level < 1 || level > info.max()) {
            sender.sendMessage(plugin.message("nivel-invalido",
                    "encantamiento", info.name(), "max", String.valueOf(info.max())));
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        for (int i = 0; i < amount; i++) {
            plugin.giveItem(target, plugin.getEnchantManager().createBook(info, level));
        }
        String roman = EnchantManager.roman(level);
        sender.sendMessage(plugin.message("libro-dado",
                "cantidad", String.valueOf(amount), "encantamiento", info.name(),
                "nivel", roman, "jugador", target.getName()));
        target.sendMessage(plugin.message("libro-recibido",
                "cantidad", String.valueOf(amount), "encantamiento", info.name(), "nivel", roman));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("encantamientosx.admin")) {
            return out;
        }
        if (args.length == 1) {
            addMatches(out, args[0], List.of("dar", "lista", "recargar"));
        } else if (args[0].equalsIgnoreCase("dar") || args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                List<String> names = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                addMatches(out, args[1], names);
            } else if (args.length == 3) {
                List<String> keys = new ArrayList<>();
                plugin.getEnchantManager().all().forEach(i -> keys.add(i.key()));
                addMatches(out, args[2], keys);
            } else if (args.length == 4) {
                EnchantInfo info = plugin.getEnchantManager().byKey(args[2]);
                int max = info == null ? 10 : info.max();
                List<String> levels = new ArrayList<>();
                for (int i = 1; i <= max; i++) {
                    levels.add(String.valueOf(i));
                }
                addMatches(out, args[3], levels);
            }
        }
        return out;
    }

    private void addMatches(List<String> out, String typed, List<String> options) {
        String lower = typed.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
    }
}
