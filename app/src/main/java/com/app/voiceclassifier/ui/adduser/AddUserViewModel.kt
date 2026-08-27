package com.app.voiceclassifier.ui.adduser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.voiceclassifier.data.audio.TitanetEmbeddingExtractor
import com.app.voiceclassifier.data.db.UserEntity
import com.app.voiceclassifier.data.db.VoiceDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddUserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VoiceDatabase.getInstance(application)
    private val dao = db.userDao()
    private val extractor = TitanetEmbeddingExtractor(application)

    val users: StateFlow<List<UserEntity>> = dao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun saveUser(name: String, audioFloats: FloatArray, onDone: () -> Unit) {
        if (name.isBlank()) {
            _saveError.value = "Name cannot be empty"
            return
        }
        if (audioFloats.isEmpty()) {
            _saveError.value = "No audio recorded"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val embedding = extractor.extractEmbedding(audioFloats)
                val entity = UserEntity(name = name.trim(), embedding = embedding)
                dao.insert(entity)
                onDone()
            } catch (e: Exception) {
                _saveError.value = e.message ?: "Failed to save user"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun clearError() {
        _saveError.value = null
    }
}
