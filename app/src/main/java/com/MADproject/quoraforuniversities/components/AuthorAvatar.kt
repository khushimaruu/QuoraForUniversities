package com.MADproject.quoraforuniversities.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

@Composable
fun AuthorAvatar(avatarBase64: String?, authorName: String, size: Dp) {
    val photo = remember(avatarBase64) {
        avatarBase64?.takeIf { it.isNotBlank() }?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded.substringAfter(','), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(photo.asImageBitmap(), "$authorName profile photo", Modifier.size(size),
                contentScale = ContentScale.Crop)
        } else {
            Text(if (authorName == "Anonymous") "?" else authorName.firstOrNull()?.uppercase() ?: "S",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
