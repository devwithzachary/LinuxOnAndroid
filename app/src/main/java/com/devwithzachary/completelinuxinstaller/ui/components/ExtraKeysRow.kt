package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private const val GRID_COLUMNS = 7

@Composable
fun ExtraKeysRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPaste: () -> Unit = {},
    isCtrlActive: Boolean = false,
    onToggleCtrl: () -> Unit = {},
    isAltActive: Boolean = false,
    onToggleAlt: () -> Unit = {}
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }

    // Primary row pinned keys that are handled in the top fixed grid
    val primaryPinnedSet = remember {
        setOf("esc", "tab", "▲", "▼", "ctrl", "alt", "paste")
    }

    // Secondary keys filtered from custom list to avoid duplication
    val secondaryKeys = remember(keys) {
        val filtered = keys.filter { it.lowercase() !in primaryPinnedSet }
        if (filtered.isEmpty()) {
            listOf("◄", "►", "/", "-", "_", "~", ":", "Ctrl+C", "Ctrl+Z", "Ctrl+D", "|", "clear", "htop", "df -h")
        } else {
            filtered
        }
    }

    val pageCount = remember(secondaryKeys) {
        maxOf(1, ceil(secondaryKeys.size.toFloat() / GRID_COLUMNS).toInt())
    }
    val pagerState = rememberPagerState { pageCount }

    Surface(
        color = Color(0xFF212121),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Row 1: Fixed Termux-style primary control row (7 equal-width columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: ESC
                TermuxKeyCell(
                    label = "ESC",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("Esc") }
                )

                // Column 2: TAB
                TermuxKeyCell(
                    label = "TAB",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("Tab") }
                )

                // Column 3: CTRL (Active highlight)
                TermuxKeyCell(
                    label = "CTRL",
                    isActive = isCtrlActive,
                    activeContainerColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = onToggleCtrl
                )

                // Column 4: ALT (Active highlight)
                TermuxKeyCell(
                    label = "ALT",
                    isActive = isAltActive,
                    activeContainerColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = onToggleAlt
                )

                // Column 5: PASTE
                TermuxKeyCell(
                    label = "PASTE",
                    containerColor = Color(0xFF1E3A5F),
                    contentColor = Color(0xFF90CAF9),
                    modifier = Modifier.weight(1f),
                    onClick = onPaste
                )

                // Column 6: UP ARROW
                TermuxKeyCell(
                    label = "▲",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("▲") }
                )

                // Column 7: DOWN ARROW
                TermuxKeyCell(
                    label = "▼",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("▼") }
                )
            }

            // Row 2: Secondary custom hotkeys aligned to the same 7-column grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val startIndex = page * GRID_COLUMNS
                        val pageKeys = secondaryKeys.drop(startIndex).take(GRID_COLUMNS)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until GRID_COLUMNS) {
                                if (i < pageKeys.size) {
                                    val key = pageKeys[i]
                                    TermuxKeyCell(
                                        label = key,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onKeyClick(key) }
                                    )
                                } else {
                                    // Preserve equal column spacing when page has fewer than 7 keys
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Page indicator dots for secondary row pagination
                    if (pageCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp)
                        ) {
                            repeat(pageCount) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (isSelected) 5.dp else 3.5.dp)
                                        .background(
                                            if (isSelected) Color(0xFF90CAF9) else Color(0xFF555555),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxKeyCell(
    label: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    containerColor: Color = Color(0xFF333333),
    contentColor: Color = Color(0xFFE0E0E0),
    activeContainerColor: Color = Color(0xFF2E7D32),
    activeContentColor: Color = Color.White,
    onClick: () -> Unit
) {
    val bg = if (isActive) activeContainerColor else containerColor
    val fg = if (isActive) activeContentColor else contentColor

    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(5.dp),
        color = bg,
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = fg,
                fontSize = if (label.length > 5) 10.sp else 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
