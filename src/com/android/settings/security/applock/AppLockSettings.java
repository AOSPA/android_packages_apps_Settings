/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.settings.security.applock;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import androidx.annotation.WorkerThread;

import android.app.Activity;
import android.app.AppLockManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.SearchView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.widget.AppLockPreference;

import com.android.settingslib.HelpUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * App lock settings.
 */
public class AppLockSettings extends SubSettings {

    private static final String TAG = "AppLockSettings";
    
    private static final int RESULT_FINISHED = BiometricEnrollBase.RESULT_FINISHED;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, AppLockSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AppLockSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.app_lock_title);
        setTitle(msg);
    }

    public static class AppLockSettingsFragment extends SettingsPreferenceFragment
            implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

        static final String KEY_LOCKED_APPS = "locked_apps";
        static final String KEY_UNLOCKED_APPS = "unlocked_apps";
        
        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private AppLockManager mAppLockManager;
        private PackageManager mPackageManager;

        private boolean mLaunchedConfirm;
        private int mUserId;
        private SearchView mSearchView;
        private SearchFilter mSearchFilter;
        private Menu mOptionsMenu;
        private PreferenceScreen mPreferenceScreen;

        private RecyclerView.Adapter mAdapter;
        private RecyclerView mRecyclerView;

        private final TreeMap<String, PackageInfo> mLabelToInfoMap = new TreeMap<>();

        @Override
        public int getMetricsCategory() {
            return -1;
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
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate start");
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            mUserId = getActivity().getIntent().getIntExtra(
                    Intent.EXTRA_USER_ID, UserHandle.myUserId());

            launchChooseOrConfirmLock();

            addPreferencesFromResource(R.xml.security_settings_applock);

            mAppLockManager = (AppLockManager) getContext().getSystemService(Context.APPLOCK_SERVICE);
            mPackageManager = getPrefContext().getPackageManager();
            
            mPreferenceScreen = getPreferenceScreen();

            List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);
            for (PackageInfo p : packages) {
                if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    mLabelToInfoMap.put(p.applicationInfo.loadLabel(mPackageManager).toString(), p);
                }
            }
            Log.d(TAG, "onCreate end");
        }

        private void handlePreferenceChange(Preference preference, boolean o) {
            if (o) {
                mAppLockManager.addAppToList(preference.getKey());
            } else {
                mAppLockManager.removeAppFromList(preference.getKey());
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.app_lock_title),
                    null, null, mUserId, true /* foregroundOnly */)) {
                intent.setClassName(SETTINGS_PACKAGE_NAME, ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }

        private void addPreferences() {
            for (PackageInfo p : mLabelToInfoMap.values()) {
                String label = p.applicationInfo.loadLabel(mPackageManager).toString();
                Drawable icon = p.applicationInfo.loadIcon(mPackageManager);
                String packageName = p.applicationInfo.packageName;
                boolean locked = mAppLockManager.isAppLocked(packageName);
                AppLockPreference pref = new AppLockPreference(getPrefContext());
                pref.setTitle(label);
                pref.setIcon(icon);
                pref.setKey(packageName);
                pref.setChecked(locked);
                mPreferenceScreen.addPreference(pref);
                pref.setOnPreferenceChangeListener((preference, o) -> {
                    Log.d(TAG, "app:" + pref.getKey() + " pos:" + pref.getOrder() + " totalScreen:" + mPreferenceScreen.getPreferenceCount());
                    handlePreferenceChange(preference, (boolean) o);
                    return true;
                });
            }
        }

        private void updatePreferences(ArrayList<PackageInfo> results) {
            ArrayList<PackageInfo> negPackages = new ArrayList<>(mLabelToInfoMap.values());
            negPackages.removeAll(results);
            for (PackageInfo p : negPackages) {
                String packageName = p.applicationInfo.packageName;
                AppLockPreference pref = mPreferenceScreen.findPreference(packageName);
                pref.setVisible(false);
            }
            for (PackageInfo p : results) {
                String packageName = p.applicationInfo.packageName;
                AppLockPreference pref = mPreferenceScreen.findPreference(packageName);
                pref.setVisible(true);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set/entered.
                    addPreferences();
                } else {
                    getActivity().finish();
                }
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            mOptionsMenu = menu;
            inflater.inflate(R.menu.applock_menu, menu);

            final MenuItem searchMenuItem = menu.findItem(R.id.search_app_list_menu);
            final MenuItem chkboxMenuItem = menu.findItem(R.id.show_only_on_wake);
            if (searchMenuItem != null) {
                mSearchView = (SearchView) searchMenuItem.getActionView();
                mSearchView.setQueryHint(getText(R.string.search_settings));
                mSearchView.setOnQueryTextListener(this);
                mSearchView.setOnCloseListener(this);
            }

            boolean showOnlyWake = Settings.System.getIntForUser(getContext().getContentResolver(), 
                    Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE, 0, UserHandle.USER_CURRENT) != 0;
            chkboxMenuItem.setChecked(showOnlyWake);
        }

        @Override
        public void onDestroyOptionsMenu() {
            mOptionsMenu = null;
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            mOptionsMenu.findItem(R.id.show_only_on_wake).setVisible(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.show_only_on_wake) {
                Log.d(TAG, "onOptionsItemSelected");
                boolean showOnlyWake = item.isChecked();
                Settings.System.putIntForUser(getContext().getContentResolver(), 
                        Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE,
                        showOnlyWake ? 0 : 1, UserHandle.USER_CURRENT);
                item.setChecked(!showOnlyWake);
            }
            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            Log.d(TAG, "onQueryTextChange");
            if (mSearchFilter == null) {
                mSearchFilter = new SearchFilter();
            }
            // If we haven't load apps list completely, don't filter anything.
            if(mLabelToInfoMap == null) {
                Log.w(TAG, "Apps haven't loaded completely yet, so nothing can be filtered");
                return false;
            }
            mSearchFilter.filter(newText);
            return false;
        }

        @Override
        public boolean onClose() {
            updatePreferences(new ArrayList<>(mLabelToInfoMap.values()));
            return false;
        }

        /**
         * An array filter that constrains the content of the array adapter with a substring.
         * Item that does not contains the specified substring will be removed from the list.</p>
         */
        private class SearchFilter extends Filter {
            @WorkerThread
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                final ArrayList<PackageInfo> matchedEntries = new ArrayList<>();
                Collection<PackageInfo> packages = mLabelToInfoMap.values();
                if (!TextUtils.isEmpty(query)) {
                    for (PackageInfo p : packages) {
                        if (p.applicationInfo.loadLabel(mPackageManager).toString()
                                .toLowerCase().contains(query.toString().toLowerCase())) {
                            matchedEntries.add(p);
                        }
                    }
                } else {
                    matchedEntries.addAll(packages);
                }
                final FilterResults results = new FilterResults();
                results.values = matchedEntries;
                results.count = matchedEntries.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                updatePreferences((ArrayList<PackageInfo>) results.values);
            }
        }
    }
}
