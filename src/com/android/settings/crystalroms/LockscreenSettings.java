package com.android.settings.crystalroms;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.os.Handler;
import android.util.Log;
import android.net.Uri;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.IWindowManager;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.IPowerManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import android.provider.Settings;
import android.os.SystemProperties;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LockscreenSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String LOCKSCREEN_ROTATION = "lockscreen_rotation";

    private CheckBoxPreference mLockScreenRotationPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.crystal_lockscreen_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        boolean configEnableLockRotation = getResources().getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation);
        Boolean lockScreenRotationEnabled = Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_ROTATION, configEnableLockRotation ? 1 : 0) != 0;
        mLockScreenRotationPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_ROTATION);
        mLockScreenRotationPref.setChecked(lockScreenRotationEnabled);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLockScreenRotationPref) {
            value = mLockScreenRotationPref.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.LOCKSCREEN_ROTATION, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return true;
    }
}
