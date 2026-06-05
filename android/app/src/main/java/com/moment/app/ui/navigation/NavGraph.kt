package com.moment.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.moment.app.ui.auth.SplashScreen
import com.moment.app.ui.auth.LoginScreen
import com.moment.app.ui.onboarding.OnboardingScreen
import com.moment.app.ui.timeline.TimelineScreen
import com.moment.app.ui.connections.ConnectionScreen
import com.moment.app.ui.moments.SendMomentScreen
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Onboarding : Screen("onboarding/{name}") {
        fun createRoute(name: String) = "onboarding/${URLEncoder.encode(name.ifBlank { " " }, StandardCharsets.UTF_8.toString())}"
    }
    object Main : Screen("main")
    object Connections : Screen("connections")
    object SendMoment : Screen("send_moment")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToOnboarding = { name ->
                    navController.navigate(Screen.Onboarding.createRoute(name)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("name") ?: ""
            val name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString()).trim()
            OnboardingScreen(
                initialName = name,
                onProfileCreated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            TimelineScreen(
                onNavigateToSendMoment = { navController.navigate(Screen.SendMoment.route) },
                onNavigateToConnections = { navController.navigate(Screen.Connections.route) }
            )
        }
        composable(
            route = Screen.Connections.route + "?inviteCode={inviteCode}",
            arguments = listOf(navArgument("inviteCode") { 
                type = NavType.StringType 
                nullable = true
                defaultValue = null
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://momentapp.in/invite/{inviteCode}" })
        ) { backStackEntry ->
            val inviteCode = backStackEntry.arguments?.getString("inviteCode")
            ConnectionScreen(
                initialInviteCode = inviteCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SendMoment.route) {
            SendMomentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
