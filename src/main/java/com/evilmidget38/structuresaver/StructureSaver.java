package com.evilmidget38.structuresaver;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_6_R3.IChunkProvider;
import net.minecraft.server.v1_6_R3.RegionFile;

public class StructureSaver extends JavaPlugin {
    private static final String[] regionLocations = new String[] {"region", "DIM-1/region", "DIM1/region"}; 
    
    public void onEnable() {
    }
    
    public void saveAllStructures() {
        getLogger().info("Saving all structures");
        for (World world : Bukkit.getWorlds()) {
            saveStructures(world);
        }
        getLogger().info("Done saving structures!");
    }

    public void saveStructures(World world) {
        getLogger().info("Generating structures for "+world.getName());
        long start = System.currentTimeMillis();
        File regionDir = getRegionsLocation(world);
        if (regionDir == null) {
            getLogger().severe("Unable to locate the region files for: "+world.getName());
            return;
        }
        // Extract the x and z coordinates from the region files.
        Pattern coordinatePattern = Pattern.compile("r\\.(.+)\\.(.+)\\.mca");
        for (File file : regionDir.listFiles()) {
            Matcher matcher = coordinatePattern.matcher(file.getName());
            int regionX = 0;
            int regionZ = 0;
            if (matcher.find()) {
                regionX = Integer.parseInt(matcher.group(1));
                regionZ = Integer.parseInt(matcher.group(2));
            } else {
                getLogger().severe("Unable to handle region: "+file.getName());
                continue;
            }
            getLogger().info("Creating structures for region: "+regionX+", "+regionZ);
            regionX = regionX << 5;
            regionZ = regionZ << 5;
            RegionFile region = new RegionFile(file);
            IChunkProvider chunkProvider = ((CraftWorld) world).getHandle().chunkProvider;
            // Iterate over all potential chunks in the region.
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                    // Check if the chunk exists.
                    if (region.c(chunkX, chunkZ)) {
                        // Create structures for that chunk!
                        chunkProvider.recreateStructures(regionX+chunkX, regionZ+chunkZ);
                    }
                }
            }
        }

        getLogger().info("Done generating structures for " + world.getName() + ".  Took "+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
        start = System.currentTimeMillis();
        getLogger().info("Saving structures for "+world.getName());
        ((CraftWorld) world).getHandle().worldMaps.a();
        getLogger().info("Done saving structures for "+world.getName() + ".  Took "+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
    }

    private File getRegionsLocation(World world) {
        for (String location : regionLocations) {
            File file = new File(((CraftWorld) world).getWorldFolder(), location);
            if (!file.isDirectory()) {
                continue;
            }
            return file;
        }
        return null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW+"Generating structures for all worlds.  See your console for details.");
            saveAllStructures();
            return true;
        } else if (args.length == 1) {
            String worldName = args[0];
            World world = getServer().getWorld(worldName);
            if (world == null) {
                sender.sendMessage(ChatColor.RED+"Unable to find world \'"+worldName+"\'.");
                return false;
            }
            sender.sendMessage(ChatColor.YELLOW+"Generating structures for "+worldName+".  See your console for details.");
            saveStructures(world);
            return true;
        } else {
            return false;
        }
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        // temporary debug code.
        if (e.isNewChunk()) {
            e.getChunk().unload(false, false);
        }
    }
}
