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
import com.android.internal.policy.IKeyguardService;

import java.io.File;
import java.io.IOException;

public class LockscreenSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private final static String TAG = "LockscreenSettings";

    private static final int REQUEST_CODE_BG_WALLPAPER = 1024;

    private static final String LOCKSCREEN_POWER_MENU = "lockscreen_power_menu";
    private static final String KEY_LOCKSCREEN_WALLPAPER = "lockscreen_wallpaper";
    private static final String KEY_SELECT_LOCKSCREEN_WALLPAPER = "select_lockscreen_wallpaper";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";

    private static final String KEY_SEE_THROUGH = "see_through";
    private static final String KEY_BLUR_RADIUS = "blur_radius";

    private CheckBoxPreference mLockScreenPowerMenu;
    private CheckBoxPreference mLockscreenWallpaper;
    private CheckBoxPreference mLockRingBattery;
    private CheckBoxPreference mSeeThrough;

    private SeekBarPreference mBlurRadius;
    private Preference mSelectLockscreenWallpaper;

    private File mWallpaperTemporary;

    private IKeyguardService mKeyguardService;

    private final ServiceConnection mKeyguardConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mKeyguardService = IKeyguardService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mKeyguardService = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_settings);

        Intent intent = new Intent();
        intent.setClassName("com.android.keyguard", "com.android.keyguard.KeyguardService");
        if (!mContext.bindServiceAsUser(intent, mKeyguardConnection,
                Context.BIND_AUTO_CREATE, UserHandle.OWNER)) {
            Log.e(TAG, "*** Keyguard: can't bind to keyguard");
        }

        PreferenceScreen prefs = getPreferenceScreen();

        mLockScreenPowerMenu = (CheckBoxPreference) prefs.findPreference(LOCKSCREEN_POWER_MENU);
        if (mLockScreenPowerMenu != null) {
            mLockScreenPowerMenu.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_POWER_MENU, 1) == 1);
        }

        mLockscreenWallpaper = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_WALLPAPER);
        mLockscreenWallpaper.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_WALLPAPER, 0) == 1);

        mSelectLockscreenWallpaper = findPreference(KEY_SELECT_LOCKSCREEN_WALLPAPER);
        mSelectLockscreenWallpaper.setEnabled(mLockscreenWallpaper.isChecked());
        mWallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");

        mLockRingBattery = (CheckBoxPreference) prefs.findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
        }

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

    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void onActivityResult(int requestCode, int resultCode,
            Intent imageReturnedIntent) {
        if (requestCode == REQUEST_CODE_BG_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                if (mWallpaperTemporary.length() == 0 || !mWallpaperTemporary.exists()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Bitmap bmp = BitmapFactory.decodeFile(mWallpaperTemporary.getAbsolutePath());
                try {
                    mKeyguardService.setWallpaper(bmp);
                    Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH, 0);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to set wallpaper: " + ex);
                }
            }
        }
        if (mWallpaperTemporary.exists()) mWallpaperTemporary.delete();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mBlurRadius) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)value);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLockScreenPowerMenu) {
            Settings.Secure.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_POWER_MENU, isToggled(preference) ? 1 : 0);
        } else if (preference == mLockRingBattery) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, isToggled(preference) ? 1 : 0);
        } else if (preference == mLockscreenWallpaper) {
            if (!mLockscreenWallpaper.isChecked()) setWallpaper(null);
            else Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_WALLPAPER, 1);
            mSelectLockscreenWallpaper.setEnabled(mLockscreenWallpaper.isChecked());
        } else if (preference == mSelectLockscreenWallpaper) {
            final Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();

            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            Point size = new Point();
            display.getSize(size);

            intent.putExtra("aspectX", isPortrait ? size.x : size.y);
            intent.putExtra("aspectY", isPortrait ? size.y : size.x);

            try {
                mWallpaperTemporary.createNewFile();
                mWallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mWallpaperTemporary));
                getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_BG_WALLPAPER);
            } catch (IOException e) {
                // Do nothing here
            } catch (ActivityNotFoundException e) {
                // Do nothing here
            }
        } else if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked() ? 1 : 0);
            if (mSeeThrough.isChecked())
                Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_WALLPAPER, 0);
            mBlurRadius.setEnabled(mSeeThrough.isChecked());
        }else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    private void setWallpaper(Bitmap bmp) {
        try {
            mKeyguardService.setWallpaper(bmp);
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to set wallpaper!");
        }
    }
}
