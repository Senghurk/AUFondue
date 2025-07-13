package edu.au.aufondue.screens.profile

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import edu.au.aufondue.R
import edu.au.aufondue.utils.LanguageManager

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Initialize avatar on first launch
    LaunchedEffect(Unit) {
        if (state.avatarUrl.isEmpty()) {
            viewModel.updateAvatar()
        }
    }

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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (state.avatarUrl.isNotEmpty()) {
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
                        // Show first letter of name as fallback
                        if (state.displayName.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.displayName.first().toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                )
            } else {
                // Show loading or placeholder
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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

        // Firebase Authentication Status
        if (state.isSignedInWithFirebase) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "✓ Authenticated with Firebase",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Update Avatar Button
        Button(
            onClick = { viewModel.updateAvatar() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.update_avatar))
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
                    text = stringResource(R.string.report_issues),
                    style = MaterialTheme.typography.titleMedium
                )

                // Notifications Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.enable_notifications))
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() }
                    )
                }

                // Language Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.language))
                    TextButton(
                        onClick = { showLanguageDialog = true }
                    ) {
                        Text(
                            text = LanguageManager.getDisplayName(
                                LanguageManager.getSelectedLanguage(context)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Delete Account Button (only show if signed in with Firebase)
        if (state.isSignedInWithFirebase) {
            Button(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Account")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sign Out Button
        Button(
            onClick = { viewModel.signOut(onSignOut) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.sign_out))
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = LanguageManager.getSelectedLanguage(context),
            onLanguageSelected = { languageCode ->
                viewModel.changeLanguage(context, languageCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = {
                Text("Are you sure you want to delete your account? This action cannot be undone and will permanently delete all your data.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(
                            onSuccess = {
                                showDeleteAccountDialog = false
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                onSignOut()
                            },
                            onError = { error ->
                                showDeleteAccountDialog = false
                                Toast.makeText(context, "Error deleting account: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.select_language))
        },
        text = {
            Column {
                // English Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLanguage == LanguageManager.ENGLISH,
                        onClick = { selectedLanguage = LanguageManager.ENGLISH }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.language_english),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Thai Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLanguage == LanguageManager.THAI,
                        onClick = { selectedLanguage = LanguageManager.THAI }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.language_thai),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLanguageSelected(selectedLanguage)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}