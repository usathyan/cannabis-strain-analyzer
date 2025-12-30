// shared/src/iosMain/kotlin/com/budmash/capture/ImageCapture.ios.kt
package com.budmash.capture

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.*
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.darwin.NSObject

actual class ImageCapture {
    actual fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized. Call initializeIosImageCapture() first."))
            return
        }
        launcher.launchCamera(onResult)
    }

    actual fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized. Call initializeIosImageCapture() first."))
            return
        }
        launcher.launchGallery(onResult)
    }
}

actual interface ImageCaptureLauncher {
    actual fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    actual fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}

// Singleton holder
private object IosImageCaptureLauncherHolder {
    var launcher: IosImageCaptureLauncherImpl? = null
    var delegate: ImagePickerDelegate? = null
}

fun initializeIosImageCapture() {
    if (IosImageCaptureLauncherHolder.launcher == null) {
        val delegate = ImagePickerDelegate()
        val launcher = IosImageCaptureLauncherImpl(delegate)
        IosImageCaptureLauncherHolder.delegate = delegate
        IosImageCaptureLauncherHolder.launcher = launcher
    }
    ImageCaptureContext.launcher = IosImageCaptureLauncherHolder.launcher
}

// Pure Kotlin implementation of the launcher interface
class IosImageCaptureLauncherImpl(private val delegate: ImagePickerDelegate) : ImageCaptureLauncher {

    @OptIn(ExperimentalForeignApi::class)
    override fun launchCamera(onResult: (ImageCaptureResult) -> Unit) {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            onResult(ImageCaptureResult.Error("Camera not available"))
            return
        }

        delegate.setCallback(onResult)

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.delegate = delegate
        picker.allowsEditing = false

        presentPicker(picker)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun launchGallery(onResult: (ImageCaptureResult) -> Unit) {
        delegate.setCallback(onResult)

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate
        picker.allowsEditing = false

        presentPicker(picker)
    }

    private fun presentPicker(picker: UIImagePickerController) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController == null) {
            delegate.invokeCallback(ImageCaptureResult.Error("No root view controller available"))
            return
        }

        // Find the topmost presented controller
        var topController: UIViewController = rootViewController
        while (topController.presentedViewController != null) {
            topController = topController.presentedViewController ?: break
        }

        topController.presentViewController(picker, animated = true, completion = null)
    }
}

// Objective-C delegate class (extends NSObject only)
@OptIn(ExperimentalForeignApi::class)
class ImagePickerDelegate : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    private var currentCallback: ((ImageCaptureResult) -> Unit)? = null

    fun setCallback(callback: (ImageCaptureResult) -> Unit) {
        currentCallback = callback
    }

    fun invokeCallback(result: ImageCaptureResult) {
        currentCallback?.invoke(result)
        currentCallback = null
    }

    // UIImagePickerControllerDelegate methods
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)

        @Suppress("UNCHECKED_CAST")
        val info = didFinishPickingMediaWithInfo as Map<String, Any?>

        val image = (info[UIImagePickerControllerOriginalImage] as? UIImage)

        if (image == null) {
            invokeCallback(ImageCaptureResult.Error("Failed to get image"))
            return
        }

        processImage(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        invokeCallback(ImageCaptureResult.Cancelled)
    }

    private fun processImage(image: UIImage) {
        // Scale down if too large (max 1920px on longest side)
        val scaledImage = scaleImage(image, maxSize = 1920.0)

        // Convert to JPEG data
        val jpegData = UIImageJPEGRepresentation(scaledImage, 0.85)

        if (jpegData == null) {
            invokeCallback(ImageCaptureResult.Error("Failed to convert image to JPEG"))
            return
        }

        // Convert to base64
        val base64String = jpegData.base64EncodedStringWithOptions(0u)

        val width = scaledImage.size.useContents { width.toInt() }
        val height = scaledImage.size.useContents { height.toInt() }

        invokeCallback(ImageCaptureResult.Success(base64String, width, height))
    }

    private fun scaleImage(image: UIImage, maxSize: Double): UIImage {
        val width = image.size.useContents { width }
        val height = image.size.useContents { height }

        if (width <= maxSize && height <= maxSize) {
            return image
        }

        val ratio = minOf(maxSize / width, maxSize / height)
        val newWidth = width * ratio
        val newHeight = height * ratio

        val newSize = CGSizeMake(newWidth, newHeight)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
        val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return scaledImage ?: image
    }
}
