package com.example.feature.carproperty.domain.exception

class CarPropertyAccessException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
