package com.example.myapplication.data.mapper;

import com.example.myapplication.data.local.room.UserRoomEntity;
import com.example.myapplication.data.remote.aidl.UserParcelable;
import com.example.myapplication.domain.model.User;

public final class UserRoomMapper {

    private UserRoomMapper() {
    }

    public static User toDomain(UserRoomEntity entity) {
        return new User(entity.id, entity.name, entity.age, entity.weight);
    }

    public static UserRoomEntity toRoomEntity(User user) {
        return new UserRoomEntity(
                user.getId(),
                user.getName(),
                user.getAge(),
                user.getWeight()
        );
    }

    public static UserParcelable toParcelable(UserRoomEntity entity) {
        return new UserParcelable(entity.id, entity.name, entity.age, entity.weight);
    }

    public static UserRoomEntity toRoomEntity(UserParcelable userParcelable) {
        return new UserRoomEntity(
                userParcelable.getId(),
                userParcelable.getName(),
                userParcelable.getAge(),
                userParcelable.getWeight()
        );
    }
}
