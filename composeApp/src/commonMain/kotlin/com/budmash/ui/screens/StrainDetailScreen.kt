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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import com.budmash.data.StrainType

// Terpene effect mappings for AI insights
private val TERPENE_EFFECTS = mapOf(
    "Myrcene" to listOf("relaxation", "sedation", "body high", "muscle relief"),
    "Limonene" to listOf("mood elevation", "stress relief", "energy", "focus"),
    "Caryophyllene" to listOf("anti-inflammatory", "pain relief", "anxiety relief"),
    "Pinene" to listOf("alertness", "memory retention", "creativity", "focus"),
    "Linalool" to listOf("calming", "sleep aid", "anxiety relief", "relaxation"),
    "Humulene" to listOf("appetite suppression", "anti-inflammatory", "earthy calm"),
    "Terpinolene" to listOf("uplifting", "creative", "slightly sedating"),
    "Ocimene" to listOf("energizing", "uplifting", "decongestant"),
    "Nerolidol" to listOf("sedation", "relaxation", "anti-anxiety"),
    "Bisabolol" to listOf("anti-inflammatory", "skin soothing", "calming"),
    "Eucalyptol" to listOf("mental clarity", "focus", "respiratory relief")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StrainDetailScreen(
    strain: StrainData,
    similarity: SimilarityResult? = null,
    idealProfile: List<Double> = emptyList(),
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

            // Match Score Card (if similarity data available)
            similarity?.let { sim ->
                item {
                    MatchScoreCard(sim)
                }
            }

            // AI Insights (if profile exists)
            if (idealProfile.isNotEmpty()) {
                item {
                    TerpeneInsightsCard(
                        strain = strain,
                        idealProfile = idealProfile,
                        matchPercent = similarity?.let { (it.overallScore * 100).toInt() }
                    )
                }
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

@Composable
private fun MatchScoreCard(similarity: SimilarityResult) {
    val matchPercent = (similarity.overallScore * 100).toInt()
    val matchColor = when {
        matchPercent >= 80 -> Color(0xFF4CAF50)
        matchPercent >= 60 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = matchColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall score header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile Match",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = matchColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$matchPercent%",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (matchPercent >= 60) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Breakdown section
            Text(
                text = "Similarity Breakdown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cosine Similarity (50% weight)
            SimilarityBreakdownRow(
                name = "Z-Scored Cosine",
                score = similarity.cosineScore,
                weight = "50%",
                description = "Direction alignment of terpene profiles"
            )

            // Euclidean Similarity (25% weight)
            SimilarityBreakdownRow(
                name = "Euclidean Distance",
                score = similarity.euclideanScore,
                weight = "25%",
                description = "Overall magnitude similarity"
            )

            // Pearson Correlation (25% weight)
            SimilarityBreakdownRow(
                name = "Pearson Correlation",
                score = similarity.pearsonScore,
                weight = "25%",
                description = "Pattern correlation in profile"
            )
        }
    }
}

@Composable
private fun SimilarityBreakdownRow(
    name: String,
    score: Double,
    weight: String,
    description: String
) {
    val scorePercent = (score * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = weight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$scorePercent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    scorePercent >= 80 -> Color(0xFF4CAF50)
                    scorePercent >= 60 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score.toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            scorePercent >= 80 -> Color(0xFF4CAF50)
                            scorePercent >= 60 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

@Composable
private fun TerpeneInsightsCard(
    strain: StrainData,
    idealProfile: List<Double>,
    matchPercent: Int?
) {
    val strainProfile = strain.terpeneProfile()
    val terpeneNames = StrainData.TERPENE_NAMES

    // Find top matching terpenes (both strain and profile have significant values)
    val matchingTerpenes = terpeneNames.indices
        .filter { i -> strainProfile[i] > 0.01 && idealProfile[i] > 0.01 }
        .sortedByDescending { i -> minOf(strainProfile[i], idealProfile[i]) }
        .take(3)
        .map { i -> terpeneNames[i] }

    // Find dominant terpenes in strain
    val dominantTerpenes = strainProfile
        .zip(terpeneNames)
        .filter { it.first > 0.1 }
        .sortedByDescending { it.first }
        .take(3)
        .map { it.second }

    // Find terpenes you want but strain lacks
    val missingTerpenes = terpeneNames.indices
        .filter { i -> idealProfile[i] > 0.2 && strainProfile[i] < 0.05 }
        .map { i -> terpeneNames[i] }

    // Generate insights
    val insights = buildList {
        // Main recommendation
        if (matchPercent != null) {
            if (matchPercent >= 80) {
                add("Excellent match! This strain closely aligns with your terpene preferences.")
            } else if (matchPercent >= 60) {
                add("Good match. This strain has several terpenes you enjoy.")
            } else if (matchPercent >= 40) {
                add("Moderate match. Worth trying if you're open to exploration.")
            } else {
                add("This strain differs from your usual preferences—could be interesting or not your style.")
            }
        }

        // Matching terpenes insight
        if (matchingTerpenes.isNotEmpty()) {
            val effects = matchingTerpenes.flatMap { TERPENE_EFFECTS[it] ?: emptyList() }.distinct().take(4)
            add("Your profile shares ${matchingTerpenes.joinToString(", ")} with this strain, suggesting ${effects.joinToString(", ")}.")
        }

        // Dominant terpene effects
        if (dominantTerpenes.isNotEmpty()) {
            val allEffects = dominantTerpenes.flatMap { TERPENE_EFFECTS[it] ?: emptyList() }.distinct().take(5)
            add("Dominant terpenes suggest: ${allEffects.joinToString(", ")}.")
        }

        // Missing terpenes
        if (missingTerpenes.isNotEmpty() && missingTerpenes.size <= 2) {
            add("Note: lower in ${missingTerpenes.joinToString(", ")} compared to your profile.")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            insights.forEach { insight ->
                Text(
                    text = "• $insight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Expected effects based on strain's terpenes
            if (dominantTerpenes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Expected Effects",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val topEffects = dominantTerpenes
                        .flatMap { TERPENE_EFFECTS[it] ?: emptyList() }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(4)
                        .map { it.key }

                    topEffects.forEach { effect ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = effect,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
            text = formatDecimal(value, 2),
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
    val percentage = if (value >= 1) value else value * 100
    val rounded = kotlin.math.round(percentage * 10) / 10
    return "${rounded}%"
}

private fun formatDecimal(value: Double, decimals: Int): String {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    return rounded.toString()
}
