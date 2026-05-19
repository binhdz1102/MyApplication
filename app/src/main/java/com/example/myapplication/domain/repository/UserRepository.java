package com.example.myapplication.domain.repository;

import com.example.myapplication.domain.model.User;

import java.util.List;

public interface UserRepository {
    boolean bindService();

    void unbindService();

    void addConnectionStateListener(ConnectionStateListener listener);

    void removeConnectionStateListener(ConnectionStateListener listener);

    List<User> getUsers();

    void addUser(User user);

    void updateUser(User user);

    void deleteUser(long userId);
}
