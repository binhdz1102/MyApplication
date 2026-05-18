package com.example.myapplication.data.remote.aidl

import android.os.Parcel
import android.os.Parcelable

data class UserParcelable(
    val id: Long,
    val name: String,
    val age: Int,
    val weight: Float,
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        name = parcel.readString().orEmpty(),
        age = parcel.readInt(),
        weight = parcel.readFloat(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeInt(age)
        parcel.writeFloat(weight)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UserParcelable> {
        override fun createFromParcel(parcel: Parcel): UserParcelable = UserParcelable(parcel)

        override fun newArray(size: Int): Array<UserParcelable?> = arrayOfNulls(size)
    }
}
