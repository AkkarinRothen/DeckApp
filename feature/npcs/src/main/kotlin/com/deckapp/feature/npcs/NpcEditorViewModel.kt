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

@HiltViewModel
class NpcEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNpcByIdUseCase: GetNpcByIdUseCase,
    private val saveNpcUseCase: SaveNpcUseCase
) : ViewModel() {

    private val npcId: Long? = savedStateHandle.get<Long>("npcId")?.takeIf { it != -1L }
    
    var npc by mutableStateOf(Npc(name = ""))
        private set
        
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    init {
        npcId?.let { id ->
            viewModelScope.launch {
                getNpcByIdUseCase(id)?.let { npc = it }
            }
        }
    }

    fun updateName(name: String) { npc = npc.copy(name = name) }
    fun updateDescription(desc: String) { npc = npc.copy(description = desc) }
    fun updateHp(hp: Int) { npc = npc.copy(maxHp = hp, currentHp = hp) }
    fun updateAc(ac: Int) { npc = npc.copy(armorClass = ac) }
    fun updateInitiative(bonus: Int) { npc = npc.copy(initiativeBonus = bonus) }
    fun updateNotes(notes: String) { npc = npc.copy(notes = notes) }
    fun updateMonster(isMonster: Boolean) { npc = npc.copy(isMonster = isMonster) }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            saveNpcUseCase(npc, selectedImageUri)
            onSaved()
        }
    }
}
