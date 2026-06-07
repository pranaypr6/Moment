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
    object Onboarding : Screen("onboarding/{name}") {
        fun createRoute(name: String) = "onboarding/${URLEncoder.encode(name.ifBlank { " " }, StandardCharsets.UTF_8.toString())}"
    }
    object Main : Screen("main")
    object CameraCapture : Screen("camera_capture")
    object ImageEditor : Screen("image_editor/{imageUri}") {
        fun createRoute(uri: String) = "image_editor/${URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())}"
    }
    object SendMoment : Screen("send_moment/{imageUri}") {
        fun createRoute(uri: String) = "send_moment/${URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())}"
    }
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
                onNavigateToOnboarding = { name ->
                    navController.navigate(Screen.Onboarding.createRoute(name)) {
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
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("name") ?: ""
            val name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString()).trim()
            OnboardingScreen(
                initialName = name,
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
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://momentapp.in/invite/{inviteCode}" })
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
                }
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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
