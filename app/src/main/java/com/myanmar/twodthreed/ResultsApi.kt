package com.myanmar.twodthreed

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ResultsApi {
    @GET("api/v1/twod-result-live")
    suspend fun live2d(): JsonObject

    @GET("api/v1/twod-results")
    suspend fun history2d(@Query("page") page: Int = 1): JsonObject

    @GET("api/v1/threed-results")
    suspend fun history3d(@Query("page") page: Int = 1): JsonObject

    @GET("api/v1/threed-result-latest")
    suspend fun latest3d(): JsonObject
}

object ApiClient {
    val api: ResultsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ResultsApi::class.java)
    }
}
