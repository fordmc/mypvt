package com.fordmc.fmutils.mixin;

import com.fordmc.fmutils.client.CinematicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        CinematicManager.recordMouseInput();
        if (CinematicManager.isControlLocked() && Minecraft.getInstance().screen == null) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        CinematicManager.recordMouseInput();
        if (CinematicManager.isControlLocked() && Minecraft.getInstance().screen == null) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        CinematicManager.recordMouseInput();
        if (CinematicManager.isControlLocked() && Minecraft.getInstance().screen == null) {
            ci.cancel();
        }
    }
}
