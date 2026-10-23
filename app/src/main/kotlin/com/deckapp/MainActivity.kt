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
import androidx.glance.appwidget.updateAll
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.StartPage
import com.deckapp.core.ui.theme.DeckAppTheme
import com.deckapp.navigation.DeckAppNavHost
import com.deckapp.widget.EXTRA_OPEN_SESSIONS
import com.deckapp.widget.EXTRA_WIDGET_SESSION_ID
import com.deckapp.widget.SessionGlanceWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository
    @Inject
    lateinit var sessionRepository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                sessionRepository.getAllSessions().collect {
                    SessionGlanceWidget().updateAll(this@MainActivity)
                }
            }
            val appTheme by settingsRepository.observeAppTheme()
                .collectAsStateWithLifecycle(initialValue = settingsRepository.getAppTheme())
            val tableAppearance by settingsRepository.observeTableAppearance()
                .collectAsStateWithLifecycle(initialValue = settingsRepository.getTableAppearance())

            val preferences by settingsRepository.observeGeneralPreferences()
                .collectAsStateWithLifecycle(initialValue = settingsRepository.getGeneralPreferences())
            val density = androidx.compose.ui.platform.LocalDensity.current
            val platformHaptics = androidx.compose.ui.platform.LocalHapticFeedback.current
            val haptics = androidx.compose.runtime.remember(platformHaptics, preferences.hapticsEnabled) {
                object : androidx.compose.ui.hapticfeedback.HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                        if (preferences.hapticsEnabled) platformHaptics.performHapticFeedback(hapticFeedbackType)
                    }
                }
            }
            // Keep the start destination stable while preferences change in this activity.
            val startPage = androidx.compose.runtime.saveable.rememberSaveable {
                if (intent?.getBooleanExtra(EXTRA_OPEN_SESSIONS, false) == true) StartPage.SESSIONS
                else preferences.startPage
            }
            val widgetSessionId = intent?.getLongExtra(EXTRA_WIDGET_SESSION_ID, -1L)
                ?.takeIf { it > 0L }

            SideEffect {
                val light = appTheme == AppTheme.LIGHT || appTheme == AppTheme.PARCHMENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }

            DeckAppTheme(appTheme = appTheme, tableAppearance = tableAppearance) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.deckapp.core.ui.theme.LocalGeneralPreferences provides preferences,
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        density.density, density.fontScale * preferences.textScale
                    ),
                    androidx.compose.ui.platform.LocalHapticFeedback provides haptics
                ) {
                    DeckAppNavHost(startPage = startPage, startSessionId = widgetSessionId)
                }
            }
        }
    }
}
