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
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCinematicBars(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!CinematicManager.isActive() || !FMConfig.get().cinematicBars) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        
        // 12% bar height
        int barHeight = (int) (height * 0.12);
        
        // Top bar
        guiGraphics.fill(0, 0, width, barHeight, 0xFF000000);
        // Bottom bar
        guiGraphics.fill(0, height - barHeight, width, height, 0xFF000000);
    }
}
