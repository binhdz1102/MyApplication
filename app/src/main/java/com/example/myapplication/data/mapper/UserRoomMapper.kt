package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.room.UserRoomEntity
import com.example.myapplication.domain.model.User

fun UserRoomEntity.toDomain(): User = User(
    id = id,
    name = name,
    age = age,
    weight = weight,
)

fun User.toRoomEntity(): UserRoomEntity = UserRoomEntity(
    id,
    name,
    age,
    weight,
)
