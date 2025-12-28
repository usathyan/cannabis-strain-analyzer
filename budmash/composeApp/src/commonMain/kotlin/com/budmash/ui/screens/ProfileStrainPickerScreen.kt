package com.budmash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.database.StrainDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileStrainPickerScreen(
    initialQuery: String = "",
    likedStrains: Set<String>,
    onStrainSelect: (StrainData) -> Unit,
    onStrainClick: (StrainData) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    val searchResults = remember(searchQuery) {
        StrainDatabase.searchStrains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Strains to Profile") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search strains by name, effect, or flavor...") },
                singleLine = true
            )

            // Results count
            Text(
                text = "${searchResults.size} strains found",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Strain list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { strain ->
                    val isInProfile = strain.name.lowercase() in likedStrains.map { it.lowercase() }
                    StrainPickerCard(
                        strain = strain,
                        isInProfile = isInProfile,
                        onToggle = { onStrainSelect(strain) },
                        onClick = { onStrainClick(strain) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StrainPickerCard(
    strain: StrainData,
    isInProfile: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isInProfile) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
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
                    fontWeight = FontWeight.Medium,
                    color = if (isInProfile) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // THC
                    strain.thcMax?.let { thc ->
                        Text(
                            text = "${thc.toInt()}% THC",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInProfile) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Effects preview
                if (strain.effects.isNotEmpty()) {
                    Text(
                        text = strain.effects.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInProfile) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Add/Remove button
            if (isInProfile) {
                FilledTonalButton(
                    onClick = onToggle,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("In Profile")
                }
            } else {
                OutlinedButton(onClick = onToggle) {
                    Text("+ Add")
                }
            }
        }
    }
}
