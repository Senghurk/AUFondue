package edu.au.aufondue.screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.au.aufondue.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val shouldNavigate by viewModel.navigationEvent.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showPasswordReset by remember { mutableStateOf(false) }

    // Initialize auth manager
    LaunchedEffect(Unit) {
        viewModel.initializeAuth(context)
    }

    // Handle navigation
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            onLoginSuccess()
        }
    }

    // Handle error
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            if (error.contains("Password reset email sent")) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo
        Icon(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .width(500.dp)
                .height(200.dp),
            tint = Color.Red
        )

        // Tagline
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Authentication Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        showPasswordReset -> "Reset Password"
                        state.isSignUp -> "Create Account"
                        else -> "Sign In"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Display Name (only for sign up)
                if (state.isSignUp && !showPasswordReset) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("AU Email") },
                    placeholder = { Text("student@au.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                // Password (hidden for password reset)
                if (!showPasswordReset) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        when {
                            showPasswordReset -> {
                                viewModel.sendPasswordReset(email)
                                showPasswordReset = false
                            }
                            state.isSignUp -> {
                                viewModel.createAccount(email, password, displayName)
                            }
                            else -> {
                                viewModel.signInWithEmailPassword(email, password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && email.isNotBlank() &&
                            (showPasswordReset || (password.isNotBlank() && (!state.isSignUp || displayName.isNotBlank())))
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            when {
                                showPasswordReset -> "Send Reset Email"
                                state.isSignUp -> "Create Account"
                                else -> "Sign In"
                            }
                        )
                    }
                }

                // Toggle buttons
                if (!showPasswordReset) {
                    // Toggle Sign Up/Sign In
                    TextButton(
                        onClick = { viewModel.toggleSignUpMode() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        Text(
                            if (state.isSignUp)
                                "Already have an account? Sign In"
                            else
                                "Don't have an account? Sign Up"
                        )
                    }

                    // Forgot Password (only for sign in)
                    if (!state.isSignUp) {
                        TextButton(
                            onClick = { showPasswordReset = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading
                        ) {
                            Text("Forgot Password?")
                        }
                    }
                } else {
                    // Back to Sign In
                    TextButton(
                        onClick = { showPasswordReset = false },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        Text("Back to Sign In")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}