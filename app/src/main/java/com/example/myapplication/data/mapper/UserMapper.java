package com.example.myapplication.data.mapper;

import com.example.myapplication.data.remote.aidl.UserParcelable;
import com.example.myapplication.domain.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserParcelable userParcelable) {
        return new User(
                userParcelable.getId(),
                userParcelable.getName(),
                userParcelable.getAge(),
                userParcelable.getWeight()
        );
    }

    public static UserParcelable toParcelable(User user) {
        return new UserParcelable(
                user.getId(),
                user.getName(),
                user.getAge(),
                user.getWeight()
        );
    }
}
