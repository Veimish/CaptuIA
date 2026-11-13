package com.innova.captuia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.innova.captuia.ui.home.HomeScreen
import com.innova.captuia.ui.onboarding.WelcomeScreen

/** Rutas de navegación. Cada etapa agrega aquí sus pantallas. */
object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
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
