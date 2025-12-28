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
                StrainCard(
                    result = result,
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
    hasProfile: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.strain.name,
                    style = MaterialTheme.typography.titleMedium
                )
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

            // Like/Dislike buttons
            Row {
                IconButton(onClick = onLike) {
                    Icon(Icons.Default.Check, "Like", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDislike) {
                    Icon(Icons.Default.Close, "Dislike", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
