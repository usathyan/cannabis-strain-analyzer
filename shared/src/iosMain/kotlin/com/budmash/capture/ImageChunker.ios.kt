package com.budmash.capture

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual class ImageChunker actual constructor() {

    actual fun getImageDimensions(base64Image: String): Pair<Int, Int>? {
        return try {
            val data = NSData.create(
                base64EncodedString = base64Image,
                options = NSDataBase64DecodingIgnoreUnknownCharacters
            ) ?: return null

            val image = UIImage.imageWithData(data) ?: return null
            val width = image.size.useContents { width.toInt() }
            val height = image.size.useContents { height.toInt() }

            if (width > 0 && height > 0) {
                Pair(width, height)
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
        val data = NSData.create(
            base64EncodedString = base64Image,
            options = NSDataBase64DecodingIgnoreUnknownCharacters
        ) ?: return listOf(base64Image)

        val fullImage = UIImage.imageWithData(data) ?: return listOf(base64Image)
        val cgImage = fullImage.CGImage ?: return listOf(base64Image)

        val chunks = mutableListOf<String>()
        var currentY = 0
        var chunkIndex = 0

        while (currentY < height) {
            // Calculate chunk bounds
            val chunkTop = currentY
            val chunkBottom = minOf(currentY + maxChunkHeight, height)
            val chunkHeight = chunkBottom - chunkTop

            println("[BudMash] Creating chunk $chunkIndex: y=$chunkTop to $chunkBottom (height=$chunkHeight)")

            // Create chunk by cropping
            // Note: CGImage y-axis is flipped compared to UIKit
            val cropRect = CGRectMake(
                0.0,
                (height - chunkBottom).toDouble(),
                width.toDouble(),
                chunkHeight.toDouble()
            )

            val croppedCGImage = platform.CoreGraphics.CGImageCreateWithImageInRect(cgImage, cropRect)
            if (croppedCGImage != null) {
                val chunkUIImage = UIImage.imageWithCGImage(croppedCGImage)
                val jpegData = UIImageJPEGRepresentation(chunkUIImage, 0.85)
                if (jpegData != null) {
                    val chunkBase64 = jpegData.base64EncodedStringWithOptions(0UL)
                    chunks.add(chunkBase64)
                }
            }

            // Move to next chunk with overlap
            currentY += maxChunkHeight - OVERLAP
            chunkIndex++
        }

        println("[BudMash] Created ${chunks.size} chunks from ${height}px tall image")
        return chunks
    }
}
