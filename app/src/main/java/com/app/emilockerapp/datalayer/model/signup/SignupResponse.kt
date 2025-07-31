package com.app.emilockerapp.datalayer.model.signup

data class SignupResponse(
    val id: String,
    val name: String,
    val email: String,
    val phone_number: String,
    val role: String, // Or UserRole enum as suggested before
    val message: String
)
