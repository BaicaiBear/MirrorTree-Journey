package top.bearcabbage.mirrortree.dream;

import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import top.bearcabbage.lanterninstorm.LanternInStormAPI;
import top.bearcabbage.lanterninstorm.lantern.BeginningLanternEntity;
import top.bearcabbage.lanterninstorm.player.LiSPlayer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static net.minecraft.state.property.Properties.WATERLOGGED;
import static top.bearcabbage.mirrortree.MirrorTree.*;

public class MTDream {
    public static final int MAX_RANGE = 3000;
    private static final int DREAM_RANDOM_RANGE = 128;
    public static BlockPos pos;
    public static long lastTime = 0;

    private static final LinkedBlockingQueue<Runnable> dreamQueue = new LinkedBlockingQueue<>();
    private static final LinkedBlockingQueue<Runnable> redreamQueue = new LinkedBlockingQueue<>();
    private static final ExecutorService dreamExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService redreamExecutor = Executors.newSingleThreadExecutor();

    // 新增远境任务队列
    private static final LinkedBlockingQueue<Runnable> farlandQueue = new LinkedBlockingQueue<>();
    private static final ExecutorService farlandExecutor = Executors.newSingleThreadExecutor();


    public static void queueDreamingTask(ServerWorld world, ServerPlayerEntity player, Position warppos) {
        dreamQueue.add(() -> dreaming(world, player, warppos));
        processDreamQueue();
    }

