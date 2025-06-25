// Location: app/src/main/java/edu/au/aufondue/screens/map/MapScreen.kt
// UPDATE THIS EXISTING FILE - REPLACE ALL CONTENT

package edu.au.aufondue.screens.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import edu.au.aufondue.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val issues by viewModel.issues.collectAsState()
    val selectedIssue by viewModel.selectedIssue.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Initialize FusedLocationProviderClient
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Default to Assumption University location
    val defaultLocation = LatLng(13.8505, 100.5678) // Assumption University coordinates
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }

    // Handle location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            getCurrentLocation(fusedLocationClient, cameraPositionState)
        }
    }

    // Request permission and load issues when the screen is first displayed
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getCurrentLocation(fusedLocationClient, cameraPositionState)
        }
        viewModel.loadIssues()
    }

    // Move camera to selected issue
    LaunchedEffect(selectedIssue) {
        selectedIssue?.let { issue ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(issue.latitude, issue.longitude),
                    18f
                ),
                durationMs = 1000
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_view)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = hasLocationPermission,
                    mapToolbarEnabled = true,
                    compassEnabled = true
                ),
                onMyLocationButtonClick = {
                    getCurrentLocation(fusedLocationClient, cameraPositionState)
                    true
                }
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

            // Information card for selected issue
            selectedIssue?.let { issue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.category)}: ${issue.category}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(R.string.status)}: ${issue.status}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { viewModel.clearSelectedIssue() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            // Error display
            if (error != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error ?: stringResource(R.string.something_went_wrong),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.loadIssues() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    cameraPositionState: CameraPositionState
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                    )
                }
            }
    } catch (e: SecurityException) {
        // Handle permission denial
    }
}