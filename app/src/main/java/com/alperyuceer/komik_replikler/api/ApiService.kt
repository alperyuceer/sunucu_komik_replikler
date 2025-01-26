package com.alperyuceer.komik_replikler.api

import com.alperyuceer.komik_replikler.Replik
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/replikler")
    suspend fun getAllReplikler(): Response<List<Replik>>

    @GET("api/replikler/kategori/{kategori}")
    suspend fun getRepliklerByKategori(@Path("kategori") kategori: String): Response<List<Replik>>

    @POST("api/favoriler")
    suspend fun addToFavorites(
        @Body body: FavoriteRequest
    ): Response<FavoriteResponse>

    @HTTP(method = "DELETE", path = "api/favoriler", hasBody = true)
    suspend fun removeFromFavorites(
        @Body body: FavoriteRequest
    ): Response<FavoriteResponse>

    @GET("api/favoriler/{deviceId}")
    suspend fun getFavorites(
        @Path("deviceId") deviceId: String
    ): Response<List<Replik>>

    @GET("api/kategoriler")
    suspend fun getKategoriler(): Response<List<String>>

    @POST("api/replik/{id}/increment-play-count")
    suspend fun incrementPlayCount(@Path("id") replikId: String): Response<Unit>
}

data class FavoriteRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("replikId")
    val replikId: String
)

data class FavoriteResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
) 