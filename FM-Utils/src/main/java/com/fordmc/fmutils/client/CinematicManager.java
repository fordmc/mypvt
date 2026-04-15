package com.fordmc.fmutils.client;

import com.fordmc.fmutils.config.FMConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CinematicManager {
    private enum StartReason {
        MANUAL,
        AUTO
    }

    public record CameraPose(Vec3 position, float yaw, float pitch) {
    }

    private static class CinematicMusicInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        public CinematicMusicInstance(net.minecraft.sounds.SoundEvent event) {
            super(event, net.minecraft.sounds.SoundSource.MASTER, net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());
            this.volume = FMConfig.get().cinematicMusicVolume;
            this.pitch = 1.0f;
            this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
            this.looping = false;
            this.delay = 0;
            this.relative = true;
        }

        @Override
        public void tick() {
            this.volume = FMConfig.get().cinematicMusicVolume;
            if (!active || this.volume <= 0.0f) {
                this.stop();
            }
        }
    }

    private static final int VIEW_COUNT = 16;
    private static final double CAMERA_WALL_PADDING = 0.28D;
    private static int lastCameraViewIndex = -1;
    private static int currentCameraView = 0;
    
    private static final List<net.minecraft.sounds.SoundEvent> MUSIC_TRACKS = List.of(
        SoundEvents.MUSIC_DISC_CREATOR.value(),
        SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX.value(),
        SoundEvents.MUSIC_DISC_OTHERSIDE.value(),
        SoundEvents.MUSIC_DISC_PIGSTEP.value(),
        SoundEvents.MUSIC_DISC_PRECIPICE.value(),
        SoundEvents.MUSIC_DISC_RELIC.value(),
        SoundEvents.MUSIC_DISC_5.value(),
        SoundEvents.MUSIC_DISC_CAT.value(),
        SoundEvents.MUSIC_DISC_CHIRP.value(),
        SoundEvents.MUSIC_DISC_STAL.value()
    );

    private static boolean active = false;
    private static StartReason startReason = StartReason.MANUAL;
    private static long lastInputTime = System.currentTimeMillis();
    private static long startTime = 0L;
    private static boolean autoStoppedByKeyboardThisTick = false;
    private static boolean changedHideGui = false;
    private static boolean previousHideGui = false;
    private static boolean cinematicMusicStarted = false;
    private static CinematicMusicInstance currentMusicInstance = null;
    private static float currentFovModifier = 0f;
    private static int originalFpsLimit = 120;
    private static boolean changedFpsLimit = false;

    public static void toggle() {
        if (active) {
            stop("Cinematic Mode Disabled");
        } else {
            start(StartReason.MANUAL, "Cinematic Mode Enabled");
        }
    }

    public static void setActive(boolean state) {
        if (state) {
            start(StartReason.MANUAL, "Cinematic Mode Started");
        } else {
            stop("Cinematic Mode Stopped");
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isControlLocked() {
        return active;
    }

    public static boolean shouldBlockGameKeybinds() {
        return active && startReason == StartReason.MANUAL;
    }

    public static void recordMouseInput() {
        if (!active) {
            lastInputTime = System.currentTimeMillis();
        }
    }

    public static void recordKeyboardInput(int action) {
        if (action == GLFW.GLFW_RELEASE) {
            return;
        }

        lastInputTime = System.currentTimeMillis();
        if (active && startReason == StartReason.AUTO) {
            stop("Cinematic Mode Stopped");
            autoStoppedByKeyboardThisTick = true;
        }
    }

    public static boolean consumeAutoStoppedByKeyboardThisTick() {
        if (autoStoppedByKeyboardThisTick) {
            autoStoppedByKeyboardThisTick = false;
            return true;
        }

        return false;
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (active) {
                stop("Cinematic Mode Stopped");
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (!active && isPlayerPressingMovement(client.player)) {
            lastInputTime = now;
        }

        long idleTime = now - lastInputTime;

        if (FMConfig.get().autoCinematicEnabled && !active) {
            if (idleTime > FMConfig.get().afkTimeoutSeconds * 1000L) {
                start(StartReason.AUTO, "Cinematic Mode Started");
            }
        }

        autoStoppedByKeyboardThisTick = false;
    }

    public static float getFovModifier() {
        return active ? currentFovModifier : 0f;
    }

    public static float getSlowMotionScale() {
        return active ? FMConfig.get().slowMotionScale : 1.0f;
    }

    public static CameraPose getCameraPose(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!active || client.player == null || client.level == null) {
            return null;
        }

        LocalPlayer player = client.player;
        Vec3 target = player.getEyePosition(partialTick).add(0.0D, -0.25D, 0.0D);
        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0D;
        FMConfig config = FMConfig.get();
        double speed = Math.max(0.03D, config.rotationSpeed) * 1.2D;
        double viewDuration = Math.max(4.0D, config.cameraViewDurationSeconds);
        int viewIndex = (int) (elapsedSeconds / viewDuration);
        double viewTime = elapsedSeconds % viewDuration;
        
        if (viewIndex != lastCameraViewIndex) {
            lastCameraViewIndex = viewIndex;
            if (!config.cycleCameraViews) {
                currentCameraView = 0;
            } else if (config.randomizeCinematics) {
                int nextView;
                java.util.Random r = new java.util.Random(startTime + viewIndex * 1337L);
                do {
                    nextView = r.nextInt(VIEW_COUNT);
                } while (nextView == currentCameraView && VIEW_COUNT > 1);
                currentCameraView = nextView;
            } else {
                currentCameraView = viewIndex % VIEW_COUNT;
            }
        }
        
        if (config.cinematicZoomAmount > 0) {
            double zoomProgress = viewTime / viewDuration;
            currentFovModifier = (float) (zoomProgress * config.cinematicZoomAmount * -30.0D);
        } else {
            currentFovModifier = 0f;
        }
        
        int view = currentCameraView;
        double distance = Math.max(3.0D, config.cameraDistance);
        double height = config.cameraHeight;

        if (config.autoAdjustCamera) {
            double[] dirs = {0, 90, 180, 270};
            double totalFree = 0;
            for (double dir : dirs) {
                Vec3 dVec = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick) + (float)dir).normalize();
                HitResult hr = client.level.clip(new ClipContext(target, target.add(dVec.scale(15.0D)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                totalFree += hr.getType() == HitResult.Type.MISS ? 15.0D : hr.getLocation().distanceTo(target);
            }
            double avgRadius = totalFree / 4.0D;
            distance = Math.max(2.0D, Math.min(distance, avgRadius * 0.8D));
            height = Math.min(height, avgRadius * 0.3D);
        }

        Vec3 position;

        switch (view) {
            case 11 -> position = skySwoop(target, elapsedSeconds, speed, distance, height, viewDuration, viewTime);
            case 12 -> position = vertigoZoom(player, target, partialTick, viewTime, viewDuration, distance, height);
            case 13 -> position = lowHeroOrbit(target, elapsedSeconds, speed, distance, height);
            case 14 -> position = birdsEyeSlow(target, elapsedSeconds, speed, distance, height);
            case 15 -> position = dynamicDollyFollow(player, target, partialTick, viewTime, distance, height);
            default -> position = orbit(target, elapsedSeconds, speed, distance, height);
        }

        if (config.handheldCameraMode) {
            double shakeTime = elapsedSeconds * 2.5D;
            double intensity = 0.05D;
            position = position.add(
                Math.sin(shakeTime * 0.7D) * intensity,
                Math.cos(shakeTime * 0.8D) * intensity,
                Math.sin(shakeTime * 0.9D) * intensity
            );
        }

        position = avoidBlocks(player, target, position);
        return lookAt(position, target);
    }

    private static Vec3 vertigoZoom(LocalPlayer player, Vec3 target, float partialTick, double viewTime, double viewDuration, double distance, double height) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        double eased = smoothStep(viewTime / viewDuration);
        double dist = Mth.lerp(eased, distance * 0.5D, distance * 1.5D);
        currentFovModifier += (float) (eased * 40.0D); // Vertigo effect zooms FOV while pulling back
        return target.add(forward.scale(-dist)).add(0.0D, 1.1D + height, 0.0D);
    }

    private static Vec3 lowHeroOrbit(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = elapsedSeconds * speed * 1.3D;
        return target.add(Math.cos(angle) * distance * 0.7D, 0.2D + height * 0.2D, Math.sin(angle) * distance * 0.7D);
    }

    private static Vec3 birdsEyeSlow(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = elapsedSeconds * speed * 0.3D;
        return target.add(Math.cos(angle) * distance * 1.5D, 10.0D + height, Math.sin(angle) * distance * 1.5D);
    }

    private static Vec3 dynamicDollyFollow(LocalPlayer player, Vec3 target, float partialTick, double viewTime, double distance, double height) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double drift = Math.sin(viewTime * 0.5D) * distance * 0.5D;
        return target.add(side.scale(distance)).add(forward.scale(drift)).add(0.0D, 1.3D + height, 0.0D);
    }

    private static void start(StartReason reason, String message) {
        if (active && startReason == reason) {
            return;
        }

        active = true;
        startReason = reason;

        lastCameraViewIndex = -1;
        currentCameraView = 0;

        startTime = System.currentTimeMillis();
        applyPresentation(Minecraft.getInstance());
        showStatus(message);
    }

    private static void stop(String message) {
        if (!active) {
            return;
        }

        active = false;
        lastInputTime = System.currentTimeMillis();
        restorePresentation(Minecraft.getInstance());
        showStatus(message);
    }

    private static void showStatus(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && FMConfig.get().showStatusMsg) {
            client.player.displayClientMessage(
                Component.literal(message).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        }
    }

    private static boolean isPlayerPressingMovement(LocalPlayer player) {
        return player.input != null && !Input.EMPTY.equals(player.input.keyPresses);
    }

    private static Vec3 orbit(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = elapsedSeconds * speed;
        return target.add(Math.cos(angle) * distance, 0.9D + height, Math.sin(angle) * distance);
    }

    private static Vec3 highOrbit(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = -elapsedSeconds * speed * 0.65D;
        double radius = distance * 1.3D;
        return target.add(Math.cos(angle) * radius, 3.2D + height, Math.sin(angle) * radius);
    }

    private static Vec3 frontHero(
        LocalPlayer player,
        Vec3 target,
        float partialTick,
        double viewTime,
        double viewDuration,
        double distance,
        double height
    ) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        double sway = Math.sin(viewTime * 0.7D) * distance * 0.22D;
        double lift = Mth.lerp(smoothStep(viewTime / viewDuration), 0.15D, 0.85D);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).scale(sway);
        return target.add(forward.scale(distance * 0.8D)).add(side).add(0.0D, lift + height * 0.5D, 0.0D);
    }

    private static Vec3 sideDolly(
        LocalPlayer player,
        Vec3 target,
        float partialTick,
        double viewTime,
        double viewDuration,
        double distance,
        double height
    ) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double slide = Mth.lerp(smoothStep(viewTime / viewDuration), -distance * 0.8D, distance * 0.8D);
        return target.add(side.scale(slide)).add(forward.scale(-distance * 0.85D)).add(0.0D, 1.1D + height, 0.0D);
    }

    private static Vec3 lowSweep(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = -elapsedSeconds * speed * 1.15D;
        double radius = distance * 0.9D;
        return target.add(Math.cos(angle) * radius, 0.35D + height * 0.4D, Math.sin(angle) * radius);
    }

    private static Vec3 craneRise(
        LocalPlayer player,
        Vec3 target,
        float partialTick,
        double viewTime,
        double viewDuration,
        double distance,
        double height
    ) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double eased = smoothStep(viewTime / viewDuration);
        double back = Mth.lerp(eased, distance * 0.55D, distance * 1.15D);
        double lift = Mth.lerp(eased, 0.3D, 4.2D + height);
        double drift = Math.sin(viewTime * 0.45D) * distance * 0.18D;
        return target.add(forward.scale(-back)).add(side.scale(drift)).add(0.0D, lift, 0.0D);
    }

    private static Vec3 shoulderFollow(
        LocalPlayer player,
        Vec3 target,
        float partialTick,
        double viewTime,
        double distance,
        double height
    ) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double sway = Math.sin(viewTime * 0.9D) * distance * 0.16D;
        return target.add(forward.scale(-distance * 0.7D))
            .add(side.scale(distance * 0.38D + sway))
            .add(0.0D, 1.25D + height * 0.7D, 0.0D);
    }

    private static Vec3 topDownSpiral(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = elapsedSeconds * speed * 0.8D;
        double radius = distance * 0.35D;
        return target.add(Math.cos(angle) * radius, 5.4D + height, Math.sin(angle) * radius);
    }

    private static Vec3 dutchPivot(LocalPlayer player, Vec3 target, float partialTick, double elapsedSeconds, double speed, double distance, double height) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double angle = elapsedSeconds * speed * 1.5D;
        double shift = Math.sin(angle) * distance * 0.5D;
        double lift = Math.cos(angle) * height * 0.5D;
        return target.add(forward.scale(distance * 0.9D)).add(side.scale(shift)).add(0.0D, 1.0D + height + lift, 0.0D);
    }
    
    private static Vec3 lowTracking(LocalPlayer player, Vec3 target, float partialTick, double viewTime, double distance, double height) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
        double sway = Math.sin(viewTime * 1.2D) * distance * 0.1D;
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).scale(sway);
        return target.add(forward.scale(-distance * 0.5D)).add(side).add(0.0D, -0.4D + height, 0.0D);
    }
    
    private static Vec3 closeUpOrbit(Vec3 target, double elapsedSeconds, double speed, double distance, double height) {
        double angle = -elapsedSeconds * speed * 1.4D;
        double radius = Math.max(1.5D, distance * 0.4D);
        return target.add(Math.cos(angle) * radius, 1.2D + height, Math.sin(angle) * radius);
    }
    
    private static Vec3 skySwoop(Vec3 target, double elapsedSeconds, double speed, double distance, double height, double viewDuration, double viewTime) {
        double angle = elapsedSeconds * speed * 0.5D;
        double eased = smoothStep(viewTime / viewDuration);
        double radius = Mth.lerp(eased, distance * 2.0D, distance * 0.5D);
        double lift = Mth.lerp(eased, 8.0D + height, 1.0D + height);
        return target.add(Math.cos(angle) * radius, lift, Math.sin(angle) * radius);
    }

    private static Vec3 avoidBlocks(LocalPlayer player, Vec3 target, Vec3 desiredPosition) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return desiredPosition;
        }

        HitResult hitResult = client.level.clip(new ClipContext(
            target,
            desiredPosition,
            ClipContext.Block.VISUAL,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hitResult.getType() == HitResult.Type.MISS) {
            return desiredPosition;
        }

        Vec3 awayFromPlayer = hitResult.getLocation().subtract(target).normalize();
        return hitResult.getLocation().subtract(awayFromPlayer.scale(CAMERA_WALL_PADDING));
    }

    private static void applyPresentation(Minecraft client) {
        FMConfig config = FMConfig.get();

        if (config.hideGuiDuringCinematic && !changedHideGui) {
            previousHideGui = client.options.hideGui;
            client.options.hideGui = true;
            changedHideGui = true;
        }

        if (config.playMusicDuringCinematic && !cinematicMusicStarted) {
            client.getMusicManager().stopPlaying();
            if (config.cinematicMusicVolume > 0.0f) {
                net.minecraft.sounds.SoundEvent track = MUSIC_TRACKS.get(client.level.random.nextInt(MUSIC_TRACKS.size()));
                currentMusicInstance = new CinematicMusicInstance(track);
                client.getSoundManager().play(currentMusicInstance);
            }
            cinematicMusicStarted = true;
        }

        if (config.lockFps30 && !changedFpsLimit) {
            originalFpsLimit = client.options.framerateLimit().get();
            client.options.framerateLimit().set(30);
            changedFpsLimit = true;
        }

        if (client.hasSingleplayerServer() && config.slowMotionScale < 1.0f) {
            client.getSingleplayerServer().getCommands().performPrefixedCommand(
                client.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(),
                "tick rate " + (20.0f * config.slowMotionScale)
            );
        }
    }

    private static void restorePresentation(Minecraft client) {
        if (changedHideGui) {
            client.options.hideGui = previousHideGui;
            changedHideGui = false;
        }

        if (cinematicMusicStarted) {
            if (currentMusicInstance != null) {
                client.getSoundManager().stop(currentMusicInstance);
                currentMusicInstance = null;
            } else {
                client.getMusicManager().stopPlaying(); // Failsafe
            }
            cinematicMusicStarted = false;
        }

        if (changedFpsLimit) {
            client.options.framerateLimit().set(originalFpsLimit);
            changedFpsLimit = false;
        }

        if (client.hasSingleplayerServer()) {
            client.getSingleplayerServer().getCommands().performPrefixedCommand(
                client.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(),
                "tick rate 20"
            );
        }
    }

    private static double smoothStep(double value) {
        double clamped = Mth.clamp(value, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static CameraPose lookAt(Vec3 position, Vec3 target) {
        Vec3 delta = target.subtract(position);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontal)));

        return new CameraPose(position, yaw, pitch);
    }
}
