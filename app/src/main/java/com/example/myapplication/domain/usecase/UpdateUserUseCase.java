package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.UserRepository;

public class UpdateUserUseCase {

    private final UserRepository userRepository;

    public UpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(User user) {
        userRepository.updateUser(user);
    }
}
