package com.app.emilockerapp.datalayer.model.passwordreset

sealed class PasswordResetConfirmUiState {
    object Loading : PasswordResetConfirmUiState()
    data class Success(val message: String) : PasswordResetConfirmUiState()
    data class Error(val message: String) : PasswordResetConfirmUiState()
    object Initial : PasswordResetConfirmUiState()
} 