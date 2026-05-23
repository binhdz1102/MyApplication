package com.example.myapplication.presentation.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
public final class BluetoothCompanionHelper {

    public interface Callback {
        void onChooserReady(@NonNull IntentSender chooserLauncher, @NonNull String requestLabel);

        void onFailure(@NonNull String message);
    }

    public static final class SelectionResult {

        @Nullable
        private final BluetoothDevice device;
        @NonNull
        private final String summary;

        public SelectionResult(@Nullable BluetoothDevice device, @NonNull String summary) {
            this.device = device;
            this.summary = summary;
        }

        @Nullable
        public BluetoothDevice getDevice() {
            return device;
        }

        @NonNull
        public String getSummary() {
            return summary;
        }
    }

    private BluetoothCompanionHelper() {
    }

    public static boolean isSupported(@NonNull Context context) {
        return TextUtils.isEmpty(getUnsupportedReason(context));
    }

    @NonNull
    public static String getUnsupportedReason(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "Companion Device API can Android 8.0+.";
        }

        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return "Khong the kiem tra Companion Device capability tren thiet bi nay.";
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            return "Companion Device demo duoc tat tren Android Automotive de tranh crash he thong com.android.car.";
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
            return "Thiet bi khong ho tro FEATURE_COMPANION_DEVICE_SETUP.";
        }

        if (context.getSystemService(CompanionDeviceManager.class) == null) {
            return "CompanionDeviceManager khong san sang tren thiet bi nay.";
        }

        return "";
    }

    public static void startClassicAssociation(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        startAssociation(
                context,
                new BluetoothDeviceFilter.Builder().build(),
                "classic",
                callback
        );
    }

    public static void startBleAssociation(@NonNull Context context, @NonNull Callback callback) {
        startAssociation(
                context,
                new BluetoothLeDeviceFilter.Builder().build(),
                "ble",
                callback
        );
    }

    @NonNull
    public static SelectionResult parseSelectionResult(@Nullable Intent data) {
        if (data == null) {
            return new SelectionResult(null, "He thong khong tra ve du lieu companion.");
        }

        BluetoothDevice classicDevice = getBluetoothDeviceExtra(data);
        if (classicDevice != null) {
            return new SelectionResult(
                    classicDevice,
                    "Companion da chon classic device: " + describeDevice(classicDevice)
            );
        }

        ScanResult bleResult = getBleScanResultExtra(data);
        if (bleResult != null && bleResult.getDevice() != null) {
            return new SelectionResult(
                    bleResult.getDevice(),
                    "Companion da chon BLE device: " + describeDevice(bleResult.getDevice())
            );
        }

        return new SelectionResult(
                null,
                "Companion chooser dong lai nhung khong tra ve Bluetooth device."
        );
    }

    private static void startAssociation(
            @NonNull Context context,
            @NonNull Object filter,
            @NonNull String requestLabel,
            @NonNull Callback callback
    ) {
        String unsupportedReason = getUnsupportedReason(context);
        if (!TextUtils.isEmpty(unsupportedReason)) {
            callback.onFailure(unsupportedReason);
            return;
        }
        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);
        if (manager == null) {
            callback.onFailure("CompanionDeviceManager khong san sang tren thiet bi nay.");
            return;
        }

        AssociationRequest request = new AssociationRequest.Builder()
                .addDeviceFilter((android.companion.DeviceFilter<?>) filter)
                .setSingleDevice(true)
                .build();

        try {
            manager.associate(request, new CompanionDeviceManager.Callback() {
                @Override
                public void onDeviceFound(IntentSender chooserLauncher) {
                    callback.onChooserReady(chooserLauncher, requestLabel);
                }

                public void onAssociationPending(IntentSender chooserLauncher) {
                    callback.onChooserReady(chooserLauncher, requestLabel);
                }

                @Override
                public void onFailure(CharSequence error) {
                    callback.onFailure(error == null
                            ? "Companion association that bai."
                            : error.toString());
                }

                public void onFailure(int errorCode, CharSequence error) {
                    callback.onFailure(error == null
                            ? "Companion association that bai, ma loi " + errorCode + "."
                            : errorCode + ": " + error);
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (IllegalStateException | IllegalArgumentException | SecurityException exception) {
            callback.onFailure("Khong the bat dau companion association: " + summarize(exception));
        } catch (RuntimeException exception) {
            callback.onFailure("CompanionDeviceManager bi loi runtime: " + summarize(exception));
        }
    }

    @Nullable
    private static BluetoothDevice getBluetoothDeviceExtra(@NonNull Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE,
                    BluetoothDevice.class
            );
        }
        Parcelable value = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
        return value instanceof BluetoothDevice ? (BluetoothDevice) value : null;
    }

    @Nullable
    private static ScanResult getBleScanResultExtra(@NonNull Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE,
                    ScanResult.class
            );
        }
        Parcelable value = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
        return value instanceof ScanResult ? (ScanResult) value : null;
    }

    @NonNull
    private static String describeDevice(@NonNull BluetoothDevice device) {
        String name = device.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed";
        }
        return name + " (" + device.getAddress() + ")";
    }

    @NonNull
    private static String summarize(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
