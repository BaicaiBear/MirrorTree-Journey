package top.bearcabbage.mirrortree.screen;

import com.fibermc.essentialcommands.ManagerLocator;
import com.fibermc.essentialcommands.types.WarpStorage;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

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
                setButton(i + 11, ItemBuilder.start(Items.LIGHT_BLUE_BANNER)
                        .name("聚落: " + warpName)
                        .button(event -> {
                            WarpStorage storage = ManagerLocator.getInstance().getWorldDataManager().getWarpStorage();
                            storage.warpToWarp(event.player, warpName);
                        }));
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
