package com.fordmc.fmutils.client;

import com.fordmc.fmutils.client.gui.ConfigScreen;
import com.fordmc.fmutils.config.FMConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FMUtilsClient implements ClientModInitializer {
    private static KeyMapping toggleCinematicKey;
    private static KeyMapping configMenuKey;
    private static KeyMapping toggleAutoSystemKey;

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("fm-utils", "main")
    );

    @Override
    public void onInitializeClient() {
        toggleCinematicKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.fm-utils.toggle_cinematic",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));

        configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.fm-utils.config_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
        ));

        toggleAutoSystemKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.fm-utils.toggle_auto",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleCinematicKey.consumeClick()) {
                if (CinematicManager.consumeAutoStoppedByKeyboardThisTick()) {
                    continue;
                }
                CinematicManager.toggle();
            }

            while (configMenuKey.consumeClick()) {
                client.setScreen(new ConfigScreen(null));
            }

            while (toggleAutoSystemKey.consumeClick()) {
                FMConfig.get().autoCinematicEnabled = !FMConfig.get().autoCinematicEnabled;
                FMConfig.save();
                if (client.player != null) {
                    boolean enabled = FMConfig.get().autoCinematicEnabled;
                    client.player.displayClientMessage(
                        Component.literal(enabled ? "Auto-Cinematic System Enabled" : "Auto-Cinematic System Disabled")
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
                }
            }

            CinematicManager.tick(client);
        });
    }
}
