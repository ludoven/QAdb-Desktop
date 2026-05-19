package com.ludoven.adbtool.entity

data class DeviceMirrorSettings(
    val launchProfile: MirrorLaunchProfile = MirrorLaunchProfile.SMOOTH,
    val alwaysOnTop: Boolean = true,
    val fullscreen: Boolean = false,
    val borderless: Boolean = false,
    val maxSize: Int? = 1280,
    val maxFps: Int? = 60,
    val videoBitRate: String = "8M",
    val audioEnabled: Boolean = true,
    val showTouches: Boolean = false,
    val stayAwake: Boolean = true,
    val turnScreenOffOnStart: Boolean = false,
    val powerOffOnClose: Boolean = false
) {
    fun applyProfile(profile: MirrorLaunchProfile): DeviceMirrorSettings {
        return when (profile) {
            MirrorLaunchProfile.SMOOTH -> copy(
                launchProfile = profile,
                maxSize = 1280,
                maxFps = 60,
                videoBitRate = "8M",
                audioEnabled = true
            )
            MirrorLaunchProfile.CLEAR -> copy(
                launchProfile = profile,
                maxSize = 1920,
                maxFps = 60,
                videoBitRate = "16M",
                audioEnabled = true
            )
            MirrorLaunchProfile.LOW_LATENCY -> copy(
                launchProfile = profile,
                maxSize = 1280,
                maxFps = 60,
                videoBitRate = "4M",
                audioEnabled = true
            )
            MirrorLaunchProfile.CUSTOM -> copy(launchProfile = profile)
        }
    }

    fun toScrcpyArgs(): List<String> {
        val args = mutableListOf<String>()
        if (alwaysOnTop) args += "--always-on-top"
        if (fullscreen) args += "--fullscreen"
        if (borderless) args += "--window-borderless"
        maxSize?.takeIf { it > 0 }?.let { args += listOf("--max-size", it.toString()) }
        maxFps?.takeIf { it > 0 }?.let { args += listOf("--max-fps", it.toString()) }
        videoBitRate.trim().takeIf { it.isNotEmpty() }?.let { args += listOf("--video-bit-rate", it) }
        if (!audioEnabled) args += "--no-audio"
        if (showTouches) args += "--show-touches"
        if (stayAwake) args += "--stay-awake"
        if (turnScreenOffOnStart) args += "--turn-screen-off"
        if (powerOffOnClose) args += "--power-off-on-close"
        return args
    }
}

enum class MirrorLaunchProfile {
    SMOOTH,
    CLEAR,
    LOW_LATENCY,
    CUSTOM
}
