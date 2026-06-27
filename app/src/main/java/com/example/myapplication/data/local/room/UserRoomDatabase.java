package com.example.myapplication.data.local.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {UserRoomEntity.class}, version = 1, exportSchema = false)
public abstract class UserRoomDatabase extends RoomDatabase {

    private static volatile UserRoomDatabase INSTANCE;

    public abstract UserDao userDao();

    public static UserRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    UserRoomDatabase.class,
                                    "user_room_database"
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (1, 'Nguyen Van An', 22, 60.5)");
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (2, 'Tran Thi Binh', 25, 48.0)");
                                    db.execSQL("INSERT INTO room_users (id, name, age, weight) VALUES (3, 'Le Quang Huy', 29, 70.2)");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
