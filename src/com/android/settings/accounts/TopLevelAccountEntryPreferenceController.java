/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.accounts;

import android.content.Context;
import android.credentials.CredentialManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class TopLevelAccountEntryPreferenceController extends BasePreferenceController {
    public TopLevelAccountEntryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setTitle(AccountScreen.getAccountScreenTitle(mContext));
        }
    }

    static void maybeAddTopLevelAccountEntryPreferenceController(Context context,
            List<AbstractPreferenceController> controllers) {
        if (Flags.enableAccountsAndBackupScreen() && CredentialManager.isServiceEnabled(context)) {
            controllers.add(new TopLevelAccountEntryPreferenceController(context,
                    "user_and_account_settings_screen"));
        }
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.account_dashboard_default_summary);
    }
}
