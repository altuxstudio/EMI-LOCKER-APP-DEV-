package com.app.emilockerapp.datalayer.model.login

data class LoginResponse(
    val imeis_added: Int,
    val message: String,
    val token: Token
)