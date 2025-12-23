package com.example.tntwars;

import com.example.tntwars.command.StartCommand;
import com.example.tntwars.command.TNTWarsCommand;
import com.example.tntwars.game.GameManager;
import com.example.tntwars.storage.MapStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TNTWarsPlugin extends JavaPlugin implements Listener {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        MapStorage storage = new MapStorage(getDataFolder(), getLogger());
        this.gameManager = new GameManager(this, storage);

        getServer().getPluginManager().registerEvents(this, this);
        TNTWarsCommand tntWarsCommand = new TNTWarsCommand(gameManager);
        getCommand("tntwars").setExecutor(tntWarsCommand);
        getCommand("tntwars").setTabCompleter(tntWarsCommand);
        getCommand("start").setExecutor(new StartCommand(gameManager));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        gameManager.handleBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        gameManager.handleExplosion(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        gameManager.handlePlayerDeath(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        gameManager.handlePlayerRespawn(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event);
    }
}
