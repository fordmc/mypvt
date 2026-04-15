package com.fordmc.fmutils.mixin;

import com.fordmc.fmutils.client.CinematicManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        CinematicManager.recordKeyboardInput(action);
    }
}
