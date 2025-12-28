// shared/src/iosMain/kotlin/com/budmash/capture/ImageCapture.ios.kt
package com.budmash.capture

actual class ImageCapture {
    actual fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit) {
        onResult(ImageCaptureResult.Error("iOS not implemented"))
    }

    actual fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit) {
        onResult(ImageCaptureResult.Error("iOS not implemented"))
    }
}

actual interface ImageCaptureLauncher {
    actual fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    actual fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
