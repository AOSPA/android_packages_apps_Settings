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

package com.android.settings.notification;

import static android.provider.Settings.Secure.NOTIFICATION_BUNDLES_ALWAYS_EXPAND;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.List;
import java.util.Set;

/**
 * Preference controller governing both the global and individual type-based bundle preferences.
 */
public class BundleCombinedPreferenceController extends BasePreferenceController {

    static final String GLOBAL_KEY = "global_pref";
    static final String WORK_PREF_KEY = "work_profile_pref";
    static final String TYPE_CATEGORY_KEY = "enabled_classification_types";
    static final String EXCLUDED_APPS_CATEGORY_KEY = "notification_bundle_excluded_apps_list";
    static final String ALWAYS_EXPAND_KEY = "always_expand_pref";
    static final String AUTO_EXPAND_KEY = "auto_expand_pref";
    static final String NEVER_EXPAND_KEY = "never_expand_pref";
    static final int AUTO_EXPAND_VALUE = 0;
    static final int NEVER_EXPAND_VALUE = -1;
    static final int ALWAYS_EXPAND_VALUE = 1;

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    @NonNull NotificationBackend mBackend;
    private @Nullable UserHandle mManagedProfile;

    private @Nullable TwoStatePreference mGlobalPref;
    private @Nullable TwoStatePreference mWorkPref;
    private @Nullable SelectorWithWidgetPreference mExpandAlwaysPref;
    private @Nullable SelectorWithWidgetPreference mExpandAutoPref;
    private @Nullable SelectorWithWidgetPreference mExpandNeverPref;
    private @Nullable PreferenceCategory mTypesPrefCategory;
    private @Nullable PreferenceCategory mExcludedAppsPrefCategory;

    public BundleCombinedPreferenceController(@NonNull Context context, @NonNull String prefKey,
            @NonNull NotificationBackend backend) {
        super(context, prefKey);
        mBackend = backend;

        // will be null if no profile is present or enabled
        mManagedProfile = Utils.getManagedProfile(UserManager.get(mContext));
    }

    private boolean hasManagedProfile() {
        return mManagedProfile != null;
    }

    private int managedProfileId() {
        return mManagedProfile != null ? mManagedProfile.getIdentifier() : UserHandle.USER_NULL;
    }

