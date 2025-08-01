package com.app.emilockerapp.datalayer.model.emailverification

sealed class EmailVerificationUiState {
    object Loading : EmailVerificationUiState()
    data class Success(val message: String) : EmailVerificationUiState()
    data class Error(val message: String) : EmailVerificationUiState()
    object Initial : EmailVerificationUiState()
} 