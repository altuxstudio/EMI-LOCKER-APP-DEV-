package com.app.emilockerapp.datalayer.model.passwordreset

data class PasswordResetConfirmResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int
) 