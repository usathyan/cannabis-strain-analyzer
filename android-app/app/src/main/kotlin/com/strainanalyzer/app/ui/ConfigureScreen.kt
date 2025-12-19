package com.strainanalyzer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfigureScreen(
    viewModel: StrainViewModel
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val availableStrains by viewModel.availableStrains.collectAsState()
    val selectedStrains by viewModel.selectedStrains.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Your Favorite Strains
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your Favorite Strains",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (userProfile?.favoriteStrains?.isEmpty() != false) {
                        Text(
                            text = "No favorite strains yet. Select from available strains below.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    } else {
                        userProfile?.favoriteStrains?.forEach { strain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(
                                        Color(0xFFF8F9FA),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strain,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFe74c3c),
                                    modifier = Modifier.clickable {
                                        viewModel.removeStrainFromProfile(strain)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Available Strains
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Available Strains",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableStrains.take(20)) { strain ->
                            val isSelected = selectedStrains.contains(strain)
                            StrainCard(
                                name = strain,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.toggleStrainSelection(strain)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Current Profile Info
        if (userProfile?.idealProfile != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        val profile = userProfile?.idealProfile
                        InfoRow("THC", "${profile?.aggregateCannabinoids?.get("thc")?.times(100)?.toInt() ?: 0}%")
                        InfoRow("CBD", "${profile?.aggregateCannabinoids?.get("cbd")?.times(100)?.toInt() ?: 0}%")
                        InfoRow("Top Terpene", profile?.dominantTerpenes?.firstOrNull()?.capitalize() ?: "N/A")
                    }
                }
            }
        }
        
        // Update Profile Button
        item {
            Button(
                onClick = { viewModel.createIdealProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedStrains.isNotEmpty() && uiState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(text = "Update Profile", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun StrainCard(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFe9ecef),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) Color(0xFFe8f5e8) else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌿", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
