package com.app.emilockerapp.datalayer.model.signup

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone_number: String,
    val role: String
)

