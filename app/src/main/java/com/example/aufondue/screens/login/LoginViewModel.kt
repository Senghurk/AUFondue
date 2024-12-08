package com.example.aufondue.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent = _navigationEvent.asStateFlow()

    fun onMicrosoftLoginClick() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate brief loading for development
            delay(1000)
            _isLoading.value = false
            _navigationEvent.value = true
        }
    }
}