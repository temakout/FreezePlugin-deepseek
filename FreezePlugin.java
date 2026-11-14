package ru.freeze;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezePlugin extends JavaPlugin implements Listener {

    private final Set<UUID> frozenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FreezePlugin успешно запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("freeze")) {
            if (!sender.hasPermission("freeze.use")) {
                sender.sendMessage("§cУ вас нет прав на использование этой команды!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /freeze <ник> <время_в_секундах>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cИгрок " + args[0] + " не найден в сети!");
                return true;
            }

            int time;
            try {
                time = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cВремя должно быть числом (в секундах)!");
                return true;
            }

            freezePlayer(target, time);
            sender.sendMessage("§aИгрок " + target.getName() + " заморожен на " + time + " секунд.");
            return true;
        }
        return false;
    }

    private void freezePlayer(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        Location freezeLocation = player.getLocation().clone(); // Запоминаем точку

        if (frozenPlayers.contains(uuid)) {
            player.sendMessage("§cВы уже заморожены! Время обновлено.");
        } else {
            frozenPlayers.add(uuid);
            player.sendMessage("§cВы были заморожены администратором! Не двигайтесь.");
        }

        // Телепортируем обратно на место каждые полсекунды (защита от падения/лаг-машин)
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (player.isOnline() && frozenPlayers.contains(uuid)) {
                player.teleport(freezeLocation);
            }
        }, 0L, 10L);

        // Разморозка через заданное время
        Bukkit.getScheduler().runTaskLater(this, () -> {
            frozenPlayers.remove(uuid);
            Bukkit.getScheduler().cancelTask(taskId); // Останавливаем телепортацию
            if (player.isOnline()) {
                player.sendMessage("§aВы были разморожены!");
            }
        }, seconds * 20L); // 20 тиков = 1 секунда
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            // Если игрок пытается изменить координаты X, Y или Z - отменяем движение
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
        }
    }
}
