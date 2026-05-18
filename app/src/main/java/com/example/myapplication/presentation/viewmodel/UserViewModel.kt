package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.ConnectionStateListener
import com.example.myapplication.domain.usecase.AddUserUseCase
import com.example.myapplication.domain.usecase.BindUserServiceUseCase
import com.example.myapplication.domain.usecase.DeleteUserUseCase
import com.example.myapplication.domain.usecase.GetUsersUseCase
import com.example.myapplication.domain.usecase.ObserveUserServiceConnectionUseCase
import com.example.myapplication.domain.usecase.UnbindUserServiceUseCase
import com.example.myapplication.domain.usecase.UpdateUserUseCase
import com.example.myapplication.presentation.model.ServiceConnectionUiState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UserViewModel(
    private val bindUserServiceUseCase: BindUserServiceUseCase,
    private val unbindUserServiceUseCase: UnbindUserServiceUseCase,
    private val observeUserServiceConnectionUseCase: ObserveUserServiceConnectionUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val addUserUseCase: AddUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) : ViewModel() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _serviceConnectionState = MutableLiveData(ServiceConnectionUiState.CONNECTING)
    val serviceConnectionState: LiveData<ServiceConnectionUiState> = _serviceConnectionState

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val connectionStateListener = ConnectionStateListener { isConnected ->
        _serviceConnectionState.postValue(
            if (isConnected) {
                ServiceConnectionUiState.CONNECTED
            } else {
                ServiceConnectionUiState.DISCONNECTED
            },
        )
        if (isConnected) {
            loadUsers()
        }
    }

    init {
        observeUserServiceConnectionUseCase.register(connectionStateListener)
    }

    fun connectService() {
        if (_serviceConnectionState.value == ServiceConnectionUiState.CONNECTED) {
            loadUsers()
            return
        }
        if (_serviceConnectionState.value == ServiceConnectionUiState.CONNECTING) {
            return
        }
        _serviceConnectionState.value = ServiceConnectionUiState.CONNECTING
        val didStartBinding = bindUserServiceUseCase()
        if (!didStartBinding) {
            _serviceConnectionState.value = ServiceConnectionUiState.DISCONNECTED
        }
    }

    fun loadUsers() {
        executeRemoteAction(showLoading = true) {
            _users.postValue(getUsersUseCase())
        }
    }

    fun addUser(user: User) {
        executeRemoteAction(showLoading = true, successMessage = "user_added") {
            addUserUseCase(user)
            _users.postValue(getUsersUseCase())
        }
    }

    fun updateUser(user: User) {
        executeRemoteAction(showLoading = true, successMessage = "user_updated") {
            updateUserUseCase(user)
            _users.postValue(getUsersUseCase())
        }
    }

    fun deleteUser(userId: Long) {
        executeRemoteAction(showLoading = true, successMessage = "user_deleted") {
            deleteUserUseCase(userId)
            _users.postValue(getUsersUseCase())
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    override fun onCleared() {
        observeUserServiceConnectionUseCase.unregister(connectionStateListener)
        unbindUserServiceUseCase()
        executor.shutdownNow()
        super.onCleared()
    }

    private fun executeRemoteAction(
        showLoading: Boolean,
        successMessage: String? = null,
        action: () -> Unit,
    ) {
        executor.execute {
            if (showLoading) {
                _isLoading.postValue(true)
            }
            try {
                action()
                when (successMessage) {
                    "user_added" -> _message.postValue("Da them user moi.")
                    "user_updated" -> _message.postValue("Da cap nhat user.")
                    "user_deleted" -> _message.postValue("Da xoa user khoi server.")
                }
            } catch (exception: IllegalStateException) {
                _serviceConnectionState.postValue(ServiceConnectionUiState.DISCONNECTED)
                _message.postValue(exception.message)
            } catch (exception: IllegalArgumentException) {
                _message.postValue(exception.message)
            } finally {
                if (showLoading) {
                    _isLoading.postValue(false)
                }
            }
        }
    }
}
