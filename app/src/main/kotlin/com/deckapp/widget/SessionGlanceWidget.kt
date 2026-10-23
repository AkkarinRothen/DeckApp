package com.deckapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.deckapp.MainActivity
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.SessionStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

internal val SessionWidgetIdKey = longPreferencesKey("session_widget_session_id")

internal const val EXTRA_WIDGET_SESSION_ID = "com.deckapp.widget.SESSION_ID"
internal const val EXTRA_OPEN_SESSIONS = "com.deckapp.widget.OPEN_SESSIONS"
internal const val ACTION_CONFIGURE_SESSION_WIDGET = "com.deckapp.widget.CONFIGURE_SESSION"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionWidgetDependencies {
    fun sessionRepository(): SessionRepository
}

class SessionGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)
        val sessionId = state[SessionWidgetIdKey]
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionWidgetDependencies::class.java
        ).sessionRepository()
        val session = sessionId?.let { repository.getSessionById(it).first() }
        val isActive = session?.status == SessionStatus.ACTIVE
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_WIDGET_SESSION_ID, sessionId ?: -1L)
                action = "com.deckapp.widget.OPEN_SESSION_${sessionId ?: "none"}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val configureIntent = Intent(context, SessionWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                action = ACTION_CONFIGURE_SESSION_WIDGET
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1F2933)))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "DECKAPP · SESIÓN ACTIVA",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE0B96E)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isActive && session != null) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity(mainIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.name,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "Toca para abrir la mesa",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFD2D7DC)),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    } else {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity(configureIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Elegir otra sesión",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (sessionId == null) {
                                    "Todavía no hay una sesión elegida"
                                } else {
                                    "La sesión vinculada ya no está activa"
                                },
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFD2D7DC)),
                                    fontSize = 11.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }
                    Text(
                        text = "Cambiar",
                        modifier = GlanceModifier
                            .padding(start = 8.dp)
                            .clickable(actionStartActivity(configureIntent)),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE0B96E)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class SessionGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SessionGlanceWidget()
}
