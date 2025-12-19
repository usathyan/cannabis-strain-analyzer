package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strainanalyzer.app.llm.LlmService

@Composable
fun ConfigureScreen(
    viewModel: StrainViewModel
) {
    val context = LocalContext.current
    val llmService = remember { LlmService.getInstance(context) }

    val userProfile by viewModel.userProfile.collectAsState()
    val availableStrains by viewModel.availableStrains.collectAsState()
    val selectedStrains by viewModel.selectedStrains.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var newStrainName by remember { mutableStateOf("") }
    var isAddingStrain by remember { mutableStateOf(false) }

    val isLlmConfigured = llmService.isConfigured()
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
        // Add New Strain Card
        item {
            val cardColor = if (isDark) Color(0xFF0D47A1) else Color(0xFFE3F2FD)
            val titleColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
            val subtitleColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Add Strain to Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = newStrainName,
                        onValueChange = { newStrainName = it },
                        placeholder = { Text("Enter any strain name...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        enabled = !isAddingStrain
                    )

                    if (isLlmConfigured) {
                        Text(
                            text = "Unknown strains will be fetched via API",
                            fontSize = 12.sp,
                            color = subtitleColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Configure API in Settings to add unknown strains",
                            fontSize = 12.sp,
                            color = onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newStrainName.isNotBlank()) {
                                isAddingStrain = true
                                viewModel.addStrainToProfile(newStrainName)
                                newStrainName = ""
                                isAddingStrain = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newStrainName.isNotBlank() && !isAddingStrain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAddingStrain) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Profile")
                    }
                }
            }
        }

        // Your Favorite Strains
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "${userProfile.favoriteStrains.size} strains",
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val strainRowBg = if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
                    val strainTextColor = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

                    if (userProfile.favoriteStrains.isEmpty()) {
                        Text(
                            text = "No favorite strains yet. Add strains above or select from the database below.",
                            color = onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    } else {
                        userProfile.favoriteStrains.forEach { strain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        strainRowBg,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strain.split(" ").joinToString(" ") {
                                        it.replaceFirstChar { c -> c.uppercase() }
                                    },
                                    fontWeight = FontWeight.Medium,
                                    color = strainTextColor
                                )
                                IconButton(
                                    onClick = { viewModel.removeStrainFromProfile(strain) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFe74c3c)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Add from Database
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Add from Database",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "${availableStrains.size} available",
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Tap to add/remove from your profile",
                        fontSize = 12.sp,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(350.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableStrains) { strain ->
                            val isInProfile = userProfile.favoriteStrains.contains(strain)
                            QuickAddStrainCard(
                                name = strain,
                                isInProfile = isInProfile,
                                onClick = {
                                    if (isInProfile) {
                                        viewModel.removeStrainFromProfile(strain)
                                    } else {
                                        viewModel.addStrainToProfile(strain)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddStrainCard(
    name: String,
    isInProfile: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val borderColor = if (isInProfile) {
        if (isDark) Color(0xFF4CAF50) else Color(0xFF4CAF50)
    } else {
        if (isDark) Color(0xFF424242) else Color(0xFFe9ecef)
    }

    val bgColor = if (isInProfile) {
        if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    } else {
        surfaceColor
    }

    val textColor = if (isInProfile) {
        if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    } else {
        onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = bgColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isInProfile) {
                Text(text = "✓ ", color = if (isDark) Color(0xFF4CAF50) else Color(0xFF4CAF50))
            }
            Text(
                text = name.split(" ").joinToString(" ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                },
                fontWeight = if (isInProfile) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = onSurface
        )
    }
}
