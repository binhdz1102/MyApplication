package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.repository.UserRepository;

public class UnbindUserServiceUseCase {

    private final UserRepository userRepository;

    public UnbindUserServiceUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute() {
        userRepository.unbindService();
    }
}
