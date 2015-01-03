/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.SparseBooleanArray;
import android.util.Log;
import android.widget.ListView;

import java.util.HashMap;
import java.util.Map;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Gesture lock pattern settings.
 */
public class PrivacySettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener {

    static final String TAG = "PrivacySettings";

    // Vendor specific
    private static final String GSETTINGS_PROVIDER = "com.google.settings";
    private static final String BACKUP_CATEGORY = "backup_category";
    private static final String BACKUP_DATA = "backup_data";
    private static final String AUTO_RESTORE = "auto_restore";
    private static final String RESET_PREFERENCES = "user_preferences_reset";
    private static final String CONFIGURE_ACCOUNT = "configure_account";
    private static final String PERSONAL_DATA_CATEGORY = "personal_data_category";
    private IBackupManager mBackupManager;
    private SwitchPreference mBackup;
    private SwitchPreference mAutoRestore;
    private Preference mResetUserPreferences;
    private Dialog mConfirmDialog;
    private PreferenceScreen mConfigure;
    private boolean mEnabled;

    private HashMap<String, String> customPrefs = new HashMap<String, String>();

    private static final int DIALOG_ERASE_BACKUP = 2;
    private int mDialogType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Don't allow any access if this is a secondary user
        mEnabled = Process.myUserHandle().isOwner();
        if (!mEnabled) {
            return;
        }

        addPreferencesFromResource(R.xml.privacy_settings);
        final PreferenceScreen screen = getPreferenceScreen();
        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        mBackup = (SwitchPreference) screen.findPreference(BACKUP_DATA);
        mBackup.setOnPreferenceChangeListener(preferenceChangeListener);

        mAutoRestore = (SwitchPreference) screen.findPreference(AUTO_RESTORE);
        mAutoRestore.setOnPreferenceChangeListener(preferenceChangeListener);

        mConfigure = (PreferenceScreen) screen.findPreference(CONFIGURE_ACCOUNT);

