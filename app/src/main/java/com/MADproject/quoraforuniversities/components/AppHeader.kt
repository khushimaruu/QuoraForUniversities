package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppHeader(modifier: Modifier = Modifier, subtitle: String? = null, onBackClick: (() -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(Modifier.statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .readingWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onBackClick != null) IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                } else BrandMark(32.dp)
                Column(Modifier.weight(1f)) {
                    Text(subtitle ?: "CampusCircle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (subtitle == null) Text("Your university community", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
