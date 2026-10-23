package com.deckapp.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.deckapp.MainActivity
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Session
import com.deckapp.core.model.SessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionWidgetConfigureActivity : ComponentActivity() {
    @Inject lateinit var sessionRepository: SessionRepository

    private val widgetId: Int
        get() = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    private val isLauncherConfiguration: Boolean
        get() = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isLauncherConfiguration) {
            setResult(
                Activity.RESULT_CANCELED,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            )
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val sessions by sessionRepository.getAllSessions().collectAsStateWithLifecycle(initialValue = emptyList())
            val activeSessions = sessions.filter { it.status == SessionStatus.ACTIVE }
            MaterialTheme {
                Scaffold { insets ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(insets)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Widget de sesión", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = if (activeSessions.isEmpty()) {
                                "No hay sesiones activas. Puedes abrir la lista y crear o iniciar una sesión."
                            } else {
                                "Elige la sesión que abrirá este widget."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (activeSessions.isEmpty()) {
                            Button(onClick = ::openSessionList, modifier = Modifier.fillMaxWidth()) {
                                Text("Abrir sesiones")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeSessions, key = Session::id) { session ->
                                    SessionChoiceCard(session = session, onSelect = { selectSession(session) })
                                }
                            }
                        }

                        TextButton(onClick = ::finish) { Text("Cancelar") }
                    }
                }
            }
        }
    }

    private fun selectSession(session: Session) {
        lifecycleScope.launch {
            runCatching {
                val manager = GlanceAppWidgetManager(this@SessionWidgetConfigureActivity)
                val glanceId = manager.getGlanceIdBy(widgetId)
                updateAppWidgetState(this@SessionWidgetConfigureActivity, glanceId) { preferences ->
                    preferences[SessionWidgetIdKey] = session.id
                }
                SessionGlanceWidget().update(this@SessionWidgetConfigureActivity, glanceId)
            }.onSuccess {
                if (isLauncherConfiguration) {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                }
                finish()
            }.onFailure {
                android.util.Log.e("SessionWidget", "No se pudo guardar la sesión del widget", it)
            }
        }
    }

    private fun openSessionList() {
        lifecycleScope.launch {
            if (isLauncherConfiguration) {
                runCatching {
                    val glanceId = GlanceAppWidgetManager(this@SessionWidgetConfigureActivity)
                        .getGlanceIdBy(widgetId)
                    updateAppWidgetState(this@SessionWidgetConfigureActivity, glanceId) {
                        it.remove(SessionWidgetIdKey)
                    }
                    SessionGlanceWidget().update(this@SessionWidgetConfigureActivity, glanceId)
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                }.onFailure {
                    android.util.Log.e("SessionWidget", "No se pudo preparar el widget vacío", it)
                }
            }
            startActivity(
                Intent(this@SessionWidgetConfigureActivity, MainActivity::class.java)
                    .putExtra(EXTRA_OPEN_SESSIONS, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun SessionChoiceCard(session: Session, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                Text("Sesión activa", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSelect) { Text("Elegir") }
        }
    }
}
