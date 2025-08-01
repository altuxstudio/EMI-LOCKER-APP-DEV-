package com.app.emilockerapp.datalayer.repositories

import com.app.emilockerapp.datalayer.datasource.remote.EmiApi
import com.app.emilockerapp.datalayer.model.login.LoginRequest
import com.app.emilockerapp.datalayer.model.login.LoginResponse
import com.app.emilockerapp.datalayer.model.signup.SignupRequest
import com.app.emilockerapp.datalayer.model.signup.SignupResponse
import com.app.emilockerapp.datalayer.model.emailverification.EmailVerificationRequest
import com.app.emilockerapp.datalayer.model.emailverification.EmailVerificationResponse
import com.app.emilockerapp.datalayer.model.passwordreset.PasswordResetRequest
import com.app.emilockerapp.datalayer.model.passwordreset.PasswordResetResponse
import com.app.emilockerapp.datalayer.model.passwordreset.PasswordResetConfirmRequest
import com.app.emilockerapp.datalayer.model.passwordreset.PasswordResetConfirmResponse
import com.app.emilockerapp.datalayer.model.profile.ProfileResponse
import com.app.emilockerapp.datalayer.model.profile.ProfileUpdateRequest
import retrofit2.Response

class MainRepository(private val api: EmiApi) {
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return api.login(LoginRequest(email, password))
    }
    
    suspend fun signUp(signUpRequest: SignupRequest): Response<SignupResponse> {
        return api.signUp(signUpRequest)
    }
    
    suspend fun verifyEmail(email: String, code: String): Response<EmailVerificationResponse> {
        return api.verifyEmail(EmailVerificationRequest(email, code))
    }
    
    suspend fun resetPassword(email: String): Response<PasswordResetResponse> {
        return api.resetPassword(PasswordResetRequest(email))
    }
    
    suspend fun resetPasswordConfirm(email: String, token: String, newPassword: String): Response<PasswordResetConfirmResponse> {
        return api.resetPasswordConfirm(PasswordResetConfirmRequest(email, token, newPassword))
    }
    
    suspend fun getProfile(token: String): Response<ProfileResponse> {
        return api.getProfile("Bearer $token")
    }
    
    suspend fun updateProfile(token: String, name: String, email: String, phoneNumber: String): Response<ProfileResponse> {
        return api.updateProfile("Bearer $token", ProfileUpdateRequest(name, email, phoneNumber))
    }
}