package com.strainanalyzer.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Horizontal bar chart for displaying terpene profiles
 */
@Composable
fun TerpeneProfileChart(
    title: String,
    profile: Map<String, Double>,
    terpeneOrder: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF4CAF50),
    showPercentages: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)

    // Filter to only terpenes with values > 0
    val activeTerpenes = terpeneOrder.filter { (profile[it] ?: 0.0) > 0.001 }
    val maxValue = profile.values.maxOrNull() ?: 0.01

    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (activeTerpenes.isEmpty()) {
            Text(
                text = "No terpene data available",
                fontSize = 14.sp,
                color = subtextColor
            )
        } else {
            activeTerpenes.forEach { terpene ->
                val value = profile[terpene] ?: 0.0
                val percentage = (value * 100)
                val normalizedValue = (value / maxValue).coerceIn(0.0, 1.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Terpene name
                    Text(
                        text = terpene.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = subtextColor,
                        modifier = Modifier.width(90.dp)
                    )

                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .background(trackColor, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(normalizedValue.toFloat())
                                .background(barColor, RoundedCornerShape(4.dp))
                        )
                    }

                    // Percentage
                    if (showPercentages) {
                        Text(
                            text = String.format("%.1f%%", percentage),
                            fontSize = 11.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(45.dp).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Combined profile visualization with individual strain contributions
 */
@Composable
fun CombinedProfileChart(
    combinedProfile: Map<String, Double>,
    individualProfiles: Map<String, Map<String, Double>>,
    terpeneOrder: List<String>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Colors for different strains
    val strainColors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548)  // Brown
    )

    var showIndividual by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Toggle between combined and individual view
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showIndividual) "Individual Profiles" else "Combined Profile",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            if (individualProfiles.size > 1) {
                TextButton(onClick = { showIndividual = !showIndividual }) {
                    Text(
                        text = if (showIndividual) "Show Combined" else "Show Individual",
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showIndividual && individualProfiles.isNotEmpty()) {
            // Show individual strain profiles
            individualProfiles.entries.forEachIndexed { index, (strainName, profile) ->
                val color = strainColors[index % strainColors.size]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strainName.split(" ").joinToString(" ") {
                                    it.replaceFirstChar { c -> c.uppercase() }
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }

                        TerpeneProfileChart(
                            title = "",
                            profile = profile,
                            terpeneOrder = terpeneOrder,
                            barColor = color,
                            showPercentages = true
                        )
                    }
                }
            }
        } else {
            // Show combined profile
            TerpeneProfileChart(
                title = "",
                profile = combinedProfile,
                terpeneOrder = terpeneOrder,
                barColor = Color(0xFF4CAF50),
                showPercentages = true
            )

            if (individualProfiles.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Combined using MAX values from ${individualProfiles.size} strains",
                    fontSize = 11.sp,
                    color = subtextColor
                )
            }
        }

        // Legend for strains (when showing combined)
        if (!showIndividual && individualProfiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Strains in profile:",
                fontSize = 12.sp,
                color = subtextColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                individualProfiles.keys.forEachIndexed { index, name ->
                    val color = strainColors[index % strainColors.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name.split(" ").joinToString(" ") {
                                it.replaceFirstChar { c -> c.uppercase() }
                            },
                            fontSize = 11.sp,
                            color = subtextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation using Row with wrapping
    // For more complex layouts, consider using accompanist FlowRow
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
