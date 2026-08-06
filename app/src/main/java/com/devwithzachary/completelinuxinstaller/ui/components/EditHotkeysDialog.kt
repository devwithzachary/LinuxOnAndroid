package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.util.HotkeyManager

@Composable
fun EditHotkeysDialog(
    currentHotkeys: List<String>,
    onSaveHotkeys: (List<String>) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var hotkeysList by remember { mutableStateOf(currentHotkeys.toMutableList()) }
    var newKeyInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF)
                        )
                        Text(
                            text = stringResource(R.string.hotkey_editor_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF8B949E)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.hotkey_editor_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B949E)
                )

                HorizontalDivider(color = Color(0xFF30363D))

                // Add Custom Key Row Input
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
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val trimmed = newKeyInput.trim()
                            if (trimmed.isNotEmpty() && !hotkeysList.contains(trimmed)) {
                                hotkeysList = (hotkeysList + trimmed).toMutableList()
                                newKeyInput = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D1117),
                            unfocusedContainerColor = Color(0xFF0D1117),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            val trimmed = newKeyInput.trim()
                            if (trimmed.isNotEmpty() && !hotkeysList.contains(trimmed)) {
                                hotkeysList = (hotkeysList + trimmed).toMutableList()
                                newKeyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_add_key), color = Color.White, fontSize = 12.sp)
                    }
                }

                // Active Hotkeys List View
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(10.dp)),
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(hotkeysList) { key ->
                            Surface(
                                color = Color(0xFF21262D),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = Color(0xFF79C0FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            hotkeysList = hotkeysList.filter { it != key }.toMutableList()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFF85149),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetDefaults()
                            hotkeysList = HotkeyManager.DEFAULT_HOTKEYS.toMutableList()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_reset_default_keys), fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onSaveHotkeys(hotkeysList)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_save_keys), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
