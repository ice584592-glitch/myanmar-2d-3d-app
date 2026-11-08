package com.myanmar.twodthreed

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YahooFinanceApi {
    @GET("api/v1/yahoo/chart")
    suspend fun chart(@Query("symbol") symbol: String): JsonObject
}

object YahooFinanceClient {
    val api: YahooFinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YahooFinanceApi::class.java)
    }
}
