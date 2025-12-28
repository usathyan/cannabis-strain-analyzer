// shared/src/commonMain/kotlin/com/budmash/capture/ImageCapture.kt
package com.budmash.capture

sealed class ImageCaptureResult {
    data class Success(val base64Image: String, val width: Int, val height: Int) : ImageCaptureResult()
    data class Error(val message: String) : ImageCaptureResult()
    data object Cancelled : ImageCaptureResult()
}

expect class ImageCapture {
    fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit)
    fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit)
}

object ImageCaptureContext {
    var launcher: ImageCaptureLauncher? = null
}

expect interface ImageCaptureLauncher {
    fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
