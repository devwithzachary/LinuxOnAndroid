package com.devwithzachary.completelinuxinstaller.ui.screens.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.components.AptInstallCard
import com.devwithzachary.completelinuxinstaller.ui.components.LogViewerDialog
import com.devwithzachary.completelinuxinstaller.ui.components.SoftwareCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareHubScreen(
    packages: List<SoftwarePackage>,
    onInstallPackageClick: (String) -> Unit,
    onInstallCustomPackageClick: (String) -> Unit = {},
    onLaunchPackageClick: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<SoftwareCategory?>(null) }
    var activeLogPackageId by remember { mutableStateOf<String?>(null) }
    var customPackageInput by remember { mutableStateOf("") }

    val filteredPackages = if (selectedCategory == null) {
        packages
    } else {
        packages.filter { it.category == selectedCategory }
    }

    val activePackage = packages.find { it.id == activeLogPackageId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Software & Package Hub",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Custom Package Quick Search & Install Card
        AptInstallCard(
            value = customPackageInput,
            onValueChange = { customPackageInput = it },
            onInstallClick = { pkgName ->
                onInstallCustomPackageClick(pkgName)
                activeLogPackageId = "custom_${pkgName.lowercase().replace(" ", "_")}"
                customPackageInput = ""
            }
        )

        // Category Filter Chips
        SecondaryScrollableTabRow(
            selectedTabIndex = if (selectedCategory == null) 0 else SoftwareCategory.entries.indexOf(selectedCategory) + 1,
            edgePadding = 0.dp,
            divider = {}
        ) {
            Tab(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                text = { Text("All (${packages.size})") }
            )
            SoftwareCategory.entries.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat.displayName) }
                )
            }
        }

        // Package Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredPackages) { pkg ->
                SoftwareCard(
                    pkg = pkg,
                    onInstallClick = {
                        onInstallPackageClick(pkg.id)
                        activeLogPackageId = pkg.id
                    },
                    onViewLogsClick = {
                        activeLogPackageId = pkg.id
                    },
                    onLaunchClick = { cmd ->
                        onLaunchPackageClick(cmd)
                    }
                )
            }
        }
    }

    // Terminal Output Popup Dialog
    activePackage?.let { pkg ->
        LogViewerDialog(
            pkg = pkg,
            onDismiss = { activeLogPackageId = null }
        )
    }
}
