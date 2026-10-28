package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.theme.CyberGlyphs
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.CollapsibleSettingsCard
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalFonts

@Composable
fun TerminalAppearanceCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    terminalTheme: TerminalTheme,
    terminalFontSize: Int,
    terminalFontFamily: String,
    onSelectTheme: (String) -> Unit,
    onUpdateCustomTheme: (Color, Color, Color, Color, List<Color>) -> Unit,
    onSetTerminalFontSize: (Int) -> Unit,
    onSetTerminalFontFamily: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var editingColorTarget by remember { mutableStateOf<String?>(null) }
    var colorHexInput by remember { mutableStateOf("") }

    var customFg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultFg) }
    var customBg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultBg) }
    var customCursor by remember(terminalTheme) { mutableStateOf(terminalTheme.cursorColor) }
    var customSelection by remember(terminalTheme) { mutableStateOf(terminalTheme.selectionColor) }
    var customAnsiColors by remember(terminalTheme) { mutableStateOf(terminalTheme.ansiColors) }

    CollapsibleSettingsCard(
        title = "Terminal Appearance & Theme",
        subtitle = "Theme: ${terminalTheme.name} (${terminalFontSize}sp, $terminalFontFamily)",
        icon = Icons.Default.Palette,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier
    ) {
        Text(
            text = "Choose from standard color themes or create your own custom ANSI palette.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Horizontal Preset Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            val allThemes = TerminalTheme.PRESETS + listOf(
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

            items(allThemes) { theme ->
                val isSelected = terminalTheme.id == theme.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (theme.id == "custom" && isSelected) {
                            showCustomThemeDialog = true
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
                            IconButton(
                                onClick = { showCustomThemeDialog = true },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Custom Theme",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        HorizontalDivider()

        // Font Size Slider Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Terminal Font Size", fontWeight = FontWeight.SemiBold)
                Text(
                    "${terminalFontSize} sp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    "10sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "14sp (Default)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "24sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Font Family Selector Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Terminal Font Family", fontWeight = FontWeight.SemiBold)
            val fontFamilies = TerminalFonts.AVAILABLE_FONTS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(fontFamilies) { family ->
                    val isSelected = terminalFontFamily == family
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetTerminalFontFamily(family) },
                        label = { Text(family) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        HorizontalDivider()

        // Theme Quick Preview Box
        val previewFontFamily = TerminalFonts.getComposeFontFamily(terminalFontFamily)
        val previewFontWeight = FontWeight.Normal

        fun toPreviewText(text: String): String {
            return if (terminalFontFamily == "CyberGlyphs") {
                CyberGlyphs.transformText(text)
            } else text
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = terminalTheme.defaultBg
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = toPreviewText("user@localhost:~$ uname -a"),
                    color = terminalTheme.defaultFg,
                    fontFamily = previewFontFamily,
                    fontWeight = previewFontWeight,
                    fontSize = terminalFontSize.sp
                )
                Text(
                    text = toPreviewText("Linux localhost 6.1.0-android-proot #1 SMP PREEMPT"),
                    color = terminalTheme.ansiColors[2],
                    fontFamily = previewFontFamily,
                    fontWeight = previewFontWeight,
                    fontSize = terminalFontSize.sp
                )
                Text(
                    text = toPreviewText("user@localhost:~$ cat /etc/issue"),
                    color = terminalTheme.defaultFg,
                    fontFamily = previewFontFamily,
                    fontWeight = previewFontWeight,
                    fontSize = terminalFontSize.sp
                )
                Text(
                    text = toPreviewText("LinuxOnAndroid Environment \\n \\l"),
                    color = terminalTheme.ansiColors[4],
                    fontFamily = previewFontFamily,
                    fontWeight = previewFontWeight,
                    fontSize = terminalFontSize.sp
                )
            }
        }

        // Custom Theme Creator / Editor Expand Button
        OutlinedButton(
            onClick = { showCustomThemeDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Palette, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Customize Palette & Colors")
        }
    }

    // Custom Theme Editor Modal Dialog
    if (showCustomThemeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomThemeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Custom Theme Creator")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Customize foreground, background, cursor, and ANSI 16 color palette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Base Interface Colors",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    ColorSwatchPickerRow(title = "Default Foreground (Text)", color = customFg) {
                        editingColorTarget = "fg"
                        colorHexInput = TerminalTheme.colorToHex(customFg)
                    }
                    ColorSwatchPickerRow(title = "Default Background", color = customBg) {
                        editingColorTarget = "bg"
                        colorHexInput = TerminalTheme.colorToHex(customBg)
                    }
                    ColorSwatchPickerRow(title = "Cursor Color", color = customCursor) {
                        editingColorTarget = "cursor"
                        colorHexInput = TerminalTheme.colorToHex(customCursor)
                    }
                    ColorSwatchPickerRow(title = "Selection Highlight", color = customSelection) {
                        editingColorTarget = "selection"
                        colorHexInput = TerminalTheme.colorToHex(customSelection)
                    }

                    HorizontalDivider()

                    Text(
                        "ANSI 16 Color Palette",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val ansiLabels = listOf(
                        "0: Black", "1: Red", "2: Green", "3: Yellow",
                        "4: Blue", "5: Magenta", "6: Cyan", "7: White",
                        "8: Bright Black", "9: Bright Red", "10: Bright Green", "11: Bright Yellow",
                        "12: Bright Blue", "13: Bright Magenta", "14: Bright Cyan", "15: Bright White"
                    )

                    ansiLabels.forEachIndexed { idx, label ->
                        val color = customAnsiColors.getOrElse(idx) { Color.Gray }
                        ColorSwatchPickerRow(title = label, color = color) {
                            editingColorTarget = "ansi_$idx"
                            colorHexInput = TerminalTheme.colorToHex(color)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCustomTheme(customFg, customBg, customCursor, customSelection, customAnsiColors)
                        onSelectTheme("custom")
                        showCustomThemeDialog = false
                    }
                ) {
                    Text("Save Theme")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Color Hex Edit Dialog
    if (editingColorTarget != null) {
        AlertDialog(
            onDismissRequest = { editingColorTarget = null },
            title = { Text("Edit Hex Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Preview:", fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(previewColor)
                                .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                        )
                    }

                    Text("Quick Swatches", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    val swatches = listOf(
                        Color(0xFF000000), Color(0xFF1E1E1E), Color(0xFF282A36), Color(0xFF002B36),
                        Color(0xFFFFFFFF), Color(0xFFF8F8F2), Color(0xFF839496), Color(0xFF50FA7B),
                        Color(0xFFFF5555), Color(0xFFBD93F9), Color(0xFF8BE9FD), Color(0xFFFFB86C),
                        Color(0xFFF1FA8C), Color(0xFF6272A4), Color(0xFF2AA198), Color(0xFF268BD2)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(swatches) { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .clickable { colorHexInput = TerminalTheme.colorToHex(swatch) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = TerminalTheme.hexToColor(colorHexInput, Color.White)
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
            },
            dismissButton = {
                TextButton(onClick = { editingColorTarget = null }) {
                    Text("Cancel")
                }
            }
        )
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
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = TerminalTheme.colorToHex(color),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
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
