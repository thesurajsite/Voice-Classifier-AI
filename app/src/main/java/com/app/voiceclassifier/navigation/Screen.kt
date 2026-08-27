package com.app.voiceclassifier.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Classify : Screen(
        route = "classify",
        title = "Classify",
        icon = Icons.Default.Mic
    )

    data object AddUser : Screen(
        route = "add_user",
        title = "Add User",
        icon = Icons.Default.PersonAdd
    )

    data object AddUserCreate : Screen(
        route = "add_user/create",
        title = "Create User",
        icon = Icons.Default.PersonAdd
    )
}

val bottomNavItems = listOf(
    Screen.Classify,
    Screen.AddUser
)
