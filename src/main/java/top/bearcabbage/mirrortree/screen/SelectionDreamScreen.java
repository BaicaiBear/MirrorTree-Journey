package top.bearcabbage.mirrortree.screen;

import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.mirrortree.dream.MTDream;

public class SelectionDreamScreen extends AbstractACScreen {

    @Override
    protected void addButtons(ServerPlayerEntity serverPlayerEntity) {
        setButton(4, ItemBuilder.start(Items.BELL).name("一旦选择，就无法回头").button());
        setButton(11, ItemBuilder.start(Items.RED_STAINED_GLASS).name("在野外随机位置入梦").button(event -> MTDream.queueDreamingTask(event.player.getServer().getOverworld(), event.player, null)));
        setButton(15, ItemBuilder.start(Items.BLUE_STAINED_GLASS).name("在安全的聚落入梦").button(event -> event.player.openHandledScreen(new WarpSelectionScreen())));
    }

    @Override
    public String getName() {
        return "选择你的入梦方式";
    }

    @Override
    public int getPage() {
        return 1;
    }
}
