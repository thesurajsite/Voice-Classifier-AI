package com.app.voiceclassifier.ui.classify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.voiceclassifier.data.audio.TitanetEmbeddingExtractor
import com.app.voiceclassifier.data.db.UserEntity
import com.app.voiceclassifier.data.db.VoiceDatabase
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class MatchResult(
    val user: UserEntity,
    val similarity: Float,
    val percent: Float
)

class ClassifyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VoiceDatabase.getInstance(application)
    private val dao = db.userDao()
    private val extractor = TitanetEmbeddingExtractor(application)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _results = MutableStateFlow<List<MatchResult>>(emptyList())
    val results: StateFlow<List<MatchResult>> = _results.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun classify(audioFloats: FloatArray) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                val users = dao.getAllUsersOnce()
                if (users.isEmpty()) {
                    _error.value = "No users enrolled. Add users first."
                    _results.value = emptyList()
                    return@launch
                }
                val queryEmbedding = extractor.extractEmbedding(audioFloats)
                Log.d("VoiceClassifier", "query: ${queryEmbedding.joinToString(", ")}")
                for (u in users) {
                    Log.d("VoiceClassifier", "${u.name}: ${u.embedding.joinToString(", ")}")
                }
                val matches = users.map { user ->
                    val sim = cosineSimilarity(queryEmbedding, user.embedding)
                    val percent = ((sim.coerceIn(-1f, 1f) + 1f) / 2f * 100f).coerceIn(0f, 100f)
                    MatchResult(user, sim, percent)
                }.sortedByDescending { it.percent }
                _results.value = matches
            } catch (e: Exception) {
                _error.value = e.message ?: "Classification failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
    }

    fun setElapsed(value: Int) {
        _elapsed.value = value
    }

    fun clearResults() {
        _results.value = emptyList()
        _error.value = null
        _elapsed.value = 0
    }

    fun clearError() {
        _error.value = null
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            val minSize = minOf(a.size, b.size)
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in 0 until minSize) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt(normA) * sqrt(normB)
            return if (denom < 1e-6f) 0f else dot / denom
        }
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot.coerceIn(-1f, 1f)
    }
}
