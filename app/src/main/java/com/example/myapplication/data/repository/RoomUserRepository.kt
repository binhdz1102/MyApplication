package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.R
import com.example.myapplication.data.local.room.UserDao
import com.example.myapplication.data.local.room.UserRoomDatabase
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toRoomEntity
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.ConnectionStateListener
import com.example.myapplication.domain.repository.UserRepository
import java.util.concurrent.CopyOnWriteArraySet

class RoomUserRepository(
    context: Context,
) : UserRepository {

    private val appContext = context.applicationContext
    private val userDao: UserDao = UserRoomDatabase.getInstance(appContext).userDao()
    private val connectionListeners = CopyOnWriteArraySet<ConnectionStateListener>()

    @Volatile
    private var isConnected = false

    override fun bindService(): Boolean {
        if (!isConnected) {
            isConnected = true
            notifyConnectionStateChanged(true)
        }
        return true
    }

    override fun unbindService() {
        isConnected = false
    }

    override fun addConnectionStateListener(listener: ConnectionStateListener) {
        connectionListeners.add(listener)
        listener.onConnectionStateChanged(isConnected)
    }

    override fun removeConnectionStateListener(listener: ConnectionStateListener) {
        connectionListeners.remove(listener)
    }

    override fun getUsers(): List<User> = userDao.getUsers().map { it.toDomain() }

    override fun addUser(user: User) {
        val insertedRowId = userDao.insert(user.toRoomEntity())
        if (insertedRowId == -1L) {
            throw IllegalArgumentException(appContext.getString(R.string.message_duplicate_user_id))
        }
    }

    override fun updateUser(user: User) {
        val affectedRows = userDao.update(user.toRoomEntity())
        if (affectedRows == 0) {
            throw IllegalArgumentException(appContext.getString(R.string.message_user_not_found))
        }
    }

    override fun deleteUser(userId: Long) {
        val affectedRows = userDao.deleteById(userId)
        if (affectedRows == 0) {
            throw IllegalArgumentException(appContext.getString(R.string.message_user_not_found))
        }
    }

    private fun notifyConnectionStateChanged(isConnected: Boolean) {
        connectionListeners.forEach { listener ->
            listener.onConnectionStateChanged(isConnected)
        }
    }
}
