package com.innova.captuia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.ui.detail.CaptureDetailScreen
import com.innova.captuia.ui.detail.CaptureDetailViewModel
import com.innova.captuia.ui.folder.FolderRoute
import com.innova.captuia.ui.folder.FolderScreen
import com.innova.captuia.ui.folder.FolderViewModel
import com.innova.captuia.ui.home.HomeScreen
import com.innova.captuia.ui.onboarding.WelcomeScreen
import com.innova.captuia.ui.schedule.ScheduleScreen
import com.innova.captuia.ui.schedule.SubjectEditScreen
import com.innova.captuia.ui.schedule.SubjectEditViewModel

/** Rutas de navegación. Cada etapa agrega aquí sus pantallas. */
object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val FOLDER = "folder/{${FolderViewModel.ARG_FILTER}}"
    const val CAPTURE = "capture/{${CaptureDetailViewModel.ARG_CAPTURE_ID}}"

    const val SCHEDULE = "schedule"
    const val SUBJECT = "subject/{${SubjectEditViewModel.ARG_SUBJECT_ID}}"

    fun folder(filter: CaptureFilter) = "folder/${FolderRoute.encode(filter)}"

    /** Con [id] null abre el formulario para una materia nueva. */
    fun subject(id: Long?) = "subject/${id ?: SubjectEditViewModel.NEW_SUBJECT}"
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
                onOpenCapture = { id -> navController.navigateFrom(Routes.HOME, Routes.capture(id)) },
                onOpenFolder = { filter -> navController.navigateFrom(Routes.HOME, Routes.folder(filter)) },
                onOpenSchedule = { navController.navigateFrom(Routes.HOME, Routes.SCHEDULE) },
            )
        }
        composable(
            route = Routes.FOLDER,
            arguments = listOf(
                navArgument(FolderViewModel.ARG_FILTER) { type = NavType.StringType },
            ),
        ) {
            FolderScreen(
                onBack = { navController.backFrom(Routes.FOLDER) },
                onOpenCapture = { id -> navController.navigateFrom(Routes.FOLDER, Routes.capture(id)) },
            )
        }
        composable(
            route = Routes.CAPTURE,
            arguments = listOf(
                navArgument(CaptureDetailViewModel.ARG_CAPTURE_ID) { type = NavType.LongType },
            ),
        ) {
            CaptureDetailScreen(
                onBack = { navController.backFrom(Routes.CAPTURE) },
            )
        }
        composable(Routes.SCHEDULE) {
            ScheduleScreen(
                onBack = { navController.backFrom(Routes.SCHEDULE) },
                onAddSubject = { navController.navigateFrom(Routes.SCHEDULE, Routes.subject(null)) },
                onEditSubject = { id -> navController.navigateFrom(Routes.SCHEDULE, Routes.subject(id)) },
            )
        }
        composable(
            route = Routes.SUBJECT,
            arguments = listOf(
                navArgument(SubjectEditViewModel.ARG_SUBJECT_ID) { type = NavType.StringType },
            ),
        ) {
            SubjectEditScreen(
                onClose = { navController.backFrom(Routes.SUBJECT) },
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

/** Abre [to] solo si [from] sigue siendo la pantalla visible (evita abrir dos veces con un doble toque). */
private fun NavHostController.navigateFrom(from: String, to: String) {
    if (currentDestination?.route == from) navigate(to)
}

/** Vuelve atrás solo si [from] sigue siendo la pantalla visible. */
private fun NavHostController.backFrom(from: String) {
    if (currentDestination?.route == from) popBackStack()
}
