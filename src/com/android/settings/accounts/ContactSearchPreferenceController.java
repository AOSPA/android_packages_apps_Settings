/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.accounts;

import static android.provider.Settings.Secure.MANAGED_PROFILE_CONTACT_REMOTE_SEARCH;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.slices.SliceData;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ContactSearchPreferenceController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener, DefaultLifecycleObserver,
        ManagedProfileQuietModeEnabler.QuietModeChangeListener {

    private final ManagedProfileQuietModeEnabler mQuietModeEnabler;
    private final UserHandle mManagedUser;
    private Preference mPreference;

    public ContactSearchPreferenceController(Context context, String key) {
        super(context, key);
        mManagedUser = Utils.getManagedProfile(context.getSystemService(UserManager.class));
        mQuietModeEnabler = new ManagedProfileQuietModeEnabler(context, this);
    }

    @Override
    public int getAvailabilityStatus() {
        return mQuietModeEnabler.isAvailable() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof RestrictedSwitchPreference) {
            final RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setChecked(isChecked());
            pref.setEnabled(!mQuietModeEnabler.isQuietModeEnabled());
            if (mManagedUser != null) {
                if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
                    DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
                    if (dpm == null) {
                        return;
                    }
                    // This preference is controlled by two admin policies so we need to combine
                    // both.
                    List<EnforcingAdmin> contactAccessAdmins = dpm.getEnforcingAdminsForPolicy(
                            DevicePolicyIdentifiers.MANAGED_PROFILE_CONTACTS_ACCESS_POLICY,
                            mManagedUser.getIdentifier()).getAllAdmins();
                    List<EnforcingAdmin> callerIdAccessAdmins = dpm.getEnforcingAdminsForPolicy(
                            DevicePolicyIdentifiers.MANAGED_PROFILE_CALLER_ID_ACCESS_POLICY,
                            mManagedUser.getIdentifier()).getAllAdmins();
                    // Combined list may contain duplicate elements, but it's not important here.
                    List<EnforcingAdmin> combinedAdmins = new ArrayList<>(contactAccessAdmins);
                    combinedAdmins.addAll(callerIdAccessAdmins);
                    PolicyEnforcementInfo combinedPolicyEnforcement = new PolicyEnforcementInfo(
                            combinedAdmins);
                    if (!combinedAdmins.isEmpty()
                            && !combinedPolicyEnforcement.isOnlyEnforcedBySystem()) {
                        pref.setDisabledByAdmin(
                                combinedPolicyEnforcement.getMostImportantEnforcingAdmin());
                    }
                } else {
                    // TODO(b/414733570): Remove RestrictedLockUtilsInternal
                    //  .checkIfRemoteContactSearchDisallowed method as part of clean-up as the
                    //  only call-site is here.
                    final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                            RestrictedLockUtilsInternal.checkIfRemoteContactSearchDisallowed(
                                    mContext, mManagedUser.getIdentifier());
                    pref.setDisabledByAdmin(enforcedAdmin);
                }
            }
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateState(mPreference);
    }

    @Override
    public void onStart(@NotNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(mQuietModeEnabler);
    }

    @Override
    public void onStop(@NotNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().removeObserver(mQuietModeEnabler);
    }

    @Override
    public boolean isChecked() {
        if (mManagedUser == null || mQuietModeEnabler.isQuietModeEnabled()) {
            return false;
        }
        return 0 != Settings.Secure.getIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, mManagedUser.getIdentifier());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mManagedUser == null || mQuietModeEnabler.isQuietModeEnabled()) {
            return false;
        }
        final int value = isChecked ? 1 : 0;
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, value, mManagedUser.getIdentifier());
        return true;
    }

    @Override
    public void onQuietModeChanged() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    @SliceData.SliceType
    public int getSliceType() {
        return SliceData.SliceType.SWITCH;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return Flags.enableAccountsAndBackupScreen() ? R.string.menu_key_accounts_and_backup
            : R.string.menu_key_accounts;
    }
}
