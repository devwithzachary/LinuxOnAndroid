package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChangelogItem(
    version: String,
    date: String,
    highlights: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = version,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            highlights.forEach { highlight ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
