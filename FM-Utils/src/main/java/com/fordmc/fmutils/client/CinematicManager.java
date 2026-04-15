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

    private static final double CAMERA_WALL_PADDING = 0.28D;
    private static int lastCameraViewIndex = -1;
    
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

    private record ShotParameters(
        double radiusBase,
        double radiusFreq,
        double radiusAmp,
        double heightBase,
        double heightFreq,
        double heightAmp,
        double yawSpeed,
        double fovPulseFreq,
        double fovPulseAmp,
        double driftSpeed,
        boolean followMode // true = orbit/follow, false = stationary/tracking
    ) {}

    private static ShotParameters currentShot = null;
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
        double viewDuration = Math.max(4.0D, config.cameraViewDurationSeconds);
        int viewIndex = (int) (elapsedSeconds / viewDuration);
        double viewTime = elapsedSeconds % viewDuration;
        
        if (viewIndex != lastCameraViewIndex || currentShot == null) {
            lastCameraViewIndex = viewIndex;
            generateNewShot();
        }
        
        // Procedural Parametric Engine
        ShotParameters p = currentShot;
        double speedScale = Math.max(0.03D, config.rotationSpeed) * 1.2D;
        double t = elapsedSeconds;
        double vt = viewTime;
        
        // FOV Pulse / Dynamic Zoom
        if (config.cinematicZoomAmount > 0) {
            double zoomProgress = vt / viewDuration;
            double baseZoom = zoomProgress * config.cinematicZoomAmount * -30.0D;
            double pulse = Math.sin(vt * p.fovPulseFreq) * p.fovPulseAmp;
            currentFovModifier = (float) (baseZoom + pulse);
        } else {
            currentFovModifier = 0f;
        }
        
        double currentRadius = p.radiusBase + Math.sin(t * p.radiusFreq) * p.radiusAmp;
        double currentHeight = p.heightBase + Math.cos(t * p.heightFreq) * p.heightAmp;

        if (config.autoAdjustCamera) {
            double[] dirs = {0, 90, 180, 270};
            double totalFree = 0;
            for (double dir : dirs) {
                Vec3 dVec = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick) + (float)dir).normalize();
                HitResult hr = client.level.clip(new ClipContext(target, target.add(dVec.scale(15.0D)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                totalFree += hr.getType() == HitResult.Type.MISS ? 15.0D : hr.getLocation().distanceTo(target);
            }
            double avgRadius = totalFree / 4.0D;
            currentRadius = Math.max(2.0D, Math.min(currentRadius, avgRadius * 0.8D));
            currentHeight = Math.min(currentHeight, avgRadius * 0.3D);
        }

        Vec3 pos;
        if (p.followMode) {
            double currentYaw = t * p.yawSpeed * speedScale;
            pos = target.add(Math.cos(currentYaw) * currentRadius, currentHeight, Math.sin(currentYaw) * currentRadius);
        } else {
            // Stationary dolly style
            Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).normalize();
            double slide = Math.sin(vt * p.yawSpeed) * currentRadius;
            pos = target.add(forward.scale(-currentRadius)).add(new Vec3(-forward.z, 0, forward.x).scale(slide)).add(0, currentHeight, 0);
        }

        return calculateFinalPose(player, target, pos, config, elapsedSeconds);
    }

    private static CameraPose calculateFinalPose(LocalPlayer player, Vec3 target, Vec3 pos, FMConfig config, double elapsedSeconds) {
        if (config.handheldCameraMode) {
            double shakeTime = elapsedSeconds * 2.5D;
            double intensity = 0.05D;
            pos = pos.add(
                Math.sin(shakeTime * 0.7D) * intensity,
                Math.cos(shakeTime * 0.8D) * intensity,
                Math.sin(shakeTime * 0.9D) * intensity
            );
        }

        pos = avoidBlocks(player, target, pos);
        return lookAt(pos, target);
    }

    private static void generateNewShot() {
        java.util.Random r = new java.util.Random();
        double distance = Math.max(3.0D, FMConfig.get().cameraDistance);
        double heightGoal = FMConfig.get().cameraHeight;

        // Archetype selection
        int archetype = r.nextInt(4);
        
        currentShot = switch (archetype) {
            case 0 -> // ORBIT
                new ShotParameters(
                    distance * (0.8 + r.nextDouble() * 0.4),
                    0.1 + r.nextDouble() * 0.5,
                    r.nextDouble() * 1.5,
                    0.8 + heightGoal + r.nextDouble() * 1.5,
                    0.2 + r.nextDouble() * 0.4,
                    r.nextDouble() * 1.0,
                    (r.nextBoolean() ? 1 : -1) * (0.4 + r.nextDouble()),
                    0.2 + r.nextDouble() * 0.4,
                    r.nextDouble() * 5.0,
                    0.05 + r.nextDouble() * 0.1,
                    true
                );
            case 1 -> // DOLLEY / LOW SWEEP
                new ShotParameters(
                    distance * (0.5 + r.nextDouble() * 0.5),
                    0.05 + r.nextDouble() * 0.2,
                    1.0 + r.nextDouble() * 2.0,
                    0.2 + heightGoal * 0.5,
                    0.1,
                    0.2,
                    (r.nextBoolean() ? 1 : -1) * (0.2 + r.nextDouble() * 0.4),
                    0.1,
                    r.nextDouble() * 3.0,
                    0.1 + r.nextDouble() * 0.2,
                    false
                );
            case 2 -> // CRANE / SKY RIDER
                new ShotParameters(
                    distance * (1.2 + r.nextDouble() * 1.0),
                    0.05 + r.nextDouble() * 0.1,
                    0.5,
                    4.0 + heightGoal + r.nextDouble() * 6.0,
                    0.1 + r.nextDouble() * 0.2,
                    2.0 + r.nextDouble() * 3.0,
                    (r.nextBoolean() ? 1 : -1) * (0.1 + r.nextDouble() * 0.3),
                    0.05,
                    r.nextDouble() * 2.0,
                    0.02,
                    true
                );
            default -> // HERO / CLOSE-UP
                new ShotParameters(
                    Math.max(2.5, distance * 0.4),
                    0.3 + r.nextDouble() * 0.6,
                    0.2 + r.nextDouble() * 0.5,
                    1.1 + heightGoal,
                    0.5 + r.nextDouble() * 0.5,
                    0.1 + r.nextDouble() * 0.3,
                    (r.nextBoolean() ? 1 : -1) * (0.8 + r.nextDouble() * 0.7),
                    0.4 + r.nextDouble() * 0.6,
                    2.0 + r.nextDouble() * 8.0,
                    0.05,
                    true
                );
        };
    }

    private static void start(StartReason reason, String message) {
        if (active && startReason == reason) {
            return;
        }

        active = true;
        startReason = reason;

        lastCameraViewIndex = -1;
        currentShot = null;

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
