package com.fordmc.fmutils.mixin;

import com.fordmc.fmutils.client.CinematicManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void injectCinematicZoom(Camera camera, float tickDelta, boolean useFOVSetting, CallbackInfoReturnable<Object> cir) {
        float modifier = CinematicManager.getFovModifier();
        if (modifier != 0) {
            Object value = cir.getReturnValue();
            if (value instanceof Double d) {
                cir.setReturnValue(d + modifier);
            } else if (value instanceof Float f) {
                cir.setReturnValue(f + modifier);
            }
        }
    }
}
