package com.app.emilockerapp.datalayer.repositories

import com.app.emilockerapp.datalayer.datasource.remote.EmiApi
import com.app.emilockerapp.datalayer.model.login.LoginRequest
import com.app.emilockerapp.datalayer.model.login.LoginResponse
import retrofit2.Response

class MainRepository(private val api: EmiApi) {
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return api.login(LoginRequest(email, password))
    }
}