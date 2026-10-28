package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.*
import java.io.File

@Composable
fun SettingsTabContent(
    container: ContainerInstance,
    bindSdCard: Boolean,
    dnsServers: List<String>,
    containerUsers: List<String>,
    upgradeState: UpgradeState,
    backupState: BackupState,
    onUpgradeRootfs: () -> Unit,
    onDismissUpgradeState: () -> Unit,
    onExportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onImportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onDismissBackupStatus: () -> Unit,
    onToggleBindSdCard: () -> Unit,
    onChangeRootPassword: (password: String) -> Unit,
    onVerifyRootPassword: suspend (password: String) -> Boolean = { true },
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    onSetDnsServers: (servers: List<String>) -> Unit,
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, onComplete: (Int) -> Unit) -> Unit,
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit,
    onOpenExternalDirectory: (context: Context) -> Unit,
    onGetExternalFiles: () -> List<File>,
    onToggleWebTerminalLanAccess: (Boolean) -> Unit = {},
    onToggleCodeServerLanAccess: (Boolean) -> Unit = {},
    onSetCodeServerPassword: (String) -> Unit = {},
    onRegenerateCodeServerPassword: () -> Unit = {},
    onToggleVncLanAccess: (Boolean) -> Unit = {},
    onRegenerateVncPassword: () -> Unit = {},
    onSetVncPassword: (String) -> Unit = {},
    onDeleteContainer: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Container Identity & Path
        ContainerInfoCard(container = container)

        // 2. RootFS Upgrade & Patching
        ContainerUpgradeCard(
            container = container,
            upgradeState = upgradeState,
            onUpgradeRootfs = onUpgradeRootfs,
            onDismissUpgradeState = onDismissUpgradeState
        )

        // 3. Container Backup & Restore
        ContainerBackupCard(
            container = container,
            backupState = backupState,
            onExportContainer = onExportContainer,
            onImportContainer = onImportContainer,
            onDismissBackupStatus = onDismissBackupStatus
        )

        // 4. Shared External Storage (/external) & SD Card Binding
        ContainerStorageCard(
            container = container,
            bindSdCard = bindSdCard,
            onToggleBindSdCard = onToggleBindSdCard,
            onOpenExternalDirectory = onOpenExternalDirectory,
            onGetExternalFiles = onGetExternalFiles,
            onImportFiles = onImportFiles,
            onExportFile = onExportFile
        )

        // 5. User & Account Management
        ContainerAccountCard(
            container = container,
            containerUsers = containerUsers,
            onChangeRootPassword = onChangeRootPassword,
            onVerifyRootPassword = onVerifyRootPassword,
            onCreateUser = onCreateUser,
            onDeleteUser = onDeleteUser,
            onSetDefaultUser = onSetDefaultUser
        )

        // 6. Network & DNS Configuration
        ContainerNetworkCard(
            dnsServers = dnsServers,
            onSetDnsServers = onSetDnsServers
        )

        // 7. Remote Access & LAN Sharing
        ContainerRemoteAccessCard(
            container = container,
            onToggleWebTerminalLanAccess = onToggleWebTerminalLanAccess,
            onToggleCodeServerLanAccess = onToggleCodeServerLanAccess,
            onSetCodeServerPassword = onSetCodeServerPassword,
            onRegenerateCodeServerPassword = onRegenerateCodeServerPassword,
            onToggleVncLanAccess = onToggleVncLanAccess,
            onRegenerateVncPassword = onRegenerateVncPassword,
            onSetVncPassword = onSetVncPassword
        )

        // 8. Danger Zone: Delete Container
        ContainerDangerZoneCard(
            container = container,
            onDeleteContainer = onDeleteContainer
        )
    }
}

@Composable
fun ContainerDetailSettingsTab(
    container: ContainerInstance,
    bindSdCard: Boolean,
    dnsServers: List<String>,
    containerUsers: List<String>,
    upgradeState: UpgradeState,
    backupState: BackupState,
    onUpgradeRootfs: () -> Unit,
    onDismissUpgradeState: () -> Unit,
    onExportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onImportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onDismissBackupStatus: () -> Unit,
    onToggleBindSdCard: () -> Unit,
    onChangeRootPassword: (password: String) -> Unit,
    onVerifyRootPassword: suspend (password: String) -> Boolean = { true },
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    onSetDnsServers: (servers: List<String>) -> Unit,
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, onComplete: (Int) -> Unit) -> Unit,
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit,
    onOpenExternalDirectory: (context: Context) -> Unit,
    onGetExternalFiles: () -> List<File>,
    onToggleWebTerminalLanAccess: (Boolean) -> Unit = {},
    onToggleCodeServerLanAccess: (Boolean) -> Unit = {},
    onSetCodeServerPassword: (String) -> Unit = {},
    onRegenerateCodeServerPassword: () -> Unit = {},
    onToggleVncLanAccess: (Boolean) -> Unit = {},
    onRegenerateVncPassword: () -> Unit = {},
    onSetVncPassword: (String) -> Unit = {},
    onDeleteContainer: () -> Unit
) = SettingsTabContent(
    container = container,
    bindSdCard = bindSdCard,
    dnsServers = dnsServers,
    containerUsers = containerUsers,
    upgradeState = upgradeState,
    backupState = backupState,
    onUpgradeRootfs = onUpgradeRootfs,
    onDismissUpgradeState = onDismissUpgradeState,
    onExportContainer = onExportContainer,
    onImportContainer = onImportContainer,
    onDismissBackupStatus = onDismissBackupStatus,
    onToggleBindSdCard = onToggleBindSdCard,
    onChangeRootPassword = onChangeRootPassword,
    onVerifyRootPassword = onVerifyRootPassword,
    onCreateUser = onCreateUser,
    onDeleteUser = onDeleteUser,
    onSetDefaultUser = onSetDefaultUser,
    onSetDnsServers = onSetDnsServers,
    onImportFiles = onImportFiles,
    onExportFile = onExportFile,
    onOpenExternalDirectory = onOpenExternalDirectory,
    onGetExternalFiles = onGetExternalFiles,
    onToggleWebTerminalLanAccess = onToggleWebTerminalLanAccess,
    onToggleCodeServerLanAccess = onToggleCodeServerLanAccess,
    onSetCodeServerPassword = onSetCodeServerPassword,
    onRegenerateCodeServerPassword = onRegenerateCodeServerPassword,
    onToggleVncLanAccess = onToggleVncLanAccess,
    onRegenerateVncPassword = onRegenerateVncPassword,
    onSetVncPassword = onSetVncPassword,
    onDeleteContainer = onDeleteContainer
)

