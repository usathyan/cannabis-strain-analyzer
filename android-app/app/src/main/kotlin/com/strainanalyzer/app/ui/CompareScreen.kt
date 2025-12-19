package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.data.StrainDataService
import com.strainanalyzer.app.llm.LlmService
import kotlinx.coroutines.launch

@Composable
fun CompareScreen(
    viewModel: StrainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analysisEngine = remember { LocalAnalysisEngine.getInstance(context) }
    val strainDataService = remember { StrainDataService.getInstance(context) }
    val llmService = remember { LlmService.getInstance(context) }
    val llmConfig by llmService.config.collectAsState()

    var strainName by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<LocalAnalysisEngine.AnalysisResult?>(null) }
    var strainSource by remember { mutableStateOf<StrainDataService.StrainSource?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var enhancedAnalysis by remember { mutableStateOf<String?>(null) }
    var isEnhancing by remember { mutableStateOf(false) }

    val userProfile by viewModel.userProfile.collectAsState()
    val favoriteStrains = userProfile.favoriteStrains

    val isLlmConfigured = llmService.isConfigured()
    val totalStrains = analysisEngine.getAvailableStrains().size
    val customStrains = analysisEngine.getCustomStrainCount()

    val isDark = isSystemInDarkTheme()
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
        // Analysis Info Card
        item {
            val infoCardBg = if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
            val infoTitleColor = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
            val infoTextColor = if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
            val infoSubtextColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF388E3C)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = infoCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Strain Analyzer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = infoTitleColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All similarity calculations run on-device:",
                        fontSize = 13.sp,
                        color = infoTextColor
                    )
                    Text(
                        text = "Z-score | Cosine | Euclidean | Correlation",
                        fontSize = 12.sp,
                        color = infoSubtextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Database: $totalStrains strains",
                                fontSize = 12.sp,
                                color = infoTextColor
                            )
                            if (customStrains > 0) {
                                Text(
                                    text = "($customStrains user-added)",
                                    fontSize = 11.sp,
                                    color = infoSubtextColor
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLlmConfigured) Color(0xFF1565C0) else Color.Gray.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = if (isLlmConfigured) "API: ${llmConfig.provider.name}" else "API: Not configured",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search Card
        item {
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
                        text = "Analyze Strain",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = strainName,
                        onValueChange = { strainName = it },
                        placeholder = { Text("Enter any strain name...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    val hintColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

                    if (isLlmConfigured) {
                        Text(
                            text = "Unknown strains will be fetched via ${llmConfig.provider.name}",
                            fontSize = 12.sp,
                            color = hintColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Configure API in Settings to analyze unknown strains",
                            fontSize = 12.sp,
                            color = onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (strainName.isNotBlank()) {
                                scope.launch {
                                    isAnalyzing = true
                                    statusMessage = "Checking local database..."
                                    enhancedAnalysis = null
                                    strainSource = null

                                    // Fetch strain data (local or via API)
                                    val fetchResult = strainDataService.getStrainData(strainName)
                                    strainSource = fetchResult.source

                                    when (fetchResult.source) {
                                        StrainDataService.StrainSource.LOCAL_DATABASE -> {
                                            statusMessage = "Found in local database"
                                        }
                                        StrainDataService.StrainSource.LOCAL_CACHE -> {
                                            statusMessage = "Found in cache (previously generated)"
                                        }
                                        StrainDataService.StrainSource.LLM_GENERATED -> {
                                            statusMessage = "Generated via ${llmConfig.provider.name} and saved"
                                        }
                                        StrainDataService.StrainSource.NOT_FOUND -> {
                                            statusMessage = fetchResult.error
                                            analysisResult = null
                                            isAnalyzing = false
                                            return@launch
                                        }
                                    }

                                    // Run local analysis
                                    statusMessage = "Calculating similarity..."
                                    analysisResult = analysisEngine.analyzeStrain(
                                        strainName,
                                        favoriteStrains
                                    )
                                    statusMessage = null
                                    isAnalyzing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = strainName.isNotBlank() && !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = statusMessage ?: "Analyzing...", fontSize = 14.sp)
                        } else {
                            Text(text = "Analyze", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Error message
        if (strainSource == StrainDataService.StrainSource.NOT_FOUND && statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Could Not Analyze",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage ?: "Unknown error",
                            fontSize = 14.sp,
                            color = Color(0xFFF57C00)
                        )
                    }
                }
            }
        }

        // Results
        analysisResult?.let { result ->
            result.strain?.let { strain ->
                // Source indicator
                item {
                    strainSource?.let { source ->
                        val (sourceText, sourceColor) = when (source) {
                            StrainDataService.StrainSource.LOCAL_DATABASE -> "From local database" to Color(0xFF4CAF50)
                            StrainDataService.StrainSource.LOCAL_CACHE -> "Previously generated" to Color(0xFF2196F3)
                            StrainDataService.StrainSource.LLM_GENERATED -> "Just generated via API (saved)" to Color(0xFF9C27B0)
                            else -> "" to Color.Gray
                        }
                        if (sourceText.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = sourceColor.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = sourceText,
                                    fontSize = 12.sp,
                                    color = sourceColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Match Score Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${result.similarity.overallScore.toInt()}%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = result.similarity.matchRating,
                                fontSize = 24.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Calculated on-device",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Strain Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strain.name.split(" ").joinToString(" ") {
                                    it.replaceFirstChar { c -> c.uppercase() }
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            InfoRow("Type", strain.type.replaceFirstChar { it.uppercase() })
                            InfoRow("THC", strain.thcRange)
                            InfoRow("CBD", strain.cbdRange)

                            if (strain.effects.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Effects",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    strain.effects.take(3).forEach { effect ->
                                        StrainChip(effect)
                                    }
                                }
                            }
                        }
                    }
                }

                // Similarity Breakdown Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Similarity Breakdown",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            MetricRow("Z-Scored Cosine", result.similarity.zScoredCosine)
                            MetricRow("Euclidean Similarity", result.similarity.euclideanSimilarity)
                            MetricRow("Correlation", result.similarity.correlationSimilarity)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All metrics calculated on-device",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                // Local Recommendation Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Analysis",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "Local",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = result.recommendation,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }

                // AI Enhancement Card (Optional)
                if (isLlmConfigured) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI Enhancement Available",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Get personalized insights using ${llmConfig.provider.name}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1976D2)
                                )

                                if (enhancedAnalysis != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = enhancedAnalysis!!,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isEnhancing = true
                                                enhancedAnalysis = llmService.analyzeStrain(
                                                    strain.name,
                                                    mapOf(
                                                        "thc_range" to strain.thcRange,
                                                        "cbd_range" to strain.cbdRange,
                                                        "type" to strain.type,
                                                        "effects" to strain.effects.joinToString(", ")
                                                    ),
                                                    mapOf("favorite_strains" to favoriteStrains.joinToString(", ")),
                                                    result.similarity.overallScore.toFloat() / 100f
                                                )
                                                isEnhancing = false
                                            }
                                        },
                                        enabled = !isEnhancing,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1976D2)
                                        )
                                    ) {
                                        if (isEnhancing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Enhance with AI")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // No favorites warning
            if (result.strain != null && favoriteStrains.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Tip: Add Favorite Strains",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF57C00)
                            )
                            Text(
                                text = "Go to Profile tab and add your favorite strains for personalized matching.",
                                fontSize = 13.sp,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double) {
    val isDark = isSystemInDarkTheme()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val trackColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = value.toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp),
                color = Color(0xFF4CAF50),
                trackColor = trackColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = onSurface
            )
        }
    }
}
