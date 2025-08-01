package com.app.emilockerapp.datalayer.model.profile

data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: ProfileData?
)

data class ProfileData(
    val id: String,
    val name: String,
    val email: String,
    val phone_number: String,
    val role: String
) 