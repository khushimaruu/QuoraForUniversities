package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BottomNavDestination(val route: String, val label: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("search", "Topics", Icons.Filled.Search, Icons.Outlined.Search),
    ADD_POST("add_post", "Ask", Icons.Filled.Add, Icons.Filled.Add),
    SAVED("saved_posts", "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun BottomNavBar(currentRoute: String?, onDestinationSelected: (BottomNavDestination) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(modifier = Modifier.readingWidth(), containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars.union(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                BottomNavDestination.entries.forEach { destination ->
                    val selected = destination.route == currentRoute
                    NavigationBarItem(selected = selected, onClick = { onDestinationSelected(destination) },
                        icon = {
                            if (destination == BottomNavDestination.ADD_POST) Box(
                                Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape), Alignment.Center
                            ) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary) }
                            else Icon(if (selected) destination.filledIcon else destination.outlinedIcon, null)
                        }, label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.surface))
                }
            }
        }
    }
}
