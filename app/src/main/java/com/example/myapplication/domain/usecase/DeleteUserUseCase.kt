package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.UserRepository

class DeleteUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: Long) {
        userRepository.deleteUser(userId)
    }
}
