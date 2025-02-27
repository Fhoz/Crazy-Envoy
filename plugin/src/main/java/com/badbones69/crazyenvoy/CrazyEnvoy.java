package com.badbones69.crazyenvoy;

import com.badbones69.crazyenvoy.api.CrazyManager;
import com.badbones69.crazyenvoy.api.FileManager;
import com.badbones69.crazyenvoy.api.enums.Messages;
import com.badbones69.crazyenvoy.api.events.EnvoyEndEvent;
import com.badbones69.crazyenvoy.api.events.EnvoyEndEvent.EnvoyEndReason;
import com.badbones69.crazyenvoy.commands.EnvoyCommand;
import com.badbones69.crazyenvoy.commands.EnvoyTab;
import com.badbones69.crazyenvoy.controllers.*;
import com.badbones69.crazyenvoy.multisupport.MVdWPlaceholderAPISupport;
import com.badbones69.crazyenvoy.multisupport.PlaceholderAPISupport;
import com.badbones69.crazyenvoy.multisupport.Support;
import com.badbones69.crazyenvoy.multisupport.Version;
import com.badbones69.crazyenvoy.multisupport.holograms.HolographicSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CrazyEnvoy extends JavaPlugin implements Listener {
    
    private FileManager fileManager = FileManager.getInstance();
    private CrazyManager envoy = CrazyManager.getInstance();
    
    @Override
    public void onEnable() {
        
        String homeFolder = Version.isNewer(Version.v1_12_R1) ? "/tiers1.13-Up" : "/tiers1.12.2-Down";
        fileManager.logInfo(true)
        .registerCustomFilesFolder("/tiers")
        .registerDefaultGenerateFiles("Basic.yml", "/tiers", homeFolder)
        .registerDefaultGenerateFiles("Lucky.yml", "/tiers", homeFolder)
        .registerDefaultGenerateFiles("Titan.yml", "/tiers", homeFolder)
        .setup();
        
        Messages.addMissingMessages();
        
        envoy.load();
        Methods.hasUpdate();
        
        new Metrics();
        
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new EditControl(), this);
        pm.registerEvents(new EnvoyControl(), this);
        pm.registerEvents(new FlareControl(), this);
        
        try {
            if (Version.isNewer(Version.v1_10_R1)) {
                pm.registerEvents(new FireworkDamageAPI(), this);
            }
        } catch (Exception ignored) {
        }
        
        if (Support.HOLOGRAPHIC_DISPLAYS.isPluginLoaded()) HolographicSupport.registerPlaceHolders();
        if (Support.PLACEHOLDER_API.isPluginLoaded()) new PlaceholderAPISupport().register();
        if (Support.MVDW_PLACEHOLDER_API.isPluginLoaded()) MVdWPlaceholderAPISupport.registerPlaceholders(this);
        
        getCommand("envoy").setExecutor(new EnvoyCommand());
        getCommand("envoy").setTabCompleter(new EnvoyTab());
    }
    
    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (EditControl.isEditor(player)) {
                EditControl.removeEditor(player);
                EditControl.removeFakeBlocks(player);
            }
        }
        if (Support.HOLOGRAPHIC_DISPLAYS.isPluginLoaded()) {
            HolographicSupport.unregisterPlaceHolders();
        }
        if (envoy.isEnvoyActive()) {
            EnvoyEndEvent event = new EnvoyEndEvent(EnvoyEndReason.SHUTDOWN);
            Bukkit.getPluginManager().callEvent(event);
            envoy.endEnvoyEvent();
        }
        envoy.unload();
    }
    
}
