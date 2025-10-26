package com.app.emilockerapp.datalayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.emilockerapp.datalayer.datasource.remote.EmiApi
import com.app.emilockerapp.datalayer.repositories.MainRepository
import com.app.emilockerapp.generators.ServiceGenerator
import com.app.emilockerapp.models.TokenUseResponse
import com.app.emilockerapp.utils.getBaseUrl
import com.app.emilockerapp.utils.saveRegistrationInfo
import com.app.emilockerapp.utils.setRegistered
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PhoneRegisterUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
    val response: TokenUseResponse? = null
)

class PhoneRegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhoneRegisterUiState())
    val uiState: StateFlow<PhoneRegisterUiState> = _uiState

    private val repository = MainRepository(
        ServiceGenerator().generate(EmiApi::class.java, getBaseUrl())
    )

    fun register(tokenString: String, imei: String) {
        viewModelScope.launch {
            _uiState.value = PhoneRegisterUiState(isLoading = true)
            try {
                val res = repository.useToken(tokenString.trim(), imei.trim())
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!

                    // Persist minimal owner + token info
                    saveRegistrationInfo(
                        getApplication(),
                        tokenString = body.token.token_string,
                        ownerName    = body.token.shop_owner_name,
                        ownerEmail   = body.token.shop_owner_email,
                        deviceEmi   = body.firebase_entries[0].imei
                    )
                    setRegistered(getApplication(), true)

                    _uiState.value = PhoneRegisterUiState(
                        isLoading = false,
                        success = true,
                        errorMessage = null,
                        response = body
                    )
                } else {
                    val errorBody = res.errorBody()?.string()?.ifBlank { null }

                    val msg = if (errorBody != null) {
                        try {
                            val json = Gson().fromJson(errorBody, JsonObject::class.java)
                            // Try to get token_string first, otherwise fall back to imei_list or default
                            when {
                                json.has("token_string") -> json.getAsJsonArray("token_string")[0].asString
                                json.has("imei_list") -> json.getAsJsonArray("imei_list")[0].asString
                                else -> "Registration failed"
                            }
                        } catch (e: Exception) {
                            "Registration failed"
                        }
                    } else {
                        "Registration failed"
                    }

                    _uiState.value = PhoneRegisterUiState(isLoading = false, success = false, errorMessage = msg)
                }
            } catch (e: Exception) {
                _uiState.value = PhoneRegisterUiState(isLoading = false, success = false, errorMessage = e.message ?: "Something went wrong")
            }
        }
    }

    fun clearError() {
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)
    }
}
