package com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@Composable
fun WizardConfigStep(
    selectedDistro: DistroDefinition,
    containerName: String,
    onContainerNameChange: (String) -> Unit,
    rootPassword: String,
    onRootPasswordChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    userPassword: String,
    onUserPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rootPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var userPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Configure Container",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Set a display name and default user credentials for this container.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LoaSegmentedGroup {
            row(
                title = selectedDistro.name,
                subtitle = "Selected distribution",
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoaMonospaceChip(text = "${selectedDistro.formattedDownloadSize} dl")
                        LoaMonospaceChip(text = "${selectedDistro.formattedInstalledSize} disk")
                    }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoaSectionHeader(title = "Credentials")

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = containerName,
                        onValueChange = onContainerNameChange,
                        label = { Text("Container Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rootPassword,
                        onValueChange = onRootPasswordChange,
                        label = { Text(stringResource(R.string.label_root_password)) },
                        singleLine = true,
                        visualTransformation = if (rootPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        trailingIcon = {
                            IconButton(onClick = { rootPasswordVisible = !rootPasswordVisible }) {
                                Icon(
                                    imageVector = if (rootPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle root password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = { Text(stringResource(R.string.label_regular_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userPassword,
                        onValueChange = onUserPasswordChange,
                        label = { Text(stringResource(R.string.label_regular_password)) },
                        singleLine = true,
                        visualTransformation = if (userPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        trailingIcon = {
                            IconButton(onClick = { userPasswordVisible = !userPasswordVisible }) {
                                Icon(
                                    imageVector = if (userPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle user password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
