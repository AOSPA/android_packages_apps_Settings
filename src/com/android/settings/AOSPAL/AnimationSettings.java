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
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.File;
import java.io.IOException;

public class AnimationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";

    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.animation_settings);
        PreferenceScreen prefs = getPreferenceScreen();

        //ListView Animations
        mListViewAnimation = (ListPreference) prefs.findPreference(KEY_LISTVIEW_ANIMATION);
        if (mListViewAnimation != null) {
           int listViewAnimation = Settings.System.getInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION, 1);
           mListViewAnimation.setValue(String.valueOf(listViewAnimation));
           mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        }
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefs.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        if (mListViewInterpolator != null) {
           int listViewInterpolator = Settings.System.getInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR, 1);
           mListViewInterpolator.setValue(String.valueOf(listViewInterpolator));
           mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        }
        mListViewInterpolator.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();

        if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewAnimation.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setValue(String.valueOf(value));
            mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        } else if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewInterpolator.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setValue(String.valueOf(value));
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
            return false;
        }
        return true;
    }
}