        mResetUserPreferences = screen.findPreference(RESET_PREFERENCES);
        mResetUserPreferences.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                updateActiveCustomPreferences();
                if (customPrefs.size() > 1) {
                    showResetList();
                } else {
                    showResetDialog();
                }
                return true;
            }
        });

        if (UserManager.get(getActivity()).hasUserRestriction(
                UserManager.DISALLOW_FACTORY_RESET)) {
            screen.removePreference(findPreference(PERSONAL_DATA_CATEGORY));
        }

        // Vendor specific
        if (getActivity().getPackageManager().
                resolveContentProvider(GSETTINGS_PROVIDER, 0) == null) {
            screen.removePreference(findPreference(BACKUP_CATEGORY));
        }
        updateToggles();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh UI
        if (mEnabled) {
            updateToggles();
        }
    }

    @Override
    public void onStop() {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        mConfirmDialog = null;
        mDialogType = 0;
        super.onStop();
    }

    private OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean nextValue = (Boolean) newValue;
            boolean result = false;
            if (preference == mBackup) {
                if (nextValue == false) {
                    // Don't change Switch status until user makes choice in dialog
                    // so return false here.
                    showEraseBackupDialog();
                } else {
                    setBackupEnabled(true);
                    result = true;
                }
            } else if (preference == mAutoRestore) {
                try {
                    mBackupManager.setAutoRestore(nextValue);
                    result = true;
                } catch (RemoteException e) {
                    mAutoRestore.setChecked(!nextValue);
                }
            }
            return result;
        }
    };

    private void showEraseBackupDialog() {
        mDialogType = DIALOG_ERASE_BACKUP;
        CharSequence msg = getResources().getText(R.string.backup_erase_dialog_message);
        // TODO: DialogFragment?
        mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(msg)
                .setTitle(R.string.backup_erase_dialog_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .show();
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();

        boolean backupEnabled = false;
        Intent configIntent = null;
        String configSummary = null;
        try {
            backupEnabled = mBackupManager.isBackupEnabled();
            String transport = mBackupManager.getCurrentTransport();
            configIntent = mBackupManager.getConfigurationIntent(transport);
            configSummary = mBackupManager.getDestinationString(transport);
        } catch (RemoteException e) {
            // leave it 'false' and disable the UI; there's no backup manager
            mBackup.setEnabled(false);
        }
        mBackup.setChecked(backupEnabled);

        mAutoRestore.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.BACKUP_AUTO_RESTORE, 1) == 1);
        mAutoRestore.setEnabled(backupEnabled);

        final boolean configureEnabled = (configIntent != null) && backupEnabled;
        mConfigure.setEnabled(configureEnabled);
        mConfigure.setIntent(configIntent);
        setConfigureSummary(configSummary);
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.reset_user_preferences_title);
        builder.setMessage(R.string.reset_user_preferences_dialog);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for(String setting : Settings.System.SETTINGS_TO_RESET) {
                    Settings.System.putInt(getContentResolver(), setting, 0);
                }
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showResetList() {
        final CharSequence[] cs =
                    customPrefs.values().toArray(new CharSequence[customPrefs.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.reset_user_preferences_title)
               .setMultiChoiceItems(cs, null, null)
               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog ad = ((AlertDialog) dialog);
                        ListView resetList = ad.getListView();
                        int resetCount = resetList.getCheckedItemCount();
                        if (resetCount > 0) {
                            SparseBooleanArray checked = resetList.getCheckedItemPositions();
                            for (int i = 0; i < resetList.getCount(); i++) {
                                if (checked.get(i)) {
                                    String value = resetList.getItemAtPosition(i).toString();
                                    Settings.System.putInt(getContentResolver(), getKey(value), 0);
                                }
                            }
                        }
                        dialog.dismiss();
                    }
                })
               .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void updateActiveCustomPreferences() {
        customPrefs.clear();
        try {
            Context con = getActivity().getApplicationContext()
                                       .createPackageContext("com.android.systemui", 0);
            Resources r = con.getResources();
            for(String setting : Settings.System.SETTINGS_TO_RESET) {
                if (!(Settings.System.getInt(getContentResolver(), setting, 0) == 0)) {
                    String key = setting.toLowerCase();
                    int resId = r.getIdentifier(setting, "string", "com.android.systemui");
                    if (resId != 0) {
                        try {
                            String value = (String) r.getText(resId);
                            customPrefs.put(key, value);
                        } catch (NotFoundException e) {
                            Log.e(TAG, "Resource not found for: " + setting, e);
                        }
                    } else {
                        Log.v(TAG, "Missing string for: " + setting);
                    }
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException", e);
        }
    }

    private String getKey(String value) {
        String key = null;
        for(Map.Entry<String, String> entry : customPrefs.entrySet()) {
            if((value == null && entry.getValue() == null) ||
                    (value != null && value.equals(entry.getValue()))) {
                return entry.getKey();
            }
        }
        return key;
    }

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            mConfigure.setSummary(summary);
        } else {
            mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    private void updateConfigureSummary() {
        try {
            String transport = mBackupManager.getCurrentTransport();
            String summary = mBackupManager.getDestinationString(transport);
            setConfigureSummary(summary);
        } catch (RemoteException e) {
            // Not much we can do here
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Dialog is triggered before Switch status change, that means marking the Switch to
        // true in showEraseBackupDialog() method will be override by following status change.
        // So we do manual switching here due to users' response.
        if (mDialogType == DIALOG_ERASE_BACKUP) {
            // Accept turning off backup
            if (which == DialogInterface.BUTTON_POSITIVE) {
                setBackupEnabled(false);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                // Reject turning off backup
                setBackupEnabled(true);
            }
            updateConfigureSummary();
        }
        mDialogType = 0;
    }

    /**
     * Informs the BackupManager of a change in backup state - if backup is disabled,
     * the data on the server will be erased.
     * @param enable whether to enable backup
     */
    private void setBackupEnabled(boolean enable) {
        if (mBackupManager != null) {
            try {
                mBackupManager.setBackupEnabled(enable);
            } catch (RemoteException e) {
                mBackup.setChecked(!enable);
                mAutoRestore.setEnabled(!enable);
                return;
            }
        }
        mBackup.setChecked(enable);
        mAutoRestore.setEnabled(enable);
        mConfigure.setEnabled(enable);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new PrivacySearchIndexProvider();

    private static class PrivacySearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public PrivacySearchIndexProvider() {
            super();

            mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            // For non-primary user, no backup or reset is available
            if (!mIsPrimary) {
                return result;
            }

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.privacy_settings;
            result.add(sir);

            return result;
        }
    }

}