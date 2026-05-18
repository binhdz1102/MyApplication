package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.repository.AidlUserRepository
import com.example.myapplication.data.repository.RoomUserRepository
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
    private var aidlUserRepository: UserRepository? = null

    @Volatile
    private var roomUserRepository: UserRepository? = null

    private fun provideAidlUserRepository(context: Context): UserRepository {
        return aidlUserRepository ?: synchronized(this) {
            aidlUserRepository ?: AidlUserRepository(context).also { aidlUserRepository = it }
        }
    }

    private fun provideRoomUserRepository(context: Context): UserRepository {
        return roomUserRepository ?: synchronized(this) {
            roomUserRepository ?: RoomUserRepository(context).also { roomUserRepository = it }
        }
    }

    fun provideAidlUserViewModelFactory(context: Context): UserViewModelFactory {
        val repository = provideAidlUserRepository(context.applicationContext)
        return createUserViewModelFactory(repository)
    }

    fun provideRoomUserViewModelFactory(context: Context): UserViewModelFactory {
        val repository = provideRoomUserRepository(context.applicationContext)
        return createUserViewModelFactory(repository)
    }

    private fun createUserViewModelFactory(repository: UserRepository): UserViewModelFactory {
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
