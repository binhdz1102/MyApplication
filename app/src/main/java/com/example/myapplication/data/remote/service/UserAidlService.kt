package com.example.myapplication.data.remote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.myapplication.data.remote.aidl.IUserService
import com.example.myapplication.data.remote.aidl.UserParcelable
import java.util.concurrent.CopyOnWriteArrayList

class UserAidlService : Service() {

    private val userStore = CopyOnWriteArrayList(
        listOf(
            UserParcelable(id = 1L, name = "Nguyen Van A", age = 22, weight = 60.5f),
            UserParcelable(id = 2L, name = "Tran Thi B", age = 25, weight = 48.0f),
            UserParcelable(id = 3L, name = "Le Quang C", age = 29, weight = 70.2f),
        ),
    )

    private val binder = object : IUserService.Stub() {
        override fun getUsers(): MutableList<UserParcelable> = ArrayList(userStore)

        override fun addUser(user: UserParcelable?): Boolean {
            user ?: return false
            synchronized(userStore) {
                if (userStore.any { it.id == user.id }) {
                    return false
                }
                userStore.add(user.copy())
                return true
            }
        }

        override fun updateUser(user: UserParcelable?): Boolean {
            user ?: return false
            synchronized(userStore) {
                val existingIndex = userStore.indexOfFirst { it.id == user.id }
                if (existingIndex == -1) {
                    return false
                }
                userStore[existingIndex] = user.copy()
                return true
            }
        }

        override fun deleteUser(userId: Long): Boolean {
            synchronized(userStore) {
                val userToDelete = userStore.firstOrNull { it.id == userId } ?: return false
                return userStore.remove(userToDelete)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
}
