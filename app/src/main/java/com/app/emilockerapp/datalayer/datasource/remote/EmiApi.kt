package com.app.emilockerapp.datalayer.datasource.remote

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
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Header

interface EmiApi {

    @POST("api/v1/auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/register")
    suspend fun signUp(@Body signUpRequest: SignupRequest): Response<SignupResponse>

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: EmailVerificationRequest): Response<EmailVerificationResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: PasswordResetRequest): Response<PasswordResetResponse>

    @POST("api/v1/auth/reset-password/confirm")
    suspend fun resetPasswordConfirm(@Body request: PasswordResetConfirmRequest): Response<PasswordResetConfirmResponse>

    @GET("api/v1/auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    @PUT("api/v1/auth/profile")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body request: ProfileUpdateRequest): Response<ProfileResponse>

}