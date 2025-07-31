package com.app.emilockerapp.datalayer.model.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

