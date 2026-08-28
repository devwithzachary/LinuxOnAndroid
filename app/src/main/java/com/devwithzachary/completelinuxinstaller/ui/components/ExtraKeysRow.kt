package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExtraKeysRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit,
    onPaste: () -> Unit = {},
    isCtrlActive: Boolean = false,
    onToggleCtrl: () -> Unit = {},
    isAltActive: Boolean = false,
    onToggleAlt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF242424),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pinned CTRL Modifier Key
            ModifierKeyButton(
                label = "CTRL",
                isActive = isCtrlActive,
                onClick = onToggleCtrl
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 2. Pinned ALT Modifier Key
            ModifierKeyButton(
                label = "ALT",
                isActive = isAltActive,
                onClick = onToggleAlt
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 3. Pinned PASTE Action Key
            Button(
                onClick = onPaste,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A5F),
                    contentColor = Color(0xFF90CAF9)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    modifier = Modifier.size(13.dp),
                    tint = Color(0xFF90CAF9)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "PASTE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Subtle Vertical Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFF424242))
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 3. Scrollable Custom Hotkeys Ribbon
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(keys) { key ->
                    ElevatedButton(
                        onClick = { onKeyClick(key) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = key,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFF2E7D32) else Color(0xFF383838),
            contentColor = if (isActive) Color.White else Color(0xFFE0E0E0)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
