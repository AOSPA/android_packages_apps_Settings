/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.actionbar;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;

/**
 * A controller that adds disability support menu to any Settings page.
 */
public class DisabilitySupportMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnOptionsItemSelected {

    @NonNull
    private final InstrumentedPreferenceFragment mHost;

    @NonNull
    private final String mDisabilitySupportUrl;

    /**
     * Initializes the DisabilitySupportMenuController for an InstrumentedPreferenceFragment.
     *
     * @param host The InstrumentedPreferenceFragment to which the menu controller will be added.
     */
    public static void init(@NonNull InstrumentedPreferenceFragment host,
            @NonNull String disabilitySupportUrl) {
        host.getSettingsLifecycle().addObserver(
                new DisabilitySupportMenuController(host, disabilitySupportUrl));
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (TextUtils.isEmpty(mDisabilitySupportUrl)) {
            return;
        }

        final MenuItem item = menu.add(Menu.NONE, MenusUtils.MenuId.DISABILITY_SUPPORT.getValue(),
                Menu.NONE, R.string.accessibility_disability_support_title);
        item.setIcon(com.android.settingslib.widget.help.R.drawable.ic_help_actionbar);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == MenusUtils.MenuId.DISABILITY_SUPPORT.getValue()) {
            final FragmentActivity activity = mHost.getActivity();
            if (activity != null) {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                browserIntent.setData(Uri.parse(mDisabilitySupportUrl));
                activity.startActivity(browserIntent);
            }
            return true;
        }
        return false;
    }

    private DisabilitySupportMenuController(@NonNull InstrumentedPreferenceFragment host,
            @NonNull String disabilitySupportUrl) {
        mHost = host;
        mDisabilitySupportUrl = disabilitySupportUrl;
    }
}
