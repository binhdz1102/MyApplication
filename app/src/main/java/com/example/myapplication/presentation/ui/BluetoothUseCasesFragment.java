package com.example.myapplication.presentation.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentBluetoothUseCasesBinding;
import com.example.myapplication.presentation.bluetooth.BluetoothCompanionHelper;
import com.example.myapplication.presentation.model.BluetoothDeviceItem;
import com.example.myapplication.presentation.ui.adapter.BluetoothDeviceAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothUseCasesFragment extends Fragment {

    public interface Navigator {
        void openBackgroundPlayback();

        void openMediaSessionPreview();

        void openMediaControllerUi();

        void openNotificationLockscreen();
    }

    public static final String TAG = "BluetoothUseCasesFragment";
    private static final UUID RFCOMM_SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final long BLE_SCAN_TIMEOUT_MS = 12_000L;
    private static final int MAX_LOG_LINES = 150;
    private static final int DESIRED_MTU = 185;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted = true;
                        for (Boolean value : result.values()) {
                            granted &= Boolean.TRUE.equals(value);
                        }
                        appendLog(granted
                                ? "Da cap quyen Bluetooth can thiet."
                                : "Con thieu quyen Bluetooth, mot so use case se bi gioi han.");
                        seedBondedDevices();
                        updateAllUi();
                    }
            );

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        boolean enabled = isBluetoothCurrentlyEnabled();
                        appendLog(enabled
                                ? "Bluetooth da duoc bat."
                                : "Bluetooth van dang tat hoac nguoi dung da tu choi.");
                        updateAllUi();
                    }
            );

    private final ActivityResultLauncher<IntentSenderRequest> companionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    this::handleCompanionResult
            );

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final Deque<String> eventLogLines = new ArrayDeque<>();
    private final Map<String, BluetoothDeviceItem> discoveredDevices = new LinkedHashMap<>();
    private final BluetoothDeviceAdapter deviceAdapter =
            new BluetoothDeviceAdapter(this::selectDevice);
    private final SimpleDateFormat timeFormatter =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private final android.os.Handler mainHandler =
            new android.os.Handler(Looper.getMainLooper());
    private final Runnable stopBleScanRunnable = this::stopBleScan;
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                appendLog("Bluetooth state -> " + formatAdapterState(state));
                if (state == BluetoothAdapter.STATE_OFF) {
                    stopAllScans(false);
                    disconnectClassicConnection("RFCOMM bi dong vi Bluetooth da tat.");
                    disconnectGattConnection("GATT bi dong vi Bluetooth da tat.");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                appendLog("Classic discovery da bat dau.");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                appendLog("Classic discovery da ket thuc.");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = getBluetoothDeviceExtra(intent, BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String deviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                upsertDevice(device, deviceName, rssi, true, false, true);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = getBluetoothDeviceExtra(intent, BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    upsertDevice(
                            device,
                            safeGetDeviceName(device),
                            BluetoothDeviceItem.RSSI_UNKNOWN,
                            false,
                            false,
                            false
                    );
                    appendLog("Bond state cua " + safeDescribeDevice(device) + " -> "
                            + formatBondState(device.getBondState()));
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = getBluetoothDeviceExtra(intent, BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    appendLog("ACL connected: " + safeDescribeDevice(device));
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = getBluetoothDeviceExtra(intent, BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    appendLog("ACL disconnected: " + safeDescribeDevice(device));
                }
            }
            updateAllUi();
        }
    };
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) {
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            String deviceName = scanRecord != null ? scanRecord.getDeviceName() : null;
            upsertDevice(result.getDevice(), deviceName, result.getRssi(), false, true, true);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (results == null) {
                return;
            }
            for (ScanResult result : results) {
                onScanResult(0, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isBleScanning = false;
            appendLog("BLE scan that bai, ma loi " + errorCode + ".");
            updateAllUi();
        }
    };
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(
                BluetoothGatt gatt,
                int status,
                int newState
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                mainHandler.post(() -> {
                    bluetoothGatt = gatt;
                    isGattConnected = true;
                    gattConnectionSummary = "Connected toi " + safeDescribeDevice(gatt.getDevice());
                    appendLog("GATT connected: " + safeDescribeDevice(gatt.getDevice()));
                    boolean mtuRequested = gatt.requestMtu(DESIRED_MTU);
                    boolean discoveryStarted = gatt.discoverServices();
                    appendLog("GATT requestMtu=" + mtuRequested
                            + ", discoverServices=" + discoveryStarted);
                    updateAllUi();
                });
                return;
            }

            mainHandler.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS && status != 0) {
                    appendLog("GATT loi, status=" + status + ".");
                }
                closeGatt(gatt);
                isGattConnected = false;
                gattConnectionSummary = "Da ngat GATT";
                gattServicesSummary = getString(R.string.bluetooth_default_gatt_services);
                updateAllUi();
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mainHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gattServicesSummary = buildGattServicesSummary(gatt.getServices());
                    appendLog("Da discover " + gatt.getServices().size() + " GATT service(s).");
                } else {
                    gattServicesSummary = "Discover GATT services that bai, status=" + status;
                    appendLog(gattServicesSummary);
                }
                updateAllUi();
            });
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                byte[] value,
                int status
        ) {
            mainHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    appendLog("GATT RX " + shortUuid(characteristic.getUuid()) + ": "
                            + describePayload(value));
                } else {
                    appendLog("Read GATT char that bai, status=" + status);
                }
            });
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            byte[] value = characteristic == null ? null : characteristic.getValue();
            onCharacteristicRead(gatt, characteristic, value, status);
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            mainHandler.post(() -> appendLog(
                    status == BluetoothGatt.GATT_SUCCESS
                            ? "GATT write thanh cong: " + shortUuid(characteristic.getUuid())
                            : "GATT write that bai, status=" + status
            ));
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                byte[] value
        ) {
            mainHandler.post(() -> appendLog(
                    "GATT notify " + shortUuid(characteristic.getUuid()) + ": "
                            + describePayload(value)
            ));
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {
            onCharacteristicChanged(gatt, characteristic, characteristic.getValue());
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mainHandler.post(() -> appendLog(
                    status == BluetoothGatt.GATT_SUCCESS
                            ? "MTU moi: " + mtu
                            : "Request MTU that bai, status=" + status
            ));
        }
    };
    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            updateAllUi();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            updateAllUi();
        }
    };

    private FragmentBluetoothUseCasesBinding binding;
    private ScreenConfigurationHost screenConfigurationHost;
    private Navigator navigator;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private AudioManager audioManager;
    private boolean isBleScanning;
    private boolean isGattConnected;
    private boolean receiverRegistered;
    private boolean audioCallbackRegistered;
    @Nullable
    private BluetoothSocket classicSocket;
    @Nullable
    private Thread classicReadThread;
    @Nullable
    private BluetoothGatt bluetoothGatt;
    @Nullable
    private BluetoothDeviceItem selectedDevice;
    private String gattServicesSummary = "";
    private String companionSummary = "";
    private String classicConnectionSummary = "";
    private String gattConnectionSummary = "";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ScreenConfigurationHost) {
            screenConfigurationHost = (ScreenConfigurationHost) context;
        }
        if (context instanceof Navigator) {
            navigator = (Navigator) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentBluetoothUseCasesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeSystemServices();
        initializeSummaries();
        setupRecyclerView();
        setupButtonListeners();
        seedBondedDevices();
        appendLog("Mo demo Bluetooth use cases cho app thu ba.");
        updateAllUi();
    }

    @Override
    public void onStart() {
        super.onStart();
        registerBluetoothReceiver();
        registerAudioDeviceCallback();
        seedBondedDevices();
        updateAllUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (screenConfigurationHost != null) {
            screenConfigurationHost.updateScreenConfiguration(
                    new ScreenConfiguration(
                            R.string.title_bluetooth_use_cases,
                            true,
                            false,
                            false
                    )
            );
        }
    }

    @Override
    public void onStop() {
        stopAllScans(false);
        unregisterBluetoothReceiver();
        unregisterAudioDeviceCallback();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        disconnectClassicConnection(null);
        disconnectGattConnection(null);
        if (binding != null) {
            binding.recyclerBluetoothDevices.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        navigator = null;
        screenConfigurationHost = null;
        super.onDetach();
    }

    private void initializeSystemServices() {
        bluetoothManager = requireContext().getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null
                ? bluetoothManager.getAdapter()
                : BluetoothAdapter.getDefaultAdapter();
        audioManager = requireContext().getSystemService(AudioManager.class);
    }

    private void initializeSummaries() {
        gattServicesSummary = getString(R.string.bluetooth_default_gatt_services);
        companionSummary = getString(R.string.bluetooth_default_companion_state);
        classicConnectionSummary = "Chua co ket noi RFCOMM";
        gattConnectionSummary = "Chua co ket noi GATT";
    }

    private void setupRecyclerView() {
        binding.recyclerBluetoothDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBluetoothDevices.setNestedScrollingEnabled(false);
        binding.recyclerBluetoothDevices.setAdapter(deviceAdapter);
    }

    private void setupButtonListeners() {
        binding.buttonRequestPermissions.setOnClickListener(v -> requestBluetoothPermissions());
        binding.buttonEnableBluetooth.setOnClickListener(v -> promptEnableBluetooth());
        binding.buttonRefreshState.setOnClickListener(v -> refreshStateSnapshot());
        binding.buttonStartClassicScan.setOnClickListener(v -> startClassicDiscovery());
        binding.buttonStartBleScan.setOnClickListener(v -> startBleScan());
        binding.buttonStopScan.setOnClickListener(v -> stopAllScans(true));
        binding.buttonPairDevice.setOnClickListener(v -> pairSelectedDevice());
        binding.buttonConnectClassic.setOnClickListener(v -> connectSelectedClassicDevice());
        binding.buttonDisconnectClassic.setOnClickListener(
                v -> disconnectClassicConnection("Da ngat RFCOMM theo yeu cau.")
        );
        binding.buttonConnectGatt.setOnClickListener(v -> connectSelectedGattDevice());
        binding.buttonDisconnectGatt.setOnClickListener(
                v -> disconnectGattConnection("Da ngat GATT theo yeu cau.")
        );
        binding.buttonDiscoverServices.setOnClickListener(v -> discoverGattServices());
        binding.buttonReadGatt.setOnClickListener(v -> readGattCharacteristic());
        binding.buttonWriteGatt.setOnClickListener(v -> writeGattCharacteristic());
        binding.buttonSendClassic.setOnClickListener(v -> sendClassicPayload());
        binding.buttonRequestAudioRoute.setOnClickListener(v -> requestBluetoothAudioRoute());
        binding.buttonClearAudioRoute.setOnClickListener(v -> clearBluetoothAudioRoute());
        binding.buttonOpenBluetoothSettings.setOnClickListener(v -> openBluetoothSettings());
        binding.buttonOpenBackgroundDemo.setOnClickListener(v -> openBackgroundPlaybackDemo());
        binding.buttonOpenMediaSessionDemo.setOnClickListener(v -> openMediaSessionDemo());
        binding.buttonOpenControllerDemo.setOnClickListener(v -> openMediaControllerDemo());
        binding.buttonOpenNotificationDemo.setOnClickListener(v -> openNotificationDemo());
        binding.buttonCompanionClassic.setOnClickListener(v -> startCompanionClassicFlow());
        binding.buttonCompanionBle.setOnClickListener(v -> startCompanionBleFlow());
        binding.buttonClearLog.setOnClickListener(v -> clearEventLog());
    }

    private void requestBluetoothPermissions() {
        String[] permissions = buildRequiredPermissions();
        if (permissions.length == 0 || hasAllPermissions(permissions)) {
            appendLog("Bluetooth permissions da duoc cap roi.");
            updateAllUi();
            return;
        }
        permissionLauncher.launch(permissions);
    }

    private void promptEnableBluetooth() {
        if (bluetoothAdapter == null) {
            appendLog("Thiet bi khong co Bluetooth adapter.");
            updateAllUi();
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            appendLog("Bluetooth da bat san.");
            updateAllUi();
            return;
        }
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetoothLauncher.launch(enableIntent);
    }

    private void refreshStateSnapshot() {
        seedBondedDevices();
        updateAllUi();
        appendLog("Da refresh Bluetooth state snapshot.");
    }

    @SuppressLint("MissingPermission")
    private void startClassicDiscovery() {
        if (!ensureAdapterReadyForBluetooth("Classic discovery")) {
            return;
        }
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            boolean started = bluetoothAdapter.startDiscovery();
            appendLog(started
                    ? "Bat dau scan classic device."
                    : "Khong the bat dau scan classic device.");
        } catch (SecurityException exception) {
            appendLog("Classic discovery bi chan boi permission: " + summarize(exception));
        }
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void startBleScan() {
        if (!ensureAdapterReadyForBluetooth("BLE scan")) {
            return;
        }
        try {
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                appendLog("BluetoothLeScanner chua san sang.");
                return;
            }
            if (isBleScanning) {
                scanner.stopScan(bleScanCallback);
            }
            scanner.startScan(bleScanCallback);
            isBleScanning = true;
            mainHandler.removeCallbacks(stopBleScanRunnable);
            mainHandler.postDelayed(stopBleScanRunnable, BLE_SCAN_TIMEOUT_MS);
            appendLog("Bat dau BLE scan trong " + (BLE_SCAN_TIMEOUT_MS / 1000L) + " giay.");
        } catch (SecurityException exception) {
            appendLog("BLE scan bi chan boi permission: " + summarize(exception));
        }
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void stopBleScan() {
        if (!isBleScanning || bluetoothAdapter == null) {
            return;
        }
        try {
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(bleScanCallback);
            }
        } catch (SecurityException exception) {
            appendLog("Dung BLE scan that bai: " + summarize(exception));
        }
        isBleScanning = false;
        mainHandler.removeCallbacks(stopBleScanRunnable);
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void stopClassicDiscovery() {
        if (bluetoothAdapter == null) {
            return;
        }
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException exception) {
            appendLog("Dung classic discovery that bai: " + summarize(exception));
        }
    }

    private void stopAllScans(boolean userInitiated) {
        boolean wasClassicDiscoveryRunning = isClassicDiscoveryRunning();
        boolean wasBleScanning = isBleScanning;
        stopClassicDiscovery();
        stopBleScan();
        if (userInitiated && (wasClassicDiscoveryRunning || wasBleScanning)) {
            appendLog("Da dung tat ca scan session.");
        }
    }

    private void clearEventLog() {
        eventLogLines.clear();
        appendLog("Da xoa event log.");
    }

    @SuppressLint("MissingPermission")
    private void seedBondedDevices() {
        if (bluetoothAdapter == null || !hasConnectPermission()) {
            return;
        }
        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices == null) {
                return;
            }
            for (BluetoothDevice device : bondedDevices) {
                upsertDevice(
                        device,
                        safeGetDeviceName(device),
                        BluetoothDeviceItem.RSSI_UNKNOWN,
                        false,
                        false,
                        false
                );
            }
        } catch (SecurityException exception) {
            appendLog("Khong doc duoc bonded devices: " + summarize(exception));
        }
    }

    private boolean ensureAdapterReadyForBluetooth(@NonNull String operationLabel) {
        if (bluetoothAdapter == null) {
            appendLog(operationLabel + ": thiet bi khong co Bluetooth adapter.");
            updateAllUi();
            return false;
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            appendLog(operationLabel + ": can cap quyen Bluetooth truoc.");
            requestBluetoothPermissions();
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            appendLog(operationLabel + ": Bluetooth dang tat, mo prompt bat Bluetooth.");
            promptEnableBluetooth();
            return false;
        }
        return true;
    }

    private void selectDevice(@NonNull BluetoothDeviceItem item) {
        selectedDevice = item;
        deviceAdapter.setSelectedAddress(item.getAddress());
        appendLog("Da chon thiet bi: " + item.getDisplayName() + " (" + item.getAddress() + ")");
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void pairSelectedDevice() {
        if (!ensureSelectionAvailable("Pair")) {
            return;
        }
        try {
            BluetoothDevice device = selectedDevice.getDevice();
            boolean started = device.createBond();
            appendLog(started
                    ? "Da goi createBond cho " + safeDescribeDevice(device)
                    : "Khong the bat dau bond voi " + safeDescribeDevice(device));
        } catch (SecurityException exception) {
            appendLog("Pair bi chan boi permission: " + summarize(exception));
        }
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void connectSelectedClassicDevice() {
        if (!ensureSelectionAvailable("RFCOMM connect")) {
            return;
        }
        BluetoothDevice device = selectedDevice.getDevice();
        classicConnectionSummary = "Dang ket noi RFCOMM toi " + safeDescribeDevice(device);
        updateAllUi();
        ioExecutor.execute(() -> {
            BluetoothSocket socket = null;
            try {
                if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                socket = device.getBondState() == BluetoothDevice.BOND_BONDED
                        ? device.createRfcommSocketToServiceRecord(RFCOMM_SPP_UUID)
                        : device.createInsecureRfcommSocketToServiceRecord(RFCOMM_SPP_UUID);
                socket.connect();
                BluetoothSocket connectedSocket = socket;
                mainHandler.post(() -> {
                    classicSocket = connectedSocket;
                    classicConnectionSummary = "Connected toi " + safeDescribeDevice(device);
                    appendLog("RFCOMM connected: " + safeDescribeDevice(device));
                    updateAllUi();
                    startClassicReader(connectedSocket);
                });
            } catch (IOException | SecurityException exception) {
                closeQuietly(socket);
                String message = "RFCOMM connect that bai: " + summarize(exception);
                mainHandler.post(() -> {
                    classicConnectionSummary = message;
                    appendLog(message);
                    updateAllUi();
                });
            }
        });
    }

    private void startClassicReader(@NonNull BluetoothSocket socket) {
        Thread existingReader = classicReadThread;
        if (existingReader != null && existingReader.isAlive()) {
            existingReader.interrupt();
        }
        classicReadThread = new Thread(() -> readClassicSocket(socket), "bt-rfcomm-reader");
        classicReadThread.start();
    }

    private void readClassicSocket(@NonNull BluetoothSocket socket) {
        byte[] buffer = new byte[1024];
        try {
            InputStream inputStream = socket.getInputStream();
            while (!Thread.currentThread().isInterrupted() && socket.isConnected()) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (bytesRead == 0) {
                    continue;
                }
                byte[] payload = Arrays.copyOf(buffer, bytesRead);
                appendLog("RFCOMM RX: " + describePayload(payload));
            }
        } catch (IOException exception) {
            appendLog("RFCOMM read ket thuc: " + summarize(exception));
        }

        mainHandler.post(() -> {
            if (classicSocket == socket) {
                disconnectClassicConnection("RFCOMM socket da dong tu phia thiet bi.");
            }
        });
    }

    private void sendClassicPayload() {
        if (classicSocket == null || !classicSocket.isConnected()) {
            appendLog("RFCOMM chua connected, khong gui duoc payload.");
            updateAllUi();
            return;
        }
        byte[] payload = readPayloadBytes();
        if (payload.length == 0) {
            appendLog("Payload dang rong, bo qua RFCOMM send.");
            return;
        }
        ioExecutor.execute(() -> {
            try {
                classicSocket.getOutputStream().write(payload);
                classicSocket.getOutputStream().flush();
                appendLog("RFCOMM TX: " + describePayload(payload));
            } catch (IOException exception) {
                appendLog("RFCOMM send that bai: " + summarize(exception));
            }
        });
    }

    private void disconnectClassicConnection(@Nullable String reason) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> disconnectClassicConnection(reason));
            return;
        }
        BluetoothSocket socket = classicSocket;
        classicSocket = null;
        Thread readerThread = classicReadThread;
        classicReadThread = null;
        if (readerThread != null && readerThread != Thread.currentThread()) {
            readerThread.interrupt();
        }
        closeQuietly(socket);
        classicConnectionSummary = "Da ngat RFCOMM";
        if (!TextUtils.isEmpty(reason)) {
            appendLog(reason);
        }
        updateAllUi();
    }

    @SuppressLint("MissingPermission")
    private void connectSelectedGattDevice() {
        if (!ensureSelectionAvailable("GATT connect")) {
            return;
        }
        BluetoothDevice device = selectedDevice.getDevice();
        if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            appendLog("Thiet bi nay duoc danh dau la classic-only, GATT co the se that bai.");
        }
        disconnectGattConnection(null);
        gattConnectionSummary = "Dang ket noi GATT toi " + safeDescribeDevice(device);
        gattServicesSummary = getString(R.string.bluetooth_default_gatt_services);
        updateAllUi();
        try {
            bluetoothGatt = device.connectGatt(
                    requireContext(),
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
            );
        } catch (SecurityException exception) {
            gattConnectionSummary = "GATT connect that bai: " + summarize(exception);
            appendLog(gattConnectionSummary);
            updateAllUi();
        }
    }

    private void discoverGattServices() {
        if (!isGattConnected || bluetoothGatt == null) {
            appendLog("GATT chua connected, khong discover services duoc.");
            return;
        }
        boolean started = bluetoothGatt.discoverServices();
        appendLog(started
                ? "Da goi discoverServices() thu cong."
                : "Khong the goi discoverServices().");
    }

    private void readGattCharacteristic() {
        if (!isGattConnected || bluetoothGatt == null) {
            appendLog("GATT chua connected, khong read duoc characteristic.");
            return;
        }
        BluetoothGattCharacteristic readableCharacteristic =
                findCharacteristicWithProperty(BluetoothGattCharacteristic.PROPERTY_READ);
        if (readableCharacteristic == null) {
            appendLog("Khong tim thay characteristic co quyen READ.");
            return;
        }
        boolean started = bluetoothGatt.readCharacteristic(readableCharacteristic);
        appendLog(started
                ? "Dang read GATT char " + shortUuid(readableCharacteristic.getUuid())
                : "Khong the bat dau read GATT char.");
    }

    private void writeGattCharacteristic() {
        if (!isGattConnected || bluetoothGatt == null) {
            appendLog("GATT chua connected, khong write duoc characteristic.");
            return;
        }
        byte[] payload = readPayloadBytes();
        if (payload.length == 0) {
            appendLog("Payload dang rong, bo qua GATT write.");
            return;
        }
        BluetoothGattCharacteristic writableCharacteristic =
                findCharacteristicWithProperty(
                        BluetoothGattCharacteristic.PROPERTY_WRITE
                                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                );
        if (writableCharacteristic == null) {
            appendLog("Khong tim thay characteristic co quyen WRITE.");
            return;
        }
        int properties = writableCharacteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            writableCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            writableCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
        writableCharacteristic.setValue(payload);
        boolean started = bluetoothGatt.writeCharacteristic(writableCharacteristic);
        appendLog(started
                ? "Dang write GATT char " + shortUuid(writableCharacteristic.getUuid())
                        + " voi payload " + describePayload(payload)
                : "Khong the bat dau GATT write.");
    }

    private void disconnectGattConnection(@Nullable String reason) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> disconnectGattConnection(reason));
            return;
        }
        BluetoothGatt gatt = bluetoothGatt;
        bluetoothGatt = null;
        isGattConnected = false;
        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (SecurityException ignored) {
            }
            closeGatt(gatt);
        }
        gattConnectionSummary = "Da ngat GATT";
        gattServicesSummary = getString(R.string.bluetooth_default_gatt_services);
        if (!TextUtils.isEmpty(reason)) {
            appendLog(reason);
        }
        updateAllUi();
    }

    private void requestBluetoothAudioRoute() {
        if (audioManager == null) {
            appendLog("AudioManager khong san sang.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<AudioDeviceInfo> candidates = audioManager.getAvailableCommunicationDevices();
            AudioDeviceInfo bluetoothDevice = null;
            for (AudioDeviceInfo candidate : candidates) {
                if (isBluetoothAudioDevice(candidate)) {
                    bluetoothDevice = candidate;
                    break;
                }
            }
            if (bluetoothDevice == null) {
                appendLog("Khong tim thay communication device Bluetooth nao.");
                updateAllUi();
                return;
            }
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            boolean routed = audioManager.setCommunicationDevice(bluetoothDevice);
            appendLog((routed ? "Da xin route toi " : "Khong the xin route toi ")
                    + describeAudioDevice(bluetoothDevice));
        } else {
            if (audioManager.isBluetoothScoAvailableOffCall()) {
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                appendLog("Da goi startBluetoothSco() cho use case communication.");
            } else {
                appendLog("Bluetooth SCO khong available off call tren thiet bi nay.");
            }
        }
        updateAllUi();
    }

    private void clearBluetoothAudioRoute() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice();
            audioManager.setMode(AudioManager.MODE_NORMAL);
            appendLog("Da clear communication device route.");
        } else {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            appendLog("Da goi stopBluetoothSco().");
        }
        updateAllUi();
    }

    private void openBluetoothSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (ActivityNotFoundException exception) {
            appendLog("Khong mo duoc Bluetooth settings: " + summarize(exception));
        }
    }

    private void openBackgroundPlaybackDemo() {
        if (navigator == null) {
            appendLog("Navigator chua san sang de mo Background Playback demo.");
            return;
        }
        appendLog("Mo Background Playback demo de thu media control qua Bluetooth headset.");
        navigator.openBackgroundPlayback();
    }

    private void openMediaSessionDemo() {
        if (navigator == null) {
            appendLog("Navigator chua san sang de mo MediaSession demo.");
            return;
        }
        appendLog("Mo MediaSession demo de quan sat metadata/AVRCP control.");
        navigator.openMediaSessionPreview();
    }

    private void openMediaControllerDemo() {
        if (navigator == null) {
            appendLog("Navigator chua san sang de mo MediaController demo.");
            return;
        }
        appendLog("Mo MediaController demo de thu play/pause/next/previous tu session service.");
        navigator.openMediaControllerUi();
    }

    private void openNotificationDemo() {
        if (navigator == null) {
            appendLog("Navigator chua san sang de mo Notification demo.");
            return;
        }
        appendLog("Mo Notification/Lockscreen demo cho media control qua Bluetooth.");
        navigator.openNotificationLockscreen();
    }

    private void startCompanionClassicFlow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            companionSummary = "Companion Device API can Android 8.0+";
            appendLog(companionSummary);
            updateAllUi();
            return;
        }
        String unsupportedReason = BluetoothCompanionHelper.getUnsupportedReason(requireContext());
        if (!TextUtils.isEmpty(unsupportedReason)) {
            companionSummary = unsupportedReason;
            appendLog(companionSummary);
            updateAllUi();
            return;
        }
        stopAllScans(false);
        companionSummary = "Dang mo system chooser cho companion classic...";
        updateAllUi();
        BluetoothCompanionHelper.startClassicAssociation(requireContext(), new BluetoothCompanionHelper.Callback() {
            @Override
            public void onChooserReady(
                    @NonNull android.content.IntentSender chooserLauncher,
                    @NonNull String requestLabel
            ) {
                companionSummary = "Dang doi nguoi dung chon companion " + requestLabel + " device.";
                updateAllUi();
                companionLauncher.launch(new IntentSenderRequest.Builder(chooserLauncher).build());
            }

            @Override
            public void onFailure(@NonNull String message) {
                companionSummary = message;
                appendLog(message);
                updateAllUi();
            }
        });
    }

    private void startCompanionBleFlow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            companionSummary = "Companion Device API can Android 8.0+";
            appendLog(companionSummary);
            updateAllUi();
            return;
        }
        String unsupportedReason = BluetoothCompanionHelper.getUnsupportedReason(requireContext());
        if (!TextUtils.isEmpty(unsupportedReason)) {
            companionSummary = unsupportedReason;
            appendLog(companionSummary);
            updateAllUi();
            return;
        }
        stopAllScans(false);
        companionSummary = "Dang mo system chooser cho companion BLE...";
        updateAllUi();
        BluetoothCompanionHelper.startBleAssociation(requireContext(), new BluetoothCompanionHelper.Callback() {
            @Override
            public void onChooserReady(
                    @NonNull android.content.IntentSender chooserLauncher,
                    @NonNull String requestLabel
            ) {
                companionSummary = "Dang doi nguoi dung chon companion " + requestLabel + " device.";
                updateAllUi();
                companionLauncher.launch(new IntentSenderRequest.Builder(chooserLauncher).build());
            }

            @Override
            public void onFailure(@NonNull String message) {
                companionSummary = message;
                appendLog(message);
                updateAllUi();
            }
        });
    }

    private void handleCompanionResult(@NonNull ActivityResult result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (result.getResultCode() != android.app.Activity.RESULT_OK) {
            companionSummary = "Nguoi dung da dong companion chooser hoac tu choi yeu cau.";
            appendLog(companionSummary);
            updateAllUi();
            return;
        }
        BluetoothCompanionHelper.SelectionResult selectionResult =
                BluetoothCompanionHelper.parseSelectionResult(result.getData());
        companionSummary = selectionResult.getSummary();
        appendLog(companionSummary);
        if (selectionResult.getDevice() != null) {
            BluetoothDevice device = selectionResult.getDevice();
            upsertDevice(
                    device,
                    safeGetDeviceName(device),
                    BluetoothDeviceItem.RSSI_UNKNOWN,
                    false,
                    false,
                    false
            );
            BluetoothDeviceItem item = discoveredDevices.get(safeGetDeviceAddress(device));
            if (item != null) {
                selectDevice(item);
            }
        }
        updateAllUi();
    }

    private void updateAllUi() {
        if (binding == null) {
            return;
        }
        updatePermissionSummaryUi();
        updateBluetoothSummaryUi();
        updateProfileSummaryUi();
        updateAudioRouteSummaryUi();
        updateCompanionSummaryUi();
        updateSelectedDeviceUi();
        updateGattServicesUi();
        updateDeviceListUi();
        updateEventLogUi();
    }

    private void updatePermissionSummaryUi() {
        binding.textPermissionsStateValue.setText(buildPermissionSummary());
        binding.buttonEnableBluetooth.setEnabled(bluetoothAdapter != null && !bluetoothAdapter.isEnabled());
    }

    private void updateBluetoothSummaryUi() {
        binding.textBluetoothSummaryValue.setText(buildBluetoothSummary());
    }

    private void updateProfileSummaryUi() {
        binding.textProfileSummaryValue.setText(buildProfileSummary());
    }

    private void updateAudioRouteSummaryUi() {
        binding.textAudioRouteSummaryValue.setText(buildAudioRouteSummary());
    }

    private void updateCompanionSummaryUi() {
        binding.textCompanionSummaryValue.setText(companionSummary);
    }

    private void updateSelectedDeviceUi() {
        if (selectedDevice == null) {
            binding.textSelectedDeviceValue.setText(
                    "Chua chon thiet bi. Tap mot item trong danh sach de test pair/connect/exchange data."
            );
        } else {
            String selectedSummary = selectedDevice.getDisplayName()
                    + "\n"
                    + selectedDevice.getAddress()
                    + "\nType: " + formatDeviceType(selectedDevice.getDeviceType())
                    + " | Bond: " + formatBondState(selectedDevice.getBondState())
                    + " | Source: " + formatSourceSummary(selectedDevice)
                    + "\nRFCOMM: " + classicConnectionSummary
                    + "\nGATT: " + gattConnectionSummary;
            binding.textSelectedDeviceValue.setText(selectedSummary);
        }

        boolean hasSelection = selectedDevice != null;
        boolean adapterEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        boolean classicConnected = classicSocket != null && classicSocket.isConnected();
        boolean gattConnected = isGattConnected && bluetoothGatt != null;
        boolean canConnect = hasSelection && hasConnectPermission() && adapterEnabled;

        binding.buttonPairDevice.setEnabled(
                hasSelection
                        && hasConnectPermission()
                        && selectedDevice != null
                        && selectedDevice.getBondState() != BluetoothDevice.BOND_BONDED
        );
        binding.buttonConnectClassic.setEnabled(canConnect && !classicConnected);
        binding.buttonDisconnectClassic.setEnabled(classicConnected);
        binding.buttonConnectGatt.setEnabled(canConnect && !gattConnected);
        binding.buttonDisconnectGatt.setEnabled(gattConnected);
        binding.buttonDiscoverServices.setEnabled(gattConnected);
        binding.buttonReadGatt.setEnabled(gattConnected && findCharacteristicWithProperty(
                BluetoothGattCharacteristic.PROPERTY_READ) != null);
        binding.buttonWriteGatt.setEnabled(gattConnected && findCharacteristicWithProperty(
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != null);
        binding.buttonSendClassic.setEnabled(classicConnected);
    }

    private void updateGattServicesUi() {
        binding.textGattServicesValue.setText(gattServicesSummary);
    }

    private void updateDeviceListUi() {
        boolean isEmpty = discoveredDevices.isEmpty();
        binding.textDeviceListEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerBluetoothDevices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateEventLogUi() {
        if (eventLogLines.isEmpty()) {
            binding.textEventLogValue.setText(R.string.bluetooth_default_log);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : eventLogLines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        binding.textEventLogValue.setText(builder.toString());
    }

    private String buildPermissionSummary() {
        List<String> lines = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lines.add("BLUETOOTH_SCAN: " + yesNo(hasPermission(Manifest.permission.BLUETOOTH_SCAN)));
            lines.add("BLUETOOTH_CONNECT: " + yesNo(hasPermission(Manifest.permission.BLUETOOTH_CONNECT)));
        } else {
            lines.add("ACCESS_FINE_LOCATION: " + yesNo(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)));
        }
        lines.add("Bluetooth adapter: " + yesNo(bluetoothAdapter != null));
        lines.add("Companion API: " + yesNo(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O));
        return joinLines(lines);
    }

    @SuppressLint("MissingPermission")
    private String buildBluetoothSummary() {
        if (bluetoothAdapter == null) {
            return "Thiet bi nay khong co Bluetooth adapter.";
        }
        List<String> lines = new ArrayList<>();
        lines.add("Enabled: " + yesNo(bluetoothAdapter.isEnabled()));
        lines.add("Adapter state: " + formatAdapterState(bluetoothAdapter.getState()));
        lines.add("Classic discovery: " + yesNo(isClassicDiscoveryRunning()));
        lines.add("BLE scan: " + yesNo(isBleScanning));
        lines.add("Bonded devices: " + getBondedDeviceCountLabel());
        lines.add("Discovered list size: " + discoveredDevices.size());
        lines.add("RFCOMM: " + classicConnectionSummary);
        lines.add("GATT: " + gattConnectionSummary);
        return joinLines(lines);
    }

    @SuppressLint("MissingPermission")
    private String buildProfileSummary() {
        if (bluetoothAdapter == null) {
            return "Khong co Bluetooth adapter de doc profile state.";
        }
        if (!hasConnectPermission()) {
            return "Can BLUETOOTH_CONNECT de doc profile state tren Android 12+.";
        }
        List<String> lines = new ArrayList<>();
        try {
            lines.add("HEADSET: " + formatProfileState(
                    bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)));
            lines.add("A2DP: " + formatProfileState(
                    bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP)));
            if (bluetoothManager != null) {
                lines.add("GATT clients: "
                        + bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size());
            }
        } catch (SecurityException exception) {
            return "Doc profile state that bai: " + summarize(exception);
        }
        return joinLines(lines);
    }

    private String buildAudioRouteSummary() {
        if (audioManager == null) {
            return "AudioManager khong san sang.";
        }
        List<String> lines = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo currentDevice = audioManager.getCommunicationDevice();
            lines.add("Communication device: " + describeAudioDevice(currentDevice));
            lines.add("Communication Bluetooth devices: "
                    + describeAudioDeviceList(audioManager.getAvailableCommunicationDevices(), true));
            lines.add("Output Bluetooth devices: "
                    + describeAudioDeviceArray(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS), true));
        } else {
            lines.add("Bluetooth SCO on: " + yesNo(audioManager.isBluetoothScoOn()));
            lines.add("Bluetooth A2DP on: " + yesNo(audioManager.isBluetoothA2dpOn()));
            lines.add("Speakerphone on: " + yesNo(audioManager.isSpeakerphoneOn()));
        }
        lines.add("Gioi han: app thu ba chi xin communication route; media output route van do he thong quyet dinh.");
        return joinLines(lines);
    }

    private void appendLog(@NonNull String message) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> appendLog(message));
            return;
        }
        String line = timeFormatter.format(new Date()) + "  " + message;
        eventLogLines.addLast(line);
        while (eventLogLines.size() > MAX_LOG_LINES) {
            eventLogLines.removeFirst();
        }
        updateEventLogUi();
    }

    @SuppressLint("MissingPermission")
    private void upsertDevice(
            @NonNull BluetoothDevice device,
            @Nullable String displayName,
            int rssi,
            boolean fromClassic,
            boolean fromBle,
            boolean logIfNew
    ) {
        String address = safeGetDeviceAddress(device);
        if (TextUtils.isEmpty(address)) {
            return;
        }
        boolean isNew = !discoveredDevices.containsKey(address);
        BluetoothDeviceItem item = discoveredDevices.get(address);
        if (item == null) {
            item = new BluetoothDeviceItem(device, displayName);
            discoveredDevices.put(address, item);
        }
        item.update(device, displayName, rssi, fromClassic, fromBle);
        if (selectedDevice != null && address.equals(selectedDevice.getAddress())) {
            selectedDevice = item;
        }
        refreshDeviceList();
        if (isNew && logIfNew) {
            appendLog("Tim thay thiet bi: " + item.getDisplayName() + " (" + address + ")");
        }
    }

    private void refreshDeviceList() {
        List<BluetoothDeviceItem> items = new ArrayList<>(discoveredDevices.values());
        Collections.sort(items, Comparator
                .comparingLong(BluetoothDeviceItem::getLastSeenTimestamp)
                .reversed()
                .thenComparing(BluetoothDeviceItem::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        deviceAdapter.submitList(items);
        deviceAdapter.setSelectedAddress(selectedDevice == null ? null : selectedDevice.getAddress());
        updateDeviceListUi();
    }

    private void registerBluetoothReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                    bluetoothReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            requireContext().registerReceiver(bluetoothReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterBluetoothReceiver() {
        if (!receiverRegistered) {
            return;
        }
        requireContext().unregisterReceiver(bluetoothReceiver);
        receiverRegistered = false;
    }

    private void registerAudioDeviceCallback() {
        if (audioCallbackRegistered || audioManager == null) {
            return;
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler);
        audioCallbackRegistered = true;
    }

    private void unregisterAudioDeviceCallback() {
        if (!audioCallbackRegistered || audioManager == null) {
            return;
        }
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        audioCallbackRegistered = false;
    }

    private boolean ensureSelectionAvailable(@NonNull String label) {
        if (selectedDevice == null) {
            appendLog(label + ": hay chon mot thiet bi truoc.");
            return false;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            appendLog(label + ": Bluetooth dang tat hoac khong co adapter.");
            return false;
        }
        if (!hasConnectPermission()) {
            appendLog(label + ": can BLUETOOTH_CONNECT truoc.");
            requestBluetoothPermissions();
            return false;
        }
        return true;
    }

    private String[] buildRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return permissions.toArray(new String[0]);
    }

    private boolean hasAllPermissions(@NonNull String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasScanPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                : hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
    }

    private boolean hasPermission(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(requireContext(), permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBluetoothCurrentlyEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @Nullable
    private BluetoothGattCharacteristic findCharacteristicWithProperty(int propertyMask) {
        if (bluetoothGatt == null) {
            return null;
        }
        List<BluetoothGattService> services = bluetoothGatt.getServices();
        if (services == null) {
            return null;
        }
        for (BluetoothGattService service : services) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if ((characteristic.getProperties() & propertyMask) != 0) {
                    return characteristic;
                }
            }
        }
        return null;
    }

    private String buildGattServicesSummary(@Nullable List<BluetoothGattService> services) {
        if (services == null || services.isEmpty()) {
            return getString(R.string.bluetooth_default_gatt_services);
        }
        StringBuilder builder = new StringBuilder();
        for (BluetoothGattService service : services) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("Service ")
                    .append(shortUuid(service.getUuid()))
                    .append(" | chars ")
                    .append(service.getCharacteristics().size());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                builder.append("\n- ")
                        .append(shortUuid(characteristic.getUuid()))
                        .append(" [")
                        .append(describeGattProperties(characteristic.getProperties()))
                        .append("]");
            }
        }
        return builder.toString();
    }

    private String describeGattProperties(int properties) {
        List<String> labels = new ArrayList<>();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            labels.add("READ");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            labels.add("WRITE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            labels.add("WRITE_NR");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            labels.add("NOTIFY");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            labels.add("INDICATE");
        }
        return labels.isEmpty() ? "none" : TextUtils.join(",", labels);
    }

    private String describePayload(@Nullable byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "<empty>";
        }
        String text = new String(payload, StandardCharsets.UTF_8)
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + text + "\"" + " | hex " + bytesToHex(payload);
    }

    private byte[] readPayloadBytes() {
        if (binding == null || binding.editPayload.getText() == null) {
            return new byte[0];
        }
        String payload = binding.editPayload.getText().toString()
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    private String bytesToHex(@NonNull byte[] payload) {
        StringBuilder builder = new StringBuilder();
        for (byte value : payload) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(String.format(Locale.US, "%02X", value));
        }
        return builder.toString();
    }

    private String joinLines(@NonNull List<String> lines) {
        return TextUtils.join("\n", lines);
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    @SuppressLint("MissingPermission")
    private String getBondedDeviceCountLabel() {
        if (!hasConnectPermission()) {
            return "permission required";
        }
        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter == null
                    ? null
                    : bluetoothAdapter.getBondedDevices();
            return bondedDevices == null ? "0" : String.valueOf(bondedDevices.size());
        } catch (SecurityException exception) {
            return "error: " + summarize(exception);
        }
    }

    private boolean isClassicDiscoveryRunning() {
        if (bluetoothAdapter == null) {
            return false;
        }
        try {
            return bluetoothAdapter.isDiscovering();
        } catch (SecurityException exception) {
            return false;
        }
    }

    private String formatAdapterState(int state) {
        if (state == BluetoothAdapter.STATE_ON) {
            return "ON";
        }
        if (state == BluetoothAdapter.STATE_TURNING_ON) {
            return "TURNING_ON";
        }
        if (state == BluetoothAdapter.STATE_TURNING_OFF) {
            return "TURNING_OFF";
        }
        if (state == BluetoothAdapter.STATE_OFF) {
            return "OFF";
        }
        return "UNKNOWN(" + state + ")";
    }

    private String formatProfileState(int state) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            return "CONNECTED";
        }
        if (state == BluetoothProfile.STATE_CONNECTING) {
            return "CONNECTING";
        }
        if (state == BluetoothProfile.STATE_DISCONNECTING) {
            return "DISCONNECTING";
        }
        if (state == BluetoothProfile.STATE_DISCONNECTED) {
            return "DISCONNECTED";
        }
        return "UNKNOWN(" + state + ")";
    }

    private String formatDeviceType(int type) {
        if (type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            return "Classic";
        }
        if (type == BluetoothDevice.DEVICE_TYPE_LE) {
            return "LE";
        }
        if (type == BluetoothDevice.DEVICE_TYPE_DUAL) {
            return "Dual";
        }
        return "Unknown";
    }

    private String formatBondState(int state) {
        if (state == BluetoothDevice.BOND_BONDED) {
            return "Bonded";
        }
        if (state == BluetoothDevice.BOND_BONDING) {
            return "Bonding";
        }
        return "Not bonded";
    }

    private String formatSourceSummary(@NonNull BluetoothDeviceItem item) {
        List<String> values = new ArrayList<>();
        if (item.isSeenFromClassic()) {
            values.add("Classic");
        }
        if (item.isSeenFromBle()) {
            values.add("BLE");
        }
        if (values.isEmpty()) {
            values.add("Bonded cache");
        }
        return TextUtils.join(", ", values);
    }

    @SuppressLint("MissingPermission")
    private String safeGetDeviceName(@NonNull BluetoothDevice device) {
        try {
            String name = device.getName();
            return TextUtils.isEmpty(name) ? "Unnamed" : name;
        } catch (SecurityException exception) {
            return "Unnamed";
        }
    }

    @SuppressLint("MissingPermission")
    private String safeGetDeviceAddress(@NonNull BluetoothDevice device) {
        try {
            String address = device.getAddress();
            return address == null ? "" : address;
        } catch (SecurityException exception) {
            return "";
        }
    }

    private String safeDescribeDevice(@NonNull BluetoothDevice device) {
        return safeGetDeviceName(device) + " (" + safeGetDeviceAddress(device) + ")";
    }

    private void closeGatt(@Nullable BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        try {
            gatt.close();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(@Nullable BluetoothSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Nullable
    private BluetoothDevice getBluetoothDeviceExtra(@NonNull Intent intent, @NonNull String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(key, BluetoothDevice.class);
        }
        Parcelable value = intent.getParcelableExtra(key);
        return value instanceof BluetoothDevice ? (BluetoothDevice) value : null;
    }

    private String summarize(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }

    private String shortUuid(@NonNull UUID uuid) {
        String value = uuid.toString();
        return value.length() > 8 ? value.substring(0, 8) : value;
    }

    private boolean isBluetoothAudioDevice(@Nullable AudioDeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return false;
        }
        int type = deviceInfo.getType();
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                || type == AudioDeviceInfo.TYPE_BLE_SPEAKER;
    }

    private String describeAudioDevice(@Nullable AudioDeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return "none";
        }
        CharSequence productName = deviceInfo.getProductName();
        String name = productName == null || productName.length() == 0
                ? "Unnamed audio device"
                : productName.toString();
        return name + " [type=" + deviceInfo.getType() + "]";
    }

    private String describeAudioDeviceList(
            @NonNull List<AudioDeviceInfo> devices,
            boolean bluetoothOnly
    ) {
        List<String> values = new ArrayList<>();
        for (AudioDeviceInfo device : devices) {
            if (!bluetoothOnly || isBluetoothAudioDevice(device)) {
                values.add(describeAudioDevice(device));
            }
        }
        return values.isEmpty() ? "none" : TextUtils.join(", ", values);
    }

    private String describeAudioDeviceArray(
            @NonNull AudioDeviceInfo[] devices,
            boolean bluetoothOnly
    ) {
        List<String> values = new ArrayList<>();
        for (AudioDeviceInfo device : devices) {
            if (!bluetoothOnly || isBluetoothAudioDevice(device)) {
                values.add(describeAudioDevice(device));
            }
        }
        return values.isEmpty() ? "none" : TextUtils.join(", ", values);
    }
}
