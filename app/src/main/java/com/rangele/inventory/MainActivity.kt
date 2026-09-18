package com.rangele.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rangele.inventory.data.settings.ThemeMode
import com.rangele.inventory.ui.navigation.RangeleNavHost
import com.rangele.inventory.ui.theme.RangeleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RangeleApplication).container

        setContent {
            // ThemeMode.SYSTEM (valeur par défaut tant que le DataStore n'a pas encore livré sa
            // première valeur) retombe sur isSystemInDarkTheme(), donc sur le comportement d'avant
            // ce réglage : jamais de flash clair/sombre incorrect le temps de la lecture initiale.
            val themeMode by container.settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme =
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            RangeleTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RangeleNavHost(container = container)
                }
            }
        }
    }
}
