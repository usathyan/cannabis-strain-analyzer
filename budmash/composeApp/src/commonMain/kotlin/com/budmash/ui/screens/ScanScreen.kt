package com.budmash.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budmash.parser.ParseStatus

@Composable
fun ScanScreen(
    status: ParseStatus,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (status) {
            is ParseStatus.Fetching -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fetching menu...")
            }

            is ParseStatus.FetchComplete -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fetched ${status.sizeBytes / 1024}kb")
            }

            is ParseStatus.ProductsFound -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Found ${status.flowerCount} flower products")
            }

            is ParseStatus.ExtractingStrains -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Extracting: ${status.current}/${status.total}")
            }

            is ParseStatus.ResolvingTerpenes -> {
                LinearProgressIndicator(
                    progress = { status.current.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Resolving terpenes: ${status.current}/${status.total}")
            }

            is ParseStatus.Complete -> {
                LaunchedEffect(Unit) { onComplete() }
                Text("Complete! Found ${status.menu.strains.size} strains")
            }

            is ParseStatus.Error -> {
                LaunchedEffect(Unit) { onError(status.message) }
                Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
