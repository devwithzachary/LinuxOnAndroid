package com.devwithzachary.completelinuxinstaller.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LoaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LoaSegmentedSingleShape = RoundedCornerShape(20.dp)

val LoaSegmentedTopShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp
)

val LoaSegmentedMiddleShape = RoundedCornerShape(4.dp)

val LoaSegmentedBottomShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 20.dp,
    bottomEnd = 20.dp
)

val LoaHeroShape = RoundedCornerShape(28.dp)

val LoaSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp
)
