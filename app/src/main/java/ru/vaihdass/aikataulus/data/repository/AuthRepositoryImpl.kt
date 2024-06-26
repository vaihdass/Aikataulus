package ru.vaihdass.aikataulus.data.repository

import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.TokenRequest
import ru.vaihdass.aikataulus.data.auth.TokenStorage
import ru.vaihdass.aikataulus.data.remote.api.AuthApi
import ru.vaihdass.aikataulus.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val appAuth: AuthApi,
) : AuthRepository {
    override fun corruptAccessToken() {
        tokenStorage.accessToken = INCORRECT_ACCESS_TOKEN_VALUE
    }

    override fun logout() {
        tokenStorage.accessToken = null
        tokenStorage.refreshToken = null
        tokenStorage.idToken = null
    }

    override fun getAuthRequest(): AuthorizationRequest {
        return appAuth.getAuthRequest()
    }

    override fun getEndSessionRequest(): EndSessionRequest {
        return appAuth.getEndSessionRequest()
    }

    override suspend fun performTokenRequest(authService: AuthorizationService, tokenRequest: TokenRequest) {
        val tokens = appAuth.performTokenRequestSuspend(authService, tokenRequest)

        tokenStorage.accessToken = tokens.accessToken.trim()
        tokenStorage.refreshToken = tokens.refreshToken.trim()
        tokenStorage.idToken = tokens.idToken.trim()
    }

    companion object {
        const val INCORRECT_ACCESS_TOKEN_VALUE = "INCORRECT_ACCESS_TOKEN_VALUE"
    }
}