package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.repository.AidlUserRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.AddUserUseCase
import com.example.myapplication.domain.usecase.BindUserServiceUseCase
import com.example.myapplication.domain.usecase.DeleteUserUseCase
import com.example.myapplication.domain.usecase.GetUsersUseCase
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase
import com.example.myapplication.domain.usecase.UpdateUserUseCase
import com.example.myapplication.presentation.viewmodel.UserViewModelFactory

object AppContainer {

    @Volatile
    private var userRepository: UserRepository? = null

    private fun provideUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: AidlUserRepository(context).also { userRepository = it }
        }
    }

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        val repository = provideUserRepository(context.applicationContext)
        return UserViewModelFactory(
            bindUserServiceUseCase = BindUserServiceUseCase(repository),
            unbindUserServiceUseCase = UnbindUserServiceUseCase(repository),
            observeUserServiceConnectionUseCase = ObserveUserServiceConnectionUseCase(repository),
            getUsersUseCase = GetUsersUseCase(repository),
            addUserUseCase = AddUserUseCase(repository),
            updateUserUseCase = UpdateUserUseCase(repository),
            deleteUserUseCase = DeleteUserUseCase(repository),
        )
    }
}
