package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.theme.CyberGlyphs
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalFonts
import com.devwithzachary.completelinuxinstaller.ui.theme.AppThemeMode
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalAppearanceCard(
    appThemeMode: AppThemeMode,
    onSetAppThemeMode: (AppThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onSetUseDynamicColor: (Boolean) -> Unit,
    terminalTheme: TerminalTheme,
    terminalFontSize: Int,
    terminalFontFamily: String,
    onSelectTheme: (String) -> Unit,
    onUpdateCustomTheme: (Color, Color, Color, Color, List<Color>) -> Unit,
    onSetTerminalFontSize: (Int) -> Unit,
    onSetTerminalFontFamily: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInspection = LocalInspectionMode.current
    val supportsDynamicColor = isInspection || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var showCustomThemeSheet by remember { mutableStateOf(false) }
    var editingColorTarget by remember { mutableStateOf<String?>(null) }
    var colorHexInput by remember { mutableStateOf("") }

    var customFg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultFg) }
    var customBg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultBg) }
    var customCursor by remember(terminalTheme) { mutableStateOf(terminalTheme.cursorColor) }
    var customSelection by remember(terminalTheme) { mutableStateOf(terminalTheme.selectionColor) }
    var customAnsiColors by remember(terminalTheme) { mutableStateOf(terminalTheme.ansiColors) }

    val allThemes = remember(customFg, customBg, customCursor, customSelection, customAnsiColors) {
        TerminalTheme.PRESETS + listOf(
            TerminalTheme(
                id = "custom",
                name = "Custom",
                defaultFg = customFg,
                defaultBg = customBg,
                cursorColor = customCursor,
                selectionColor = customSelection,
                ansiColors = customAnsiColors
            )
        )
    }

    val previewFontFamily = TerminalFonts.getComposeFontFamily(terminalFontFamily)
    fun toPreviewText(text: String): String {
        return if (terminalFontFamily == "CyberGlyphs") CyberGlyphs.transformText(text) else text
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = "Appearance")

        LoaSegmentedGroup {
            // 1. App Theme Mode (System / Light / Dark)
            item { shape ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Choose system, light, or dark appearance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AppThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = appThemeMode == mode,
                                    onClick = { onSetAppThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AppThemeMode.entries.size
                                    ),
                                    label = {
                                        Text(
                                            text = mode.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Dynamic Color Toggle
            row(
                title = "Dynamic Color",
                subtitle = if (supportsDynamicColor) {
                    if (useDynamicColor) "Material You wallpaper palette" else "LinuxOnAndroid Classic palette"
                } else {
                    "Requires Android 12 or higher"
                },
                leadingIcon = Icons.Rounded.ColorLens,
                enabled = supportsDynamicColor,
                trailingContent = {
                    Switch(
                        checked = useDynamicColor && supportsDynamicColor,
                        onCheckedChange = onSetUseDynamicColor,
                        enabled = supportsDynamicColor
                    )
                },
                onClick = {
                    if (supportsDynamicColor) {
                        onSetUseDynamicColor(!useDynamicColor)
                    }
                }
            )

            // 3. Terminal Theme Presets & Live Preview
            item { shape ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terminal Theme",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            LoaMonospaceChip(text = terminalTheme.name)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(allThemes) { theme ->
                                val isSelected = terminalTheme.id == theme.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (theme.id == "custom" && isSelected) {
                                            showCustomThemeSheet = true
                                        } else {
                                            onSelectTheme(theme.id)
                                        }
                                    },
                                    label = { Text(theme.name) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(theme.defaultBg)
                                                .border(1.dp, theme.defaultFg, CircleShape)
                                        )
                                    },
                                    trailingIcon = if (theme.id == "custom") {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit custom theme",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = terminalTheme.defaultBg,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = toPreviewText("user@localhost:~$ uname -a"),
                                    color = terminalTheme.defaultFg,
                                    fontFamily = previewFontFamily,
                                    fontSize = terminalFontSize.sp
                                )
                                Text(
                                    text = toPreviewText("Linux localhost 6.1.0-android-proot #1 SMP PREEMPT"),
                                    color = terminalTheme.ansiColors[2],
                                    fontFamily = previewFontFamily,
                                    fontSize = terminalFontSize.sp
                                )
                                Text(
                                    text = toPreviewText("user@localhost:~$ cat /etc/issue"),
                                    color = terminalTheme.defaultFg,
                                    fontFamily = previewFontFamily,
                                    fontSize = terminalFontSize.sp
                                )
                                Text(
                                    text = toPreviewText("LinuxOnAndroid Environment \\n \\l"),
                                    color = terminalTheme.ansiColors[4],
                                    fontFamily = previewFontFamily,
                                    fontSize = terminalFontSize.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Terminal Font Size Slider
            item { shape ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terminal Font Size",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            LoaMonospaceChip(text = "${terminalFontSize}sp")
                        }

                        Slider(
                            value = terminalFontSize.toFloat(),
                            onValueChange = { onSetTerminalFontSize(it.toInt()) },
                            valueRange = 10f..24f,
                            steps = 13,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "10sp",
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "14sp",
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "24sp",
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Terminal Font Family
            item { shape ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Terminal Font Family",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TerminalFonts.AVAILABLE_FONTS) { family ->
                                val isSelected = terminalFontFamily == family
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetTerminalFontFamily(family) },
                                    label = { Text(family) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // 6. Customize Terminal Palette
            row(
                title = "Customize Terminal Palette",
                subtitle = "Edit foreground, background, cursor, and ANSI 16 colors",
                leadingIcon = Icons.Rounded.Tune,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { showCustomThemeSheet = true }
            )
        }
    }

    if (showCustomThemeSheet) {
        val ansiLabels = remember {
            listOf(
                "0: Black", "1: Red", "2: Green", "3: Yellow",
                "4: Blue", "5: Magenta", "6: Cyan", "7: White",
                "8: Bright Black", "9: Bright Red", "10: Bright Green", "11: Bright Yellow",
                "12: Bright Blue", "13: Bright Magenta", "14: Bright Cyan", "15: Bright White"
            )
        }

        LoaBottomSheet(
            onDismissRequest = { showCustomThemeSheet = false },
            title = "Custom Terminal Palette",
            subtitle = "Customize foreground, background, cursor, and ANSI 16 colors"
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = "Base Colors",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    ColorSwatchPickerRow(title = "Default Foreground", color = customFg) {
                        editingColorTarget = "fg"
                        colorHexInput = TerminalTheme.colorToHex(customFg)
                    }
                }
                item {
                    ColorSwatchPickerRow(title = "Default Background", color = customBg) {
                        editingColorTarget = "bg"
                        colorHexInput = TerminalTheme.colorToHex(customBg)
                    }
                }
                item {
                    ColorSwatchPickerRow(title = "Cursor Color", color = customCursor) {
                        editingColorTarget = "cursor"
                        colorHexInput = TerminalTheme.colorToHex(customCursor)
                    }
                }
                item {
                    ColorSwatchPickerRow(title = "Selection Highlight", color = customSelection) {
                        editingColorTarget = "selection"
                        colorHexInput = TerminalTheme.colorToHex(customSelection)
                    }
                }
                item {
                    Text(
                        text = "ANSI 16 Palette",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(ansiLabels.indices.toList()) { idx ->
                    val color = customAnsiColors.getOrElse(idx) { MaterialTheme.colorScheme.onSurface }
                    ColorSwatchPickerRow(title = ansiLabels[idx], color = color) {
                        editingColorTarget = "ansi_$idx"
                        colorHexInput = TerminalTheme.colorToHex(color)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showCustomThemeSheet = false }) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onUpdateCustomTheme(customFg, customBg, customCursor, customSelection, customAnsiColors)
                        onSelectTheme("custom")
                        showCustomThemeSheet = false
                    }
                ) {
                    Text("Save Theme")
                }
            }
        }
    }

    if (editingColorTarget != null) {
        val swatches = remember {
            listOf(
                Color(0xFF000000), Color(0xFF1E1E1E), Color(0xFF282A36), Color(0xFF002B36),
                Color(0xFFFFFFFF), Color(0xFFF8F8F2), Color(0xFF839496), Color(0xFF50FA7B),
                Color(0xFFFF5555), Color(0xFFBD93F9), Color(0xFF8BE9FD), Color(0xFFFFB86C),
                Color(0xFFF1FA8C), Color(0xFF6272A4), Color(0xFF2AA198), Color(0xFF268BD2)
            )
        }

        LoaBottomSheet(
            onDismissRequest = { editingColorTarget = null },
            title = "Edit Hex Color"
        ) {
            OutlinedTextField(
                value = colorHexInput,
                onValueChange = { colorHexInput = it },
                label = { Text("Hex Code (e.g. #FF0055)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val previewColor = TerminalTheme.hexToColor(colorHexInput, Color.Transparent)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Preview", style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(swatches) { swatch ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { colorHexInput = TerminalTheme.colorToHex(swatch) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { editingColorTarget = null }) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsed = TerminalTheme.hexToColor(colorHexInput, customFg)
                        when (val target = editingColorTarget) {
                            "fg" -> customFg = parsed
                            "bg" -> customBg = parsed
                            "cursor" -> customCursor = parsed
                            "selection" -> customSelection = parsed
                            else -> {
                                if (target != null && target.startsWith("ansi_")) {
                                    val idx = target.removePrefix("ansi_").toIntOrNull() ?: 0
                                    if (idx in customAnsiColors.indices) {
                                        customAnsiColors = customAnsiColors.toMutableList().also { it[idx] = parsed }
                                    }
                                }
                            }
                        }
                        editingColorTarget = null
                    }
                ) {
                    Text("Apply Color")
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchPickerRow(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = TerminalTheme.colorToHex(color),
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
        }
    }
}
