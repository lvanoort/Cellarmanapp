package com.lukevanoort.cellarman.util

sealed class SuccessIndicator {
    object Success : SuccessIndicator()
    data class Failure(val reason: Exception) : SuccessIndicator()
}