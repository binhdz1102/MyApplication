package com.example.myapplication.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.ConnectionStateListener;
import com.example.myapplication.domain.usecase.AddUserUseCase;
import com.example.myapplication.domain.usecase.BindUserServiceUseCase;
import com.example.myapplication.domain.usecase.DeleteUserUseCase;
import com.example.myapplication.domain.usecase.GetUsersUseCase;
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase;
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase;
import com.example.myapplication.domain.usecase.UpdateUserUseCase;
import com.example.myapplication.presentation.model.ServiceConnectionUiState;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserViewModel extends ViewModel {

    private static final String SUCCESS_USER_ADDED = "user_added";
    private static final String SUCCESS_USER_UPDATED = "user_updated";
    private static final String SUCCESS_USER_DELETED = "user_deleted";

    private final BindUserServiceUseCase bindUserServiceUseCase;
    private final UnbindUserServiceUseCase unbindUserServiceUseCase;
    private final ObserveUserServiceConnectionUseCase observeUserServiceConnectionUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final AddUserUseCase addUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<User>> users = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(Boolean.FALSE);
    private final MutableLiveData<ServiceConnectionUiState> serviceConnectionState =
            new MutableLiveData<>(ServiceConnectionUiState.DISCONNECTED);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private final ConnectionStateListener connectionStateListener = isConnected -> {
        serviceConnectionState.postValue(
                isConnected ? ServiceConnectionUiState.CONNECTED : ServiceConnectionUiState.DISCONNECTED
        );
        if (isConnected) {
            loadUsers();
        }
    };

    public UserViewModel(
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
        this.observeUserServiceConnectionUseCase.register(connectionStateListener);
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<ServiceConnectionUiState> getServiceConnectionState() {
        return serviceConnectionState;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void connectService() {
        ServiceConnectionUiState currentState = serviceConnectionState.getValue();
        if (currentState == ServiceConnectionUiState.CONNECTED) {
            loadUsers();
            return;
        }
        if (currentState == ServiceConnectionUiState.CONNECTING) {
            return;
        }
        serviceConnectionState.setValue(ServiceConnectionUiState.CONNECTING);
        boolean didStartBinding = bindUserServiceUseCase.execute();
        if (!didStartBinding) {
            serviceConnectionState.setValue(ServiceConnectionUiState.DISCONNECTED);
        }
    }

    public void loadUsers() {
        executeRemoteAction(true, null, () -> users.postValue(getUsersUseCase.execute()));
    }

    public void addUser(User user) {
        executeRemoteAction(true, SUCCESS_USER_ADDED, () -> {
            addUserUseCase.execute(user);
            users.postValue(getUsersUseCase.execute());
        });
    }

    public void updateUser(User user) {
        executeRemoteAction(true, SUCCESS_USER_UPDATED, () -> {
            updateUserUseCase.execute(user);
            users.postValue(getUsersUseCase.execute());
        });
    }

    public void deleteUser(long userId) {
        executeRemoteAction(true, SUCCESS_USER_DELETED, () -> {
            deleteUserUseCase.execute(userId);
            users.postValue(getUsersUseCase.execute());
        });
    }

    public void consumeMessage() {
        message.setValue(null);
    }

    @Override
    protected void onCleared() {
        observeUserServiceConnectionUseCase.unregister(connectionStateListener);
        unbindUserServiceUseCase.execute();
        executor.shutdownNow();
        super.onCleared();
    }

    private void executeRemoteAction(boolean showLoading, String successMessage, Runnable action) {
        executor.execute(() -> {
            if (showLoading) {
                isLoading.postValue(Boolean.TRUE);
            }
            try {
                action.run();
                if (SUCCESS_USER_ADDED.equals(successMessage)) {
                    message.postValue("Da them user moi.");
                } else if (SUCCESS_USER_UPDATED.equals(successMessage)) {
                    message.postValue("Da cap nhat user.");
                } else if (SUCCESS_USER_DELETED.equals(successMessage)) {
                    message.postValue("Da xoa user khoi server.");
                }
            } catch (IllegalStateException exception) {
                serviceConnectionState.postValue(ServiceConnectionUiState.DISCONNECTED);
                message.postValue(exception.getMessage());
            } catch (IllegalArgumentException exception) {
                message.postValue(exception.getMessage());
            } finally {
                if (showLoading) {
                    isLoading.postValue(Boolean.FALSE);
                }
            }
        });
    }
}
