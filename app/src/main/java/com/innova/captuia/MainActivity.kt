package com.innova.captuia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.innova.captuia.data.media.MediaAccess
import com.innova.captuia.data.media.MediaPermissions
import com.innova.captuia.ui.navigation.CaptuNavHost
import com.innova.captuia.ui.navigation.Routes
import com.innova.captuia.ui.theme.CaptuIATheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La cabecera de marca (teal oscuro) llega hasta la barra de estado:
        // íconos claros arriba, íconos oscuros en la barra de navegación.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val startDestination =
            if (MediaPermissions.current(this) == MediaAccess.DENIED) Routes.WELCOME else Routes.HOME

        setContent {
            CaptuIATheme {
                CaptuNavHost(startDestination = startDestination)
            }
        }
    }
}
