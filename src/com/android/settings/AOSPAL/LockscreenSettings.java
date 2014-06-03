package com.android.settings.AOSPAL;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.policy.IKeyguardService;

import java.io.File;
import java.io.IOException;

public class LockscreenSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private final static String TAG = "LockscreenSettings";

    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String PERSONALIZE_CATEGORY = "personalize_category";

    //private static final String KEY_SEE_THROUGH = "see_through";
    //private static final String KEY_BLUR_RADIUS = "blur_radius";

    private CheckBoxPreference mLockRingBattery;
    //private CheckBoxPreference mSeeThrough;

    //private SeekBarPreference mBlurRadius;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mLockRingBattery = (CheckBoxPreference) prefs.findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
        }

        /*
        // lockscreen see through
        mSeeThrough = (CheckBoxPreference) prefs.findPreference(KEY_SEE_THROUGH);
        if (mSeeThrough != null) {
            mSeeThrough.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
        }
        mBlurRadius = (SeekBarPreference) prefs.findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);
        mBlurRadius.setEnabled(mSeeThrough.isChecked() && mSeeThrough.isEnabled());
        */

        PreferenceCategory personalizeCategory = (PreferenceCategory) findPreference(PERSONALIZE_CATEGORY);

        // Only add if device has LockClock installed
        if (!Utils.isPackageInstalled(getActivity(), "com.cyanogenmod.lockclock")) {
            personalizeCategory.removePreference(findPreference(KEY_LOCK_CLOCK));
        }
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        /* if (preference == mBlurRadius) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)value);
        } */
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLockRingBattery) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, isToggled(preference) ? 1 : 0);
        /* } else if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked() ? 1 : 0);
            mBlurRadius.setEnabled(mSeeThrough.isChecked()); */
        }else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
