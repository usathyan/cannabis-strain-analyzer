package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.llm.LlmService

@Composable
fun HomeScreen(
    viewModel: StrainViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val analysisEngine = LocalAnalysisEngine.getInstance(context)
    val llmService = LlmService.getInstance(context)

    val totalStrains = analysisEngine.getAvailableStrains().size
    val isLlmConfigured = llmService.isConfigured()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Profile Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ProfileStat(
                                value = "${userProfile.favoriteStrains.size}",
                                label = "In Profile"
                            )
                            ProfileStat(
                                value = "$totalStrains",
                                label = "Database"
                            )
                        }
                    }

                    if (userProfile.favoriteStrains.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your favorite strains:",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            userProfile.favoriteStrains.take(3).forEach { strain ->
                                StrainChip(strain)
                            }
                            if (userProfile.favoriteStrains.size > 3) {
                                StrainChip("+${userProfile.favoriteStrains.size - 3}")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No favorite strains yet. Add strains to build your profile.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        item {
            // Quick Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ActionButton(
                        title = "Build Your Profile",
                        description = "Add strains you enjoy",
                        onClick = onNavigateToConfig
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ActionButton(
                        title = "Search Strains",
                        description = "Find and analyze any strain",
                        onClick = onNavigateToCompare
                    )
                }
            }
        }

        // API Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLlmConfigured) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isLlmConfigured) "API Connected" else "API Not Configured",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLlmConfigured) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLlmConfigured)
                            "You can search for any strain. Unknown strains will be fetched via API."
                        else
                            "Configure an API provider in Settings to search for strains not in the database.",
                        fontSize = 13.sp,
                        color = if (isLlmConfigured) Color(0xFF388E3C) else Color(0xFFF57C00)
                    )
                }
            }
        }

        // How It Works Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How It Works",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. Add strains you enjoy to your Profile\n" +
                               "2. Search for any strain to analyze\n" +
                               "3. See match % based on terpene similarity\n" +
                               "4. All calculations run locally on-device",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ActionButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8F9FA)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Text(text = "→", fontSize = 20.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StrainChip(name: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8F5E9)
    ) {
        Text(
            text = name.split(" ").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            },
            fontSize = 12.sp,
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
