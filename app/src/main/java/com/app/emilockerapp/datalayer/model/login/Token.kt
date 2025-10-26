package com.app.emilockerapp.datalayer.model.login

data class Token(
    val created_at: String,
    val id: Int,
    val imeis: List<Imei>,
    val is_used: Boolean,
    val shop_owner_email: String,
    val shop_owner_name: String,
    val token_string: String,
    val used_at: String
)