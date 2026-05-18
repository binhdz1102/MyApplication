package com.example.myapplication.domain.repository

fun interface ConnectionStateListener {
    fun onConnectionStateChanged(isConnected: Boolean)
}
