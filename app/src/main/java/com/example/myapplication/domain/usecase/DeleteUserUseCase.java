package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.repository.UserRepository;

public class DeleteUserUseCase {

    private final UserRepository userRepository;

    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(long userId) {
        userRepository.deleteUser(userId);
    }
}
