package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.UserRepository;

import java.util.List;

public class GetUsersUseCase {

    private final UserRepository userRepository;

    public GetUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> execute() {
        return userRepository.getUsers();
    }
}
