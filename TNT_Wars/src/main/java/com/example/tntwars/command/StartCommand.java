package com.example.tntwars.command;

import com.example.tntwars.game.GameManager;
import com.example.tntwars.util.FormatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    private final GameManager gameManager;

    public StartCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern benutzt werden.");
            return true;
        }
        if (!gameManager.startGame(player)) {
            player.sendMessage(FormatUtil.error("Spiel konnte nicht gestartet werden."));
        }
        return true;
    }
}
