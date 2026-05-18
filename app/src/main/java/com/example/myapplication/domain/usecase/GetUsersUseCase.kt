package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.UserRepository

class GetUsersUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): List<User> = userRepository.getUsers()
}
