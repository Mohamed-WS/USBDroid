package com.usbdroid.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.usbdroid.ui.theme.Background
import com.usbdroid.ui.theme.Divider
import com.usbdroid.ui.theme.PrimaryCyan
import com.usbdroid.ui.theme.Surface

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String) {
    val items = listOf(
        NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("modules", "Modules", Icons.Filled.Extension, Icons.Outlined.Extension),
        NavItem("logs", "Logs", Icons.Filled.List, Icons.Outlined.List),
        NavItem("ai", "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    Surface(
        color = Background,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Surface.copy(alpha = 0.95f),
            contentColor = PrimaryCyan,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val iconColor by animateColorAsState(
                    targetValue = if (selected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    label = "iconColor_${item.route}"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    label = "textColor_${item.route}"
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryCyan,
                        selectedTextColor = PrimaryCyan,
                        indicatorColor = PrimaryCyan.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
