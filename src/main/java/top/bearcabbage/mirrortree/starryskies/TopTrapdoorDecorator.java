package top.bearcabbage.mirrortree.starryskies;

import com.mojang.serialization.Codec;
import de.dafuqs.starryskies.StarrySkies;
import de.dafuqs.starryskies.registries.StarryRegistries;
import de.dafuqs.starryskies.worldgen.*;
import de.dafuqs.starryskies.worldgen.decorators.XMarksTheSpotDecorator;
import de.dafuqs.starryskies.worldgen.decorators.XMarksTheSpotDecoratorConfig;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

import static net.minecraft.block.Blocks.*;
import static top.bearcabbage.mirrortree.MirrorTree.MOD_ID;

public class TopTrapdoorDecorator extends SphereDecorator<SphereDecoratorConfig.DefaultSphereDecoratorConfig> {

    public static SphereDecorator<SphereDecoratorConfig.DefaultSphereDecoratorConfig> TOPTRAPDOOR = register("toptrapdoor", new TopTrapdoorDecorator(SphereDecoratorConfig.DefaultSphereDecoratorConfig.CODEC));


    private static final BlockState GLASS = TINTED_GLASS.getDefaultState();

    public static void init() {}

    private static <C extends SphereDecoratorConfig, F extends SphereDecorator<C>> F register(String name, F feature) {
        return Registry.register(StarryRegistries.SPHERE_DECORATOR, Identifier.of(MOD_ID, name), feature);
    }

    public TopTrapdoorDecorator(Codec<SphereDecoratorConfig.DefaultSphereDecoratorConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(SphereFeatureContext<SphereDecoratorConfig.DefaultSphereDecoratorConfig> context) {
        StructureWorldAccess world = context.getWorld();
        PlacedSphere<?> sphere = context.getSphere();

        BlockPos center = sphere.getPosition();
        int radius = sphere.getRadius() + 2;

        // -------------------------------
        // 顶部中央表面玻璃块
        // -------------------------------
        BlockPos topCenter = center.up(radius);
        while (world.getBlockState(topCenter).isAir() && topCenter.getY() > center.getY()) {
            topCenter = topCenter.down();
        }
        world.setBlockState(topCenter, GLASS, 3);

        // -------------------------------
        // 三正交方向圆环逻辑（Copper Bulb/Copper Grate）
        // -------------------------------
        float innerRadius = radius + 2; // 内径
        int ringWidth = Math.max(1, Math.min(2, Math.round(radius * 0.1F))); // 自适应宽度 1-2 方块
        float outerRadius = innerRadius + ringWidth;
        double verticalThickness = radius/4.0; // 半径>4时垂直方向加宽

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        Random random = context.getRandom();

        int minX = center.getX() - (int)Math.ceil(outerRadius);
        int maxX = center.getX() + (int)Math.ceil(outerRadius);
        int minY = center.getY() - (int)Math.ceil(outerRadius);
        int maxY = center.getY() + (int)Math.ceil(outerRadius);
        int minZ = center.getZ() - (int)Math.ceil(outerRadius);
        int maxZ = center.getZ() + (int)Math.ceil(outerRadius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutablePos.set(x, y, z);

                    double dx = x - center.getX();
                    double dy = y - center.getY();
                    double dz = z - center.getZ();

                    // XY平面圆环
                    double distXY = Math.sqrt(dx*dx + dy*dy);
                    if (distXY >= innerRadius && distXY <= outerRadius && Math.abs(dz) < verticalThickness) {
                        world.setBlockState(mutablePos, random.nextFloat() > 0.6F ?
                                OXIDIZED_COPPER_BULB.getDefaultState().cycle(Properties.LIT) :
                                EXPOSED_COPPER_GRATE.getDefaultState(), 3);
                    }

                    // XZ平面圆环
                    double distXZ = Math.sqrt(dx*dx + dz*dz);
                    if (distXZ >= innerRadius && distXZ <= outerRadius && Math.abs(dy) < verticalThickness) {
                        world.setBlockState(mutablePos, random.nextFloat() > 0.6F ?
                                OXIDIZED_COPPER_BULB.getDefaultState().cycle(Properties.LIT) :
                                EXPOSED_COPPER_GRATE.getDefaultState(), 3);
                    }

                    // YZ平面圆环
                    double distYZ = Math.sqrt(dy*dy + dz*dz);
                    if (distYZ >= innerRadius && distYZ <= outerRadius && Math.abs(dx) < verticalThickness) {
                        world.setBlockState(mutablePos, random.nextFloat() > 0.6F ?
                                OXIDIZED_COPPER_BULB.getDefaultState().cycle(Properties.LIT) :
                                EXPOSED_COPPER_GRATE.getDefaultState(), 3);
                    }
                }
            }
        }

        return true;
    }
}