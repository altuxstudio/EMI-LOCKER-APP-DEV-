package com.app.emilockerapp.datalayer.model.passwordreset

data class PasswordResetResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int
) 