package top.bearcabbage.mirrortree;

import com.fibermc.essentialcommands.ManagerLocator;
import com.fibermc.essentialcommands.types.MinecraftLocation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.text2speech.Narrator;
import eu.pb4.universalshops.registry.TradeShopBlock;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bearcabbage.lanterninstorm.LanternInStormAPI;
import top.bearcabbage.mirrortree.dream.MTDream;
import top.bearcabbage.mirrortree.dream.MTDreamingPoint;
import top.bearcabbage.mirrortree.screen.SelectionDreamScreen;
import top.bearcabbage.mirrortree.starryskies.TopTrapdoorDecorator;
import top.bearcabbage.mirrortree.starryskies.TrialDungeonSphere;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.state.property.Properties.WATERLOGGED;

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

	public static RegistryKey<World> brokenDream = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("starry_skies", "overworld"));
	public static int brokenDreamX;
	public static int brokenDreamY;
	public static int brokenDreamZ;

	public static RegistryKey<World> farland = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID, "farland"));

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
		brokenDreamX = config.getInt("brokenDreamX", 16);
		brokenDreamY = config.getInt("brokenDreamY", 90);
		brokenDreamZ = config.getInt("brokenDreamZ", 16);

		// 注册球球装饰器
		TopTrapdoorDecorator.init();
		TrialDungeonSphere.init();

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
			boolean inDreamWorld = entity.getWorld().getRegistryKey().getValue().getNamespace().equals("minecraft");
			String wakeTarget = inDreamWorld ? "卧室" : "梦境";
			String wakeMsg = inDreamWorld ? "出生点大厅「卧室」" : "主世界「梦境」";
			entity.sendMessage(Text.literal("\n"));
			entity.sendMessage(Text.literal("========= [你躺下了] =========").formatted(Formatting.BOLD).withColor(0xDDD605).styled((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("这里是梦境世界旅行导航，点击下面的文字可以到达对应的世界")))));
			entity.sendMessage(Text.literal("\n"));
			entity.sendMessage(Text.literal(" ⏰要试着醒来吗？").formatted(Formatting.BOLD).withColor(0xDDD605));
			entity.sendMessage(Text.literal(" ——————>「"+ wakeTarget +"」").formatted(Formatting.BOLD).formatted(Formatting.BLUE).styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wakeup")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击回到"+ wakeMsg)))));
			if (inDreamWorld) {
				entity.sendMessage(Text.literal("\n"));
				entity.sendMessage(Text.literal(" 🛏️要睡得更深吗？").formatted(Formatting.BOLD).withColor(0xDDD605));
				entity.sendMessage(Text.literal(" ——————>「碎梦」").formatted(Formatting.BOLD).formatted(Formatting.DARK_RED).styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dream-to-broken")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击进入球球世界「碎梦」")))));
				entity.sendMessage(Text.literal(" ——————>「远境」").formatted(Formatting.BOLD).formatted(Formatting.DARK_PURPLE).styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dream-to-farland")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击进入未至之境「远境」")))));
			}
			entity.sendMessage(Text.literal("\n"));
			entity.sendMessage(Text.literal(" 🌙或者，你只是想平静的度过一晚").formatted(Formatting.BOLD).withColor(0xDDD605));
			entity.sendMessage(Text.literal(" 那就静静等待吧……").formatted(Formatting.BOLD).withColor(0xDDD605));
			entity.sendMessage(Text.literal("\n"));
			entity.sendMessage(Text.literal("===== [棱镜树]梦境世界导航 =====").formatted(Formatting.BOLD).withColor(0xDDD605).styled((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("这里是梦境世界旅行导航，点击下面的文字可以到达对应的世界")))));
			entity.sendMessage(Text.literal("\n"));
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

		// 球球世界禁用鞘翅
		EntityElytraEvents.ALLOW.register((entity) -> {
			if (entity instanceof PlayerEntity player && player.getWorld().getRegistryKey().getValue().getNamespace().equals("starry_skies")) {
				if (player.isCreative() || player.isSpectator()) {
					return true;
				} else {
					if (player instanceof ServerPlayerEntity serverPlayerEntity) serverPlayerEntity.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.literal("「碎梦」中的你遗忘了如何飞行").formatted(Formatting.DARK_PURPLE)));
					return false;
				}
			}
			return true;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.getRegistryKey().equals(bedroom)) {
				if (!player.isCreative()) return ActionResult.FAIL;
			}
			// 防止球球维度地牢球壳被破坏
			if (world.getRegistryKey().getValue().getNamespace().equals("starry_skies") && world.getBlockState(pos).getBlock().equals(Blocks.REINFORCED_DEEPSLATE) && !player.isCreative()) return ActionResult.FAIL;
			return ActionResult.PASS;
		});

//		// 球球世界的鞘翅限速 在「秋水」更新中被ban鞘翅取代
//		ServerTickEvents.START_SERVER_TICK.register(
//				server -> {
//					for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
//						if (player.isFallFlying()) {
//							reduceElytraSpeed(player);
//						}}
//				}
//		);


