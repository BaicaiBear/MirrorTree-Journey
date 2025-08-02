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
        setButton(26, ItemBuilder.start(Items.OAK_SIGN).name("设立新的聚落").tooltip("直接去联系小熊白菜就好了！（当然建议出生点附近先完成一些基础的建设，比如欢迎水池x）").button());
        List<String> warps = ManagerLocator.getInstance().getWorldDataManager().getWarpNames();
        if (warps.isEmpty()) {
            setButton(11, ItemBuilder.start(Items.BARRIER).name("没有可用的聚落").button());
        } else {
            int index = 0;
            for (String warp : warps) {
                if (ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).dim() != serverPlayerEntity.getServer().getOverworld().getRegistryKey())
                    continue;
                if (!warp.startsWith("聚落：")) continue;
                setButton(index + 11, ItemBuilder.start(BannerUtils.getRandomBanner())
                        .name(warp)
                        .tooltip("[" + (int) ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getX() + ", " +
                                (int) ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getY() + ", " +
                                (int) ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getZ() + "]")
                        .button(event -> {
                            event.player.teleport(
                                    event.player.getServer().getOverworld(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getX(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getY(),
                                    ManagerLocator.getInstance().getWorldDataManager().getWarp(warp).pos().getZ(),
                                    event.player.getYaw(),
                                    event.player.getPitch()
                            );
                        }));
                index++;
            }
        }
    }

    @Override
    public String getName() {
        return "点击聚落进行传送";
    }
}
