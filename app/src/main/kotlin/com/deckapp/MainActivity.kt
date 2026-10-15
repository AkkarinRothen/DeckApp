package com.deckapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.deckapp.core.model.AppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.ui.theme.DeckAppTheme
import com.deckapp.navigation.DeckAppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by settingsRepository.observeAppTheme()
                .collectAsStateWithLifecycle(initialValue = settingsRepository.getAppTheme())
            val tableAppearance by settingsRepository.observeTableAppearance()
                .collectAsStateWithLifecycle(initialValue = settingsRepository.getTableAppearance())

            SideEffect {
                val light = appTheme == AppTheme.LIGHT || appTheme == AppTheme.PARCHMENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }

            DeckAppTheme(appTheme = appTheme, tableAppearance = tableAppearance) {
                DeckAppNavHost()
            }
        }
    }
}
