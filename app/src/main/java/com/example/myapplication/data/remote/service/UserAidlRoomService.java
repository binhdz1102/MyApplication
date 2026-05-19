package com.example.myapplication.data.remote.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.myapplication.data.local.room.UserAidlRoomDatabase;
import com.example.myapplication.data.local.room.UserDao;
import com.example.myapplication.data.local.room.UserRoomEntity;
import com.example.myapplication.data.mapper.UserRoomMapper;
import com.example.myapplication.data.remote.aidl.IUserService;
import com.example.myapplication.data.remote.aidl.UserParcelable;

import java.util.ArrayList;
import java.util.List;

public class UserAidlRoomService extends Service {

    private UserDao userDao;

    private final IUserService.Stub binder = new IUserService.Stub() {
        @Override
        public List<UserParcelable> getUsers() {
            List<UserRoomEntity> roomUsers = getUserDao().getUsers();
            List<UserParcelable> users = new ArrayList<>(roomUsers.size());
            for (UserRoomEntity entity : roomUsers) {
                users.add(UserRoomMapper.toParcelable(entity));
            }
            return users;
        }

        @Override
        public boolean addUser(UserParcelable user) {
            if (user == null) {
                return false;
            }
            return getUserDao().insert(UserRoomMapper.toRoomEntity(user)) != -1L;
        }

        @Override
        public boolean updateUser(UserParcelable user) {
            if (user == null) {
                return false;
            }
            return getUserDao().update(UserRoomMapper.toRoomEntity(user)) > 0;
        }

        @Override
        public boolean deleteUser(long userId) {
            return getUserDao().deleteById(userId) > 0;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private UserDao getUserDao() {
        if (userDao == null) {
            userDao = UserAidlRoomDatabase.getInstance(getApplicationContext()).userDao();
        }
        return userDao;
    }
}
