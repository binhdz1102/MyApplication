package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.UserRepository

class UpdateUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(user: User) {
        userRepository.updateUser(user)
    }
}
