package com.fordmc.fmutils.mixin;

import com.fordmc.fmutils.client.CinematicManager;
import com.fordmc.fmutils.config.FMConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    private static float barProgress = 0f;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCinematicBars(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean active = CinematicManager.isActive() && FMConfig.get().cinematicBars;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        
        // Smoothly animate bars in/out
        if (active) {
            barProgress = Math.min(1.0f, barProgress + partialTick * 0.05f);
        } else {
            barProgress = Math.max(0.0f, barProgress - partialTick * 0.05f);
        }

        if (barProgress <= 0) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        
        // 12% max bar height, scaled by progress
        int maxBarHeight = (int) (height * 0.12);
        int currentBarHeight = (int) (maxBarHeight * barProgress);
        
        // Top bar
        guiGraphics.fill(0, 0, width, currentBarHeight, 0xFF000000);
        // Bottom bar
        guiGraphics.fill(0, height - currentBarHeight, width, height, 0xFF000000);
    }
}
