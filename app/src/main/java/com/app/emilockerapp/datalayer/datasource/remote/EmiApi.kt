package com.app.emilockerapp.datalayer.datasource.remote

import com.app.emilockerapp.datalayer.model.login.LoginRequest
import com.app.emilockerapp.datalayer.model.login.LoginResponse
import com.app.emilockerapp.models.TokenUseRequest
import com.app.emilockerapp.models.TokenUseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface EmiApi {

    @POST("use-token/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("use-token/")
    suspend fun useToken(@Body request: TokenUseRequest): Response<TokenUseResponse>

}