package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.UserRepository;

public class AddUserUseCase {

    private final UserRepository userRepository;

    public AddUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(User user) {
        userRepository.addUser(user);
    }
}
