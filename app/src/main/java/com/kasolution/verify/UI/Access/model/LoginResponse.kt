package com.kasolution.verify.UI.Access.model

data class LoginResponse(
    val action: String,
    val status: String,
    val message: String?,
    val data: UserData?
)

