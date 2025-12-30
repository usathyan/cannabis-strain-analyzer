package com.budmash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.budmash.capture.ImageCaptureContext
import com.budmash.capture.ImageCaptureLauncher
import com.budmash.capture.ImageCaptureResult
import com.budmash.llm.LlmConfigStorage
import com.budmash.profile.ProfileStorageContext
import com.budmash.ui.App
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "BudMash"

class MainActivity : ComponentActivity(), ImageCaptureLauncher {

    private var cameraCallback: ((ImageCaptureResult) -> Unit)? = null
    private var galleryCallback: ((ImageCaptureResult) -> Unit)? = null
    private var photoUri: Uri? = null

    // Launchers must be registered before STARTED state
    private lateinit var takePictureLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Register activity result launchers BEFORE super.onCreate()
        // This ensures they're registered before the activity reaches STARTED state
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val callback = cameraCallback
            cameraCallback = null

            if (success && photoUri != null) {
                processImage(photoUri!!, callback)
            } else {
                callback?.invoke(ImageCaptureResult.Cancelled)
            }
        }

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            val callback = galleryCallback
            galleryCallback = null

            if (uri != null) {
                processImage(uri, callback)
            } else {
                callback?.invoke(ImageCaptureResult.Cancelled)
            }
        }

        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started")

        // Initialize contexts
        LlmConfigStorage.init(this)
        ProfileStorageContext.init(this)
        ImageCaptureContext.launcher = this
        Log.d(TAG, "All contexts initialized")

        setContent {
            App()
        }
    }

    override fun launchCamera(onResult: (ImageCaptureResult) -> Unit) {
        cameraCallback = onResult
        val photoFile = File(cacheDir, "menu_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri!!)
    }

    override fun launchGallery(onResult: (ImageCaptureResult) -> Unit) {
        galleryCallback = onResult
        pickImageLauncher.launch("image/*")
    }

    private fun processImage(uri: Uri, callback: ((ImageCaptureResult) -> Unit)?) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                callback?.invoke(ImageCaptureResult.Error("Failed to decode image"))
                return
            }

            // For tall scroll screenshots (height > 2x width), preserve full height for chunking
            // Only scale width to max 2000px to keep file size reasonable
            // For regular images, scale longest side to 1920px
            val scaledBitmap = if (bitmap.height > bitmap.width * 2) {
                // Tall image - preserve height for proper chunking, just limit width
                scaleBitmapByWidth(bitmap, 2000)
            } else {
                // Regular image - scale by longest side
                scaleBitmap(bitmap, 1920)
            }

            // Convert to base64 JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            Log.d(TAG, "Image processed: ${scaledBitmap.width}x${scaledBitmap.height}, base64 length: ${base64.length}")

            callback?.invoke(ImageCaptureResult.Success(base64, scaledBitmap.width, scaledBitmap.height))

            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            callback?.invoke(ImageCaptureResult.Error("Failed to process image: ${e.message}"))
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun scaleBitmapByWidth(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) return bitmap

        val ratio = maxWidth.toFloat() / width
        val newWidth = maxWidth
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
