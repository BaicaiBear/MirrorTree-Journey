package top.bearcabbage.mirrortree;

import com.fibermc.essentialcommands.ManagerLocator;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.glisco.numismaticoverhaul.currency.MoneyBagLootEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.text2speech.Narrator;
import eu.pb4.universalshops.registry.TradeShopBlock;
import me.alpestrine.c.reward.screen.screens.SelectionScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
import top.bearcabbage.mirrortree.dream.MTDream;
import top.bearcabbage.mirrortree.dream.MTDreamingPoint;
import top.bearcabbage.mirrortree.screen.SelectionDreamScreen;
import top.bearcabbage.mirrortree.screen.WarpSelectionScreen;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.CONFIG;
import static net.minecraft.server.command.CommandManager.argument;

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

	@Override
	public void onInitialize() {


		bedroomX = config.getInt("bedroomX", 0);
		bedroomY = config.getInt("bedroomY", 80);
		bedroomZ = config.getInt("bedroomZ", 0);
		bedroomX_init = config.getInt("bedroomX_init", 0);
		bedroomY_init = config.getInt("bedroomY_init", 100);
		bedroomZ_init = config.getInt("bedroomZ_init", 0);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)->MTCommand.registerCommands(dispatcher)); // 调用静态方法注册命令

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LanternInStormAPI.overrideRTPSpawnSetting();
			LanternInStormAPI.addSafeWorld(bedroom);
			MTDream.dreamingPos.clear();
            try {
				dream.getAll(Map.class).forEach((uuid, pos) -> {
					ArrayList<Double> posList = (ArrayList<Double>) pos;
					double[] posArray = new double[3];
					for (int i = 0; i < 3; i++) {
						posArray[i] = posList.get(i);
					}
					MTDream.dreamingPos.put(UUID.fromString((String) uuid), posArray);
				});
            } catch (Exception e) {
				LOGGER.error(e.getMessage());
            }
			CompletableFuture.runAsync(() -> MTDreamingPoint.init(server.getOverworld()));
        });

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			MTDream.dreamingPos.forEach((uuid, pos) -> dream.set(String.valueOf(uuid), pos));
			dream.save();
		});

		EntitySleepEvents.START_SLEEPING.register((entity, sleepingLocation) -> {
			entity.sendMessage(Texts.bracketed(Text.literal("=======[醒来走走]=======").formatted(Formatting.BOLD).styled((style) -> style.withColor(Formatting.LIGHT_PURPLE).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wakeup")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击回到卧室（出生点大厅）维度"))))));
		});


		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.getRegistryKey().getValue().equals(Identifier.of(MOD_ID,"bedroom"))) {
				if (!world.isClient && !player.isCreative() && !(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof DoorBlock || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof TradeShopBlock || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof GrassBlock || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LecternBlock || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof StairsBlock)) return ActionResult.FAIL;
				if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock) {
					if (world.isClient) return ActionResult.SUCCESS;
					if (LanternInStormAPI.getRTPSpawn((ServerPlayerEntity) player)!=null) MTDream.queueDreamingTask(player.getServer().getOverworld(), (ServerPlayerEntity) player, null);
					else ((ServerPlayerEntity) player).openHandledScreen(new SelectionDreamScreen());
					return ActionResult.SUCCESS;
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

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS)) == 0
					&& player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) == 0) {
				player.setYaw(90);
				player.setSpawnPoint(bedroom, new BlockPos(bedroomX_init, bedroomY_init, bedroomZ_init), 90, true, false);
				player.changeGameMode(GameMode.ADVENTURE);
				player.sendMessage(Text.literal("欢迎来到棱镜树·逍遥周目！\n").formatted(Formatting.DARK_RED).formatted(Formatting.BOLD).formatted(Formatting.ITALIC)); //游戏画面将不定间隔截图并上传，截图可能用于反作弊、宣传、运营组找乐子等用途。继续游戏即视为同意；若不同意，请立即退出游戏。
				player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("你来到了狐狸的生前住所").formatted(Formatting.BOLD)));
				player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("原来这里就是梦境的入口…").formatted(Formatting.GRAY).formatted(Formatting.ITALIC)));
				fresh_player.put(player, 0);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			if (((PlayerAuth) player).easyAuth$isAuthenticated() && !((PlayerAuth) player).easyAuth$canSkipAuth()) {
				((PlayerAuth) player).easyAuth$setAuthenticated(false);
				((PlayerAuth) player).easyAuth$saveLastLocation(true);
			}
		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getRegistryKey().equals(bedroom)) {
				Iterator<Map.Entry<ServerPlayerEntity, Integer>> iterator = fresh_player.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<ServerPlayerEntity, Integer> entry = iterator.next();
					ServerPlayerEntity player = entry.getKey();
					int time = entry.getValue();
					if (time < 100) {
						fresh_player.put(player, time + 1);
					} else {
						player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("现在，进到房子里去吧…").formatted(Formatting.BOLD).formatted(Formatting.BLUE)));
						player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("去床上睡一觉").formatted(Formatting.GRAY).formatted(Formatting.ITALIC)));
						iterator.remove(); // 使用 Iterator 进行删除操作
					}
				}
			}
			if (world.getTime() % 20 == 0) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					if (!player.isCreative() && !player.isSpectator() && player.getServerWorld().getRegistryKey().equals(bedroom)) player.changeGameMode(GameMode.ADVENTURE);
				}
			}
		});

		// 球球世界的鞘翅限速
		ServerTickEvents.START_SERVER_TICK.register(
				server -> {
					for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
						if (player.isFallFlying()) {
							reduceElytraSpeed(player);
						}}
				}
		);

		// 防止球球维度传送门被破坏
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.getRegistryKey().getValue().equals(Identifier.of(MOD_ID,"bedroom"))) {
				if (!player.isCreative()) return ActionResult.FAIL;
			}
			if (world.getBlockState(pos).getBlock().equals(Blocks.REINFORCED_DEEPSLATE) && !player.hasPermissionLevel(2)) return ActionResult.FAIL;
			return ActionResult.PASS;
		});

		if (CONFIG.generateCurrencyInChests()) LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (dungeonsLootTables.contains(key.getValue().getNamespace())) {
				tableBuilder.pool(LootPool.builder().with(MoneyBagLootEntry.builder(CONFIG.lootOptions.dungeonMinLoot(), CONFIG.lootOptions.dungeonMaxLoot()))
						.conditionally(RandomChanceLootCondition.builder(0.45f)));
			} else if (structureLootTables.contains(key.getValue().getNamespace())) {
				tableBuilder.pool(LootPool.builder().with(MoneyBagLootEntry.builder(CONFIG.lootOptions.structureMinLoot(), CONFIG.lootOptions.structureMaxLoot()))
						.conditionally(RandomChanceLootCondition.builder(0.45f)));
			}
		});

	}

	private void reduceElytraSpeed(PlayerEntity player)
	{
		if (!player.getWorld().getRegistryKey().getValue().getNamespace().equals("starry_skies")) return;
		double elytra_speed_multiplier = 0.6;
		Vec3d velocity = player.getVelocity();
		double velocity_length = velocity.length();

		float max_horizontal_speed = 1.66f;
		double smoothing_factor = 0.3;
		double dynamic_threshold = elytra_speed_multiplier * ((1-smoothing_factor) * max_horizontal_speed);

		boolean angle_check = playerFlyingHorizontal(velocity);

		if ((velocity_length > dynamic_threshold) && angle_check)
		{
			Vec3d target_velocity = velocity.normalize().multiply(elytra_speed_multiplier * velocity_length);
			Vec3d velocity_diff = velocity.subtract(target_velocity);

			Vec3d new_velocity = velocity.subtract(velocity_diff.multiply(smoothing_factor));

			player.setVelocity(new_velocity);
			player.velocityModified = true;
			player.sendAbilitiesUpdate();
		}
	}

	private static boolean playerFlyingHorizontal(Vec3d velocity)
	{
		double max_elevation_angle = Math.toRadians(50);
		double min_elevation_angle = -Math.toRadians(50);
		double vertical_velocity = velocity.y;
		double horizontal_speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double elevation_angle = Math.atan2(vertical_velocity, horizontal_speed);
		return (elevation_angle >= min_elevation_angle && elevation_angle <= max_elevation_angle);
	}

	public static class MTConfig {
		private final Path filePath;
		private JsonObject jsonObject;
		private final Gson gson;

		public MTConfig(Path filePath) {
			this.filePath = filePath;
			this.gson = new GsonBuilder().setPrettyPrinting().create();
			try {
				if (Files.notExists(filePath.getParent())) {
					Files.createDirectories(filePath.getParent());
				}
				if (Files.notExists(filePath)) {
					Files.createFile(filePath);
					try (FileWriter writer = new FileWriter(filePath.toFile())) {
						writer.write("{}");
					}
				}

			} catch (IOException e) {
				Narrator.LOGGER.error(e.toString());
			}
			load();
		}

		public void load() {
			try (FileReader reader = new FileReader(filePath.toFile())) {
				this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (IOException e) {
				this.jsonObject = new JsonObject();
			}
		}

		public void save() {
			try (FileWriter writer = new FileWriter(filePath.toFile())) {
				gson.toJson(jsonObject, writer);
			} catch (IOException e) {
				Narrator.LOGGER.error(e.toString());
			}
		}

		public void set(String key, Object value) {
			jsonObject.add(key, gson.toJsonTree(value));
		}

		public <T> T get(String key, Class<T> clazz) {
			return gson.fromJson(jsonObject.get(key), clazz);
		}

		public <T> T getOrDefault(String key, T defaultValue) {
			if (jsonObject.has(key)) {
				return gson.fromJson(jsonObject.get(key), (Class<T>) defaultValue.getClass());
			}
			else {
				set(key, defaultValue);
				save();
				return defaultValue;
			}
		}

		public int getInt(String key, int defaultValue) {
			if (jsonObject.has(key)) {
				return jsonObject.get(key).getAsInt();
			}
			else {
				set(key, defaultValue);
				save();
				return defaultValue;
			}
		}

		public <T> T getAll(Class<T> clazz) {
			return gson.fromJson(jsonObject, clazz);
		}

	}

	public static class MTCommand {

		public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("wakeup")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player==null) return 0;
						if (!player.isSleeping()) return 0;
						player.wakeUp();
						double[] pos = {player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ()};
						MTDream.dreamingPos.put(player.getUuid(), pos);
						ServerWorld bedroom = player.getServer().getWorld(MirrorTree.bedroom);
						player.teleport(bedroom, bedroomX, bedroomY, bedroomZ, 0,0);
						player.changeGameMode(GameMode.ADVENTURE);
						MTDream.dreamingEffects.put(player.getUuid(), player.getStatusEffects());
						ArrayList<Float> healthAndHunger = new ArrayList<>();
						healthAndHunger.add(player.getHealth());
						healthAndHunger.add((float) player.getHungerManager().getFoodLevel());
						healthAndHunger.add(player.getHungerManager().getSaturationLevel());
						healthAndHunger.add(player.getHungerManager().getExhaustion());
						MTDream.dreamingHealthAndHunger.put(player.getUuid(), healthAndHunger);
						player.clearStatusEffects();
						player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("你醒来了").formatted(Formatting.BOLD).formatted(Formatting.BLUE)));
						return 0;
					})
			);
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("redream")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player==null) return 0;
						try {
							MTDream.queueRedreamingTask(player.getServer().getOverworld(), player);
						} catch (Exception e) {
							LOGGER.error(e.getMessage());
						}
						return 0;
					})
			);
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("mtwarp")
					.then(argument("name", StringArgumentType.string())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayer();
							LanternInStormAPI.setWarpSpawn(
									player.getServer().getOverworld(),
									player.getPos(),
									context.getArgument("name", String.class)
							);
							ManagerLocator.getInstance().getWorldDataManager().setWarp("聚落："+context.getArgument("name", String.class), new MinecraftLocation(player.getServer().getOverworld().getRegistryKey(), player.getPos().getX(), player.getPos().getY(), player.getPos().getZ()),false);
							return 0;
						})
					)
			);

			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("mtwarp-delete")
					.then(argument("name", StringArgumentType.string())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayer();
							ManagerLocator.getInstance().getWorldDataManager().delWarp("聚落："+context.getArgument("name", String.class));
							player.sendMessage(Text.literal("请小心地手动kill掉灯笼实体。"));
							return 0;
						})
					)
			);


		}
	}
}