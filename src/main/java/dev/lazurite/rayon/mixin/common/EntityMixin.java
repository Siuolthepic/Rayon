package dev.lazurite.rayon.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
//    @Unique
//    private final Entity entity = (Entity) (Object) this;

    /*
     * TODO
     *  figure out how to make entities unable to move (sounds included)
     */

    @Inject(method = "setPos", at = @At("HEAD"), cancellable = true)
    public void setPos(double x, double y, double z, CallbackInfo info) {

    }

    @Inject(method = "updatePosition", at = @At("HEAD"), cancellable = true)
    public void updatePosition(double x, double y, double z, CallbackInfo info) {

    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move(MovementType type, Vec3d movement, CallbackInfo info) {

    }
}