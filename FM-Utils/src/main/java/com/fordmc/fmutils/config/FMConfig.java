package com.fordmc.fmutils.config;

import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FMConfig {
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fm-utils.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int configVersion = 7;
    public boolean autoCinematicEnabled = true;
    public int afkTimeoutSeconds = 15;
    public float rotationSpeed = 0.12f;
    public boolean showStatusMsg = true;
    public boolean hideGuiDuringCinematic = true;
    public boolean playMusicDuringCinematic = true;
    public boolean cycleCameraViews = true;
    public boolean randomizeCinematics = true;
    public int cameraViewDurationSeconds = 6;
    public float cameraDistance = 5.5f;
    public float cameraHeight = 1.0f;
    public boolean autoAdjustCamera = false;
    public float cinematicZoomAmount = 0.3f;
    public float slowMotionScale = 0.6f;
    public boolean lockFps30 = true;
    public float cinematicMusicVolume = 0.5f;
    public boolean handheldCameraMode = true;
    public boolean cinematicBars = true;
    public float lookAtSmoothing = 0.15f;
    public float pathSmoothing = 0.1f;
    public float cloudShotFrequency = 0.15f;

    private static FMConfig instance;

    public static FMConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        boolean shouldSave = false;

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                instance = GSON.fromJson(json, FMConfig.class);

                if (json == null || !json.has("configVersion") || json.get("configVersion").getAsInt() < 7) {
                    migrateToVersion7();
                    shouldSave = true;
                }
            } catch (IOException | RuntimeException e) {
                instance = new FMConfig();
                shouldSave = true;
            }
        } else {
            instance = new FMConfig();
            shouldSave = true;
        }

        if (instance == null) {
            instance = new FMConfig();
            shouldSave = true;
        }

        if (validate()) {
            shouldSave = true;
        }

        if (shouldSave) {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrateToVersion7() {
        if (instance == null) {
            instance = new FMConfig();
        }

        instance.configVersion = 7;
        instance.pathSmoothing = 0.1f;
        
        // Ensure version 6 migrations also happen if needed
        if (instance.afkTimeoutSeconds == 0) instance.afkTimeoutSeconds = 15;
        if (instance.cameraDistance == 0) instance.cameraDistance = 5.5f;
    }

    private static boolean validate() {
        boolean changed = false;

        changed |= setConfigVersion(6);
        changed |= setAfkTimeoutSeconds(clamp(instance.afkTimeoutSeconds, 5, 600));
        changed |= setRotationSpeed(clamp(instance.rotationSpeed, 0.03f, 0.6f));
        changed |= setCameraViewDurationSeconds(clamp(instance.cameraViewDurationSeconds, 4, 20));
        changed |= setCameraDistance(clamp(instance.cameraDistance, 3.0f, 12.0f));
        changed |= setCameraHeight(clamp(instance.cameraHeight, -1.0f, 5.0f));
        changed |= setCinematicMusicVolume(clamp(instance.cinematicMusicVolume, 0.0f, 1.0f));
        
        instance.cinematicZoomAmount = clamp(instance.cinematicZoomAmount, 0.0f, 1.0f);
        instance.slowMotionScale = clamp(instance.slowMotionScale, 0.2f, 1.0f);
        instance.lookAtSmoothing = clamp(instance.lookAtSmoothing, 0.0f, 1.0f);
        instance.pathSmoothing = clamp(instance.pathSmoothing, 0.0f, 1.0f);
        instance.cloudShotFrequency = clamp(instance.cloudShotFrequency, 0.0f, 1.0f);

        return changed;
    }

    private static boolean setConfigVersion(int value) {
        if (instance.configVersion == value) {
            return false;
        }

        instance.configVersion = value;
        return true;
    }

    private static boolean setAfkTimeoutSeconds(int value) {
        if (instance.afkTimeoutSeconds == value) {
            return false;
        }

        instance.afkTimeoutSeconds = value;
        return true;
    }

    private static boolean setRotationSpeed(float value) {
        if (instance.rotationSpeed == value) {
            return false;
        }

        instance.rotationSpeed = value;
        return true;
    }

    private static boolean setCameraViewDurationSeconds(int value) {
        if (instance.cameraViewDurationSeconds == value) {
            return false;
        }

        instance.cameraViewDurationSeconds = value;
        return true;
    }

    private static boolean setCameraDistance(float value) {
        if (instance.cameraDistance == value) {
            return false;
        }

        instance.cameraDistance = value;
        return true;
    }

    private static boolean setCameraHeight(float value) {
        if (instance.cameraHeight == value) {
            return false;
        }

        instance.cameraHeight = value;
        return true;
    }

    private static boolean setCinematicMusicVolume(float value) {
        if (instance.cinematicMusicVolume == value) {
            return false;
        }

        instance.cinematicMusicVolume = value;
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }
}
