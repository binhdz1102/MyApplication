package com.example.myapplication.presentation.model;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BluetoothDeviceItem {

    public static final int RSSI_UNKNOWN = Integer.MIN_VALUE;

    private BluetoothDevice device;
    private final String address;
    private String name;
    private int deviceType;
    private int bondState;
    private boolean seenFromClassic;
    private boolean seenFromBle;
    private int rssi = RSSI_UNKNOWN;
    private long lastSeenTimestamp = System.currentTimeMillis();

    public BluetoothDeviceItem(@NonNull BluetoothDevice device, @Nullable String name) {
        this.device = device;
        this.address = device.getAddress();
        this.name = normalizeName(name);
        this.deviceType = device.getType();
        this.bondState = device.getBondState();
    }

    public void update(
            @NonNull BluetoothDevice latestDevice,
            @Nullable String latestName,
            int latestRssi,
            boolean fromClassic,
            boolean fromBle
    ) {
        device = latestDevice;
        if (!isBlank(latestName)) {
            name = latestName.trim();
        }
        if (latestDevice.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            deviceType = latestDevice.getType();
        }
        bondState = latestDevice.getBondState();
        if (latestRssi != RSSI_UNKNOWN) {
            rssi = latestRssi;
        }
        seenFromClassic |= fromClassic;
        seenFromBle |= fromBle;
        lastSeenTimestamp = System.currentTimeMillis();
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @NonNull
    public String getDisplayName() {
        return isBlank(name) ? address : name;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public int getBondState() {
        return bondState;
    }

    public boolean isSeenFromClassic() {
        return seenFromClassic;
    }

    public boolean isSeenFromBle() {
        return seenFromBle;
    }

    public int getRssi() {
        return rssi;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    private String normalizeName(@Nullable String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
