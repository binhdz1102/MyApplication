package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.ConnectionStateListener
import com.example.myapplication.domain.repository.UserRepository

class ObserveUserServiceConnectionUseCase(
    private val userRepository: UserRepository,
) {
    fun register(listener: ConnectionStateListener) {
        userRepository.addConnectionStateListener(listener)
    }

    fun unregister(listener: ConnectionStateListener) {
        userRepository.removeConnectionStateListener(listener)
    }
}
