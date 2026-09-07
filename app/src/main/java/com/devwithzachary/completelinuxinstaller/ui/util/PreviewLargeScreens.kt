package com.devwithzachary.completelinuxinstaller.ui.util

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation representing various device form factors:
 * - Phone Portrait (Compact)
 * - 7-inch Tablet Portrait (Medium)
 * - 10-inch Tablet Landscape (Expanded)
 * - Desktop Freeform Window (Expanded)
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Phone (Compact)", device = "spec:width=411dp,height=891dp,dpi=420")
@Preview(name = "7-inch Tablet Portrait (Medium)", device = "spec:width=600dp,height=1024dp,dpi=213")
@Preview(name = "10-inch Tablet Landscape (Expanded)", device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(name = "Desktop Freeform Window", device = "spec:width=1000dp,height=700dp,dpi=160")
annotation class PreviewLargeScreens
