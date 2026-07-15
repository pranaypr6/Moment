package com.moment.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.moment.app.ui.auth.SplashScreen
import com.moment.app.ui.auth.LoginScreen
import com.moment.app.ui.main.MainScreen
import com.moment.app.ui.onboarding.OnboardingScreen
import com.moment.app.ui.moments.CameraCaptureScreen
import com.moment.app.ui.moments.ImageEditorScreen
import com.moment.app.ui.moments.SendMomentScreen
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Onboarding : Screen("onboarding/{name}?profilePicUrl={profilePicUrl}") {
        fun createRoute(name: String, profilePicUrl: String) = 
            "onboarding/${URLEncoder.encode(name.ifBlank { " " }, StandardCharsets.UTF_8.toString())}?profilePicUrl=${URLEncoder.encode(profilePicUrl, StandardCharsets.UTF_8.toString())}"
    }
    object Main : Screen("main")
    object CameraCapture : Screen("camera_capture")
    object ImageEditor : Screen("image_editor/{imageUri}") {
        fun createRoute(uri: String) = "image_editor/${URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())}"
    }
    object SendMoment : Screen("send_moment/{imageUri}") {
        fun createRoute(uri: String) = "send_moment/${URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())}"
    }
    object SpaceSettings : Screen("space_settings")
    object Paywall : Screen("paywall")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    targetTab: String? = null,
    onTargetTabConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToOnboarding = { name, profilePicUrl ->
                    navController.navigate(Screen.Onboarding.createRoute(name, profilePicUrl)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("profilePicUrl") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("name") ?: ""
            val name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString()).trim()
            val encodedPic = backStackEntry.arguments?.getString("profilePicUrl") ?: ""
            val picUrl = if (encodedPic.isNotBlank()) URLDecoder.decode(encodedPic, StandardCharsets.UTF_8.toString()) else ""
            
            OnboardingScreen(
                initialName = name,
                initialProfilePictureUrl = picUrl,
                onProfileCreated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Main.route + "?inviteCode={inviteCode}",
            arguments = listOf(navArgument("inviteCode") { 
                type = NavType.StringType 
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val inviteCode = backStackEntry.arguments?.getString("inviteCode")
            MainScreen(
                initialInviteCode = inviteCode,
                onNavigateToCamera = { navController.navigate(Screen.CameraCapture.route) },
                onNavigateToEditor = { uri -> 
                    navController.navigate(Screen.ImageEditor.createRoute(uri)) 
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDeleteAccount = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSpaceSettings = {
                    navController.navigate(Screen.SpaceSettings.route)
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                externalTargetTab = targetTab,
                onTargetTabConsumed = onTargetTabConsumed
            )
        }
        composable(Screen.SpaceSettings.route) {
            com.moment.app.ui.settings.SpaceSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CameraCapture.route) {
            CameraCaptureScreen(
                onImageCaptured = { uri ->
                    navController.navigate(Screen.ImageEditor.createRoute(uri)) {
                        popUpTo(Screen.CameraCapture.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ImageEditor.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("imageUri") ?: ""
            ImageEditorScreen(
                imageUri = Uri.parse(URLDecoder.decode(uri, StandardCharsets.UTF_8.toString())),
                onFinishEditing = { editedUri ->
                    navController.navigate(Screen.SendMoment.createRoute(editedUri.toString()))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.SendMoment.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("imageUri") ?: ""
            SendMomentScreen(
                initialImageUri = Uri.parse(URLDecoder.decode(uri, StandardCharsets.UTF_8.toString())),
                onFinish = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Paywall.route) {
            com.moment.app.ui.paywall.PaywallScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
