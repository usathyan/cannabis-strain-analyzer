// shared/src/androidMain/kotlin/com/budmash/capture/ImageCapture.android.kt
package com.budmash.capture

actual class ImageCapture {
    actual fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized"))
            return
        }
        launcher.launchCamera(onResult)
    }

    actual fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized"))
            return
        }
        launcher.launchGallery(onResult)
    }
}

actual interface ImageCaptureLauncher {
    actual fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    actual fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
