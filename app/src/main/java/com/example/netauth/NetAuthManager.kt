package com.example.netauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetAuthManager {
    private var prefs: SharedPreferences? = null
    var currentUserId: String? = null
        private set
    var isGuest: Boolean = true
        private set

    var baseUrl: String? = null
        private set

    private var api: NetAuthApi? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("netauth_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs?.getString("userId", null)
        isGuest = currentUserId == null
    }

    fun setServerUrl(url: String) {
        baseUrl = url
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        api = retrofit.create(NetAuthApi::class.java)
    }

    suspend fun login(email: String, passwordHash: String): Result<UserProfile> {
        val safeApi = api ?: return Result.failure(Exception("API not initialized"))
        return try {
            val response = safeApi.login(LoginRequest(email, passwordHash))
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                currentUserId = profile.id
                isGuest = false
                prefs?.edit()?.putString("userId", profile.id)?.apply()
                Result.success(profile)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginAsGuest() {
        currentUserId = null
        isGuest = true
        prefs?.edit()?.remove("userId")?.apply()
    }

    suspend fun uploadFile(fileName: String, data: ByteArray): Result<Unit> {
        if (data.isEmpty()) return Result.failure(Exception("Empty file"))
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val safeApi = api ?: return Result.failure(Exception("API not initialized"))

        val requestFile = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", fileName, requestFile)

        return try {
            val response = safeApi.uploadSave(userId, body)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else if (response.code() == 413) {
                Result.failure(Exception("413_PAYLOAD_TOO_LARGE"))
            } else if (response.code() == 401) {
                loginAsGuest()
                Result.failure(Exception("401_UNAUTHORIZED"))
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(): Result<List<String>> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val safeApi = api ?: return Result.failure(Exception("API not initialized"))

        return try {
            val response = safeApi.getSavesList(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                loginAsGuest()
                Result.failure(Exception("401_UNAUTHORIZED"))
            } else {
                Result.failure(Exception("Failed to list files: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(fileName: String): Result<ByteArray> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val safeApi = api ?: return Result.failure(Exception("API not initialized"))

        return try {
            val response = safeApi.downloadSave(userId, fileName)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.bytes())
            } else {
                Result.failure(Exception("Download failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
