package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.components.SoftwareCard

@Composable
fun SoftwareTabContent(
    container: ContainerInstance,
    packages: List<SoftwarePackage>,
    onInstallPackage: (packageId: String) -> Unit,
    onInstallCustomPackage: (packageName: String) -> Unit,
    onViewLogs: (packageId: String) -> Unit,
    onRunPresetCommand: (command: String) -> Unit
) {
    var selectedSoftwareCategory by remember { mutableStateOf<SoftwareCategory?>(null) }
    var customPackageInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredPackages = packages.filter { pkg ->
        selectedSoftwareCategory == null || pkg.category == selectedSoftwareCategory
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Custom Package Quick Search & Install Card (Full Width Span)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Install Custom Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Enter any package name to download and configure directly into ${container.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPackageInput,
                            onValueChange = { customPackageInput = it },
                            placeholder = { Text("e.g. htop, neofetch, git, vim", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (customPackageInput.isNotBlank()) {
                                    onInstallCustomPackage(customPackageInput.trim())
                                    keyboardController?.hide()
                                    customPackageInput = ""
                                }
                            })
                        )
                        Button(
                            onClick = {
                                if (customPackageInput.isNotBlank()) {
                                    onInstallCustomPackage(customPackageInput.trim())
                                    keyboardController?.hide()
                                    customPackageInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Install")
                        }
                    }
                }
            }
        }

        // 2. Category Filter Tabs (Full Width Span)
        item(span = { GridItemSpan(maxLineSpan) }) {
            SecondaryScrollableTabRow(
                selectedTabIndex = if (selectedSoftwareCategory == null) 0 else SoftwareCategory.entries.indexOf(selectedSoftwareCategory) + 1,
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedSoftwareCategory == null,
                    onClick = { selectedSoftwareCategory = null },
                    text = { Text("All (${packages.size})") }
                )
                SoftwareCategory.entries.forEach { cat ->
                    Tab(
                        selected = selectedSoftwareCategory == cat,
                        onClick = { selectedSoftwareCategory = cat },
                        text = { Text(cat.displayName) }
                    )
                }
            }
        }

        // 3. Preset Software Cards (Adaptive Columns)
        items(filteredPackages, key = { it.id }) { pkg ->
            SoftwareCard(
                pkg = pkg,
                onInstallClick = { onInstallPackage(pkg.id) },
                onViewLogsClick = { onViewLogs(pkg.id) },
                onLaunchClick = { cmd -> onRunPresetCommand(cmd) },
                onUpgradeClick = { onInstallPackage(pkg.id) }
            )
        }
    }
}
