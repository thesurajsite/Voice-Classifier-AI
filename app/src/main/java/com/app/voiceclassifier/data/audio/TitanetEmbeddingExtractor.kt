package com.app.voiceclassifier.data.audio

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TitanetEmbeddingExtractor(private val context: Context) {

    companion object {
        @Volatile private var cachedExtractor: SpeakerEmbeddingExtractor? = null
        @Volatile private var cachedModelPath: String? = null
    }

    private fun getModelPath(): String {
        cachedModelPath?.let { if (File(it).exists()) return it }
        val dest = File(context.filesDir, "models/embedding/titanet_large.onnx")
        if (dest.exists()) {
            cachedModelPath = dest.absolutePath
            return dest.absolutePath
        }
        dest.parentFile?.mkdirs()
        val candidates = listOf(
            "models/embedding/titanet_large.onnx",
            "titanet_large.onnx",
            "titanet-large.onnx",
            "models/titanet_large.onnx"
        )
        var copied = false
        for (candidate in candidates) {
            try {
                context.assets.open(candidate).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                Log.d("SherpaEmbedding", "Copied $candidate -> ${dest.absolutePath} (${dest.length()} bytes)")
                copied = true
                break
            } catch (_: Exception) {
            }
        }
        if (!copied) {
            throw IllegalStateException(
                "titanet_large.onnx not found in assets. Place it at assets/titanet_large.onnx or assets/models/embedding/titanet_large.onnx (found candidates tried: $candidates)"
            )
        }
        cachedModelPath = dest.absolutePath
        return dest.absolutePath
    }

    private fun getOrCreateExtractor(): SpeakerEmbeddingExtractor {
        cachedExtractor?.let { return it }
        synchronized(this) {
            cachedExtractor?.let { return it }
            val path = getModelPath()
            val config = SpeakerEmbeddingExtractorConfig(
                model = path,
                numThreads = 2,
                debug = false,
                provider = "cpu"
            )
            val extractor = try {
                SpeakerEmbeddingExtractor(null, config)
            } catch (e: Exception) {
                Log.w("SherpaEmbedding", "null AssetManager init failed, trying with assets: ${e.message}")
                SpeakerEmbeddingExtractor(context.assets, config)
            }
            Log.d("SherpaEmbedding", "Extractor ready dim=${extractor.dim()} model=$path")
            cachedExtractor = extractor
            return extractor
        }
    }

    suspend fun extractEmbedding(audioFloats: FloatArray): FloatArray = withContext(Dispatchers.Default) {
        if (audioFloats.isEmpty()) throw IllegalArgumentException("Empty audio")
        val extractor = getOrCreateExtractor()
        val stream = extractor.createStream()
        try {
            stream.acceptWaveform(audioFloats, 16000)
            stream.inputFinished()
            if (!extractor.isReady(stream)) {
                Log.w("SherpaEmbedding", "Extractor not ready after inputFinished")
            }
            val embedding = extractor.compute(stream)
                ?: throw IllegalStateException("Sherpa returned null embedding")
            if (embedding.isEmpty()) throw IllegalStateException("Sherpa returned empty embedding")
            val norm = kotlin.math.sqrt(embedding.fold(0f) { acc, v -> acc + v * v })
            if (norm > 1e-6f) {
                for (i in embedding.indices) embedding[i] /= norm
            }
            Log.d("SherpaEmbedding", "Generated embedding dim=${embedding.size} norm=$norm")
            embedding
        } finally {
            stream.release()
        }
    }
}
