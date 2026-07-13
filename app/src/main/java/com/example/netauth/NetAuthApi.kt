package com.example.netauth

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val passwordHash: String
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String,
    val email: String,
    val name: String? = null
)

interface NetAuthApi {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<UserProfile>

    @Multipart
    @POST("api/users/{userId}/storage")
    suspend fun uploadSave(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @GET("api/users/{userId}/storage")
    suspend fun getSavesList(
        @Path("userId") userId: String
    ): Response<List<String>>

    @GET("api/users/{userId}/storage/{fileName}")
    suspend fun downloadSave(
        @Path("userId") userId: String,
        @Path("fileName") fileName: String
    ): Response<ResponseBody>
}
