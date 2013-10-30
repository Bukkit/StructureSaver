package com.evilmidget38.structuresaver;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R3.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_6_R3.ChunkProviderServer;
import net.minecraft.server.v1_6_R3.IChunkProvider;
import net.minecraft.server.v1_6_R3.RegionFile;
import org.apache.commons.lang.StringUtils;

public class StructureSaver extends JavaPlugin {
    private static final String[] regionLocations = new String[] {"region", "DIM-1/region", "DIM1/region"};
    private static final String[] STRUCTURE_FILES = new String[] {"Mineshaft.dat", "Fortress.dat", "Stronghold.dat", "Temple.dat", "Village.dat"};

    private void saveAllStructures(boolean force) {
        getLogger().info("Saving all structures...");
        for (World world : Bukkit.getWorlds()) {
            saveStructures(world, force);
        }
        getLogger().info("Done saving all structures!");
    }

    private void saveStructures(World world, boolean force) {
        getLogger().info("Generating structures for '"+world.getName()+"'...");
        long start = System.currentTimeMillis();
        File regionDir = getRegionsLocation(world);
        if (regionDir == null) {
            getLogger().severe("Unable to locate the region files for: "+world.getName());
            return;
        }

        if (force) {
            removeExistingStructures(world);
        }

        // Extract the x and z coordinates from the region files.
        Pattern coordinatePattern = Pattern.compile("r\\.(.+)\\.(.+)\\.mca");
        File[] files = regionDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mca");
            }
        });

        for (File file : files) {
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
            regionX = regionX << 5;
            regionZ = regionZ << 5;
            RegionFile region = new RegionFile(file);
            IChunkProvider chunkProvider = ((ChunkProviderServer) ((CraftWorld) world).getHandle().chunkProvider).chunkProvider;
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
            // Close the region file.
            try {
                region.c();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Done generating structures for '" + world.getName() + "'. Took "+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
        start = System.currentTimeMillis();
        getLogger().info("Saving structures for '"+world.getName()+"'");
        ((CraftWorld) world).getHandle().worldMaps.a();
        getLogger().info("Done saving structures for '"+world.getName() + "'. Took "+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean force = false;

        for (int pos = args.length - 1; pos >= 0; pos--) {
            if (args[pos].equalsIgnoreCase("force")) {
                force = true;
                args = Arrays.copyOf(args, pos);
                break;
            }
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW+"Saving structures for all worlds. See your console for details.");
            if (force) {
                sender.sendMessage(ChatColor.YELLOW + "Forcing removal of existing structure data for all worlds.");
            }
            saveAllStructures(force);
            sender.sendMessage(ChatColor.YELLOW+"Done saving structures for all worlds.");
            return true;
        } else if (args.length == 1) {
            String worldName = args[0];
            World world = getServer().getWorld(worldName);
            if (world == null) {
                sender.sendMessage(ChatColor.RED+"Unable to find world '"+worldName+"'.");
                return false;
            }
            sender.sendMessage(ChatColor.YELLOW+"Saving structures for '"+worldName+"'. See your console for details.");
            if (force) {
                sender.sendMessage(ChatColor.YELLOW + "Forcing removal of existing structure data for '" + worldName + "'");
            }
            saveStructures(world, force);
            sender.sendMessage(ChatColor.YELLOW+"Done saving structures for '"+worldName+"'.");
            return true;
        } else {
            return false;
        }
    }

    private void removeExistingStructures(World world) {
        File baseFile = new File(((CraftWorld) world).getWorldFolder(), "data");
        if (!baseFile.isDirectory()) {
            return;
        }

        for (String structure : STRUCTURE_FILES) {
            File structureFile = new File(baseFile, structure);
            if (structureFile.exists()) {
                structureFile.delete();
            }
        }

        getLogger().log(Level.INFO, "Deleted existing structure files for '{0}'", world.getName());
    }
}
