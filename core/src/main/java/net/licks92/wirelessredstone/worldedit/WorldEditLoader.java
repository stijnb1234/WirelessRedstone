package net.licks92.wirelessredstone.worldedit;

import net.licks92.wirelessredstone.compat.InternalProvider;
import net.licks92.wirelessredstone.WirelessRedstone;
import org.bukkit.Bukkit;

import java.util.Objects;

public class WorldEditLoader {
    public WorldEditLoader() {
        try {
            WorldEditVersion version;
            try {
                Class.forName("com.sk89q.worldedit.math.BlockVector3");
                version = WorldEditVersion.v7;
            } catch (ClassNotFoundException ex) {
                version = WorldEditVersion.v6;
            }

            InternalProvider.getCompatWorldEditHooker(version).register();
            WirelessRedstone.getWRLogger().debug("Hooked into WorldEdit");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public enum WorldEditVersion {
        v6, v7
    }
}