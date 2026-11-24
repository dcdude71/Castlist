package de.danoeh.antennapod.ui.screen.car;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class CarPlayerSettings {
    private static final String TAG = "CarPlayerSettings";
    private static final String PREF_PINNED = "prefCarPinned";
    private static final String PREF_SHORTCUT = "prefCarShortcut";
    private static final String PREF_BLUETOOTH = "prefCarBluetooth";

    private CarPlayerSettings() {
    }

    public static void setup(@NonNull PreferenceFragmentCompat fragment) {
        ListPreference screenOn = fragment.findPreference(CarPlayerPreferences.PREF_SCREEN_ON);
        if (screenOn != null && screenOn.getValue() == null) {
            screenOn.setValue(CarPlayerPreferences.getScreenOn(fragment.requireContext()));
        }
        Preference pinned = fragment.findPreference(PREF_PINNED);
        if (pinned != null) {
            pinned.setOnPreferenceClickListener(preference -> {
                fragment.startActivity(new Intent(fragment.requireContext(), CarPinnedActivity.class));
                return true;
            });
        }
        Preference bluetooth = fragment.findPreference(PREF_BLUETOOTH);
        if (bluetooth != null) {
            ActivityResultLauncher<String> permission = fragment.registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (granted) {
                            showDevicePicker(fragment, bluetooth);
                        } else {
                            Toast.makeText(fragment.requireContext(), R.string.car_bluetooth_permission_needed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
            bluetooth.setOnPreferenceClickListener(preference -> {
                if (CarBluetooth.hasPermission(fragment.requireContext())) {
                    showDevicePicker(fragment, bluetooth);
                } else {
                    permission.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }
                return true;
            });
        }
        Preference shortcut = fragment.findPreference(PREF_SHORTCUT);
        if (shortcut != null) {
            shortcut.setOnPreferenceClickListener(preference -> {
                if (!CarPlayerActivity.pinShortcut(fragment.requireContext())) {
                    Toast.makeText(fragment.requireContext(), R.string.playlist_shortcut_unsupported,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    private static void showDevicePicker(@NonNull PreferenceFragmentCompat fragment, @NonNull Preference preference) {
        Context context = fragment.requireContext();
        List<CarBluetooth.Device> devices = CarBluetooth.getPairedDevices(context);
        if (devices.isEmpty()) {
            new MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.car_bluetooth_none_paired)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        Set<String> chosen = CarBluetooth.getDevices(context);
        String[] names = new String[devices.size()];
        boolean[] checked = new boolean[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = devices.get(i).name;
            checked[i] = chosen.contains(devices.get(i).address);
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.car_bluetooth_picker_title)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Set<String> addresses = new HashSet<>();
                    for (int i = 0; i < devices.size(); i++) {
                        if (checked[i]) {
                            addresses.add(devices.get(i).address);
                        }
                    }
                    CarBluetooth.setDevices(context, addresses);
                    updateBluetoothSummary(context, preference);
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private static void updateBluetoothSummary(@NonNull Context context, @NonNull Preference preference) {
        Set<String> chosen = CarBluetooth.getDevices(context);
        List<String> names = new ArrayList<>();
        for (CarBluetooth.Device device : CarBluetooth.getPairedDevices(context)) {
            if (chosen.contains(device.address)) {
                names.add(device.name);
            }
        }
        preference.setSummary(chosen.isEmpty() ? context.getString(R.string.car_bluetooth_off)
                : names.isEmpty() ? context.getResources().getQuantityString(R.plurals.car_num_devices,
                chosen.size(), chosen.size()) : TextUtils.join(", ", names));
    }

    public static void updateSummary(@NonNull PreferenceFragmentCompat fragment) {
        Preference bluetooth = fragment.findPreference(PREF_BLUETOOTH);
        if (bluetooth != null) {
            updateBluetoothSummary(fragment.requireContext(), bluetooth);
        }
        Preference pinned = fragment.findPreference(PREF_PINNED);
        if (pinned == null) {
            return;
        }
        Context context = fragment.requireContext().getApplicationContext();
        Single.fromCallable(() -> {
            CarPins.ensureInitialized(context);
            int playlists = CarPins.getPlaylists(context).size();
            int podcasts = CarPins.getPodcasts(context).size();
            return context.getString(R.string.car_source,
                    context.getResources().getQuantityString(R.plurals.num_playlists, playlists, playlists),
                    context.getResources().getQuantityString(R.plurals.car_num_podcasts, podcasts, podcasts));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pinned::setSummary, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
