package com.app.emilockerapp.datalayer.model.profile

data class ProfileUpdateRequest(
    val name: String,
    val email: String,
    val phone_number: String
) 