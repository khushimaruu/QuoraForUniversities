package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.utils.TimestampFormatter
import kotlinx.coroutines.delay

@Composable
fun AuthorByline(authorName: String, createdAt: String?, modifier: Modifier = Modifier) {
    val timestamp by produceState(TimestampFormatter.format(createdAt), createdAt) {
        do {
            value = TimestampFormatter.format(createdAt)
            if (value == null) break
            delay(30_000)
        } while (true)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        timestamp?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