    @VisibleForTesting
    void setManagedProfile(UserHandle profile) {
        mManagedProfile = profile;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if (mBackend.isNotificationBundlingSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;

        // Find and cache relevant preferences for later updates, then set values
        mGlobalPref = category.findPreference(GLOBAL_KEY);
        if (mGlobalPref != null) {
            mGlobalPref.setOnPreferenceChangeListener(mGlobalPrefListener);
        }

        mWorkPref = category.findPreference(WORK_PREF_KEY);
        if (mWorkPref != null) {
            mWorkPref.setVisible(hasManagedProfile());
            mWorkPref.setOnPreferenceChangeListener(mWorkPrefListener);
        }

        mExpandAlwaysPref = category.findPreference(ALWAYS_EXPAND_KEY);
        if (mExpandAlwaysPref != null) {
            mExpandAlwaysPref.setOnClickListener(mExpansionListener);
        }
        mExpandAutoPref = category.findPreference(AUTO_EXPAND_KEY);
        if (mExpandAutoPref != null) {
            mExpandAutoPref.setOnClickListener(mExpansionListener);
        }
        mExpandNeverPref = category.findPreference(NEVER_EXPAND_KEY);
        if (mExpandNeverPref != null) {
            mExpandNeverPref.setOnClickListener(mExpansionListener);
        }

        mTypesPrefCategory = category.findPreference(TYPE_CATEGORY_KEY);
        if (mTypesPrefCategory != null) {
            List<NotificationBackend.ClassificationType> classificationTypes =
                    mBackend.getClassificationTypes(mContext);
            for (NotificationBackend.ClassificationType type : classificationTypes) {
                String prefKey = String.valueOf(type.typeId);
                TwoStatePreference typePref =
                        mTypesPrefCategory.findPreference(prefKey);
                if (typePref == null) {
                    typePref = new CheckBoxPreference(mContext);
                    typePref.setKey(prefKey);
                    typePref.setTitle(type.typeName);
                    typePref.setSummary(type.typeDesc);
                    mTypesPrefCategory.addPreference(typePref);
                }
                typePref.setOnPreferenceChangeListener(getListenerForType());
                typePref.setChecked(type.enabled);
            }
        }

        mExcludedAppsPrefCategory = category.findPreference(EXCLUDED_APPS_CATEGORY_KEY);

        updatePrefValues();
    }

    void updatePrefValues() {
        boolean isBundlingEnabled = mBackend.isNotificationBundlingEnabled(mContext.getUserId());
        Set<Integer> allowedTypes = mBackend.getAllowedBundleTypes();

        // State check: if bundling is globally enabled, but there are no allowed bundle types,
        // disable the global bundling state from here before proceeding.
        if (isBundlingEnabled && allowedTypes.size() == 0) {
            mBackend.setNotificationBundlingEnabled(mContext.getUserId(), false);
            isBundlingEnabled = false;
        }

        if (mGlobalPref != null) {
            mGlobalPref.setChecked(isBundlingEnabled);
        }

        if (mWorkPref != null && hasManagedProfile()) {
            // profile preference should only be active if the global switch is on
            mWorkPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mWorkPref.setChecked(mBackend.isNotificationBundlingEnabled(managedProfileId()));
            }
        }

        // if global switch is off hide the whole category
        if (mTypesPrefCategory != null) {
            mTypesPrefCategory.setVisible(isBundlingEnabled);
        }

        // if global switch is off hide the whole category
        if (mExcludedAppsPrefCategory != null) {
            mExcludedAppsPrefCategory.setVisible(isBundlingEnabled);
        }

        if (mExpandNeverPref != null) {
            mExpandNeverPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mExpandNeverPref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                        NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE)
                        == NEVER_EXPAND_VALUE);
            }
        }
        if (mExpandAutoPref != null) {
            mExpandAutoPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mExpandAutoPref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                        NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE)
                        == AUTO_EXPAND_VALUE);
            }
        }
        if (mExpandAlwaysPref != null) {
            mExpandAlwaysPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mExpandAlwaysPref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                        NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE)
                        == ALWAYS_EXPAND_VALUE);
            }
        }
    }

    private Preference.OnPreferenceChangeListener mGlobalPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        mBackend.setNotificationBundlingEnabled(mContext.getUserId(), checked);
        // update state to hide or show preferences for individual types
        updatePrefValues();
        return true;
    };

    private Preference.OnPreferenceChangeListener mWorkPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        if (hasManagedProfile()) {
            mBackend.setNotificationBundlingEnabled(managedProfileId(), checked);
        }
        return true;
    };

    private SelectorWithWidgetPreference.OnClickListener mExpansionListener =
            new SelectorWithWidgetPreference.OnClickListener() {
                @Override
                public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
                    if (ALWAYS_EXPAND_KEY.equals(preference.getKey())) {
                        mExpandAutoPref.setChecked(false);
                        mExpandNeverPref.setChecked(false);
                        mExpandAlwaysPref.setChecked(true);

                        Settings.Secure.putInt(mContext.getContentResolver(),
                                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                                ALWAYS_EXPAND_VALUE);
                    } else if (AUTO_EXPAND_KEY.equals(preference.getKey())) {
                        mExpandAlwaysPref.setChecked(false);
                        mExpandNeverPref.setChecked(false);
                        mExpandAutoPref.setChecked(true);

                        Settings.Secure.putInt(mContext.getContentResolver(),
                                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                                AUTO_EXPAND_VALUE);
                    } else if (NEVER_EXPAND_KEY.equals(preference.getKey())) {
                        mExpandAlwaysPref.setChecked(false);
                        mExpandAutoPref.setChecked(false);
                        mExpandNeverPref.setChecked(true);
                        Settings.Secure.putInt(mContext.getContentResolver(),
                                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                                NEVER_EXPAND_VALUE);
                    }
                }
            };

    // Returns a preference listener for a type pref that:
    //   * sets the backend state for whether that type is enabled
    //   * if it is disabled, trigger a new update sync global switch if needed
    private Preference.OnPreferenceChangeListener getListenerForType() {
        return (p, val) -> {
            boolean checked = (boolean) val;
            mBackend.setBundleTypeState(Integer.parseInt(p.getKey()), checked);
            if (!checked) {
                // goes from checked to un-checked; update state in case this was the last enabled
                // individual category
                updatePrefValues();
            }
            return true;
        };
    }
}
