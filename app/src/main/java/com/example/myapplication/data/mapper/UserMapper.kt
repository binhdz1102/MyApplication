package com.example.myapplication.data.mapper

import com.example.myapplication.data.remote.aidl.UserParcelable
import com.example.myapplication.domain.model.User

fun UserParcelable.toDomain(): User = User(
    id = id,
    name = name,
    age = age,
    weight = weight,
)

fun User.toParcelable(): UserParcelable = UserParcelable(
    id = id,
    name = name,
    age = age,
    weight = weight,
)
