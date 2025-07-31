package top.bearcabbage.mirrortree.screen;

import com.fibermc.essentialcommands.ManagerLocator;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.mirrortree.dream.MTDream;

import java.util.List;

public class WarpTeleportScreen extends WarpSelectionScreen {
    @Override
    protected void addButtons(ServerPlayerEntity serverPlayerEntity) {
        setButton(26, ItemBuilder.start(Items.COAL).name("返回上一级").button(event -> event.player.openHandledScreen(new SelectionDreamScreen())));
        List<String> warps = ManagerLocator.getInstance().getWorldDataManager().getWarpNames();
        if (warps.isEmpty()) {
            setButton(11, ItemBuilder.start(Items.BARRIER).name("没有可用的聚落").button());
        } else {
            for (int i = 0; i < warps.size(); i++) {
                if (ManagerLocator.getInstance().getWorldDataManager().getWarp(warps.get(i)).dim()!=serverPlayerEntity.getServer().getOverworld().getRegistryKey()) continue;
                String warpName = warps.get(i);
                if (!warpName.startsWith("聚落：")) continue;
                setButton(i + 11, ItemBuilder.start(BannerUtils.getRandomBanner())
                        .name(warpName)
                        .tooltip("["+ (int)ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getX() + ", " +
                                (int)ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getY() + ", " +
                                (int)ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getZ() + "]")
                        .button(event -> {
                            event.player.teleport(
                                    event.player.getServer().getOverworld(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getX(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getY(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos().getZ(),
                                    event.player.getYaw(),
                                    event.player.getPitch()
                            );
                        }));
            }
        }
    }

    @Override
    public String getName() {
        return "点击聚落进行传送";
    }
}
