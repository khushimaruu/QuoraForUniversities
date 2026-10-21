package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.R

data class CampusAuthField(val key: String, val label: String, val placeholder: String,
                           val keyboard: KeyboardType = KeyboardType.Text, val secret: Boolean = false)

@Composable
fun CampusAuthPage(
    signUp: Boolean, fields: List<CampusAuthField>, values: Map<String, String>,
    onChange: (String, String) -> Unit, busy: Boolean, error: String?,
    onSubmit: () -> Unit, onAlternate: () -> Unit, onBack: () -> Unit
) {
    val focus = LocalFocusManager.current
    Scaffold(topBar = { AppHeader(subtitle = "CampusCircle", onBackClick = onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).imePadding().readingWidth(520.dp)
            .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (signUp) "Ask anything." else "Your campus awaits.",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (signUp) "Learn together." else "Welcome back.",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(if (signUp) "Join the campus conversations, study circles, and shared experiences."
                    else "Log in to your community of questions, ideas, and useful answers.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Image(painterResource(R.drawable.campus_community), "Students studying on campus",
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            SectionCard {
                Text(if (signUp) "YOUR CAMPUS ACCOUNT" else "CAMPUS LOGIN", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                fields.forEachIndexed { index, field ->
                    var visible by rememberSaveable(field.key) { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(field.label, style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(values[field.key].orEmpty(), { onChange(field.key, it) },
                            modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                            placeholder = { Text(field.placeholder, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                            visualTransformation = if (field.secret && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = field.keyboard,
                                imeAction = if (index == fields.lastIndex) ImeAction.Done else ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) },
                                onDone = { focus.clearFocus(); if (!busy) onSubmit() }),
                            trailingIcon = { if (field.secret) IconButton(onClick = { visible = !visible }) {
                                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    if (visible) "Hide " + field.label else "Show " + field.label, Modifier.size(18.dp))
                            } })
                    }
                }
                if (error != null) ErrorBanner(error)
                Button(onClick = { focus.clearFocus(); onSubmit() }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(8.dp)) {
                    if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                    Text(if (busy) "Please wait…" else if (signUp) "Create campus account" else "Log in to campus")
                }
                TextButton(onClick = onAlternate, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (signUp) "Already a member? Log in" else "New to CampusCircle? Create an account",
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Text("Questions. Shared knowledge. A closer campus community.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
