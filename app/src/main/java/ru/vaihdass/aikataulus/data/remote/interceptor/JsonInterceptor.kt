package ru.vaihdass.aikataulus.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class JsonInterceptor @Inject constructor(): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Accept", "application/json")
            .build()

        return chain.proceed(request)
    }
}