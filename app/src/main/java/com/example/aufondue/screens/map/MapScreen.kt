package com.example.aufondue.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val issues by viewModel.issues.collectAsState()
    val selectedIssue by viewModel.selectedIssue.collectAsState()

    val defaultLocation = LatLng(-33.865143, 151.209900) // Default to Sydney
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    LaunchedEffect(Unit) {
        viewModel.loadIssues()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map View") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false)
            ) {
                issues.forEach { issue ->
                    Marker(
                        state = MarkerState(position = LatLng(issue.latitude, issue.longitude)),
                        title = issue.title,
                        snippet = issue.description,
                        onClick = {
                            viewModel.selectIssue(issue)
                            true
                        },
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when (issue.category) {
                                "Broken" -> BitmapDescriptorFactory.HUE_RED
                                "Cracked" -> BitmapDescriptorFactory.HUE_ORANGE
                                "Leaking" -> BitmapDescriptorFactory.HUE_BLUE
                                "Flooded" -> BitmapDescriptorFactory.HUE_AZURE
                                else -> BitmapDescriptorFactory.HUE_RED
                            }
                        )
                    )
                }
            }

            selectedIssue?.let { issue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Category: ${issue.category}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Priority: ${issue.priority}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { viewModel.clearSelectedIssue() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}