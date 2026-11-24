package de.danoeh.antennapod.ui.screen.car;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;

public final class CarBluetooth {
    public static final String PREF_DEVICES = "prefCarBluetoothDevices";
    public static final String PREF_NOTIFICATION = "prefCarBluetoothNotification";
    public static final String EXTRA_FROM_BLUETOOTH = "fromBluetooth";
    static final String KEY_CONNECTED = "bluetoothConnected";
    private static final String PREF_NAME = "CarPlayer";
    private static final String KEY_DISMISSED = "bluetoothDismissed";
    private static final String CHANNEL_ID = "car_player";
    private static final int NOTIFICATION_ID = 7301;

    public static final class Device {
        public final String address;
        public final String name;

        Device(@NonNull String address, @NonNull String name) {
            this.address = address;
            this.name = name;
        }
    }

    private CarBluetooth() {
    }

    static void onDeviceEvent(@NonNull Context context, @Nullable String action, @Nullable BluetoothDevice device) {
        if (device == null || !hasPermission(context)) {
            return;
        }
        String address = device.getAddress();
        if (!getDevices(context).contains(address) || !CarPlayerPreferences.isEnabled(context)) {
            return;
        }
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            getPrefs(context).edit().putString(KEY_CONNECTED, address).putBoolean(KEY_DISMISSED, false).apply();
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_NOTIFICATION, true)) {
                showNotification(context, nameOf(device));
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)
                && address.equals(getPrefs(context).getString(KEY_CONNECTED, null))) {
            getPrefs(context).edit().remove(KEY_CONNECTED).remove(KEY_DISMISSED).apply();
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
        }
    }

    public static boolean isConnected(@NonNull Context context) {
        return getPrefs(context).getString(KEY_CONNECTED, null) != null;
    }

    public static void openIfConnected(@NonNull Activity activity) {
        if (isConnected(activity) && !getPrefs(activity).getBoolean(KEY_DISMISSED, false)
                && CarPlayerPreferences.isEnabled(activity)) {
            activity.startActivity(CarPlayerActivity.getIntent(activity).putExtra(EXTRA_FROM_BLUETOOTH, true));
        }
    }

    static void markDismissed(@NonNull Context context) {
        if (isConnected(context)) {
            getPrefs(context).edit().putBoolean(KEY_DISMISSED, true).apply();
        }
    }

    static void cancelNotification(@NonNull Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static Set<String> getDevices(@NonNull Context context) {
        return new HashSet<>(PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(PREF_DEVICES, Collections.emptySet()));
    }

    public static void setDevices(@NonNull Context context, @NonNull Set<String> addresses) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(PREF_DEVICES, addresses).apply();
        if (!addresses.contains(getPrefs(context).getString(KEY_CONNECTED, ""))) {
            getPrefs(context).edit().remove(KEY_CONNECTED).remove(KEY_DISMISSED).apply();
            cancelNotification(context);
        }
    }

    public static boolean hasPermission(@NonNull Context context) {
        return Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    @NonNull
    public static List<Device> getPairedDevices(@NonNull Context context) {
        List<Device> result = new ArrayList<>();
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
        if (adapter == null || !hasPermission(context)) {
            return result;
        }
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                result.add(new Device(device.getAddress(), nameOf(device)));
            }
        } catch (SecurityException e) {
            return result;
        }
        Collections.sort(result, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    @SuppressLint("MissingPermission")
    @NonNull
    private static String nameOf(@NonNull BluetoothDevice device) {
        String name = null;
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                name = device.getAlias();
            }
            if (name == null) {
                name = device.getName();
            }
        } catch (SecurityException e) {
            name = null;
        }
        return name != null ? name : device.getAddress();
    }

    @SuppressLint("MissingPermission")
    private static void showNotification(@NonNull Context context, @Nullable String deviceName) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return;
        }
        manager.createNotificationChannel(new NotificationChannelCompat.Builder(CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.car_player_title))
                .setDescription(context.getString(R.string.car_bluetooth_channel_description))
                .setSound(null, null)
                .build());
        Intent intent = CarPlayerActivity.getIntent(context).putExtra(EXTRA_FROM_BLUETOOTH, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.car_bluetooth_connected, deviceName))
                .setContentText(context.getString(R.string.car_bluetooth_open))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .build());
    }
}
