package de.danoeh.antennapod.ui.screen.car;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.IntentCompat;

public class CarBluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        CarBluetooth.onDeviceEvent(context, intent.getAction(),
                IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class));
    }
}
