package top.bearcabbage.mirrortree;

import com.glisco.numismaticoverhaul.currency.MoneyBagLootEntry;
import eu.pb4.universalshops.registry.TradeShopBlock;
import me.alpestrine.c.reward.screen.screens.SelectionScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bearcabbage.lanterninstorm.LanternInStormAPI;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.util.*;
import java.util.concurrent.*;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.CONFIG;

public class MirrorTree implements ModInitializer {
	public static final String MOD_ID = "mirrortree";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final MTConfig config = new MTConfig(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("config.json"));
	public static final MTConfig dream = new MTConfig(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("dream_data.json"));
	public static RegistryKey<World> bedroom = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID, "bedroom"));
	public static int bedroomX;
	public static int bedroomY;
	public static int bedroomZ;
	public static int bedroomX_init;
	public static int bedroomY_init;
	public static int bedroomZ_init;

	public static final Map<ServerPlayerEntity, Integer> fresh_player = new ConcurrentHashMap<>();
	public static final Item FOX_TAIL_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "fox_tail_item"), new Item(new Item.Settings().maxCount(64)));

	private static final Set<String> dungeonsLootTables = Set.of("dungeons_arise", "dungeons_arise_seven_seas");
	private static final Set<String> structureLootTables = Set.of("betteroceanmonuments", "betterwitchhuts");

	// 活动物品
	public static final Item MEAT_ZONGZI = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "meat_zongzi"), new Item(new Item.Settings().food((new FoodComponent.Builder()).nutrition(10).saturationModifier(0.3F).build())));
	public static final Item EGG_ZONGZI = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "egg_zongzi"), new Item(new Item.Settings().food((new FoodComponent.Builder()).nutrition(10).saturationModifier(0.3F).build())));
	public static final Item BERRY_ZONGZI = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "berry_zongzi"), new Item(new Item.Settings().food((new FoodComponent.Builder()).nutrition(10).saturationModifier(0.3F).build())));

	// 奖券
	public static final Item TICKET_1 = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ticket_1"), new Item(new Item.Settings().maxCount(64)));
	public static final Item TICKET_2 = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ticket_2"), new Item(new Item.Settings().maxCount(64)));
	public static final Item TICKET_3 = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ticket_3"), new Item(new Item.Settings().maxCount(64)));
	public static final Item TICKET_4 = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ticket_4"), new Item(new Item.Settings().maxCount(64)));
	public static final Item TICKET_5 = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ticket_5"), new Item(new Item.Settings().maxCount(64)));

	@Override
	public void onInitialize() {


		bedroomX = config.getInt("bedroomX", 0);
		bedroomY = config.getInt("bedroomY", 80);
		bedroomZ = config.getInt("bedroomZ", 0);
		bedroomX_init = config.getInt("bedroomX_init", 0);
		bedroomY_init = config.getInt("bedroomY_init", 100);
		bedroomZ_init = config.getInt("bedroomZ_init", 0);


		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.getRegistryKey().getValue().equals(Identifier.of(MOD_ID,"bedroom"))) {
				if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock) {
					if (world.isClient) return ActionResult.SUCCESS;
				}
			}
            return ActionResult.PASS;
        });

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.getRegistryKey().getValue().equals(Identifier.of(MOD_ID,"bedroom"))) {
				if (entity instanceof VillagerEntity) return ActionResult.PASS;
				if (!player.isCreative()) return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.getRegistryKey().getValue().equals(Identifier.of(MOD_ID,"bedroom"))) {
				if (!player.isCreative()) return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		if(FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT)) ClientLifecycleEvents.CLIENT_STARTED.register(MTClient::onStarted);
	}
}