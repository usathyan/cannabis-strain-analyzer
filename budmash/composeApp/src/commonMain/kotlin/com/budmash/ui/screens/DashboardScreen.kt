package com.budmash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import kotlin.math.round

@Composable
fun DashboardScreen(
    strains: List<SimilarityResult>,
    likedStrains: Set<String> = emptySet(),
    dislikedStrains: Set<String> = emptySet(),
    hasProfile: Boolean,
    onStrainClick: (StrainData) -> Unit,
    onLikeClick: (StrainData) -> Unit,
    onDislikeClick: (StrainData) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strains.size} flowers found",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!hasProfile) {
                    Text(
                        text = "Mark strains to build profile",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Strain list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(strains) { result ->
                val isLiked = result.strain.name in likedStrains
                val isDisliked = result.strain.name in dislikedStrains
                StrainCard(
                    result = result,
                    isLiked = isLiked,
                    isDisliked = isDisliked,
                    hasProfile = hasProfile,
                    onClick = { onStrainClick(result.strain) },
                    onLike = { onLikeClick(result.strain) },
                    onDislike = { onDislikeClick(result.strain) }
                )
            }
        }
    }
}

@Composable
private fun StrainCard(
    result: SimilarityResult,
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    hasProfile: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    // Visual feedback for liked/disliked state
    val cardColor = when {
        isLiked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isDisliked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.strain.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Show badge for liked/disliked
                    if (isLiked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Liked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (isDisliked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Disliked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = result.strain.type.name,
                    style = MaterialTheme.typography.bodySmall
                )
                // Top terpenes
                val topTerpenes = result.strain.terpeneProfile()
                    .zip(StrainData.TERPENE_NAMES)
                    .sortedByDescending { it.first }
                    .take(3)
                    .filter { it.first > 0 }
                    .joinToString(", ") { "${it.second} ${round(it.first * 100) / 100}" }
                if (topTerpenes.isNotEmpty()) {
                    Text(
                        text = topTerpenes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Match score (if profile exists)
            if (hasProfile) {
                Text(
                    text = "${(result.overallScore * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = when {
                        result.overallScore >= 0.8 -> MaterialTheme.colorScheme.primary
                        result.overallScore >= 0.6 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Like/Dislike buttons with filled state
            Row {
                IconButton(onClick = onLike) {
                    Icon(
                        Icons.Default.Check,
                        "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDislike) {
                    Icon(
                        Icons.Default.Close,
                        "Dislike",
                        tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
