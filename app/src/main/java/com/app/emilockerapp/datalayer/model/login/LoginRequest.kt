package com.app.emilockerapp.datalayer.model.login
data class LoginRequest(
    val imei_list: List<String>?,
    val token_string: String
)
