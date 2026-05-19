package com.example.myapplication.data.remote.aidl;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class UserParcelable implements Parcelable {

    public static final Creator<UserParcelable> CREATOR = new Creator<UserParcelable>() {
        @Override
        public UserParcelable createFromParcel(Parcel source) {
            return new UserParcelable(source);
        }

        @Override
        public UserParcelable[] newArray(int size) {
            return new UserParcelable[size];
        }
    };

    private final long id;
    private final String name;
    private final int age;
    private final float weight;

    public UserParcelable(long id, String name, int age, float weight) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.age = age;
        this.weight = weight;
    }

    private UserParcelable(Parcel parcel) {
        this.id = parcel.readLong();
        String parcelName = parcel.readString();
        this.name = parcelName == null ? "" : parcelName;
        this.age = parcel.readInt();
        this.weight = parcel.readFloat();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public float getWeight() {
        return weight;
    }

    public UserParcelable copy() {
        return new UserParcelable(id, name, age, weight);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeInt(age);
        dest.writeFloat(weight);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserParcelable)) {
            return false;
        }
        UserParcelable that = (UserParcelable) other;
        return id == that.id
                && age == that.age
                && Float.compare(that.weight, weight) == 0
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, weight);
    }

    @Override
    public String toString() {
        return "UserParcelable{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", age=" + age
                + ", weight=" + weight
                + '}';
    }
}
