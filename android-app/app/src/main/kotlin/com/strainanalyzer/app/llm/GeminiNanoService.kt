package com.strainanalyzer.app.llm

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for on-device Gemini Nano via ML Kit GenAI
 * Supports Pixel 9+ and other compatible devices
 */
class GeminiNanoService private constructor(private val context: Context) {

    private var generativeModel: GenerativeModel? = null

    private val _status = MutableStateFlow<NanoStatus>(NanoStatus.Checking)
    val status: StateFlow<NanoStatus> = _status

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    sealed class NanoStatus {
        object Checking : NanoStatus()
        object Available : NanoStatus()
        object Downloading : NanoStatus()
        object Downloadable : NanoStatus()
        object Unavailable : NanoStatus()
        data class Error(val message: String) : NanoStatus()
    }

    /**
     * Initialize and check Gemini Nano availability
     */
    suspend fun initialize() {
        try {
            _status.value = NanoStatus.Checking
            generativeModel = Generation.getClient()
            checkStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemini Nano", e)
            _status.value = NanoStatus.Error(e.message ?: "Initialization failed")
        }
    }

    /**
     * Check the current status of Gemini Nano on this device
     */
    suspend fun checkStatus() {
        val model = generativeModel ?: run {
            _status.value = NanoStatus.Unavailable
            return
        }

        try {
            when (val featureStatus = model.checkStatus()) {
                FeatureStatus.AVAILABLE -> {
                    Log.d(TAG, "Gemini Nano is available")
                    _status.value = NanoStatus.Available
                }
                FeatureStatus.DOWNLOADABLE -> {
                    Log.d(TAG, "Gemini Nano is downloadable")
                    _status.value = NanoStatus.Downloadable
                }
                FeatureStatus.DOWNLOADING -> {
                    Log.d(TAG, "Gemini Nano is downloading")
                    _status.value = NanoStatus.Downloading
                }
                FeatureStatus.UNAVAILABLE -> {
                    Log.d(TAG, "Gemini Nano is unavailable on this device")
                    _status.value = NanoStatus.Unavailable
                }
                else -> {
                    Log.w(TAG, "Unknown Gemini Nano status: $featureStatus")
                    _status.value = NanoStatus.Unavailable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Gemini Nano status", e)
            _status.value = NanoStatus.Error(e.message ?: "Status check failed")
        }
    }

    /**
     * Download Gemini Nano if available
     */
    suspend fun download() {
        val model = generativeModel ?: return

        try {
            _status.value = NanoStatus.Downloading
            model.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is com.google.mlkit.genai.common.DownloadStatus.DownloadStarted -> {
                        Log.d(TAG, "Gemini Nano download started")
                        _downloadProgress.value = 0f
                    }
                    is com.google.mlkit.genai.common.DownloadStatus.DownloadProgress -> {
                        val progress = downloadStatus.totalBytesDownloaded.toFloat()
                        Log.d(TAG, "Gemini Nano download progress: $progress bytes")
                        // Note: We don't have total size, so just show activity
                        _downloadProgress.value = (progress / 1_000_000_000f).coerceAtMost(0.99f)
                    }
                    com.google.mlkit.genai.common.DownloadStatus.DownloadCompleted -> {
                        Log.d(TAG, "Gemini Nano download completed")
                        _downloadProgress.value = 1f
                        _status.value = NanoStatus.Available
                    }
                    is com.google.mlkit.genai.common.DownloadStatus.DownloadFailed -> {
                        Log.e(TAG, "Gemini Nano download failed")
                        _status.value = NanoStatus.Error("Download failed")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Gemini Nano", e)
            _status.value = NanoStatus.Error(e.message ?: "Download failed")
        }
    }

    /**
     * Check if Gemini Nano is ready for use
     */
    fun isAvailable(): Boolean = _status.value == NanoStatus.Available

    /**
     * Check if Gemini Nano can be downloaded
     */
    fun isDownloadable(): Boolean = _status.value == NanoStatus.Downloadable

    /**
     * Generate content using Gemini Nano
     */
    suspend fun generate(prompt: String): String? {
        if (!isAvailable()) {
            Log.w(TAG, "Gemini Nano not available for generation")
            return null
        }

        val model = generativeModel ?: return null

        return try {
            val response = model.generateContent(prompt)
            response.candidates.firstOrNull()?.text
        } catch (e: Exception) {
            Log.e(TAG, "Error generating with Gemini Nano", e)
            null
        }
    }

    /**
     * Generate strain data JSON using Gemini Nano
     */
    suspend fun generateStrainData(strainName: String): String? {
        val prompt = """
            Generate cannabis strain information for "$strainName" in JSON format.
            Include these fields:
            - name: the strain name
            - type: indica, sativa, or hybrid
            - thc_range: typical THC percentage range (e.g., "18-24%")
            - cbd_range: typical CBD percentage range (e.g., "0.1-0.5%")
            - effects: array of common effects (e.g., ["relaxed", "happy", "euphoric"])
            - flavors: array of flavor notes (e.g., ["earthy", "pine", "citrus"])
            - terpenes: object with terpene names and approximate percentages

            Return ONLY valid JSON, no explanation or markdown.
        """.trimIndent()

        return generate(prompt)
    }

    companion object {
        private const val TAG = "GeminiNanoService"

        @Volatile
        private var instance: GeminiNanoService? = null

        fun getInstance(context: Context): GeminiNanoService {
            return instance ?: synchronized(this) {
                instance ?: GeminiNanoService(context.applicationContext).also { instance = it }
            }
        }
    }
}
