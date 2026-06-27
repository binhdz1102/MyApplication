package com.example.myapplication.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.domain.usecase.AddUserUseCase;
import com.example.myapplication.domain.usecase.BindUserServiceUseCase;
import com.example.myapplication.domain.usecase.DeleteUserUseCase;
import com.example.myapplication.domain.usecase.GetUsersUseCase;
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase;
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase;
import com.example.myapplication.domain.usecase.UpdateUserUseCase;

public class UserViewModelFactory implements ViewModelProvider.Factory {

    private final BindUserServiceUseCase bindUserServiceUseCase;
    private final UnbindUserServiceUseCase unbindUserServiceUseCase;
    private final ObserveUserServiceConnectionUseCase observeUserServiceConnectionUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final AddUserUseCase addUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public UserViewModelFactory(
            BindUserServiceUseCase bindUserServiceUseCase,
            UnbindUserServiceUseCase unbindUserServiceUseCase,
            ObserveUserServiceConnectionUseCase observeUserServiceConnectionUseCase,
            GetUsersUseCase getUsersUseCase,
            AddUserUseCase addUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase
    ) {
        this.bindUserServiceUseCase = bindUserServiceUseCase;
        this.unbindUserServiceUseCase = unbindUserServiceUseCase;
        this.observeUserServiceConnectionUseCase = observeUserServiceConnectionUseCase;
        this.getUsersUseCase = getUsersUseCase;
        this.addUserUseCase = addUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (!modelClass.isAssignableFrom(UserViewModel.class)) {
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
        return (T) new UserViewModel(
                bindUserServiceUseCase,
                unbindUserServiceUseCase,
                observeUserServiceConnectionUseCase,
                getUsersUseCase,
                addUserUseCase,
                updateUserUseCase,
                deleteUserUseCase
        );
    }
}
