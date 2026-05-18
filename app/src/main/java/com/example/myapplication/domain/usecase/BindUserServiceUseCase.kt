package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.UserRepository

class BindUserServiceUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Boolean = userRepository.bindService()
}
