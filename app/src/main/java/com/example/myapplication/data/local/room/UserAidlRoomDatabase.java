package com.example.myapplication.data.local.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {UserRoomEntity.class}, version = 1, exportSchema = false)
public abstract class UserAidlRoomDatabase extends RoomDatabase {

    private static volatile UserAidlRoomDatabase INSTANCE;

    public abstract UserDao userDao();

    public static UserAidlRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserAidlRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    UserAidlRoomDatabase.class,
                                    "user_aidl_room_database"
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (101, 'Aidl Room One', 30, 66.5)");
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (102, 'Aidl Room Two', 27, 52.0)");
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (103, 'Aidl Room Three', 35, 74.3)");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
