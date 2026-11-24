package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
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
    val isExpanded by rememberSaveable { mutableStateOf(true) }

    val primaryPinnedSet = remember {
        setOf("esc", "tab", "▲", "▼", "ctrl", "alt", "paste")
    }

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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TermuxKeyCell(
                    label = "ESC",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("Esc") }
                )

                TermuxKeyCell(
                    label = "TAB",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("Tab") }
                )

                TermuxKeyCell(
                    label = "CTRL",
                    isActive = isCtrlActive,
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleCtrl
                )

                TermuxKeyCell(
                    label = "ALT",
                    isActive = isAltActive,
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleAlt
                )

                TermuxKeyCell(
                    label = "PASTE",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onPaste
                )

                TermuxKeyCell(
                    label = "▲",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("▲") }
                )

                TermuxKeyCell(
                    label = "▼",
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick("▼") }
                )
            }

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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (pageCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            repeat(pageCount) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (isSelected) 6.dp else 4.dp)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    activeContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    activeContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit
) {
    val bg = if (isActive) activeContainerColor else containerColor
    val fg = if (isActive) activeContentColor else contentColor

    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(6.dp),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = fg,
                style = LoaMonoTextStyle.copy(
                    fontSize = if (label.length > 5) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
