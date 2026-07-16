package com.kasolution.verify.domain.auth.model

sealed class AuthResult {
    data class Success(val AuthSession: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}