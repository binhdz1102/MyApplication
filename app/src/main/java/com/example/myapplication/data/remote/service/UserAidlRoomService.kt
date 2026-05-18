package com.example.myapplication.data.remote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.myapplication.data.local.room.UserAidlRoomDatabase
import com.example.myapplication.data.mapper.toParcelable
import com.example.myapplication.data.mapper.toRoomEntity
import com.example.myapplication.data.remote.aidl.IUserService
import com.example.myapplication.data.remote.aidl.UserParcelable

class UserAidlRoomService : Service() {

    private val userDao by lazy(LazyThreadSafetyMode.NONE) {
        UserAidlRoomDatabase.getInstance(applicationContext).userDao()
    }

    private val binder = object : IUserService.Stub() {
        override fun getUsers(): MutableList<UserParcelable> {
            return userDao.getUsers()
                .map { it.toParcelable() }
                .toMutableList()
        }

        override fun addUser(user: UserParcelable?): Boolean {
            user ?: return false
            return userDao.insert(user.toRoomEntity()) != -1L
        }

        override fun updateUser(user: UserParcelable?): Boolean {
            user ?: return false
            return userDao.update(user.toRoomEntity()) > 0
        }

        override fun deleteUser(userId: Long): Boolean {
            return userDao.deleteById(userId) > 0
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
}
