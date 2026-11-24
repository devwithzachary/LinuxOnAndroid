package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val FfmpegMediaRecipe = SoftwareRecipe(
    id = "ffmpeg_media",
    name = "FFmpeg",
    description = "Complete cross-platform audio and video conversion suite",
    category = SoftwareCategory.CREATIVE,
    iconName = "Tune",
    expectedBinaries = listOf("usr/bin/ffmpeg"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("ffmpeg"),
        PackageManagerType.DNF to listOf("ffmpeg-free"),
        PackageManagerType.APK to listOf("ffmpeg"),
        PackageManagerType.PACMAN to listOf("ffmpeg"),
        PackageManagerType.XBPS to listOf("ffmpeg")
    ),
    postInstallNotes = "Run 'ffmpeg -version' to verify codecs and configuration."
)