    private static void processDreamQueue() {
        dreamExecutor.submit(() -> {
            try {
                while (!dreamQueue.isEmpty()) {
                    Runnable task = dreamQueue.take();
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void queueRedreamingTask(ServerWorld world, ServerPlayerEntity player) {
        redreamQueue.add(() -> redreaming(world, player));
        processRedreamQueue();
    }

    private static void processRedreamQueue() {
        redreamExecutor.submit(() -> {
            try {
                while (!redreamQueue.isEmpty()) {
                    Runnable task = redreamQueue.take();
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /** 进入远境任务 */
    public static void queueFarlandTask(ServerPlayerEntity player, ServerWorld farlandWorld, int radius) {
        farlandQueue.add(() -> {
            Random random = farlandWorld.getRandom();
            int maxAttempts = 3;
            int height = 200;
            boolean found = false;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                BlockPos blockPos = new BlockPos(x, height, z);
                if (!farlandWorld.getBlockState(blockPos).isAir()) continue;
                while (farlandWorld.getBlockState(blockPos).isAir() && blockPos.getY() > 0) {
                    blockPos = blockPos.down();
                }
                int y = blockPos.getY();
                BlockState blockState = farlandWorld.getBlockState(new BlockPos(x, y, z));
                boolean isSafe = !(blockState.isLiquid() || blockState.isIn(BlockTags.FIRE) ||
                        blockState.isIn(BlockTags.LEAVES) || (blockState.contains(WATERLOGGED) && blockState.get(WATERLOGGED)));

                if (isSafe) {
                    found = true;
                    player.getServer().execute(() -> {
                        player.wakeUp();
                        double[] pos = {player.getBlockPos().getX(), player.getBlockPos().getY()+1, player.getBlockPos().getZ()};
                        dreamingPos.put(player.getUuid(), pos);
                        player.teleport(farlandWorld, x + 0.5, y, z + 0.5, player.getYaw(), player.getPitch());
                        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("「远境」").formatted(Formatting.BOLD, Formatting.DARK_PURPLE)));
                        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("深层梦境：未至之境").formatted(Formatting.DARK_GRAY)));
                    });
                    break;
                }
            }

            if (!found) {
                player.getServer().execute(() ->
                        player.sendMessage(Text.literal("🌀你失眠了，辗转反侧……").formatted(Formatting.RED), false)
                );
            }
        });
        processFarlandQueue();
    }

    private static void processFarlandQueue() {
        farlandExecutor.submit(() -> {
            try {
                while (!farlandQueue.isEmpty()) {
                    farlandQueue.take().run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }


    // 正常关服时写文件储存数据
    public static final Map<UUID, double[]> dreamingPos = Maps.newHashMap();
    // 这个不写入
    public static final Map<UUID, Collection<StatusEffectInstance>> dreamingEffects = Maps.newHashMap();
    public static final Map<UUID, ArrayList<Float>> dreamingHealthAndHunger = Maps.newHashMap();

    public static void dreaming(ServerWorld world, ServerPlayerEntity player, Position warppos) {
        if(LanternInStormAPI.getRTPSpawn(player)==null) {
            if (warppos==null) {
                if (lastTime == 0 || System.currentTimeMillis() - lastTime > 3000) {
                    lastTime = System.currentTimeMillis();
                    pos = getRandomPos(0, 0, MAX_RANGE);
                } else {
                    lastTime = System.currentTimeMillis();
                    pos = getRandomPos(pos.getX(), pos.getZ(), DREAM_RANDOM_RANGE);
                }
                player.teleport(world, pos.toCenterPos().getX(), pos.toCenterPos().getY(), pos.toCenterPos().getZ(), 0, 0);
                LanternInStormAPI.setRTPSpawn(player,pos, false);
            } else {
                player.teleport(world, warppos.getX(), warppos.getY(), warppos.getZ(), 0, 0);
                LanternInStormAPI.setRTPSpawn(player, new BlockPos((int)warppos.getX(),(int)warppos.getY(),(int)warppos.getZ()), true);
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("大鹏的梦").withColor(0x525288)));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("这个「聚落」是你的起点").withColor(0xFFFFFF)));
            }
        } else {
            pos = LanternInStormAPI.getRTPSpawn(player);
            lastTime = System.currentTimeMillis();
            if (dreamingPos.containsKey(player.getUuid())) {
                ((ServerPlayerEntity) player).teleport(world.getServer().getOverworld(), dreamingPos.get(player.getUuid())[0]+0.5, dreamingPos.get(player.getUuid())[1]+0.5, dreamingPos.get(player.getUuid())[2]+0.5, 0, 0);
                dreamingPos.remove(player.getUuid());
            } else {
                ((ServerPlayerEntity) player).teleport(world.getServer().getOverworld(), pos.toCenterPos().getX(), pos.toCenterPos().getY(), pos.toCenterPos().getZ(), 0, 0);
            }
        }
        player.changeGameMode(GameMode.SURVIVAL);
        if (dreamingEffects.containsKey(player.getUuid())) {
            for (StatusEffectInstance effect : dreamingEffects.get(player.getUuid())) {
                player.addStatusEffect(effect);
            }
            dreamingEffects.remove(player.getUuid());
        }
        if (dreamingHealthAndHunger.containsKey(player.getUuid())) {
            ArrayList<Float> healthAndHunger = dreamingHealthAndHunger.get(player.getUuid());
            player.setHealth(healthAndHunger.get(0));
            player.getHungerManager().setFoodLevel((int) Math.floor(healthAndHunger.get(1)));
            player.getHungerManager().setSaturationLevel(healthAndHunger.get(2));
            player.getHungerManager().setExhaustion(healthAndHunger.get(3));
            dreamingHealthAndHunger.remove(player.getUuid());
        }
    }

    public static void redreaming(ServerWorld world, ServerPlayerEntity player) {
        ((LiSPlayer)player).getLS().setRtpSpawn(null);
        List<BeginningLanternEntity> entities = (List<BeginningLanternEntity>) player.getServerWorld().getEntitiesByType(BeginningLanternEntity.BEGINNING_LANTERN, (entity)-> entity.getCustomName().getString().contains("入梦点["+player.getName().getString()+"]"));
        if (!entities.isEmpty()) entities.forEach(Entity::discard);
        dreamingPos.remove(player.getUuid());
        dreamingEffects.remove(player.getUuid());
        dreamingHealthAndHunger.remove(player.getUuid());
        player.teleport(world.getServer().getWorld(bedroom), bedroomX_init, bedroomY_init, bedroomZ_init, 90,0);
        player.clearStatusEffects();
        player.changeGameMode(GameMode.ADVENTURE);
        player.setSpawnPoint(bedroom, new BlockPos(bedroomX_init, bedroomY_init, bedroomZ_init), 90, true, false);
    }

    private static BlockPos getRandomPos(int xx, int zz, int range) {
        return (xx==0 && zz==0) ? MTDreamingPoint.getRandomPos() : MTDreamingPoint.getNearbyRandomPos(xx, zz, range);
    }
}