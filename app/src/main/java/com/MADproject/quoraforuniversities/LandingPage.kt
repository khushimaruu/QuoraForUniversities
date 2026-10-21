package com.MADproject.quoraforuniversities

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.*
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

@Composable
fun LandingPage(onLoginClick: () -> Unit = {}, onSignUpClick: () -> Unit = {}) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().readingWidth(520.dp).verticalScroll(rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.Forum, null, Modifier.padding(9.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text("CAMPUSCIRCLE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ask anything.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Learn together.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("A space for university students to ask questions, share knowledge, and find their community.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CampusIllustration(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(12.dp)))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.School, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Your university. Your community.", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                }
                listOf("Find answers from fellow students", "Share your perspective and experience", "Ask anonymously when you need to").forEach { text ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Log in to your campus"); Spacer(Modifier.width(10.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
                OutlinedButton(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Create an account") }
            }
            Text("A little curiosity can start a great conversation.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bundled campus imagery keeps onboarding independent of network availability. */
@Composable
fun CampusIllustration(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(painterResource(R.drawable.campus_community), "Students studying together on a university campus",
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
        Text("Your campus community", Modifier.align(Alignment.BottomStart).padding(12.dp),
            style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Preview(name = "Welcome · compact", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Welcome · large text", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
fun LandingPagePreview() { CampusQnATheme { LandingPage() } }