//		if (CONFIG.generateCurrencyInChests()) LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
//			if (dungeonsLootTables.contains(key.getValue().getNamespace())) {
//				tableBuilder.pool(LootPool.builder().with(MoneyBagLootEntry.builder(CONFIG.lootOptions.dungeonMinLoot(), CONFIG.lootOptions.dungeonMaxLoot()))
//						.conditionally(RandomChanceLootCondition.builder(0.45f)));
//			} else if (structureLootTables.contains(key.getValue().getNamespace())) {
//				tableBuilder.pool(LootPool.builder().with(MoneyBagLootEntry.builder(CONFIG.lootOptions.structureMinLoot(), CONFIG.lootOptions.structureMaxLoot()))
//						.conditionally(RandomChanceLootCondition.builder(0.45f)));
//			}
//		});
//
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
						RegistryKey<World> worldKey = player.getWorld().getRegistryKey();
						if (worldKey.equals(bedroom)) return 0;
						player.wakeUp();
						// 从「梦境」醒来到「卧室」
						if (worldKey.getValue().getNamespace().equals("minecraft")) {
							double[] pos = {player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ()};
							MTDream.dreamingPos.put(player.getUuid(), pos);
							ServerWorld bedroom = player.getServer().getWorld(MirrorTree.bedroom);
							player.teleport(bedroom, bedroomX, bedroomY, bedroomZ, 0, 0);
							player.changeGameMode(GameMode.ADVENTURE);
							MTDream.dreamingEffects.put(player.getUuid(), player.getStatusEffects());
							ArrayList<Float> healthAndHunger = new ArrayList<>();
							healthAndHunger.add(player.getHealth());
							healthAndHunger.add((float) player.getHungerManager().getFoodLevel());
							healthAndHunger.add(player.getHungerManager().getSaturationLevel());
							healthAndHunger.add(player.getHungerManager().getExhaustion());
							MTDream.dreamingHealthAndHunger.put(player.getUuid(), healthAndHunger);
							player.clearStatusEffects();
							player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("「卧室」").formatted(Formatting.BOLD).formatted(Formatting.BLUE)));
							player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("你醒来了").formatted(Formatting.WHITE)));
						}
						// 从「碎梦」/「远境」醒来到「梦境」
						else if (worldKey.equals(brokenDream) || worldKey.equals(farland)) {
							ServerWorld dreamWorld = Objects.requireNonNull(player.getServer()).getOverworld();
							if(LanternInStormAPI.getRTPSpawn(player)!=null) {
								BlockPos pos = LanternInStormAPI.getRTPSpawn(player);
								if (MTDream.dreamingPos.containsKey(player.getUuid())) {
									player.teleport(dreamWorld, MTDream.dreamingPos.get(player.getUuid())[0] + 0.5, MTDream.dreamingPos.get(player.getUuid())[1] + 0.5, MTDream.dreamingPos.get(player.getUuid())[2] + 0.5, 0, 0);
									MTDream.dreamingPos.remove(player.getUuid());
								} else {
									player.teleport(dreamWorld, pos.toCenterPos().getX(), pos.toCenterPos().getY(), pos.toCenterPos().getZ(), 0, 0);
								}
								player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("「梦境」").formatted(Formatting.BOLD).formatted(Formatting.GOLD)));
								player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("这是熟悉的梦境").formatted(Formatting.GRAY)));
							}
						}
						return 0;
					})
			);

			// 从「梦境」进入「碎梦」（固定出生点）
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("dream-to-broken")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player==null) return 0;
						if (!player.isSleeping()) return 0;
						if (!player.getWorld().equals(Objects.requireNonNull(player.getServer()).getOverworld())) return 0;
						player.sendMessage(Text.literal("💤你感受到困倦，视野慢慢模糊……").formatted(Formatting.GRAY), false);
						player.wakeUp();
						double[] pos = {player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ()};
						MTDream.dreamingPos.put(player.getUuid(), pos);
						ServerWorld brokenDreamWorld = player.getServer().getWorld(brokenDream);
						if (brokenDreamWorld == null) return 0;
						player.teleport(brokenDreamWorld, brokenDreamX, brokenDreamY, brokenDreamZ, 0, 0);
						player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("「碎梦」").formatted(Formatting.BOLD).formatted(Formatting.DARK_RED)));
						player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("深层梦境：球球世界").formatted(Formatting.DARK_GRAY)));
						return 0;
					})
			);

			// 从「梦境」进入「远境」（10000格正方形内随机位置）
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("dream-to-farland")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;
						if (!player.isSleeping()) return 0;
						if (!player.getWorld().equals(Objects.requireNonNull(player.getServer()).getOverworld())) return 0;
						ServerWorld farlandWorld = player.getServer().getWorld(farland);
						if (farlandWorld == null) return 0;
						player.sendMessage(Text.literal("💤你感受到困倦，视野慢慢模糊……").formatted(Formatting.GRAY), false);
						MTDream.queueFarlandTask(player, farlandWorld, 10000);
						return 1;
					})
			);

			// 重新入梦
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

			// 设置新的聚落
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

			// 删除聚落
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