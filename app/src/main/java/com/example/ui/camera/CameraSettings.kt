package com.example.ui.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy

enum class FlashOption(val label: String) {
    OFF("OFF"),
    ON("ON"),
    AUTO("AUTO"),
    STAY_ON("STAY ON")
}

enum class CameraScanResolution(val label: String, val approxWidth: Int, val approxHeight: Int) {
    RES_12M("12M", 4000, 3000),
    RES_8M("8M", 3264, 2448),
    RES_5M("5M", 2560, 1920),
    RES_3M("3M", 2048, 1536)
}

enum class ScanCaptureMode {
    SINGLE,
    BATCH
}

enum class CameraOrientationMode {
    AUTO,
    PORTRAIT,
    LANDSCAPE
}

data class CameraPreferences(
    val flashOption: FlashOption = FlashOption.OFF,
    val resolution: CameraScanResolution = CameraScanResolution.RES_12M,
    val autoCapture: Boolean = false,
    val orientation: CameraOrientationMode = CameraOrientationMode.AUTO,
    val showGrid: Boolean = false,
    val shutterSound: Boolean = true,
    val spiritLevel: Boolean = false,
    val autoCrop: Boolean = true,
    val timerSeconds: Int = 0
)

class CameraSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("scanpro_camera_settings", Context.MODE_PRIVATE)

    fun load(): CameraPreferences {
        val flashName = prefs.getString("flash_option", FlashOption.OFF.name) ?: FlashOption.OFF.name
        val resName = prefs.getString("resolution", CameraScanResolution.RES_12M.name) ?: CameraScanResolution.RES_12M.name
        val orientName = prefs.getString("orientation", CameraOrientationMode.AUTO.name) ?: CameraOrientationMode.AUTO.name

        return CameraPreferences(
            flashOption = runCatching { FlashOption.valueOf(flashName) }.getOrDefault(FlashOption.OFF),
            resolution = runCatching { CameraScanResolution.valueOf(resName) }.getOrDefault(CameraScanResolution.RES_12M),
            autoCapture = prefs.getBoolean("auto_capture", false),
            orientation = runCatching { CameraOrientationMode.valueOf(orientName) }.getOrDefault(CameraOrientationMode.AUTO),
            showGrid = prefs.getBoolean("show_grid", false),
            shutterSound = prefs.getBoolean("shutter_sound", true),
            spiritLevel = prefs.getBoolean("spirit_level", false),
            autoCrop = prefs.getBoolean("auto_crop", true),
            timerSeconds = prefs.getInt("timer_seconds", 0)
        )
    }

    fun save(preferences: CameraPreferences) {
        prefs.edit()
            .putString("flash_option", preferences.flashOption.name)
            .putString("resolution", preferences.resolution.name)
            .putString("orientation", preferences.orientation.name)
            .putBoolean("auto_capture", preferences.autoCapture)
            .putBoolean("show_grid", preferences.showGrid)
            .putBoolean("shutter_sound", preferences.shutterSound)
            .putBoolean("spirit_level", preferences.spiritLevel)
            .putBoolean("auto_crop", preferences.autoCrop)
            .putInt("timer_seconds", preferences.timerSeconds)
            .apply()
    }

    fun buildResolutionSelector(resolution: CameraScanResolution): ResolutionSelector {
        val targetSize = Size(resolution.approxWidth, resolution.approxHeight)
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    targetSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }
}
