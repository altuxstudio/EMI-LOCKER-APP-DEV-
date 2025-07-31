package com.app.emilockerapp.datalayer.model.login

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val refresh: String?,
    val access: String?,
    val is_verify: Boolean?,
    val data: Any?,
    val statusCode: Int
)
