package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domain.usecase.AddUserUseCase
import com.example.myapplication.domain.usecase.BindUserServiceUseCase
import com.example.myapplication.domain.usecase.DeleteUserUseCase
import com.example.myapplication.domain.usecase.GetUsersUseCase
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase
import com.example.myapplication.domain.usecase.UpdateUserUseCase

class UserViewModelFactory(
    private val bindUserServiceUseCase: BindUserServiceUseCase,
    private val unbindUserServiceUseCase: UnbindUserServiceUseCase,
    private val observeUserServiceConnectionUseCase: ObserveUserServiceConnectionUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val addUserUseCase: AddUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UserViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return UserViewModel(
            bindUserServiceUseCase = bindUserServiceUseCase,
            unbindUserServiceUseCase = unbindUserServiceUseCase,
            observeUserServiceConnectionUseCase = observeUserServiceConnectionUseCase,
            getUsersUseCase = getUsersUseCase,
            addUserUseCase = addUserUseCase,
            updateUserUseCase = updateUserUseCase,
            deleteUserUseCase = deleteUserUseCase,
        ) as T
    }
}
