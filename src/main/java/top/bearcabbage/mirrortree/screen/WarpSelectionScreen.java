package top.bearcabbage.mirrortree.screen;

import com.fibermc.essentialcommands.ManagerLocator;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.mirrortree.dream.MTDream;

import java.util.List;
import java.util.Random;

public class WarpSelectionScreen extends AbstractACScreen {
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

    protected static class BannerUtils {

        private static final Item[] BANNERS = new Item[] {
                Items.WHITE_BANNER,
                Items.ORANGE_BANNER,
                Items.MAGENTA_BANNER,
                Items.LIGHT_BLUE_BANNER,
                Items.YELLOW_BANNER,
                Items.LIME_BANNER,
                Items.PINK_BANNER,
                Items.GRAY_BANNER,
                Items.LIGHT_GRAY_BANNER,
                Items.CYAN_BANNER,
                Items.PURPLE_BANNER,
                Items.BLUE_BANNER,
                Items.BROWN_BANNER,
                Items.GREEN_BANNER,
                Items.RED_BANNER,
                Items.BLACK_BANNER
        };

        private static final Random RANDOM = new Random();

        public static Item getRandomBanner() {
            return BANNERS[RANDOM.nextInt(BANNERS.length)];
        }
    }
}
