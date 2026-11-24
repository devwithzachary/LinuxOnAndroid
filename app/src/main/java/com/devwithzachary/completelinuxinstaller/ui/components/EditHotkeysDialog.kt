package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.loaSegmentedPositionFor
import com.devwithzachary.completelinuxinstaller.ui.designsystem.loaSegmentedShape
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.util.HotkeyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHotkeysDialog(
    currentHotkeys: List<String>,
    onSaveHotkeys: (List<String>) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var hotkeysList by remember { mutableStateOf<List<String>>(currentHotkeys) }
    var newKeyInput by remember { mutableStateOf("") }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.hotkey_editor_title),
        subtitle = stringResource(R.string.hotkey_editor_desc)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newKeyInput,
                onValueChange = { newKeyInput = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.hotkey_add_placeholder),
                        style = LoaMonoTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                textStyle = LoaMonoTextStyle,
                modifier = Modifier.weight(1f),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = newKeyInput.trim()
                    if (trimmed.isNotEmpty() && !hotkeysList.contains(trimmed)) {
                        hotkeysList = hotkeysList + trimmed
                        newKeyInput = ""
                    }
                })
            )

            FilledTonalButton(
                onClick = {
                    val trimmed = newKeyInput.trim()
                    if (trimmed.isNotEmpty() && !hotkeysList.contains(trimmed)) {
                        hotkeysList = hotkeysList + trimmed
                        newKeyInput = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_add_key))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(hotkeysList) { index, key ->
                val shape = loaSegmentedShape(loaSegmentedPositionFor(index, hotkeysList.size))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = shape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = key,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = LoaMonoTextStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val list = hotkeysList.toMutableList()
                                        val item = list.removeAt(index)
                                        list.add(index - 1, item)
                                        hotkeysList = list
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move $key up",
                                    tint = if (index > 0) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (index < hotkeysList.size - 1) {
                                        val list = hotkeysList.toMutableList()
                                        val item = list.removeAt(index)
                                        list.add(index + 1, item)
                                        hotkeysList = list
                                    }
                                },
                                enabled = index < hotkeysList.size - 1,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move $key down",
                                    tint = if (index < hotkeysList.size - 1) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val list = hotkeysList.toMutableList()
                                    list.removeAt(index)
                                    hotkeysList = list
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove $key",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    onResetDefaults()
                    hotkeysList = HotkeyManager.DEFAULT_HOTKEYS
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_reset_default_keys))
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    onSaveHotkeys(hotkeysList)
                    onDismiss()
                },
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_save_keys))
            }
        }
    }
}
