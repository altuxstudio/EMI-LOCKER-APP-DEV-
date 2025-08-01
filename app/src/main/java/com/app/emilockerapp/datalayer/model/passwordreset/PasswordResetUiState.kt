package com.app.emilockerapp.datalayer.model.passwordreset

sealed class PasswordResetUiState {
    object Loading : PasswordResetUiState()
    data class Success(val message: String) : PasswordResetUiState()
    data class Error(val message: String) : PasswordResetUiState()
    object Initial : PasswordResetUiState()
} 