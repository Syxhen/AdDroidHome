package com.syxhen.addroidhome.data.network

import android.annotation.SuppressLint
import com.syxhen.addroidhome.data.network.api.AdGuardHomeApi
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object AdGuardNetworkFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createOkHttpClient(
        username: String? = null,
        password: String? = null,
        trustSelfSignedCerts: Boolean = false,
        trustedHost: String? = null,
        enableLogging: Boolean = false,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url
                if (!url.isHttps && !LocalNetworkPolicy.isLocalOrPrivateHost(url.host)) {
                    throw IOException(
                        "Plain HTTP is only allowed for local/LAN addresses. Use HTTPS for ${url.host}.",
                    )
                }
                chain.proceed(request)
            }
            .apply {
                if (!username.isNullOrBlank() && password != null) {
                    val credentials = Credentials.basic(username, password)
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("Authorization", credentials)
                            .build()
                        chain.proceed(request)
                    }
                }
                if (trustSelfSignedCerts) {
                    configureSelfSignedTls(trustedHost = trustedHost)
                }
                if (enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
    }

    fun createApi(
        baseUrl: String,
        username: String? = null,
        password: String? = null,
        trustSelfSignedCerts: Boolean = false,
        enableLogging: Boolean = false,
    ): AdGuardHomeApi {
        val targetHost = runCatching { URI(baseUrl).host }.getOrNull()
        val client = createOkHttpClient(
            username = username,
            password = password,
            trustSelfSignedCerts = trustSelfSignedCerts,
            trustedHost = targetHost,
            enableLogging = enableLogging,
        )
        return createRetrofit(baseUrl = baseUrl, okHttpClient = client)
            .create(AdGuardHomeApi::class.java)
    }

    /**
     * Opt-in TLS configuration for user-designated homelab servers using self-signed certificates.
     * Hostname verification remains scoped strictly to [trustedHost] when provided.
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun OkHttpClient.Builder.configureSelfSignedTls(trustedHost: String?) {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        sslSocketFactory(sslContext.socketFactory, trustManager)
        hostnameVerifier { hostname, _ ->
            trustedHost != null && hostname.equals(trustedHost, ignoreCase = true)
        }
    }
}
