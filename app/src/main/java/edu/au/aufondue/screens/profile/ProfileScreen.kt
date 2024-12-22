// ProfileScreen.kt
package edu.au.aufondue.screens.profile

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.CircleCropTransformation

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.avatarUrl)
                    .transformations(listOf(CircleCropTransformation()))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                },
                error = {
                    Log.e("ProfileScreen", "Error loading image: ${it.result}")
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name
        Text(
            text = state.displayName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        // Email
        Text(
            text = state.email,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Update Avatar Button
        Button(
            onClick = { viewModel.updateAvatar() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Avatar")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Issues",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Notifications")
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out Button
        Button(
            onClick = { viewModel.signOut(onSignOut) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Sign out")
        }
    }
}