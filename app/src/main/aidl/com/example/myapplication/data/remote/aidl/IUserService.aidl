package com.example.myapplication.data.remote.aidl;

import com.example.myapplication.data.remote.aidl.UserParcelable;

interface IUserService {
    List<UserParcelable> getUsers();
    boolean addUser(in UserParcelable user);
    boolean updateUser(in UserParcelable user);
    boolean deleteUser(long userId);
}
