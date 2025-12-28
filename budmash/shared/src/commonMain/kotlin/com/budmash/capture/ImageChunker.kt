package com.budmash.capture

/**
 * Splits tall images into smaller chunks for vision API processing.
 * Vision APIs have resolution limits and will downsample large images,
 * losing text detail needed for strain name extraction.
 */
expect class ImageChunker() {
    /**
     * Get the dimensions of a base64-encoded image.
     * Returns Pair(width, height) or null if unable to decode.
     */
    fun getImageDimensions(base64Image: String): Pair<Int, Int>?

    /**
     * Split a tall image into overlapping chunks.
     * Each chunk will be maxChunkHeight pixels tall with overlap.
     * Returns list of base64-encoded chunk images.
     */
    fun chunkImage(base64Image: String, maxChunkHeight: Int = 3000): List<String>
}

/** Maximum height per chunk (pixels) - keeps detail readable */
const val MAX_CHUNK_HEIGHT: Int = 3000
/** Overlap between chunks to catch strains at boundaries */
const val OVERLAP: Int = 200
/** Threshold above which we chunk the image */
const val CHUNK_THRESHOLD: Int = 4000
