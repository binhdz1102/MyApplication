package com.example.myapplication.di;

import android.content.Context;

import com.example.myapplication.data.repository.AidlRoomUserRepository;
import com.example.myapplication.data.repository.AidlUserRepository;
import com.example.myapplication.data.repository.RoomUserRepository;
import com.example.myapplication.domain.repository.UserRepository;
import com.example.myapplication.domain.usecase.AddUserUseCase;
import com.example.myapplication.domain.usecase.BindUserServiceUseCase;
import com.example.myapplication.domain.usecase.DeleteUserUseCase;
import com.example.myapplication.domain.usecase.GetUsersUseCase;
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase;
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase;
import com.example.myapplication.domain.usecase.UpdateUserUseCase;
import com.example.myapplication.presentation.viewmodel.UserViewModelFactory;

public final class AppContainer {

    private static volatile UserRepository aidlUserRepository;
    private static volatile UserRepository roomUserRepository;
    private static volatile UserRepository aidlRoomUserRepository;

    private AppContainer() {
    }

    private static UserRepository provideAidlUserRepository(Context context) {
        if (aidlUserRepository == null) {
            synchronized (AppContainer.class) {
                if (aidlUserRepository == null) {
                    aidlUserRepository = new AidlUserRepository(context.getApplicationContext());
                }
            }
        }
        return aidlUserRepository;
    }

    private static UserRepository provideRoomUserRepository(Context context) {
        if (roomUserRepository == null) {
            synchronized (AppContainer.class) {
                if (roomUserRepository == null) {
                    roomUserRepository = new RoomUserRepository(context.getApplicationContext());
                }
            }
        }
        return roomUserRepository;
    }

    private static UserRepository provideAidlRoomUserRepository(Context context) {
        if (aidlRoomUserRepository == null) {
            synchronized (AppContainer.class) {
                if (aidlRoomUserRepository == null) {
                    aidlRoomUserRepository = new AidlRoomUserRepository(context.getApplicationContext());
                }
            }
        }
        return aidlRoomUserRepository;
    }

    public static UserViewModelFactory provideAidlUserViewModelFactory(Context context) {
        return createUserViewModelFactory(provideAidlUserRepository(context.getApplicationContext()));
    }

    public static UserViewModelFactory provideRoomUserViewModelFactory(Context context) {
        return createUserViewModelFactory(provideRoomUserRepository(context.getApplicationContext()));
    }

    public static UserViewModelFactory provideAidlRoomUserViewModelFactory(Context context) {
        return createUserViewModelFactory(provideAidlRoomUserRepository(context.getApplicationContext()));
    }

    private static UserViewModelFactory createUserViewModelFactory(UserRepository repository) {
        return new UserViewModelFactory(
                new BindUserServiceUseCase(repository),
                new UnbindUserServiceUseCase(repository),
                new ObserveUserServiceConnectionUseCase(repository),
                new GetUsersUseCase(repository),
                new AddUserUseCase(repository),
                new UpdateUserUseCase(repository),
                new DeleteUserUseCase(repository)
        );
    }
}
