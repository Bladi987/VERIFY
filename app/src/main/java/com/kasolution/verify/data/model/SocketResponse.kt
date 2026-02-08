package com.kasolution.verify.data.model

data class SocketResponse<T>(
    val action: String,
    val status: String,
    val data: T? = null,
    val message: String? = null
)