package top.bearcabbage.mirrortree.starryskies;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import de.dafuqs.starryskies.*;
import de.dafuqs.starryskies.registries.StarryRegistries;
import de.dafuqs.starryskies.state_providers.*;
import de.dafuqs.starryskies.worldgen.*;
import de.dafuqs.starryskies.worldgen.spheres.DungeonSphere;
import de.dafuqs.starryskies.worldgen.spheres.TreasureChestEntry;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.floatprovider.*;
import net.minecraft.util.math.random.*;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.stateprovider.*;

import java.awt.*;
import java.util.List;
import java.util.*;

import static top.bearcabbage.mirrortree.MirrorTree.MOD_ID;

public class TrialDungeonSphere extends Sphere<TrialDungeonSphere.Config> {

    public static final Sphere<Config> TRIALDUNGEON = register("trialdungeon", new TrialDungeonSphere(Config.CODEC));

    private static <C extends SphereConfig, F extends Sphere<C>> F register(String name, F feature) {
        return Registry.register(StarryRegistries.SPHERE, Identifier.of(MOD_ID, name), feature);
    }

    public static void init() {
    }

    public TrialDungeonSphere(Codec<TrialDungeonSphere.Config> codec) {
        super(codec);
    }

    @Override
    public PlacedSphere<?> generate(ConfiguredSphere<? extends Sphere<Config>, Config> configuredSphere, Config config, ChunkRandom random, DynamicRegistryManager registryManager, BlockPos pos, float radius) {
        BlockStateProvider shellProvider = config.shellBlock.getForSphere(random, pos);

        return new TrialDungeonSphere.Placed(configuredSphere, radius, configuredSphere.getDecorators(random), configuredSphere.getSpawns(random), random,
                shellProvider,
                config.topBlock.isPresent() ? config.topBlock.get().getForSphere(random, pos) : shellProvider,
                config.bottomBlock.isPresent() ? config.bottomBlock.get().getForSphere(random, pos) : shellProvider,
                config.caveFloorBlock.isPresent() ? config.caveFloorBlock.get().getForSphere(random, pos) : shellProvider,
                config.shellThickness.get(random), config.treasureEntry, Registries.ENTITY_TYPE.get(config.entityType));
    }

