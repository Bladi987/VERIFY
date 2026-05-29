package com.kasolution.verify.domain.auth.model

sealed class AuthResult {
    data class Success(val userSession: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}