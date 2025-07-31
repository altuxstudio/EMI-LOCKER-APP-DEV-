package com.app.emilockerapp.datalayer.model.emailverification

data class EmailVerificationRequest(
    val email: String,
    val code: String
) 