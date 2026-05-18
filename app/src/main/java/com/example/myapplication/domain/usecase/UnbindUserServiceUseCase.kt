package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.UserRepository

class UnbindUserServiceUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke() {
        userRepository.unbindService()
    }
}
