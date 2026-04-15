package com.fordmc.fmutils.client.gui;

import com.fordmc.fmutils.config.FMConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("FM Utils Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonWidth = 150;
        int spacingVertical = 24;
        int spacingHorizontal = 10;
        
        // --- Left Column (General & Audio) ---
        int leftX = centerX - buttonWidth - spacingHorizontal;
        int y = 50;

        addLabel(leftX, y - 12, "General Settings");
        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().autoCinematicEnabled)
            .create(leftX, y, buttonWidth, 20, Component.literal("Auto Cinematic"), (button, value) -> FMConfig.get().autoCinematicEnabled = value));
        y += spacingVertical;

        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().showStatusMsg)
            .create(leftX, y, buttonWidth, 20, Component.literal("Status Messages"), (button, value) -> FMConfig.get().showStatusMsg = value));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(leftX, y, buttonWidth, 20, FMConfig.get().afkTimeoutSeconds, 5.0, 300.0, 5.0,
            val -> Component.literal("AFK: " + val.intValue() + "s"),
            val -> FMConfig.get().afkTimeoutSeconds = val.intValue()
        ));
        y += spacingVertical + 10;

        addLabel(leftX, y - 12, "Audio Settings");
        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().playMusicDuringCinematic)
            .create(leftX, y, buttonWidth, 20, Component.literal("MC Music"), (button, value) -> FMConfig.get().playMusicDuringCinematic = value));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(leftX, y, buttonWidth, 20, FMConfig.get().cinematicMusicVolume, 0.0, 1.0, 0.01,
            val -> Component.literal("Music Vol: " + (Math.round(val * 100) == 0 ? "Off" : Math.round(val * 100) + "%")),
            val -> FMConfig.get().cinematicMusicVolume = val.floatValue()
        ));

        // --- Right Column (Visuals & Camera) ---
        int rightX = centerX + spacingHorizontal;
        y = 50;

        addLabel(rightX, y - 12, "Visual Effects");
        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().hideGuiDuringCinematic)
            .create(rightX, y, buttonWidth, 20, Component.literal("Hide GUI"), (button, value) -> FMConfig.get().hideGuiDuringCinematic = value));
        y += spacingVertical;

        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().cinematicBars)
            .create(rightX, y, buttonWidth, 20, Component.literal("Cinematic Bars"), (button, value) -> FMConfig.get().cinematicBars = value));
        y += spacingVertical;

        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().handheldCameraMode)
            .create(rightX, y, buttonWidth, 20, Component.literal("Handheld Mode"), (button, value) -> FMConfig.get().handheldCameraMode = value));
        y += spacingVertical;

        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().lockFps30)
            .create(rightX, y, buttonWidth, 20, Component.literal("FPS Lock (30)"), (button, value) -> FMConfig.get().lockFps30 = value));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(rightX, y, buttonWidth, 20, FMConfig.get().cinematicZoomAmount, 0.0, 1.0, 0.05,
            val -> Component.literal("Zoom: " + (int)(val * 100) + "%"),
            val -> FMConfig.get().cinematicZoomAmount = val.floatValue()
        ));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(rightX, y, buttonWidth, 20, FMConfig.get().slowMotionScale, 0.2, 1.0, 0.05,
            val -> Component.literal("Speed: " + (int)(val * 100) + "%"),
            val -> FMConfig.get().slowMotionScale = val.floatValue()
        ));
        y += spacingVertical + 10;

        addLabel(rightX, y - 12, "Camera Controls");
        this.addRenderableWidget(CycleButton.onOffBuilder(FMConfig.get().cycleCameraViews)
            .create(rightX, y, buttonWidth, 20, Component.literal("Cycle Views"), (button, value) -> FMConfig.get().cycleCameraViews = value));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(rightX, y, buttonWidth, 20, FMConfig.get().rotationSpeed, 0.03, 0.6, 0.01,
            val -> Component.literal("Cam Speed: " + String.format(Locale.ROOT, "%.2f", val)),
            val -> FMConfig.get().rotationSpeed = val.floatValue()
        ));
        y += spacingVertical;

        this.addRenderableWidget(new FMSlider(rightX, y, buttonWidth, 20, FMConfig.get().cameraDistance, 3.0, 12.0, 0.5,
            val -> Component.literal("Distance: " + String.format(Locale.ROOT, "%.1f", val)),
            val -> FMConfig.get().cameraDistance = val.floatValue()
        ));

        this.addRenderableWidget(Button.builder(Component.literal("Done"), (button) -> {
            this.onClose();
        }).bounds(centerX - 100, this.height - 30, 200, 20).build());
    }

    private void addLabel(int x, int y, String text) {
        // We'll draw these in render()
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFAA00);
        
        int centerX = this.width / 2;
        int leftX = centerX - 150 - 10;
        int rightX = centerX + 10;

        guiGraphics.drawString(this.font, "General Settings", leftX, 38, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Audio Settings", leftX, 120, 0xAAAAAA);
        
        guiGraphics.drawString(this.font, "Visual Effects", rightX, 38, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Camera Controls", rightX, 144, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        FMConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
