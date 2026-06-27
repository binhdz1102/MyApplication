package com.example.myapplication.data.repository;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;

import com.example.myapplication.R;
import com.example.myapplication.data.mapper.UserMapper;
import com.example.myapplication.data.remote.aidl.IUserService;
import com.example.myapplication.data.remote.aidl.UserParcelable;
import com.example.myapplication.data.remote.service.UserAidlService;
import com.example.myapplication.domain.model.User;
import com.example.myapplication.domain.repository.ConnectionStateListener;
import com.example.myapplication.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AidlUserRepository implements UserRepository {

    private final Context appContext;
    private final Class<? extends Service> serviceClass;
    private final Set<ConnectionStateListener> connectionListeners = new CopyOnWriteArraySet<>();

    private volatile IUserService userService;
    private volatile boolean isBound;
    private volatile boolean isBinding;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            userService = IUserService.Stub.asInterface(service);
            isBound = true;
            isBinding = false;
            notifyConnectionStateChanged(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            clearConnectionState(true);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            clearConnectionState(true);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            clearConnectionState(true);
        }
    };

    public AidlUserRepository(Context context) {
        this(context, UserAidlService.class);
    }

    public AidlUserRepository(Context context, Class<? extends Service> serviceClass) {
        this.appContext = context.getApplicationContext();
        this.serviceClass = serviceClass;
    }

    @Override
    public boolean bindService() {
        if (isBound || userService != null || isBinding) {
            return true;
        }

        isBinding = true;
        boolean didBind = appContext.bindService(
                new Intent(appContext, serviceClass),
                connection,
                Context.BIND_AUTO_CREATE
        );

        if (!didBind) {
            isBinding = false;
            notifyConnectionStateChanged(false);
        }

        return didBind;
    }

    @Override
    public void unbindService() {
        if (isBound) {
            try {
                appContext.unbindService(connection);
            } catch (IllegalArgumentException ignored) {
            }
        }
        clearConnectionState(false);
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.add(listener);
        listener.onConnectionStateChanged(isBound && userService != null);
    }

    @Override
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public List<User> getUsers() {
        return executeRemoteCall(service -> {
            List<UserParcelable> remoteUsers = service.getUsers();
            List<User> users = new ArrayList<>();
            if (remoteUsers != null) {
                for (UserParcelable remoteUser : remoteUsers) {
                    users.add(UserMapper.toDomain(remoteUser));
                }
            }
            return users;
        });
    }

    @Override
    public void addUser(User user) {
        boolean isAdded = executeRemoteCall(service -> service.addUser(UserMapper.toParcelable(user)));
        if (!isAdded) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_duplicate_user_id));
        }
    }

    @Override
    public void updateUser(User user) {
        boolean isUpdated = executeRemoteCall(service -> service.updateUser(UserMapper.toParcelable(user)));
        if (!isUpdated) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_user_not_found));
        }
    }

    @Override
    public void deleteUser(long userId) {
        boolean isDeleted = executeRemoteCall(service -> service.deleteUser(userId));
        if (!isDeleted) {
            throw new IllegalArgumentException(appContext.getString(R.string.message_user_not_found));
        }
    }

    private void notifyConnectionStateChanged(boolean isConnected) {
        for (ConnectionStateListener listener : connectionListeners) {
            listener.onConnectionStateChanged(isConnected);
        }
    }

    private void clearConnectionState(boolean notify) {
        userService = null;
        isBound = false;
        isBinding = false;
        if (notify) {
            notifyConnectionStateChanged(false);
        }
    }

    private IUserService requireService() {
        if (userService == null) {
            throw new IllegalStateException(appContext.getString(R.string.message_service_not_connected));
        }
        return userService;
    }

    private <T> T executeRemoteCall(RemoteOperation<T> operation) {
        try {
            return operation.execute(requireService());
        } catch (DeadObjectException exception) {
            clearConnectionState(true);
            throw new IllegalStateException(
                    appContext.getString(R.string.message_service_not_connected),
                    exception
            );
        } catch (RemoteException exception) {
            throw new IllegalStateException(
                    appContext.getString(R.string.message_service_call_failed),
                    exception
            );
        }
    }

    private interface RemoteOperation<T> {
        T execute(IUserService service) throws RemoteException;
    }
}
