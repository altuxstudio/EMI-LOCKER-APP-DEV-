package com.app.emilockerapp.datalayer.datasource.remote

import com.app.emilockerapp.datalayer.model.login.LoginRequest
import com.app.emilockerapp.datalayer.model.login.LoginResponse
import com.app.emilockerapp.datalayer.model.signup.SignupRequest
import com.app.emilockerapp.datalayer.model.signup.SignupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface EmiApi {

    @POST("api/v1/auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun signUp(@Body signUpRequest: SignupRequest): Response<SignupResponse>

}