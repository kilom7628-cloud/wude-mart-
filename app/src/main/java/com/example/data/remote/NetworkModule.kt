package com.example.data.remote

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
  // Production backend configured internally; never exposed to user settings.
  private const val BASE_URL = "https://wude-mart-backend.kilom7628.workers.dev/"

  val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  fun createApi(tokenProvider: suspend () -> String?): WudeMartApi {
    val authInterceptor = Interceptor { chain ->
      val token = runBlocking { tokenProvider() }
      val requestBuilder = chain.request().newBuilder()
      if (!token.isNullOrBlank()) {
        requestBuilder.header("Authorization", "Bearer $token")
      }
      requestBuilder.header("Accept", "application/json")
      requestBuilder.header("User-Agent", "WudeMartAndroid/1.0")
      chain.proceed(requestBuilder.build())
    }

    val loggingInterceptor =
      HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
      }

    val okHttpClient =
      OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(WudeMartApi::class.java)
  }

  fun createImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context.applicationContext)
      .memoryCache {
        MemoryCache.Builder(context.applicationContext)
          .maxSizePercent(0.25)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(context.applicationContext.cacheDir.resolve("wude_image_cache"))
          .maxSizeBytes(64L * 1024L * 1024L)
          .build()
      }
      .crossfade(true)
      .build()
  }
}
