package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strainanalyzer.app.llm.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val llmService = remember { LlmService.getInstance(context) }
    val currentConfig by llmService.config.collectAsState()

    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "LLM Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Configure AI provider for strain analysis",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Provider Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Provider",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = providerExpanded,
                        onExpandedChange = { providerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = getProviderDisplayName(selectedProvider),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, "Select provider")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {
                            LlmProviderType.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(getProviderDisplayName(provider))
                                            Text(
                                                text = getProviderDescription(provider),
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProvider = provider
                                        model = DefaultModels.getDefault(provider)
                                        providerExpanded = false
                                    },
                                    trailingIcon = {
                                        if (provider == selectedProvider) {
                                            Icon(Icons.Default.Check, "Selected", tint = Color(0xFF4CAF50))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // API Key (if provider needs it)
        if (selectedProvider != LlmProviderType.NONE) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "API Key",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("Enter your API key") },
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Checkbox(
                                checked = showApiKey,
                                onCheckedChange = { showApiKey = it }
                            )
                            Text("Show API key", fontSize = 14.sp)
                        }

                        Text(
                            text = getApiKeyHint(selectedProvider),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Model Selection
        if (selectedProvider != LlmProviderType.NONE) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            placeholder = { Text("Model name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Text(
                            text = "Default: ${DefaultModels.getDefault(selectedProvider)}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Custom Base URL (for Ollama or custom endpoints)
        if (selectedProvider == LlmProviderType.OLLAMA) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Base URL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            placeholder = { Text("http://localhost:11434/v1") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    llmService.saveConfig(
                        LlmConfig(
                            provider = selectedProvider,
                            apiKey = apiKey,
                            model = model.ifBlank { DefaultModels.getDefault(selectedProvider) },
                            baseUrl = baseUrl.ifBlank { null }
                        )
                    )
                    showSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", fontSize = 16.sp)
            }

            if (showSaved) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSaved = false
                }
                Text(
                    text = "Settings saved!",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Provider Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About LLM Providers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = """
                            • OpenAI: GPT-4, GPT-4o-mini (best quality)
                            • Anthropic: Claude 3 (excellent reasoning)
                            • OpenRouter: Access to 100+ models
                            • Groq: Ultra-fast inference
                            • Google: Gemini models
                            • Ollama: Self-hosted, free
                            • None: Template-based (no API needed)
                        """.trimIndent(),
                        fontSize = 13.sp,
                        color = Color(0xFF1565C0),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun getProviderDisplayName(provider: LlmProviderType): String {
    return when (provider) {
        LlmProviderType.OPENAI -> "OpenAI"
        LlmProviderType.ANTHROPIC -> "Anthropic (Claude)"
        LlmProviderType.OPENROUTER -> "OpenRouter"
        LlmProviderType.GOOGLE -> "Google (Gemini)"
        LlmProviderType.GROQ -> "Groq"
        LlmProviderType.OLLAMA -> "Ollama (Self-hosted)"
        LlmProviderType.NONE -> "None (Template only)"
    }
}

private fun getProviderDescription(provider: LlmProviderType): String {
    return when (provider) {
        LlmProviderType.OPENAI -> "GPT-4, GPT-4o-mini"
        LlmProviderType.ANTHROPIC -> "Claude 3 Haiku, Sonnet, Opus"
        LlmProviderType.OPENROUTER -> "100+ models, pay-per-use"
        LlmProviderType.GOOGLE -> "Gemini 1.5 Flash/Pro"
        LlmProviderType.GROQ -> "Ultra-fast, Llama 3"
        LlmProviderType.OLLAMA -> "Local, free, private"
        LlmProviderType.NONE -> "No API calls, free"
    }
}

private fun getApiKeyHint(provider: LlmProviderType): String {
    return when (provider) {
        LlmProviderType.OPENAI -> "Get key at platform.openai.com"
        LlmProviderType.ANTHROPIC -> "Get key at console.anthropic.com"
        LlmProviderType.OPENROUTER -> "Get key at openrouter.ai/keys"
        LlmProviderType.GOOGLE -> "Get key at aistudio.google.com"
        LlmProviderType.GROQ -> "Get key at console.groq.com"
        LlmProviderType.OLLAMA -> "Usually not required for local"
        LlmProviderType.NONE -> ""
    }
}
