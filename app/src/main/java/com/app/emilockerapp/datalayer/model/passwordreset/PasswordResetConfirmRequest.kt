package com.app.emilockerapp.datalayer.model.passwordreset

data class PasswordResetConfirmRequest(
    val email: String,
    val token: String,
    val new_password: String
) 