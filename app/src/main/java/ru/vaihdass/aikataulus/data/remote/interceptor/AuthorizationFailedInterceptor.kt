package ru.vaihdass.aikataulus.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationService
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import ru.vaihdass.aikataulus.base.Constants.PREF_GOOGLE_TASKS_LIST_ID
import ru.vaihdass.aikataulus.base.Constants.PREF_IS_CALENDARS_SELECTED
import ru.vaihdass.aikataulus.data.auth.TokenStorage
import ru.vaihdass.aikataulus.data.local.pref.AikataulusSharedPreferencesManager
import ru.vaihdass.aikataulus.data.remote.api.AuthApi
import ru.vaihdass.aikataulus.utils.ResManager
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthorizationFailedInterceptor @Inject constructor(
    private val authorizationService: AuthorizationService,
    private val appAuth: AuthApi,
    private val tokenStorage: TokenStorage,
    private val preferencesManager: AikataulusSharedPreferencesManager,
    private val resManager: ResManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequestTimestamp = System.currentTimeMillis()
        val originalResponse = chain.proceed(chain.request())
        return if (originalResponse.code != 401) {
            originalResponse
        } else {
            handleUnauthorizedResponse(chain, originalResponse, originalRequestTimestamp)
        }
    }

    private fun handleUnauthorizedResponse(
        chain: Interceptor.Chain, originalResponse: Response, requestTimestamp: Long
    ): Response {
        val latch = getLatch()
        return if (latch != null && latch.count > 0) {
            handleTokenIsUpdating(chain, latch, requestTimestamp) ?: originalResponse
        } else if (tokenUpdateTime > requestTimestamp) {
            updateTokenAndProceedChain(chain)
        } else {
            handleTokenNeedRefresh(chain) ?: originalResponse
        }
    }

    private fun handleTokenIsUpdating(
        chain: Interceptor.Chain, latch: CountDownLatch, requestTimestamp: Long
    ): Response? {
        return if (latch.await(REQUEST_TIMEOUT, TimeUnit.SECONDS) && tokenUpdateTime > requestTimestamp) {
            updateTokenAndProceedChain(chain)
        } else {
            null
        }
    }

    private fun handleTokenNeedRefresh(
        chain: Interceptor.Chain
    ): Response? {
        return if (refreshToken()) {
            updateTokenAndProceedChain(chain)
        } else {
            null
        }
    }

    private fun updateTokenAndProceedChain(
        chain: Interceptor.Chain
    ): Response {
        val newRequest = updateOriginalCallWithNewToken(chain.request())
        return chain.proceed(newRequest)
    }

    private fun refreshToken(): Boolean {
        initLatch()

        val tokenRefreshed = runBlocking {
            runCatching {
                val refreshRequest = appAuth.getRefreshTokenRequest(tokenStorage.refreshToken.orEmpty())
                appAuth.performTokenRequestSuspend(authorizationService, refreshRequest)
            }.onSuccess { tokens ->
                tokenStorage.accessToken = tokens.accessToken
                tokenStorage.refreshToken = tokens.refreshToken
                tokenStorage.idToken = tokens.idToken
            }.onFailure {
                Timber.tag("Oauth").d("Exception due performing refresh request: $it")
            }.getOrNull() != null
        }

        if (tokenRefreshed) {
            tokenUpdateTime = System.currentTimeMillis()
        } else {
            tokenStorage.accessToken = null
            tokenStorage.refreshToken = null
            tokenStorage.idToken = null
            preferencesManager.putString(PREF_GOOGLE_TASKS_LIST_ID, "")
            preferencesManager.putBoolean(PREF_IS_CALENDARS_SELECTED, false)
        }

        getLatch()?.countDown()
        return tokenRefreshed
    }

    private fun updateOriginalCallWithNewToken(request: Request): Request {
        val newAccessToken = tokenStorage.accessToken
        return if (newAccessToken != null) {
            request.newBuilder().header("Authorization", newAccessToken).build()
        } else {
            request
        }
    }

    companion object {
        private const val REQUEST_TIMEOUT = 30L

        @Volatile
        private var tokenUpdateTime: Long = 0L

        private var countDownLatch: CountDownLatch? = null

        @Synchronized
        fun initLatch() {
            if (countDownLatch == null || countDownLatch?.count == 0L) {
                countDownLatch = CountDownLatch(1)
            }
        }

        @Synchronized
        fun getLatch() = countDownLatch
    }
}
