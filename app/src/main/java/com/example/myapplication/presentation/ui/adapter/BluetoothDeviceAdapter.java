package com.example.myapplication.presentation.ui.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemBluetoothDeviceBinding;
import com.example.myapplication.presentation.model.BluetoothDeviceItem;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BluetoothDeviceAdapter
        extends RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothDeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClicked(BluetoothDeviceItem item);
    }

    private final OnDeviceClickListener listener;
    private final List<BluetoothDeviceItem> items = new ArrayList<>();
    @Nullable
    private String selectedAddress;

    public BluetoothDeviceAdapter(@NonNull OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<BluetoothDeviceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelectedAddress(@Nullable String address) {
        selectedAddress = address;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BluetoothDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new BluetoothDeviceViewHolder(ItemBluetoothDeviceBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDeviceViewHolder holder, int position) {
        BluetoothDeviceItem item = items.get(position);
        boolean isSelected = selectedAddress != null && selectedAddress.equals(item.getAddress());
        holder.bind(item, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BluetoothDeviceViewHolder extends RecyclerView.ViewHolder {

        private final ItemBluetoothDeviceBinding binding;

        BluetoothDeviceViewHolder(@NonNull ItemBluetoothDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull BluetoothDeviceItem item,
                boolean isSelected,
                @NonNull OnDeviceClickListener listener
        ) {
            binding.textDeviceName.setText(item.getDisplayName());
            binding.textDeviceAddress.setText(item.getAddress());
            binding.textDeviceMeta.setText(buildMetaSummary(item));
            binding.textDeviceSources.setText(buildSourceSummary(item));

            int primaryColor = MaterialColors.getColor(
                    binding.getRoot(),
                    androidx.appcompat.R.attr.colorPrimary
            );
            int outlineColor = MaterialColors.getColor(
                    binding.getRoot(),
                    com.google.android.material.R.attr.colorOutline
            );
            int surfaceColor = MaterialColors.getColor(
                    binding.getRoot(),
                    com.google.android.material.R.attr.colorSurface
            );
            binding.cardDevice.setStrokeWidth(isSelected ? 4 : 1);
            binding.cardDevice.setStrokeColor(isSelected ? primaryColor : outlineColor);
            binding.cardDevice.setCardBackgroundColor(surfaceColor);

            binding.cardDevice.setOnClickListener(view -> listener.onDeviceClicked(item));
        }

        private CharSequence buildMetaSummary(@NonNull BluetoothDeviceItem item) {
            String rssiLabel = item.getRssi() == BluetoothDeviceItem.RSSI_UNKNOWN
                    ? "RSSI ?"
                    : String.format(Locale.US, "RSSI %d dBm", item.getRssi());
            return formatDeviceType(item.getDeviceType())
                    + " | "
                    + formatBondState(item.getBondState())
                    + " | "
                    + rssiLabel;
        }

        private CharSequence buildSourceSummary(@NonNull BluetoothDeviceItem item) {
            List<String> sources = new ArrayList<>();
            if (item.isSeenFromClassic()) {
                sources.add("Classic scan");
            }
            if (item.isSeenFromBle()) {
                sources.add("BLE scan");
            }
            if (sources.isEmpty()) {
                sources.add("Bonded cache");
            }
            return "Nguon: " + join(sources);
        }

        private String join(@NonNull List<String> values) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(values.get(index));
            }
            return builder.toString();
        }

        private String formatDeviceType(int deviceType) {
            if (deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                return "Classic";
            }
            if (deviceType == BluetoothDevice.DEVICE_TYPE_LE) {
                return "LE";
            }
            if (deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
                return "Dual";
            }
            return "Unknown type";
        }

        private String formatBondState(int bondState) {
            if (bondState == BluetoothDevice.BOND_BONDED) {
                return "Bonded";
            }
            if (bondState == BluetoothDevice.BOND_BONDING) {
                return "Bonding";
            }
            return "Not bonded";
        }
    }
}
