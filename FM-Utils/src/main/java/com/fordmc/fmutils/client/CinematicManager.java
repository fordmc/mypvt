package com.fordmc.fmutils.client;

import com.fordmc.fmutils.client.cinematic.CinematicMath;
import com.fordmc.fmutils.client.cinematic.ICinematicPreset;
import com.fordmc.fmutils.client.cinematic.presets.*;
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

    private static ICinematicPreset currentShot = null;
    private static ICinematicPreset previousShot = null;
    private static long shotStartTime = 0L;
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
    private static double smoothedAvoidanceDistance = -1.0;
    private static Vec3 smoothedPosition = null;
    private static float smoothedYaw = 0f;
    private static float smoothedPitch = 0f;
    private static boolean firstFrame = true;
    private static int lastArchetype = -1;

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

        // Music Playlist System: Auto-restart music if it stops during cinematic mode
        if (active && FMConfig.get().playMusicDuringCinematic && cinematicMusicStarted) {
            if (currentMusicInstance == null || !client.getSoundManager().isActive(currentMusicInstance)) {
                playNextTrack(client);
            }
        }

        autoStoppedByKeyboardThisTick = false;
    }

    private static void playNextTrack(Minecraft client) {
        if (FMConfig.get().cinematicMusicVolume <= 0.0f) return;
        
        net.minecraft.sounds.SoundEvent track = MUSIC_TRACKS.get(client.level.random.nextInt(MUSIC_TRACKS.size()));
        currentMusicInstance = new CinematicMusicInstance(track);
        client.getSoundManager().play(currentMusicInstance);
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
        long now = System.currentTimeMillis();
        FMConfig config = FMConfig.get();
        double viewDuration = Math.max(4.0D, config.cameraViewDurationSeconds);
        int viewIndex = (int) (elapsedSeconds / viewDuration);
        double viewTime = elapsedSeconds % viewDuration;
        
        if (viewIndex != lastCameraViewIndex || currentShot == null) {
            lastCameraViewIndex = viewIndex;
            previousShot = currentShot;
            shotStartTime = now;
            generateNewShot();
        }
        
        Vec3 posB = currentShot.getOffset(viewTime, viewDuration);
        Vec3 finalPos = target.add(posB);

        // Transition Blending
        double transitionTime = (now - shotStartTime) / 1000.0D;
        double blendDuration = 2.0D;
        if (previousShot != null && transitionTime < blendDuration) {
            double progress = smoothStep(transitionTime / blendDuration);
            Vec3 posA = previousShot.getOffset(viewTime + viewDuration, viewDuration);
            finalPos = target.add(posA.lerp(posB, progress));
        }

        // FOV Pulse / Dynamic Zoom
        if (config.cinematicZoomAmount > 0) {
            currentFovModifier = currentShot.getFovModifier(viewTime, viewDuration) * config.cinematicZoomAmount;
        } else {
            currentFovModifier = 0f;
        }

        return calculateFinalPose(player, target, finalPos, config, elapsedSeconds, partialTick);
    }

    private static void generateNewShot() {
        java.util.Random r = new java.util.Random();
        double distance = Math.max(3.0D, FMConfig.get().cameraDistance);
        double heightGoal = FMConfig.get().cameraHeight;
        long seed = r.nextLong();

        int archetype = r.nextInt(7);
        if (archetype == lastArchetype) {
            archetype = (archetype + 1) % 7;
        }
        lastArchetype = archetype;

        currentShot = switch (archetype) {
            case 0 -> new OrbitPreset(seed, distance, heightGoal, 0.5);
            case 1 -> new DriftPreset(seed, distance, heightGoal);
            case 2 -> new ZoomPreset(seed, distance, heightGoal);
            case 3 -> new SideShotPreset(seed, distance, heightGoal);
            case 4 -> new TopDownPreset(seed, distance);
            case 5 -> new EstablishingShotPreset(seed, distance, heightGoal);
            default -> new OrbitPreset(seed, distance * 2.0, heightGoal + 4.0, 0.2); // Majestic Far Orbit
        };
    }

    private static CameraPose calculateFinalPose(LocalPlayer player, Vec3 target, Vec3 pos, FMConfig config, double elapsedSeconds, float partialTick) {
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

        // --- Path Smoothing (Position) ---
        if (firstFrame || smoothedPosition == null || config.pathSmoothing <= 0) {
            smoothedPosition = pos;
        } else {
            // Apply frame-rate independent position smoothing
            float lerp = 1.0f - (float)Math.pow(1.0 - config.pathSmoothing, partialTick * 0.8);
            smoothedPosition = smoothedPosition.lerp(pos, lerp);
        }
        pos = smoothedPosition;

        // --- Final Pose Calculation (Smoothing) ---
        CameraPose targetPose = lookAt(pos, target);
        float pitchFinal = targetPose.pitch();
        float yawFinal = targetPose.yaw();
        
        if (firstFrame || config.lookAtSmoothing <= 0) {
            smoothedYaw = yawFinal;
            smoothedPitch = pitchFinal;
            firstFrame = false;
        } else {
            // Frame-rate stable smoothing factor for look-at
            float lerp = 1.0f - (float)Math.pow(1.0 - config.lookAtSmoothing, partialTick * 0.4);
            
            float yawDelta = (yawFinal - smoothedYaw) % 360.0f;
            if (yawDelta < -180.0f) yawDelta += 360.0f;
            if (yawDelta > 180.0f) yawDelta -= 360.0f;
            
            smoothedYaw += yawDelta * lerp;
            smoothedPitch = Mth.lerp(lerp, smoothedPitch, pitchFinal);
        }

        return new CameraPose(pos, smoothedYaw, smoothedPitch);
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

        double radius = 0.35D;
        Vec3[] hullPoints = {
            Vec3.ZERO,
            new Vec3(radius, 0, 0), new Vec3(-radius, 0, 0),
            new Vec3(0, radius, 0), new Vec3(0, -radius, 0),
            new Vec3(0, 0, radius), new Vec3(0, 0, -radius)
        };

        double minDistanceFound = desiredPosition.distanceTo(target);
        boolean hitOccurred = false;

        for (Vec3 offset : hullPoints) {
            HitResult hit = client.level.clip(new ClipContext(
                target.add(offset),
                desiredPosition.add(offset),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                double dist = hit.getLocation().distanceTo(target.add(offset));
                if (dist < minDistanceFound) {
                    minDistanceFound = dist;
                    hitOccurred = true;
                }
            }
        }

        if (!hitOccurred) {
            smoothedAvoidanceDistance = -1.0;
            return desiredPosition;
        }

        // --- Smart Adjustment ---
        // If the camera is pushed too close (< 1.5 blocks), try to move it UP
        double safeDistance = Math.max(0.1D, minDistanceFound - 0.1D);
        if (safeDistance < 1.5D) {
            // Try to find a higher vantage point
            Vec3 highTarget = target.add(0, 1.0, 0);
            Vec3 highPos = desiredPosition.add(0, 1.5, 0);
            HitResult highHit = client.level.clip(new ClipContext(highTarget, highPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (highHit.getType() == HitResult.Type.MISS) {
                desiredPosition = highPos;
                safeDistance = highPos.distanceTo(target);
            }
        }

        if (smoothedAvoidanceDistance < 0) {
            smoothedAvoidanceDistance = safeDistance;
        } else {
            smoothedAvoidanceDistance = Mth.lerp(0.15, smoothedAvoidanceDistance, safeDistance);
        }

        Vec3 awayFromPlayer = desiredPosition.subtract(target).normalize();
        return target.add(awayFromPlayer.scale(smoothedAvoidanceDistance));
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
