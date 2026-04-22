package com.deckapp.feature.npcs

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.GetNpcByIdUseCase
import com.deckapp.core.domain.usecase.SaveNpcUseCase
import com.deckapp.core.model.Npc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

@HiltViewModel
class NpcEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNpcByIdUseCase: GetNpcByIdUseCase,
    private val saveNpcUseCase: SaveNpcUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val recorder by lazy { AudioRecorder(context) }
    private val player by lazy { AudioPlayer(context) }
    private var tempAudioFile: File? = null

    var isRecording by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var hasNewSample by mutableStateOf(false)
        private set
    var selectedVoiceUri by mutableStateOf<Uri?>(null)
        private set

    private val npcId: Long? = savedStateHandle.get<Long>("npcId")?.takeIf { it != -1L }
    
    var npc by mutableStateOf(Npc(name = ""))
        private set

    var hpInput by mutableStateOf("10")
        private set
    var acInput by mutableStateOf("10")
        private set
    var initiativeInput by mutableStateOf("0")
        private set
        
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    init {
        npcId?.let { id ->
            viewModelScope.launch {
                getNpcByIdUseCase(id)?.let { 
                    npc = it 
                    hpInput = it.maxHp.toString()
                    acInput = it.armorClass.toString()
                    initiativeInput = it.initiativeBonus.toString()
                }
            }
        }
    }

    fun updateName(name: String) { npc = npc.copy(name = name) }
    fun updateDescription(desc: String) { npc = npc.copy(description = desc) }
    fun updateHp(hp: String) { hpInput = hp }
    fun updateAc(ac: String) { acInput = ac }
    fun updateInitiative(bonus: String) { initiativeInput = bonus }
    fun updateNotes(notes: String) { npc = npc.copy(notes = notes) }
    fun updateMonster(isMonster: Boolean) { npc = npc.copy(isMonster = isMonster) }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
    }

    fun onVoiceFileSelected(uri: Uri?) {
        selectedVoiceUri = uri
        if (uri != null) {
            hasNewSample = true
            tempAudioFile = null // Si elegimos archivo, descartamos grabacion temporal
        }
    }

    // --- Voz ---

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.m4a")
            tempAudioFile = file
            recorder.start(file)
            isRecording = true
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun stopRecording() {
        recorder.stop()
        isRecording = false
        hasNewSample = true
    }

    fun playSample() {
        if (selectedVoiceUri != null) {
            isPlaying = true
            player.playUri(selectedVoiceUri!!) {
                isPlaying = false
            }
            return
        }
        
        val fileToPlay = if (hasNewSample) tempAudioFile else npc.voiceSamplePath?.let { File(it) }
        if (fileToPlay != null && fileToPlay.exists()) {
            isPlaying = true
            player.playFile(fileToPlay) {
                isPlaying = false
            }
        }
    }

    fun stopPlayback() {
        player.stop()
        isPlaying = false
    }

    fun deleteSample() {
        stopPlayback()
        tempAudioFile?.delete()
        tempAudioFile = null
        selectedVoiceUri = null
        hasNewSample = false
        npc = npc.copy(voiceSamplePath = null)
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val finalNpc = npc.copy(
                maxHp = hpInput.toIntOrNull() ?: npc.maxHp,
                currentHp = hpInput.toIntOrNull() ?: npc.currentHp,
                armorClass = acInput.toIntOrNull() ?: npc.armorClass,
                initiativeBonus = initiativeInput.toIntOrNull() ?: npc.initiativeBonus
            )
            saveNpcUseCase(
                npc = finalNpc, 
                imageUri = selectedImageUri, 
                voiceSampleFile = if (hasNewSample && tempAudioFile != null) tempAudioFile else null,
                voiceSampleUri = selectedVoiceUri
            )
            onSaved()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.stop()
        player.stop()
        tempAudioFile?.delete()
    }
}
