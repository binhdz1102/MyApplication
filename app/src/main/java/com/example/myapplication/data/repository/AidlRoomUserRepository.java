package com.example.myapplication.data.repository;

import android.content.Context;

import com.example.myapplication.data.remote.service.UserAidlRoomService;

public class AidlRoomUserRepository extends AidlUserRepository {

    public AidlRoomUserRepository(Context context) {
        super(context, UserAidlRoomService.class);
    }
}
