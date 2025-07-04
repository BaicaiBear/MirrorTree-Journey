package top.bearcabbage.mirrortree.screen;

import com.fibermc.essentialcommands.ManagerLocator;
import com.fibermc.essentialcommands.types.WarpStorage;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.mirrortree.MTDream;

import java.util.List;

public class WarpSelectionScreen extends AbstractACScreen {
    @Override
    protected void addButtons(ServerPlayerEntity serverPlayerEntity) {
        setButton(26, ItemBuilder.start(Items.COAL).button(event -> event.player.openHandledScreen(new SelectionDreamScreen())));
        List<String> warps = ManagerLocator.getInstance().getWorldDataManager().getWarpNames();
        if (warps.isEmpty()) {
            setButton(11, ItemBuilder.start(Items.BARRIER).name("没有可用的聚落").button());
        } else {
            for (int i = 0; i < warps.size(); i++) {
                if (ManagerLocator.getInstance().getWorldDataManager().getWarp(warps.get(i)).dim()!=serverPlayerEntity.getServer().getOverworld().getRegistryKey()) continue;
                String warpName = warps.get(i);
                if (!warpName.startsWith("聚落：")) continue;
                setButton(i + 11, ItemBuilder.start(Items.LIGHT_BLUE_BANNER)
                        .name(warpName)
                        .button(event -> MTDream.queueDreamingTask(event.player.getServer().getOverworld(), event.player, ManagerLocator.getInstance().getWorldDataManager().getWarp(warpName).pos())));
            }
        }
    }

    @Override
    public String getName() {
        return "请选择你要加入的聚落";
    }

    @Override
    public int getPage() {
        return 1;
    }
}
