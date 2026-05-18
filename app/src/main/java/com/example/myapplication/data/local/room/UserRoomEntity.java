package com.example.myapplication.data.local.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "room_users")
public class UserRoomEntity {

    @PrimaryKey
    public long id;

    @NonNull
    public String name;

    public int age;

    public float weight;

    public UserRoomEntity(long id, @NonNull String name, int age, float weight) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.weight = weight;
    }
}
