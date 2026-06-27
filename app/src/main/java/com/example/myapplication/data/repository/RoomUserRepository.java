package com.example.myapplication.data.repository;

import android.content.Context;

import com.example.myapplication.R;
import com.example.myapplication.data.local.room.UserDao;
import com.example.myapplication.data.local.room.UserRoomDatabase;
import com.example.myapplication.data.local.room.UserRoomEntity;
import com.example.myapplication.data.mapper.UserRoomMapper;
import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.ConnectionStateListener;
import com.example.myapplication.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RoomUserRepository implements UserRepository {

    private final Context appContext;
    private final UserDao userDao;
    private final Set<ConnectionStateListener> connectionListeners = new CopyOnWriteArraySet<>();

    private volatile boolean isConnected;

    public RoomUserRepository(Context context) {
        appContext = context.getApplicationContext();
        userDao = UserRoomDatabase.getInstance(appContext).userDao();
    }

    @Override
    public boolean bindService() {
        if (!isConnected) {
            isConnected = true;
            notifyConnectionStateChanged(true);
        }
        return true;
    }

    @Override
    public void unbindService() {
        isConnected = false;
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.add(listener);
        listener.onConnectionStateChanged(isConnected);
    }

    @Override
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public List<User> getUsers() {
        List<UserRoomEntity> entities = userDao.getUsers();
        List<User> users = new ArrayList<>(entities.size());
        for (UserRoomEntity entity : entities) {
            users.add(UserRoomMapper.toDomain(entity));
        }
        return users;
    }

    @Override
    public void addUser(User user) {
        long insertedRowId = userDao.insert(UserRoomMapper.toRoomEntity(user));
        if (insertedRowId == -1L) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_duplicate_user_id));
        }
    }

    @Override
    public void updateUser(User user) {
        int affectedRows = userDao.update(UserRoomMapper.toRoomEntity(user));
        if (affectedRows == 0) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_user_not_found));
        }
    }

    @Override
    public void deleteUser(long userId) {
        int affectedRows = userDao.deleteById(userId);
        if (affectedRows == 0) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_user_not_found));
        }
    }

    private void notifyConnectionStateChanged(boolean connected) {
        for (ConnectionStateListener listener : connectionListeners) {
            listener.onConnectionStateChanged(connected);
        }
    }
}
