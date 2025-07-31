package com.app.emilockerapp.datalayer.model.signup

data class SignUpUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val successMessage: String? = null, // To hold messages like "User created. Check email..."
    val errorMessage: String? = null
)
