package com.app.emilockerapp.datalayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.emilockerapp.datalayer.datasource.remote.EmiApi
import com.app.emilockerapp.datalayer.model.login.LoginUiState
import com.app.emilockerapp.datalayer.repositories.MainRepository
import com.app.emilockerapp.generators.ServiceGenerator
import com.app.emilockerapp.utils.getBaseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val repository = MainRepository(
        ServiceGenerator().generate(EmiApi::class.java, getBaseUrl())
    )

    fun login(token: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true)
            try {
                val response = repository.login(null, password)
                if (response.isSuccessful && response.code() == 200) {
                    _loginState.value = LoginUiState(success = true)
                } else {
                    val errorMsg =  response.errorBody()?.string()
                        ?: "Login failed"
                    _loginState.value = LoginUiState(errorMessage = errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState(errorMessage = e.message ?: "Something went wrong")
            }
        }
    }
}
