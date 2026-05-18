package com.example.myapplication.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import com.example.myapplication.R
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toParcelable
import com.example.myapplication.data.remote.aidl.IUserService
import com.example.myapplication.data.remote.service.UserAidlService
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.ConnectionStateListener
import com.example.myapplication.domain.repository.UserRepository
import java.util.concurrent.CopyOnWriteArraySet

class AidlUserRepository(
    context: Context,
) : UserRepository {

    private val appContext = context.applicationContext
    private val connectionListeners = CopyOnWriteArraySet<ConnectionStateListener>()

    @Volatile
    private var userService: IUserService? = null

    @Volatile
    private var isBound = false

    @Volatile
    private var isBinding = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = IUserService.Stub.asInterface(service)
            isBound = true
            isBinding = false
            notifyConnectionStateChanged(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clearConnectionState(notify = true)
        }

        override fun onBindingDied(name: ComponentName?) {
            clearConnectionState(notify = true)
        }

        override fun onNullBinding(name: ComponentName?) {
            clearConnectionState(notify = true)
        }
    }

    override fun bindService(): Boolean {
        if (isBound || userService != null || isBinding) {
            return true
        }

        isBinding = true
        val didBind = appContext.bindService(
            Intent(appContext, UserAidlService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )

        if (!didBind) {
            isBinding = false
            notifyConnectionStateChanged(false)
        }

        return didBind
    }

    override fun unbindService() {
        if (isBound) {
            runCatching { appContext.unbindService(connection) }
        }
        clearConnectionState(notify = false)
    }

    override fun addConnectionStateListener(listener: ConnectionStateListener) {
        connectionListeners.add(listener)
        listener.onConnectionStateChanged(isBound && userService != null)
    }

    override fun removeConnectionStateListener(listener: ConnectionStateListener) {
        connectionListeners.remove(listener)
    }

    override fun getUsers(): List<User> = executeRemoteCall {
        getUsers().map { it.toDomain() }
    }

    override fun addUser(user: User) {
        val isAdded = executeRemoteCall {
            addUser(user.toParcelable())
        }
        if (!isAdded) {
            throw IllegalArgumentException(appContext.getString(R.string.message_duplicate_user_id))
        }
    }

    override fun updateUser(user: User) {
        val isUpdated = executeRemoteCall {
            updateUser(user.toParcelable())
        }
        if (!isUpdated) {
            throw IllegalArgumentException(appContext.getString(R.string.message_user_not_found))
        }
    }

    override fun deleteUser(userId: Long) {
        val isDeleted = executeRemoteCall {
            deleteUser(userId)
        }
        if (!isDeleted) {
            throw IllegalArgumentException(appContext.getString(R.string.message_user_not_found))
        }
    }

    private fun notifyConnectionStateChanged(isConnected: Boolean) {
        connectionListeners.forEach { listener ->
            listener.onConnectionStateChanged(isConnected)
        }
    }

    private fun clearConnectionState(notify: Boolean) {
        userService = null
        isBound = false
        isBinding = false
        if (notify) {
            notifyConnectionStateChanged(false)
        }
    }

    private fun requireService(): IUserService {
        return userService ?: throw IllegalStateException(
            appContext.getString(R.string.message_service_not_connected),
        )
    }

    private fun <T> executeRemoteCall(block: IUserService.() -> T): T {
        return try {
            requireService().block()
        } catch (exception: DeadObjectException) {
            clearConnectionState(notify = true)
            throw IllegalStateException(
                appContext.getString(R.string.message_service_not_connected),
                exception,
            )
        } catch (exception: RemoteException) {
            throw IllegalStateException(
                appContext.getString(R.string.message_service_call_failed),
                exception,
            )
        }
    }
}
