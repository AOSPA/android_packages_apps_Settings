/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.widget.SwitchBar;

public class ScreenStateAnimations extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

  private static final String TAG = "ScreenStateAnimations";

  private static final String SCREEN_OFF_ANIMATION = "screen_off_animation";

  private ListPreference mScreenOffAnimation;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.display_settings);

    ContentResolver resolver = getActivity().getContentResolver();

    mScreenOffAnimation = (ListPreference) findPreference(SCREEN_OFF_ANIMATION);
    int screenOffAnimation = Settings.System.getInt(resolver,
        Settings.System.SCREEN_OFF_ANIMATION, 0);
    mScreenOffAnimation.setValue(Integer.toString(screenOffAnimation));
    mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntry());
    mScreenOffAnimation.setOnPreferenceChangeListener(this);
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    ContentResolver resolver = getActivity().getContentResolver();
    if (preference == mScreenOffAnimation) {
      int value = Integer.valueOf((String) newValue);
      int index = mScreenOffAnimation.findIndexOfValue((String) newValue);
      mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntries()[index]);
      Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_ANIMATION, value);
      return true;
    }
    return false;
  }

  @Override
  public int getMetricsCategory() {
    return -1;
  }
}
