package com.innova.captuia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innova.captuia.ui.detail.CaptureDetailScreen
import com.innova.captuia.ui.detail.CaptureDetailViewModel
import com.innova.captuia.ui.home.HomeScreen
import com.innova.captuia.ui.onboarding.WelcomeScreen

/** Rutas de navegación. Cada etapa agrega aquí sus pantallas. */
object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val CAPTURE = "capture/{${CaptureDetailViewModel.ARG_CAPTURE_ID}}"

    fun capture(id: Long) = "capture/$id"
}

@Composable
fun CaptuNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onAccessReady = { navController.replace(from = Routes.WELCOME, to = Routes.HOME) },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAccessLost = { navController.replace(from = Routes.HOME, to = Routes.WELCOME) },
                onOpenCapture = { id ->
                    // Evita abrir dos veces el detalle con un doble toque rápido.
                    if (navController.currentDestination?.route == Routes.HOME) {
                        navController.navigate(Routes.capture(id))
                    }
                },
            )
        }
        composable(
            route = Routes.CAPTURE,
            arguments = listOf(
                navArgument(CaptureDetailViewModel.ARG_CAPTURE_ID) { type = NavType.LongType },
            ),
        ) {
            CaptureDetailScreen(
                onBack = {
                    if (navController.currentDestination?.route == Routes.CAPTURE) {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}

/**
 * Reemplaza la pantalla [from] por [to] sin dejarla en la pila.
 * Solo actúa si [from] es la pantalla visible, de modo que llamarla dos veces
 * seguidas no navega dos veces.
 */
private fun NavHostController.replace(from: String, to: String) {
    if (currentDestination?.route != from) return
    navigate(to) {
        popUpTo(from) { inclusive = true }
        launchSingleTop = true
    }
}
