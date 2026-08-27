package com.app.voiceclassifier.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.voiceclassifier.ui.adduser.CreateUserScreen

@Composable
fun VoiceClassifierNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Classify.route,
        modifier = modifier
    ) {
        composable(Screen.Classify.route) {
            ClassifyScreen()
        }
        composable(Screen.AddUser.route) {
            AddUserScreen(
                onAddClick = { navController.navigate(Screen.AddUserCreate.route) }
            )
        }
        composable(Screen.AddUserCreate.route) {
            CreateUserScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
