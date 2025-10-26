package com.app.emilockerapp.models

data class TokenUseRequest( // NEW
    val token_string: String,
    val imei_list: List<String>
)

data class TokenUseResponse( // NEW
    val message: String,
    val token: TokenInfo,
    val imeis_added: Int,
    val firebase_entries: List<FirebaseEntry>
)

data class TokenInfo( // NEW
    val id: Int,
    val token_string: String,
    val is_used: Boolean,
    val created_at: String,
    val used_at: String?,
    val shop_owner_name: String?,
    val shop_owner_email: String?,
    val imeis: List<TokenImei>
)

data class TokenImei( // NEW
    val imei_number: String
)

data class FirebaseEntry( // NEW
    val imei: String,
    val firebase_created: Boolean
)