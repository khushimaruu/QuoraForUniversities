package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PostingIdentity(anonymous: Boolean, profileName: String, enabled: Boolean = true, onSelect: (Boolean) -> Unit) {
    BoxWithConstraints {
        val stacked = maxWidth < 280.dp
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IdentityChoice("Anonymous", "Your name stays hidden from other students.", Icons.Outlined.VisibilityOff,
                anonymous, enabled, Modifier.fillMaxWidth()) { onSelect(true) }
            IdentityChoice(profileName, "Post with your campus profile.", Icons.Outlined.Person,
                !anonymous, enabled, Modifier.fillMaxWidth()) { onSelect(false) }
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdentityChoice("Anonymous", "Your name stays hidden from other students.", Icons.Outlined.VisibilityOff,
                anonymous, enabled, Modifier.weight(1f)) { onSelect(true) }
            IdentityChoice(profileName, "Post with your campus profile.", Icons.Outlined.Person,
                !anonymous, enabled, Modifier.weight(1f)) { onSelect(false) }
        }
    }
}

@Composable
private fun IdentityChoice(title: String, body: String, icon: ImageVector, chosen: Boolean, enabled: Boolean,
                           modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, enabled = enabled,
        modifier = modifier.semantics { selected = chosen; role = Role.RadioButton },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)
            else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                RadioButton(selected = chosen, onClick = null, modifier = Modifier.size(16.dp))
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
