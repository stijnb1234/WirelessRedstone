package net.licks92.wirelessredstone.compat;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import net.licks92.wirelessredstone.WirelessRedstone;
import org.bukkit.Bukkit;

public class InternalWorldEditHooker_6 implements InternalWorldEditHooker {

    @Subscribe
    public void wrapForLogging(EditSessionEvent event) {
        Actor actor = event.getActor();
        World world = event.getWorld();
        if ((actor != null) && (event.getStage().equals(EditSession.Stage.BEFORE_CHANGE))) {
            event.setExtent(new InternalWorldEditLogger_6(actor, world, event.getExtent()));
        }
    }

    @Override
    public void register() {
        try {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WirelessRedstone.getInstance(), () -> {
                try {
                    InternalWorldEditHooker_6 worldEditHooker = new InternalWorldEditHooker_6();
                    WirelessRedstone.getInstance().setWorldEditHooker(worldEditHooker);
                    WorldEdit.getInstance().getEventBus().register(worldEditHooker);
                } catch (Exception e) {
                    WirelessRedstone.getWRLogger().severe("Error while hooking worldedit");
                }
            }, 0L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void unRegister() {
        try {
            try {
                WorldEdit.getInstance().getEventBus().unregister(WirelessRedstone.getInstance().getWorldEditHooker());
            } catch (Exception e) {
                WirelessRedstone.getWRLogger().severe("Error while unhooking worldedit");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
