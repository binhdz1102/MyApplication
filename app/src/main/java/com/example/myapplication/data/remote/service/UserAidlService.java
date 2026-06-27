package com.example.myapplication.data.remote.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.myapplication.data.remote.aidl.IUserService;
import com.example.myapplication.data.remote.aidl.UserParcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserAidlService extends Service {

    private final CopyOnWriteArrayList<UserParcelable> userStore = new CopyOnWriteArrayList<>(Arrays.asList(
            new UserParcelable(1L, "Nguyen Van A", 22, 60.5f),
            new UserParcelable(2L, "Tran Thi B", 25, 48.0f),
            new UserParcelable(3L, "Le Quang C", 29, 70.2f)
    ));

    private final IUserService.Stub binder = new IUserService.Stub() {
        @Override
        public List<UserParcelable> getUsers() {
            return new ArrayList<>(userStore);
        }

        @Override
        public boolean addUser(UserParcelable user) {
            if (user == null) {
                return false;
            }
            synchronized (userStore) {
                for (UserParcelable existingUser : userStore) {
                    if (existingUser.getId() == user.getId()) {
                        return false;
                    }
                }
                userStore.add(user.copy());
                return true;
            }
        }

        @Override
        public boolean updateUser(UserParcelable user) {
            if (user == null) {
                return false;
            }
            synchronized (userStore) {
                for (int index = 0; index < userStore.size(); index++) {
                    if (userStore.get(index).getId() == user.getId()) {
                        userStore.set(index, user.copy());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean deleteUser(long userId) {
            synchronized (userStore) {
                for (UserParcelable user : userStore) {
                    if (user.getId() == userId) {
                        return userStore.remove(user);
                    }
                }
                return false;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
