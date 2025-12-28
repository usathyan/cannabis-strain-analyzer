package com.budmash.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

actual class ImageChunker actual constructor() {

    actual fun getImageDimensions(base64Image: String): Pair<Int, Int>? {
        return try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else null
        } catch (e: Exception) {
            println("[BudMash] Failed to get image dimensions: ${e.message}")
            null
        }
    }

    actual fun chunkImage(base64Image: String, maxChunkHeight: Int): List<String> {
        val dimensions = getImageDimensions(base64Image)
        if (dimensions == null) {
            println("[BudMash] Could not get dimensions, returning original image")
            return listOf(base64Image)
        }

        val (width, height) = dimensions
        println("[BudMash] Image dimensions: ${width}x${height}")

        // If image is not tall enough, return as-is
        if (height <= CHUNK_THRESHOLD) {
            println("[BudMash] Image height $height <= threshold $CHUNK_THRESHOLD, no chunking needed")
            return listOf(base64Image)
        }

        // Decode the full image
        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val fullBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return listOf(base64Image)

        val chunks = mutableListOf<String>()
        var currentY = 0
        var chunkIndex = 0

        while (currentY < height) {
            // Calculate chunk bounds
            val chunkTop = currentY
            val chunkBottom = minOf(currentY + maxChunkHeight, height)
            val chunkHeight = chunkBottom - chunkTop

            println("[BudMash] Creating chunk $chunkIndex: y=$chunkTop to $chunkBottom (height=$chunkHeight)")

            // Create chunk bitmap
            val chunkBitmap = Bitmap.createBitmap(fullBitmap, 0, chunkTop, width, chunkHeight)

            // Convert to base64
            val outputStream = ByteArrayOutputStream()
            chunkBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val chunkBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            chunks.add(chunkBase64)

            // Clean up chunk bitmap
            if (chunkBitmap != fullBitmap) {
                chunkBitmap.recycle()
            }

            // Move to next chunk with overlap
            currentY += maxChunkHeight - OVERLAP
            chunkIndex++
        }

        // Clean up full bitmap
        fullBitmap.recycle()

        println("[BudMash] Created ${chunks.size} chunks from ${height}px tall image")
        return chunks
    }
}
