package com.fordmc.fmutils.mixin;

import com.fordmc.fmutils.client.CinematicManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void lockKeybindActionsDuringCinematic(CallbackInfo ci) {
        if (CinematicManager.shouldBlockGameKeybinds()) {
            ci.cancel();
        }
    }
}