    public static class Config extends SphereConfig {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                SphereConfig.CONFIG_CODEC.forGetter((config) -> config),
                SphereStateProvider.CODEC.fieldOf("shell_block").forGetter((config) -> config.shellBlock),
                SphereStateProvider.CODEC.optionalFieldOf("top_block").forGetter((config) -> config.topBlock),
                SphereStateProvider.CODEC.optionalFieldOf("bottom_block").forGetter((config) -> config.bottomBlock),
                SphereStateProvider.CODEC.optionalFieldOf("cave_floor_block").forGetter((config) -> config.bottomBlock),
                FloatProvider.createValidatedCodec(1.0F, 32.0F).fieldOf("shell_thickness").forGetter((config) -> config.shellThickness),
                TreasureChestEntry.CODEC.fieldOf("treasure_chest").forGetter((config) -> config.treasureEntry),
                RegistryKey.createCodec(RegistryKeys.ENTITY_TYPE).fieldOf("entity_type").forGetter((config) -> config.entityType)
        ).apply(instance, (sphereConfig, shellBlock, topBlock, bottomBlock, caveFloorBlock, shellRadius, treasureEntry, entityType) -> new TrialDungeonSphere.Config(sphereConfig.size, sphereConfig.decorators, sphereConfig.spawns, sphereConfig.generation, shellBlock, topBlock, bottomBlock, caveFloorBlock, shellRadius, treasureEntry, entityType)));

        private final SphereStateProvider shellBlock;
        private final Optional<SphereStateProvider> topBlock;
        private final Optional<SphereStateProvider> bottomBlock;
        private final Optional<SphereStateProvider> caveFloorBlock;
        private final FloatProvider shellThickness;
        private final TreasureChestEntry treasureEntry;
        private final RegistryKey<EntityType<?>> entityType;

        public Config(FloatProvider size, Map<RegistryEntry<ConfiguredSphereDecorator<?, ?>>, Float> decorators, List<SphereEntitySpawnDefinition> spawns, Optional<Generation> generation, SphereStateProvider shellBlock,
                      Optional<SphereStateProvider> topBlock, Optional<SphereStateProvider> bottomBlock, Optional<SphereStateProvider> caveFloorBlock, FloatProvider shellThickness, TreasureChestEntry treasureEntry, RegistryKey<EntityType<?>> entityType) {
            super(size, decorators, spawns, generation);

            this.shellBlock = shellBlock;
            this.topBlock = topBlock;
            this.bottomBlock = bottomBlock;
            this.caveFloorBlock = caveFloorBlock;
            this.shellThickness = shellThickness;
            this.treasureEntry = treasureEntry;
            this.entityType = entityType;
        }

    }

    public static class Placed extends PlacedSphere<Config> {

        private static final BlockState CHEST_STATE = Blocks.CHEST.getDefaultState();

        private final BlockStateProvider shellBlock;
        private final BlockStateProvider topBlock;
        private final BlockStateProvider bottomBlock;
        private final BlockStateProvider caveFloorBlock;
        private final float shellThickness;
        private final EntityType<?> entityType;
        private final TreasureChestEntry treasureEntry;

        public Placed(ConfiguredSphere<? extends Sphere<Config>, Config> configuredSphere, float radius, List<RegistryEntry<ConfiguredSphereDecorator<?, ?>>> decorators, List<Pair<EntityType<?>, Integer>> spawns, ChunkRandom random,
                      BlockStateProvider shellBlock, BlockStateProvider topBlock, BlockStateProvider bottomBlock, BlockStateProvider caveFloorBlock, float shellRadius, TreasureChestEntry treasureEntry, EntityType<?> entityType) {
            super(configuredSphere, radius, decorators, spawns, random);
            this.shellBlock = shellBlock;
            this.topBlock = topBlock;
            this.bottomBlock = bottomBlock;
            this.caveFloorBlock = caveFloorBlock;
            this.shellThickness = shellRadius;
            this.treasureEntry = treasureEntry;
            this.entityType = entityType;
        }

        @Override
        public void generate(Chunk chunk, DynamicRegistryManager registryManager) {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
            BlockPos spherePos = this.getPosition();
            int x = spherePos.getX();
            int y = spherePos.getY();
            int z = spherePos.getZ();

            int ceiledRadius = (int) Math.ceil(this.radius);
            int maxX = Math.min(chunkX * 16 + 15, x + ceiledRadius);
            int maxZ = Math.min(chunkZ * 16 + 15, z + ceiledRadius);

            Map<Point, Integer> floorBlocks = new Object2ObjectArrayMap<>();
            BlockPos.Mutable currBlockPos = new BlockPos.Mutable();

            boolean chestPlaced = false; // 标记是否生成过奖励箱

            for (int x2 = Math.max(chunkX * 16, x - ceiledRadius); x2 <= maxX; x2++) {
                for (int y2 = y - ceiledRadius; y2 <= y + ceiledRadius; y2++) {
                    for (int z2 = Math.max(chunkZ * 16, z - ceiledRadius); z2 <= maxZ; z2++) {
                        long d = Math.round(Support.getDistance(x, y, z, x2, y2, z2));
                        if (d > this.radius) continue;

                        currBlockPos.set(x2, y2, z2);

                        if (d > this.radius - 1) {
                            if (isBottomBlock(d, x2, y2, z2)) {
                                chunk.setBlockState(currBlockPos, this.bottomBlock.get(random, currBlockPos), false);
                            } else if (isTopBlock(d, x2, y2, z2)) {
                                if (x2==x&&y2==y) chunk.setBlockState(currBlockPos, BlockStateProvider.of(Blocks.GLASS).get(random, currBlockPos), false);
                                else chunk.setBlockState(currBlockPos, this.topBlock.get(random, currBlockPos), false);
                            } else {
                                chunk.setBlockState(currBlockPos, this.shellBlock.get(random, currBlockPos), false);
                            }
                        } else if (d <= this.radius - this.shellThickness) {
                            Point point = new Point(x2, z2);
                            if (!floorBlocks.containsKey(point)) {
                                floorBlocks.put(new Point(x2, z2), y2);
                                chunk.setBlockState(currBlockPos.down(), this.caveFloorBlock.get(random, currBlockPos), false);

                                // 只生成一个奖励箱
                                if (!chestPlaced) {
                                    BlockPos immutable = currBlockPos.toImmutable();
                                    chunk.setBlockState(immutable, CHEST_STATE, false);
                                    chunk.setBlockEntity(new ChestBlockEntity(immutable, CHEST_STATE));
                                    if (chunk.getBlockEntity(immutable) instanceof ChestBlockEntity chestBlockEntity) {
                                        chestBlockEntity.setLootTable(treasureEntry.lootTable(), random.nextLong());
                                    }
                                    chestPlaced = true; // 标记已生成
                                }
                            }
                        } else if (d < this.radius) {
                            chunk.setBlockState(currBlockPos, this.shellBlock.get(random, currBlockPos), false);
                        }
                    }
                }
            }
        }

        @Override
        public void populateEntities(ChunkPos chunkPos, ChunkRegion chunkRegion, ChunkRandom chunkRandom) {
            if (isCenterInChunk(chunkPos)) {
                StarrySkies.LOGGER.debug("Populating entities for sphere in chunk x:{} z:{} (StartX:{} StartZ:{}) {}", chunkPos.x, chunkPos.z, chunkPos.getStartX(), chunkPos.getStartZ(), this.getDescription(chunkRegion.getRegistryManager()));
                for (Pair<EntityType<?>, Integer> spawnEntry : spawns) {

                    int xCord = chunkPos.getStartX();
                    int zCord = chunkPos.getStartZ();

                    chunkRandom.setPopulationSeed(chunkRegion.getSeed(), xCord, zCord);

                    for (int i = 0; i < spawnEntry.getRight(); i++) {
                        int startingX = this.getPosition().getX();
                        int startingY = this.getPosition().getY();
                        int startingZ = this.getPosition().getZ();
                        int minHeight = this.getPosition().getY() - this.getRadius();
                        BlockPos.Mutable blockPos = new BlockPos.Mutable(startingX, startingY, startingZ);
                        int height = Support.getLowerGroundBlock(chunkRegion, blockPos, minHeight) + 1;

                        if (height != 0) {
                            Entity entity = spawnEntry.getLeft().create(chunkRegion.toServerWorld());
                            if (entity != null) {
                                float width = entity.getWidth();
                                double xPos = MathHelper.clamp(startingX, (double) xCord + (double) width, (double) xCord + 16.0D - (double) width);
                                double zLength = MathHelper.clamp(startingZ, (double) zCord + (double) width, (double) zCord + 16.0D - (double) width);

                                try {
                                    entity.refreshPositionAndAngles(xPos, height, zLength, chunkRandom.nextFloat() * 360.0F, 0.0F);
                                    if (entity instanceof MobEntity mobentity) {
                                        if (mobentity.canSpawn(chunkRegion, SpawnReason.CHUNK_GENERATION) && mobentity.canSpawn(chunkRegion)) {
                                            mobentity.initialize(chunkRegion, chunkRegion.getLocalDifficulty(mobentity.getBlockPos()), SpawnReason.CHUNK_GENERATION, null);
                                            boolean success = chunkRegion.spawnEntity(mobentity);
                                            if (!success) {
                                                return;
                                            }
                                        }
                                    }
                                } catch (Exception exception) {
                                    StarrySkies.LOGGER.warn("Failed to spawn mob on sphere{}\nException: {}", this.getDescription(chunkRegion.getRegistryManager()), exception);
                                }
                            }
                        }
                    }
                }
                StarrySkies.LOGGER.debug("Finished populating");
            }
        }


        @Override
        public String getDescription(DynamicRegistryManager registryManager) {
            return "+++ DungeonSphere +++" +
                    "\nPosition: x=" + this.getPosition().getX() + " y=" + this.getPosition().getY() + " z=" + this.getPosition().getZ() +
                    "\nTemplateID: " + this.getID(registryManager) +
                    "\nRadius: " + this.radius +
                    "\nShellBlock: " + this.shellBlock +
                    "\nShellRadius: " + this.shellThickness +
                    "\nEntityType: " + this.entityType.getUntranslatedName();
        }
    }

}

