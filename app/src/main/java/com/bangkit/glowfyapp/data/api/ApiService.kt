package com.bangkit.glowfyapp.data.api

import com.bangkit.glowfyapp.data.models.auth.LoginResponse
import com.bangkit.glowfyapp.data.models.auth.RegisterResponse
import com.bangkit.glowfyapp.data.models.response.ArticlesResponse
import com.bangkit.glowfyapp.data.models.response.ProductResponse
import com.bangkit.glowfyapp.data.models.response.ProfileResponse
import com.bangkit.glowfyapp.data.models.response.ScanResponse
import com.bangkit.glowfyapp.data.models.response.SkinsResponse
import okhttp3.MultipartBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @FormUrlEncoded
    @POST("register")
    suspend fun userRegister(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): RegisterResponse

    @FormUrlEncoded
    @POST("login")
    suspend fun userLogin(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("products")
    suspend fun getProducts(): ProductResponse

    @GET("articles")
    suspend fun getArticles(): ArticlesResponse

    @GET("skins")
    suspend fun getSkins(): SkinsResponse

    @GET("products")
    suspend fun getProductsByCategory(
        @Query("tipe") tipe: String
    ): ProductResponse

    @Multipart
    @POST("predict")
    suspend fun faceDetection(
        @Part file: MultipartBody.Part
    ): ScanResponse

    @Multipart
    @PATCH("register/{id}")
    suspend fun profileUpdate(
        @Path("id") id: String,
        @Part img: MultipartBody.Part
    ): ProfileResponse
}