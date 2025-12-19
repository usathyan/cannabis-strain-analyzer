package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.llm.GeminiNanoService
import com.strainanalyzer.app.llm.LlmService
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: StrainViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val analysisEngine = LocalAnalysisEngine.getInstance(context)
    val llmService = LlmService.getInstance(context)

    // Gemini Nano status
    val geminiNanoStatus by llmService.geminiNanoStatus.collectAsState()
    val downloadProgress by llmService.geminiNanoDownloadProgress.collectAsState()

    // Initialize Gemini Nano on first composition
    LaunchedEffect(Unit) {
        llmService.initializeGeminiNano()
    }

    val totalStrains = analysisEngine.getAvailableStrains().size
    val isLlmConfigured = llmService.isConfigured()
    val isGeminiNanoAvailable = llmService.isGeminiNanoAvailable()
    val isGeminiNanoDownloadable = llmService.isGeminiNanoDownloadable()
    val isCloudApiConfigured = llmService.isCloudApiConfigured()

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Profile Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
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
                            color = onSurfaceVariant
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
                            color = onSurfaceVariant
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
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

        // AI Status Card - Shows Gemini Nano or Cloud API status
        item {
            when {
                isGeminiNanoAvailable -> {
                    // Gemini Nano is ready
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "On-Device AI Ready",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50)
                                ) {
                                    Text(
                                        text = "Gemini Nano",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Search for any strain. Unknown strains will be generated on-device - no internet required.",
                                fontSize = 13.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
                geminiNanoStatus is GeminiNanoService.NanoStatus.Downloading -> {
                    // Downloading Gemini Nano
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Downloading Gemini Nano...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "On-device AI will be ready shortly...",
                                fontSize = 13.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
                isGeminiNanoDownloadable -> {
                    // Gemini Nano can be downloaded
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "On-Device AI Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your device supports Gemini Nano. Download to enable on-device strain generation.",
                                fontSize = 13.sp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        llmService.downloadGeminiNano()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Download Gemini Nano")
                            }
                        }
                    }
                }
                isCloudApiConfigured -> {
                    // Cloud API configured
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Cloud API Connected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You can search for any strain. Unknown strains will be fetched via API.",
                                fontSize = 13.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
                else -> {
                    // No AI configured
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "AI Not Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (geminiNanoStatus) {
                                    is GeminiNanoService.NanoStatus.Checking ->
                                        "Checking for on-device AI..."
                                    is GeminiNanoService.NanoStatus.Unavailable ->
                                        "On-device AI not supported. Configure a cloud API provider in Settings."
                                    is GeminiNanoService.NanoStatus.Error ->
                                        "Error checking AI. Configure a cloud API provider in Settings."
                                    else ->
                                        "Configure an API provider in Settings to search for strains not in the database."
                                },
                                fontSize = 13.sp,
                                color = Color(0xFFF57C00)
                            )
                        }
                    }
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
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = onSurface
                )
                Text(
                    text = description,
                    color = onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Text(text = "→", fontSize = 20.sp, color = onSurfaceVariant)
        }
    }
}

@Composable
fun StrainChip(name: String) {
    val isDark = isSystemInDarkTheme()
    val chipColor = if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    val textColor = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = chipColor
    ) {
        Text(
            text = name.split(" ").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            },
            fontSize = 12.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
