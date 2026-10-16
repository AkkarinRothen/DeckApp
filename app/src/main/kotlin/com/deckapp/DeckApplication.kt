package com.deckapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class DeckApplication : Application() {
    @Inject lateinit var artworkUpdater: com.deckapp.app.BundledArtworkUpdater
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                artworkUpdater.updateInstalledArtwork()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("BundledArtwork", "No se pudieron actualizar las ilustraciones del paquete.", e)
            }
        }
    }
}
