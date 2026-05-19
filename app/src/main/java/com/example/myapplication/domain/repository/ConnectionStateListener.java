package com.example.myapplication.domain.repository;

public interface ConnectionStateListener {
    void onConnectionStateChanged(boolean isConnected);
}
