package com.budmash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budmash.analysis.AnalysisEngine
import com.budmash.capture.ImageCapture
import com.budmash.capture.ImageCaptureResult
import com.budmash.data.DispensaryMenu
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.database.StrainDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    hasApiKey: Boolean,
    hasProfile: Boolean,
    idealProfile: List<Double>,
    analysisEngine: AnalysisEngine,
    likedStrains: Set<String>,
    dislikedStrains: Set<String>,
    lastScannedMenu: DispensaryMenu?,
    onPhotoCapture: (String) -> Unit,
    onViewLastScan: () -> Unit,
    onStrainClick: (StrainData, SimilarityResult?) -> Unit,
    onLikeClick: (StrainData) -> Unit,
    onDislikeClick: (StrainData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture() }

    // Calculate similarity for search results
    val searchResults = remember(searchQuery, idealProfile) {
        val strains = StrainDatabase.searchStrains(searchQuery)
        strains.map { strain ->
            if (hasProfile) {
                analysisEngine.calculateMatch(idealProfile, strain)
            } else {
                SimilarityResult(
                    strain = strain,
                    overallScore = 0.0,
                    cosineScore = 0.0,
                    euclideanScore = 0.0,
                    pearsonScore = 0.0
                )
            }
        }.sortedByDescending { it.overallScore }
    }

    fun handleCaptureResult(result: ImageCaptureResult) {
        isCapturing = false
        when (result) {
            is ImageCaptureResult.Success -> {
                errorMessage = null
                onPhotoCapture(result.base64Image)
            }
            is ImageCaptureResult.Error -> {
                errorMessage = result.message
            }
            is ImageCaptureResult.Cancelled -> {
                // User cancelled
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo capture section
            item {
                PhotoCaptureCard(
                    hasApiKey = hasApiKey,
                    isCapturing = isCapturing,
                    errorMessage = errorMessage,
                    hasLastScan = lastScannedMenu != null,
                    lastScanCount = lastScannedMenu?.strains?.size ?: 0,
                    onTakePhoto = {
                        isCapturing = true
                        errorMessage = null
                        imageCapture.captureFromCamera { result ->
                            handleCaptureResult(result)
                        }
                    },
                    onPickPhoto = {
                        isCapturing = true
                        errorMessage = null
                        imageCapture.pickFromGallery { result ->
                            handleCaptureResult(result)
                        }
                    },
                    onViewLastScan = onViewLastScan
                )
            }

            // Divider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  OR search by name  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }

            // Text search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search strains by name, effect, flavor...") },
                    singleLine = true
                )
            }

            // Profile status hint
            if (!hasProfile && searchQuery.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Add strains to your profile to see match percentages",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Search results
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "${searchResults.size} results",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(searchResults) { result ->
                    SearchResultCard(
                        result = result,
                        hasProfile = hasProfile,
                        isLiked = result.strain.name in likedStrains,
                        isDisliked = result.strain.name in dislikedStrains,
                        onClick = { onStrainClick(result.strain, result) },
                        onLike = { onLikeClick(result.strain) },
                        onDislike = { onDislikeClick(result.strain) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PhotoCaptureCard(
    hasApiKey: Boolean,
    isCapturing: Boolean,
    errorMessage: String?,
    hasLastScan: Boolean,
    lastScanCount: Int,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onViewLastScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scan Dispensary Menu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Take a photo of a menu to analyze strains with AI",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasApiKey) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "API key required - configure in Settings",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    enabled = !isCapturing && hasApiKey,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isCapturing) "Capturing..." else "Take Photo")
                }
                OutlinedButton(
                    onClick = onPickPhoto,
                    enabled = !isCapturing && hasApiKey
                ) {
                    Text("Gallery")
                }
            }

            // Show button to view last scan results
            if (hasLastScan) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onViewLastScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("View Last Scan ($lastScanCount strains)")
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SimilarityResult,
    hasProfile: Boolean,
    isLiked: Boolean,
    isDisliked: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val strain = result.strain

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strain.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (strain.type) {
                                StrainType.INDICA -> MaterialTheme.colorScheme.primary
                                StrainType.SATIVA -> MaterialTheme.colorScheme.tertiary
                                StrainType.HYBRID -> MaterialTheme.colorScheme.secondary
                                StrainType.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = strain.type.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        strain.thcMax?.let { thc ->
                            Text(
                                text = "${thc.toInt()}% THC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Match percentage
                if (hasProfile) {
                    val matchPercent = (result.overallScore * 100).toInt()
                    Surface(
                        color = when {
                            matchPercent >= 80 -> Color(0xFF4CAF50)
                            matchPercent >= 60 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$matchPercent%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (matchPercent >= 60) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Effects
            if (strain.effects.isNotEmpty()) {
                Text(
                    text = strain.effects.take(4).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Like/Dislike buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isLiked,
                    onClick = onLike,
                    label = { Text(if (isLiked) "Liked" else "Like") }
                )
                FilterChip(
                    selected = isDisliked,
                    onClick = onDislike,
                    label = { Text(if (isDisliked) "Disliked" else "Dislike") }
                )
            }
        }
    }
}
