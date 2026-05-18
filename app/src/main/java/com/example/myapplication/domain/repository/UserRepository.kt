package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.User

interface UserRepository {
    fun bindService(): Boolean

    fun unbindService()

    fun addConnectionStateListener(listener: ConnectionStateListener)

    fun removeConnectionStateListener(listener: ConnectionStateListener)

    fun getUsers(): List<User>

    fun addUser(user: User)

    fun updateUser(user: User)

    fun deleteUser(userId: Long)
}
