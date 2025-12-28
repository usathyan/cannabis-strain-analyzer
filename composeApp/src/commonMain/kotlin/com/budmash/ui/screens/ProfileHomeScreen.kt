package com.budmash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budmash.analysis.AnalysisEngine
import com.budmash.data.StrainData
import com.budmash.database.StrainDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHomeScreen(
    likedStrains: Set<String>,
    analysisEngine: AnalysisEngine,
    onAddStrain: () -> Unit,
    onStrainClick: (StrainData) -> Unit,
    onRemoveStrain: (String) -> Unit
) {
    val likedStrainData = likedStrains.mapNotNull { StrainDatabase.getStrainByName(it) }
    val idealProfile = analysisEngine.buildIdealProfile(likedStrainData)
    val hasProfile = likedStrainData.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") }
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
            // Profile summary card
            item {
                ProfileSummaryCard(
                    strainCount = likedStrainData.size,
                    hasProfile = hasProfile
                )
            }

            // Terpene profile visualization
            if (hasProfile) {
                item {
                    TerpeneProfileCard(idealProfile)
                }
            }

            // Liked strains section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Favorite Strains",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddStrain) {
                        Text("+ Add")
                    }
                }
            }

            if (likedStrainData.isEmpty()) {
                item {
                    EmptyProfileCard(onAddStrain = onAddStrain)
                }
            } else {
                items(likedStrainData) { strain ->
                    ProfileStrainCard(
                        strain = strain,
                        onClick = { onStrainClick(strain) },
                        onRemove = { onRemoveStrain(strain.name) }
                    )
                }
            }

            // Quick add section - popular strains not in profile
            item {
                QuickAddSection(
                    likedStrains = likedStrains,
                    onAddStrain = onAddStrain
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    strainCount: Int,
    hasProfile: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Profile Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (hasProfile) {
                Text(
                    text = "$strainCount strain${if (strainCount != 1) "s" else ""} in your profile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your ideal terpene profile is ready for matching!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = "Add strains you enjoy to build your profile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TerpeneProfileCard(idealProfile: List<Double>) {
    val terpeneNames = StrainData.TERPENE_NAMES
    val maxValue = idealProfile.maxOrNull() ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Terpene Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Based on MAX pooling of your favorite strains",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            idealProfile.zip(terpeneNames)
                .filter { it.first > 0.01 }
                .sortedByDescending { it.first }
                .take(6)
                .forEach { (value, name) ->
                    TerpeneBar(name = name, value = value, maxValue = maxValue)
                }
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
        Text(
            text = name,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / maxValue).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            text = String.format("%.2f", value),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyProfileCard(onAddStrain: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No strains yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Add strains you've enjoyed to build your personalized terpene profile. We'll use it to find your perfect matches!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddStrain) {
                Text("Add Your First Strain")
            }
        }
    }
}

@Composable
private fun ProfileStrainCard(
    strain: StrainData,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strain.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = when (strain.type) {
                            com.budmash.data.StrainType.INDICA -> MaterialTheme.colorScheme.primary
                            com.budmash.data.StrainType.SATIVA -> MaterialTheme.colorScheme.tertiary
                            com.budmash.data.StrainType.HYBRID -> MaterialTheme.colorScheme.secondary
                            com.budmash.data.StrainType.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
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
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun QuickAddSection(
    likedStrains: Set<String>,
    onAddStrain: () -> Unit
) {
    // Get popular strains not in profile
    val popularStrains = listOf(
        "OG Kush", "Blue Dream", "Girl Scout Cookies", "Gelato",
        "Wedding Cake", "Gorilla Glue", "Jack Herer", "Northern Lights"
    )
    val suggestions = popularStrains
        .filter { it.lowercase() !in likedStrains.map { s -> s.lowercase() } }
        .take(4)

    if (suggestions.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Quick Add Popular Strains",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { name ->
                    OutlinedButton(
                        onClick = onAddStrain,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
