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

package com.android.settings.biometrics.face;

import static android.app.Activity.RESULT_OK;

import static com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;

import static android.provider.Settings.Secure.FACE_UNLOCK_KEYGUARD_ENABLED;
import static android.provider.Settings.Secure.FACE_UNLOCK_APP_ENABLED;
import static android.provider.Settings.Secure.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION;
import static android.provider.Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings screen for face authentication.
 */
@SearchIndexable
public class ParanoidFaceSettings extends DashboardFragment implements 
    Preference.OnPreferenceChangeListener, View.OnClickListener {

    private static final String TAG = "ParanoidFaceSettings";
    private static final String KEY_TOKEN = "hw_auth_token";

    private static final String KEY_ENROLL_FACE = "security_settings_face_sense_enroll";
    private static final String KEY_REMOVE_FACE = "security_settings_face_sense_delete";

    private static final String KEY_FACE_APP = "security_settings_face_sense_app";
    private static final String KEY_FACE_UNLOCK_BYPASS = "security_settings_face_sense_bypass";
    private static final String KEY_FACE_UNLOCK_CONFIRM = "security_settings_face_sense_confirm";
    private static final String KEY_FACE_UNLOCK_KEYGUARD = "security_settings_face_sense_keyguard";

    private UserManager mUserManager;
    private FaceManager mFaceManager;
    private int mUserId;
    private byte[] mToken;
    private FaceSettingsLockscreenBypassPreferenceController mLockscreenController;

    private Button mEnrollButton;
    private Button mRemoveButton;

    private List<Preference> mTogglePreferences;
    private Preference mRemoveButtonPref;
    private Preference mEnrollButtonPref;

    private TwoStatePreference mKeyguardPref;
    private TwoStatePreference mAppPref;
    private TwoStatePreference mBypassPref;
    private TwoStatePreference mConfirmPref;

    private ParanoidFaceSenseConnector mSenseConnector;

    private boolean mConfirmingPassword;

    private final DialogInterface.OnClickListener mOnClickListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mRemoveButton.setEnabled(false);
                if (!mSenseConnector.hasEnrolledFaceSenseUsers()) {
                    Log.e(TAG, "No faces");
                    return;
                }

                mSenseConnector.removeFaceSenseUsers();
                if (mSenseConnector.hasEnrolledFaceSenseUsers()) {
                    mRemoveButton.setEnabled(false);
                }

                // Disable the toggles until the user re-enrolls
                for (Preference preference : mTogglePreferences) {
                    preference.setEnabled(false);
                }

                // Hide the "remove" button and show the "set up face authentication" button.
                mRemoveButtonPref.setVisible(false);
                mEnrollButtonPref.setVisible(true);
            } else {
                mRemoveButton.setEnabled(true);
            }
        }
    };

    public static boolean isAvailable(Context context) {
        ParanoidFaceSenseConnector senseConnector = ParanoidFaceSenseConnector.getInstance(context);
        return senseConnector.isParanoidFaceSenseEnabled();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_face_sense;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(KEY_TOKEN, mToken);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToken = getIntent().getByteArrayExtra(KEY_TOKEN);
        mUserManager = getPrefContext().getSystemService(UserManager.class);
        mFaceManager = getPrefContext().getSystemService(FaceManager.class);
        mUserId = getActivity().getIntent().getIntExtra(
                Intent.EXTRA_USER_ID, UserHandle.myUserId());
        mSenseConnector = ParanoidFaceSenseConnector.getInstance(getContext());

        if (mUserManager.getUserInfo(mUserId).isManagedProfile()) {
            getActivity().setTitle(getActivity().getResources().getString(
                    R.string.security_settings_face_profile_preference_title));
        }

        mKeyguardPref = findPreference(KEY_FACE_UNLOCK_KEYGUARD);
        mKeyguardPref.setOnPreferenceChangeListener(this);

        mAppPref = findPreference(KEY_FACE_APP);
        mAppPref.setOnPreferenceChangeListener(this);

        mConfirmPref = findPreference(KEY_FACE_UNLOCK_CONFIRM);
        mConfirmPref.setOnPreferenceChangeListener(this);

        mBypassPref = findPreference(KEY_FACE_UNLOCK_BYPASS);
        mBypassPref.setOnPreferenceChangeListener(this);
        mTogglePreferences = new ArrayList<>(
                Arrays.asList(mKeyguardPref, mAppPref, mConfirmPref, mBypassPref));

        mRemoveButtonPref = findPreference(KEY_REMOVE_FACE);
        mRemoveButton = ((LayoutPreference) mRemoveButtonPref)
                .findViewById(R.id.security_settings_face_settings_remove_button);
        mRemoveButton.setOnClickListener(this);
        mEnrollButtonPref = findPreference(KEY_ENROLL_FACE);
        mEnrollButton = ((LayoutPreference) mRemoveButtonPref)
                .findViewById(R.id.security_settings_face_settings_enroll_button);
        mEnrollButton.setOnClickListener(this);

        // Don't show keyguard controller for work profile settings.
        if (mUserManager.isManagedProfile(mUserId)) {
            removePreference(KEY_FACE_UNLOCK_KEYGUARD);
            removePreference(KEY_FACE_UNLOCK_BYPASS);
        }

        if (savedInstanceState != null) {
            mToken = savedInstanceState.getByteArray(KEY_TOKEN);
        }
        updatePrefs();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mToken == null && !mConfirmingPassword) {
            // Generate challenge in onResume instead of onCreate, since FaceSettings can be
            // created while Keyguard is showing, in which case the resetLockout revokeChallenge
            // will invalidate the too-early created challenge here.
            final long challenge = mFaceManager.generateChallenge();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);

            mConfirmingPassword = true;
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_face_preference_title),
                    null, null, challenge, mUserId, true /* foregroundOnly */)) {
                Log.e(TAG, "Password not set");
                finish();
            }
        }

        boolean hasEnrolled = mSenseConnector.hasEnrolledFaceSenseUsers();
        mEnrollButtonPref.setVisible(!hasEnrolled);
        mRemoveButtonPref.setVisible(hasEnrolled);
        updatePrefs();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_REQUEST) {
            mConfirmingPassword = false;
            if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                mFaceManager.setActiveUser(mUserId);
                // The pin/pattern/password was set.
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
            }
        }

        if (mToken == null) {
            // Didn't get an authentication, finishing
            getActivity().finish();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!getActivity().isChangingConfigurations()
                && !mConfirmingPassword) {
            // Revoke challenge and finish
            if (mToken != null) {
                final int result = mFaceManager.revokeChallenge();
                if (result < 0) {
                    Log.w(TAG, "revokeChallenge failed, result: " + result);
                }
                mToken = null;
            }
            getActivity().finish();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mKeyguardPref) {
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    FACE_UNLOCK_KEYGUARD_ENABLED, mKeyguardPref.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mAppPref) {
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    FACE_UNLOCK_APP_ENABLED, mAppPref.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mConfirmPref) {
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, mConfirmPref.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mBypassPref) {
            Settings.Secure.putInt(getContext().getContentResolver(),
                    FACE_UNLOCK_DISMISSES_KEYGUARD, mBypassPref.isChecked() ? 1 : 0);
            return true;
        }
        updatePrefs();
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == mRemoveButton) {
            ConfirmRemoveDialog dialog = new ConfirmRemoveDialog();
            dialog.setOnClickListener(mOnClickListener);
            dialog.show(getActivity().getSupportFragmentManager(), ConfirmRemoveDialog.class.getName());
        } else if (v == mEnrollButton) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", FaceEnrollIntroduction.class.getName());
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            getContext().startActivity(intent);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_face;
    }

    private void updatePrefs() {
        boolean unlockKeyguard = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                FACE_UNLOCK_KEYGUARD_ENABLED, 1, UserHandle.USER_CURRENT) != 0;
        boolean appEnabled = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                FACE_UNLOCK_APP_ENABLED, 1, UserHandle.USER_CURRENT) != 0;
        boolean alwaysConfirm = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, 0, UserHandle.USER_CURRENT) != 0;
        int bypassDefault = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthDismissesKeyguard) ? 1 : 0;
        boolean bypassEnabled = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                FACE_UNLOCK_DISMISSES_KEYGUARD, bypassDefault, UserHandle.USER_CURRENT) != 0;
        mKeyguardPref.setChecked(unlockKeyguard);
        mAppPref.setChecked(appEnabled);
        mConfirmPref.setChecked(alwaysConfirm);
        mBypassPref.setChecked(bypassEnabled);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new FaceSettingsVideoPreferenceController(context));
        controllers.add(new FaceSettingsFooterPreferenceController(context));
        return controllers;
    }

    public static class ConfirmRemoveDialog extends InstrumentedDialogFragment {

        private DialogInterface.OnClickListener mOnClickListener;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FACE_REMOVE;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.security_settings_face_settings_remove_dialog_title)
                    .setMessage(R.string.security_settings_face_settings_remove_dialog_details)
                    .setPositiveButton(R.string.delete, mOnClickListener)
                    .setNegativeButton(R.string.cancel, mOnClickListener);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        public void setOnClickListener(DialogInterface.OnClickListener listener) {
            mOnClickListener = listener;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.security_settings_face_sense;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    if (isAvailable(context)) {
                        return buildPreferenceControllers(context, null /* lifecycle */);
                    } else {
                        return null;
                    }
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isAvailable(context);
                }
            };

}
