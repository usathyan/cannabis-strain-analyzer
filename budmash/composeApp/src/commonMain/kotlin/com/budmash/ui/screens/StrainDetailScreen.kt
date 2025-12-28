package com.budmash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budmash.data.StrainData
import com.budmash.data.StrainType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StrainDetailScreen(
    strain: StrainData,
    isLiked: Boolean,
    isDisliked: Boolean,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strain Details") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Name, Type, THC, Price
            item {
                StrainHeader(strain)
            }

            // Description
            if (strain.description.isNotBlank()) {
                item {
                    Text(
                        text = strain.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Effects
            if (strain.effects.isNotEmpty()) {
                item {
                    ChipSection(title = "Effects", items = strain.effects)
                }
            }

            // Flavors
            if (strain.flavors.isNotEmpty()) {
                item {
                    ChipSection(title = "Flavors", items = strain.flavors)
                }
            }

            // Terpene Profile
            item {
                TerpeneChart(strain)
            }

            // Like/Dislike buttons
            item {
                LikeDislikeRow(
                    isLiked = isLiked,
                    isDisliked = isDisliked,
                    onLike = onLike,
                    onDislike = onDislike
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StrainHeader(strain: StrainData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Strain name
        Text(
            text = strain.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Type badge + THC/CBD + Price row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // THC
            strain.thcMax?.let { thc ->
                Text(
                    text = "${formatPercent(thc)} THC",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // CBD
            strain.cbdMax?.let { cbd ->
                if (cbd > 0) {
                    Text(
                        text = "${formatPercent(cbd)} CBD",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Price
            strain.price?.let { price ->
                Text(
                    text = "$${price.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                SuggestionChip(
                    onClick = { },
                    label = { Text(item) }
                )
            }
        }
    }
}

@Composable
private fun TerpeneChart(strain: StrainData) {
    val terpenes = strain.terpeneProfile()
        .zip(StrainData.TERPENE_NAMES)
        .filter { it.first > 0 }
        .sortedByDescending { it.first }

    if (terpenes.isEmpty()) return

    val maxValue = terpenes.maxOfOrNull { it.first } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Terpene Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        terpenes.forEach { (value, name) ->
            TerpeneBar(name = name, value = value, maxValue = maxValue)
        }
    }
}

@Composable
private fun TerpeneBar(name: String, value: Double, maxValue: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Terpene name
        Text(
            text = name,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / maxValue).toFloat())
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Value
        Text(
            text = String.format("%.2f", value),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LikeDislikeRow(
    isLiked: Boolean,
    isDisliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Like button
        Button(
            onClick = onLike,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLiked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isLiked)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLiked) "Liked" else "Like")
        }

        // Dislike button
        Button(
            onClick = onDislike,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDisliked)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isDisliked)
                    MaterialTheme.colorScheme.onError
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isDisliked) "Disliked" else "Dislike")
        }
    }
}

private fun formatPercent(value: Double): String {
    return if (value >= 1) {
        String.format("%.1f%%", value)
    } else {
        String.format("%.1f%%", value * 100)
    }
}
