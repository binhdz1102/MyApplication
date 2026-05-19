package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.repository.ConnectionStateListener;
import com.example.myapplication.domain.repository.UserRepository;

public class ObserveUserServiceConnectionUseCase {

    private final UserRepository userRepository;

    public ObserveUserServiceConnectionUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(ConnectionStateListener listener) {
        userRepository.addConnectionStateListener(listener);
    }

    public void unregister(ConnectionStateListener listener) {
        userRepository.removeConnectionStateListener(listener);
    }
}
