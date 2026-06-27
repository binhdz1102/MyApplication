package com.example.myapplication.data.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM room_users ORDER BY id ASC")
    List<UserRoomEntity> getUsers();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserRoomEntity user);

    @Update
    int update(UserRoomEntity user);

    @Query("DELETE FROM room_users WHERE id = :userId")
    int deleteById(long userId);
}
