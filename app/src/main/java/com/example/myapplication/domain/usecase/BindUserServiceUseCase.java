package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.repository.UserRepository;

public class BindUserServiceUseCase {

    private final UserRepository userRepository;

    public BindUserServiceUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean execute() {
        return userRepository.bindService();
    }
}
