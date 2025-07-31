package com.app.emilockerapp.datalayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.emilockerapp.datalayer.datasource.remote.EmiApi
import com.app.emilockerapp.datalayer.model.signup.SignupRequest
import com.app.emilockerapp.datalayer.model.signup.SignupResponse
import com.app.emilockerapp.datalayer.model.signup.SignUpUiState
import com.app.emilockerapp.datalayer.model.login.LoginUiState
import com.app.emilockerapp.datalayer.repositories.MainRepository
import com.app.emilockerapp.generators.ServiceGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // Add StateFlow for SignUp
    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    private val repository = MainRepository(
        ServiceGenerator().generate(EmiApi::class.java, "https://emi-locker.onrender.com/")
    )

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true)
            try {
                val response = repository.login(email, password)
                // Assuming your Login API response has a 'success' boolean and 'message' string
                if (response.isSuccessful && response.body()?.success == true) {
                    _loginState.value = LoginUiState(success = true)
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Login failed"
                    _loginState.value = LoginUiState(errorMessage = errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState(errorMessage = e.message ?: "Something went wrong")
            }
        }
    }

    // New SignUp Function
    fun signUp(signUpRequest: SignupRequest) {
        viewModelScope.launch {
            _signUpState.value = SignUpUiState(isLoading = true)
            try {
                // You'll need to add a signUp method to your MainRepository
                val response = repository.signUp(signUpRequest)

                // Adjust this based on your actual API response structure for signup
                // Assuming your SignUp API response also has a 'success' boolean and 'message' string
                // and potentially a 'data' field for the SignUpResponse
                if (response.isSuccessful && response.body()?.success == true) { // Assuming your response wrapper has a 'success' field
                    val successMessage = response.body()?.message // Or response.body()?.data?.message
                    _signUpState.value = SignUpUiState(success = true, successMessage = successMessage)
                } else {
                    val errorMsg = response.body()?.message // Or from errorBody()
                        ?: response.errorBody()?.string()
                        ?: "Sign up failed"
                    _signUpState.value = SignUpUiState(errorMessage = errorMsg)
                }
            } catch (e: Exception) {
                _signUpState.value = SignUpUiState(errorMessage = e.message ?: "Something went wrong during sign up")
            }
        }
    }

    // Optional: Function to reset the signup state if needed (e.g., when navigating away)
    fun resetSignUpState() {
        _signUpState.value = SignUpUiState()
    }
}
