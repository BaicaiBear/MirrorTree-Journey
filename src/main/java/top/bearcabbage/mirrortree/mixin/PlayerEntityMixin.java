package top.bearcabbage.mirrortree.mixin;


import net.adventurez.entity.DragonEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (this.getWorld().getRegistryKey().getValue().getNamespace().equals("starry_skies")) {
            if (this.getVehicle() instanceof LivingEntity vehicle && (vehicle instanceof EnderDragonEntity || vehicle instanceof DragonEntity)) {
                vehicle.remove(RemovalReason.UNLOADED_WITH_PLAYER);
            }
        }
    }
}
