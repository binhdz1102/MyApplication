package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.service.UserAidlRoomService

class AidlRoomUserRepository(
    context: Context,
) : AidlUserRepository(
    context = context,
    serviceClass = UserAidlRoomService::class.java,
)
