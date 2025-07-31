package com.app.emilockerapp.datalayer.model.profile

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profileData: ProfileData) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object Initial : ProfileUiState()
} 