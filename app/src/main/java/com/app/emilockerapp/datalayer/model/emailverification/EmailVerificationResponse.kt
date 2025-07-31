package com.app.emilockerapp.datalayer.model.emailverification

data class EmailVerificationResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int
) 