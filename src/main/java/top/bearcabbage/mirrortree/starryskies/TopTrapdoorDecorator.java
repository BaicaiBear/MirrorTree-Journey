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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

public class TopTrapdoorDecorator extends SphereDecorator<SphereDecoratorConfig.DefaultSphereDecoratorConfig> {

    public static SphereDecorator<SphereDecoratorConfig.DefaultSphereDecoratorConfig> TOPTRAPDOOR = register("toptrapdoor", new TopTrapdoorDecorator(SphereDecoratorConfig.DefaultSphereDecoratorConfig.CODEC));


    private static final BlockState TRAPDOOR = Blocks.IRON_TRAPDOOR.getDefaultState();
    private static final BlockState STONE_BUTTON = Blocks.STONE_BUTTON.getDefaultState();

    public static void init() {}

    private static <C extends SphereDecoratorConfig, F extends SphereDecorator<C>> F register(String name, F feature) {
        return Registry.register(StarryRegistries.SPHERE_DECORATOR, Identifier.of("mirrortree", name), feature);
    }

    public TopTrapdoorDecorator(Codec<SphereDecoratorConfig.DefaultSphereDecoratorConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(SphereFeatureContext<SphereDecoratorConfig.DefaultSphereDecoratorConfig> context) {
        StructureWorldAccess world = context.getWorld();
        PlacedSphere<?> sphere = context.getSphere();

        BlockPos center = sphere.getPosition();
        int radius = sphere.getRadius();

        // 顶部中央表面方块
        BlockPos topPos = center.up(radius);
        BlockPos buttonPos = topPos.north();

        // 替换为铁活板门（如果空气或可替换方块）
        world.setBlockState(topPos, TRAPDOOR, 3);
        // 在顶部中央表面方块的北方放置石头按钮
        world.setBlockState(buttonPos, STONE_BUTTON, 3);

        return true;
    }
}