package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.components.SoftwareCard
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.designsystem.loaSegmentedPositionFor
import com.devwithzachary.completelinuxinstaller.ui.designsystem.loaSegmentedShape
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

private enum class SoftwareStatusFilter(val label: String) {
    ALL("All"),
    INSTALLED("Installed"),
    AVAILABLE("Available")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SoftwareTabContent(
    container: ContainerInstance,
    packages: List<SoftwarePackage>,
    onInstallPackage: (packageId: String) -> Unit,
    onUninstallPackage: (packageId: String) -> Unit = {},
    onInstallCustomPackage: (packageName: String) -> Unit,
    onViewLogs: (packageId: String) -> Unit,
    onRunPresetCommand: (command: String) -> Unit
) {
    var selectedSoftwareCategory by remember { mutableStateOf<SoftwareCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(SoftwareStatusFilter.ALL) }
    var showCustomPackageSheet by remember { mutableStateOf(false) }
    var pendingUninstallPackage by remember { mutableStateOf<SoftwarePackage?>(null) }
    val uriHandler = LocalUriHandler.current
    val githubIssuesUrl = stringResource(R.string.github_issues_url)
    val discordUrl = stringResource(R.string.discord_url)

    pendingUninstallPackage?.let { pkgToUninstall ->
        AlertDialog(
            onDismissRequest = { pendingUninstallPackage = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Uninstall ${pkgToUninstall.name}?") },
            text = {
                Text("This will stop any running ${pkgToUninstall.name} service and remove its packages from ${container.name}. Your files in /home and /sdcard will not be touched.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUninstallPackage(pkgToUninstall.id)
                        pendingUninstallPackage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstallPackage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomPackageSheet) {
        CustomPackageSheet(
            containerName = container.name,
            onDismiss = { showCustomPackageSheet = false },
            onInstall = { pkgName ->
                onInstallCustomPackage(pkgName)
                showCustomPackageSheet = false
            }
        )
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredPackages = remember(packages, selectedSoftwareCategory, statusFilter, normalizedQuery) {
        packages.filter { pkg ->
            val matchesCategory = selectedSoftwareCategory == null || pkg.category == selectedSoftwareCategory
            val matchesStatus = when (statusFilter) {
                SoftwareStatusFilter.ALL -> true
                SoftwareStatusFilter.INSTALLED -> pkg.status == InstallStatus.INSTALLED
                SoftwareStatusFilter.AVAILABLE -> pkg.status != InstallStatus.INSTALLED
            }
            val matchesQuery = normalizedQuery.isEmpty() ||
                pkg.name.lowercase().contains(normalizedQuery) ||
                pkg.description.lowercase().contains(normalizedQuery) ||
                pkg.id.lowercase().contains(normalizedQuery)
            matchesCategory && matchesStatus && matchesQuery
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item(key = "software_controls") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search software catalog...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        val installedCount = packages.count { it.status == InstallStatus.INSTALLED }
                        val availableCount = packages.size - installedCount
                        SoftwareStatusFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                SoftwareStatusFilter.ALL -> packages.size
                                SoftwareStatusFilter.INSTALLED -> installedCount
                                SoftwareStatusFilter.AVAILABLE -> availableCount
                            }
                            FilterChip(
                                selected = statusFilter == filter,
                                onClick = { statusFilter = filter },
                                label = { Text("${filter.label} ($count)") }
                            )
                        }

                        FilledTonalButton(
                            onClick = { showCustomPackageSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Custom package")
                        }
                    }

                    SecondaryScrollableTabRow(
                        selectedTabIndex = if (selectedSoftwareCategory == null) {
                            0
                        } else {
                            SoftwareCategory.entries.indexOf(selectedSoftwareCategory) + 1
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredPackages.any { it.isBeta }) {
                item(key = "beta_notice") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = LoaSegmentedSingleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LoaStatusBadge(
                                    label = "Beta",
                                    tone = LoaStatusTone.Warning
                                )
                                Text(
                                    text = "Desktop GUI Applications",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "GUI apps use software rendering until hardware GPU support is added. Report any visual or launch issues on GitHub or Discord.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.widthIn(max = 560.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { uriHandler.openUri(githubIssuesUrl) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Report Issue")
                                }
                                TextButton(onClick = { uriHandler.openUri(discordUrl) }) {
                                    Text("Join Discord")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (filteredPackages.isEmpty()) {
                item(key = "empty_software") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = LoaSegmentedSingleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No matching packages found",
                                style = MaterialTheme.typography.titleSmall
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    statusFilter = SoftwareStatusFilter.ALL
                                    selectedSoftwareCategory = null
                                }
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filteredPackages, key = { _, pkg -> pkg.id }) { index, pkg ->
                    val shape = loaSegmentedShape(
                        loaSegmentedPositionFor(index, filteredPackages.size)
                    )
                    SoftwareCard(
                        pkg = pkg,
                        onInstallClick = { onInstallPackage(pkg.id) },
                        onUninstallClick = { pendingUninstallPackage = pkg },
                        onViewLogsClick = { onViewLogs(pkg.id) },
                        onLaunchClick = { cmd -> onRunPresetCommand(cmd) },
                        onUpgradeClick = { onInstallPackage(pkg.id) },
                        shape = shape
                    )
                    if (index < filteredPackages.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPackageSheet(
    containerName: String,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    var customPackageInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "Install Custom Package",
        subtitle = "Download and configure any package in $containerName"
    ) {
        OutlinedTextField(
            value = customPackageInput,
            onValueChange = { customPackageInput = it },
            label = { Text("Package name") },
            placeholder = { Text("e.g. htop, neofetch, git, vim") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val trimmed = customPackageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        keyboardController?.hide()
                        onInstall(trimmed)
                    }
                }
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val trimmed = customPackageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        keyboardController?.hide()
                        onInstall(trimmed)
                    }
                },
                enabled = customPackageInput.trim().isNotEmpty()
            ) {
                Text("Install")
            }
        }
    }
}

@Composable
fun ContainerDetailSoftwareTab(
    container: ContainerInstance,
    packages: List<SoftwarePackage>,
    onInstallPackage: (packageId: String) -> Unit,
    onUninstallPackage: (packageId: String) -> Unit = {},
    onInstallCustomPackage: (packageName: String) -> Unit,
    onViewLogs: (packageId: String) -> Unit,
    onRunPresetCommand: (command: String) -> Unit
) = SoftwareTabContent(
    container = container,
    packages = packages,
    onInstallPackage = onInstallPackage,
    onUninstallPackage = onUninstallPackage,
    onInstallCustomPackage = onInstallCustomPackage,
    onViewLogs = onViewLogs,
    onRunPresetCommand = onRunPresetCommand
)
