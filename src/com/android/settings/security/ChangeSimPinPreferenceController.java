/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security;

import static android.telephony.PinResult.PIN_RESULT_TYPE_SUCCESS;

import android.content.Context;
import android.telephony.PinResult;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * Controller for changing the SIM card's PIN when it's managed by the user.
 */
public class ChangeSimPinPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceClickListener, EnterSimPinDialogFragment.SimPinEntryListener {
    protected static final String TAG = "SimPinController";

    private static final int STATE_ENTER_CURRENT_PIN = 0;
    private static final int STATE_ENTER_NEW_PIN = 1;
    private static final int STATE_CONFIRM_NEW_PIN = 2;

    private final AutoManagedSimPinHelper mAutoManagedSimPinHelper;
    private final SubscriptionManager mSubscriptionManager;
    private final int mSubId;
    // TODO: http://b/487265436 - store state of controller in case of fragment re-creation.
    private int mState = STATE_ENTER_CURRENT_PIN;

    @Nullable private String mCurrentPin;
    @Nullable private String mNewPin;
    @Nullable private Preference mPreference;
    @Nullable private BaseSimPinFragment mFragment;

    public ChangeSimPinPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mAutoManagedSimPinHelper = new AutoManagedSimPinHelper(mContext);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        // TODO: b/476046816 - Handle multiple SIM slots, probably by taking in the slot index
        // during construction.
        mSubId = mSubscriptionManager.getEnabledSubscriptionId(0);
    }

    /**
     * @return true if the user may change the SIM card PIN: If the PIN requirement is on
     * and the SIM is not enrolled into automatic PIN management.
     */
    @VisibleForTesting
    public boolean mayChangeSimPin() {
        return mAutoManagedSimPinHelper.isIccLockEnabled(mSubId)
                && !mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mayChangeSimPin()) {
            return AVAILABLE;
        }
        return DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mayChangeSimPin());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        resetState();
        showPinEntryDialog(getDialogForState(mState, false));
        return true;
    }

    private void showPinEntryDialog(@Nullable EnterSimPinDialogFragment df) {
        if (df == null) {
            return;
        }
        if (mFragment != null) {
            mFragment.setCurrentListener(this);
            df.showNow(mFragment.getChildFragmentManager(), "CurrentPin");
        }
    }

    public void setFragment(BaseSimPinFragment fragment) {
        mFragment = fragment;
    }

    private void resetState() {
        mState = STATE_ENTER_CURRENT_PIN;
        mCurrentPin = null;
        mNewPin = null;
    }

    @NonNull
    private EnterSimPinDialogFragment getDialogForState(int state, boolean invalidPinInput) {
        return switch (state) {
            case STATE_ENTER_CURRENT_PIN ->
                invalidPinInput ? EnterSimPinDialogFragment.newEnterCurrentPinWithHint()
                        : EnterSimPinDialogFragment.newEnterCurrentPin();
            case STATE_ENTER_NEW_PIN ->
                invalidPinInput ? EnterSimPinDialogFragment.newEnterNewPinWithHint()
                        : EnterSimPinDialogFragment.newEnterNewPin();
            case STATE_CONFIRM_NEW_PIN ->
                invalidPinInput
                        ? EnterSimPinDialogFragment.newConfirmNewPinInstanceWithHint()
                        : EnterSimPinDialogFragment.newConfirmNewPin();
            default -> throw new RuntimeException("Unknown state: " + state);
        };
    }

    @Override
    public void onPinEntered(String pin) {
        boolean isPinValid = mAutoManagedSimPinHelper.isPinValid(pin);
        if (!isPinValid) {
            showPinEntryDialog(getDialogForState(mState, true));
            return;
        }

        if (mState == STATE_ENTER_CURRENT_PIN) {
            mCurrentPin = pin;
            mState = STATE_ENTER_NEW_PIN;
            showPinEntryDialog(getDialogForState(mState, false));
        } else if (mState == STATE_ENTER_NEW_PIN) {
            mNewPin = pin;
            mState = STATE_CONFIRM_NEW_PIN;
            showPinEntryDialog(getDialogForState(mState, false));
        } else if (mState == STATE_CONFIRM_NEW_PIN) {
            if (mNewPin == null || !mNewPin.equals(pin)) {
                // Show error.
                showPinEntryDialog(
                        EnterSimPinDialogFragment.newConfirmNewPinAfterMismatch());
            } else {
                tryChangeSimPin();
            }
        }
    }

    private void tryChangeSimPin() {
        if (mCurrentPin == null || mNewPin == null) {
            Log.e(TAG, "Invalid state: Current or new PIN are uninitialized.");
            return;
        }
        ChangeSimPin changeSimPin = new ChangeSimPin(mCurrentPin, mNewPin);
        ListenableFuture<PinResult> future = ThreadUtils.postOnBackgroundThread(changeSimPin);
        Futures.addCallback(future, new FutureCallback<PinResult>() {
            @Override
            public void onSuccess(PinResult result) {
                Log.d(TAG, "PIN change result: " + result);
                if (result.getResult() != PIN_RESULT_TYPE_SUCCESS) {
                    resetState();
                    showPinEntryDialog(
                            EnterSimPinDialogFragment.newEnterCurrentPin(
                                            result.getAttemptsRemaining()));
                } else {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.sim_change_succeeded),
                            Toast.LENGTH_SHORT).show();
                    resetState();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.w(TAG, "SIM PIN change failed: " + t.getMessage());
                resetState();
            }
        }, mContext.getMainExecutor());
    }

    @Override
    public void onEntryCancelled() {
        resetState();
        if (mFragment != null) {
            mFragment.forceUpdatePreferences();
        }
    }

    private class ChangeSimPin implements Callable<PinResult> {
        @NonNull private final String mCurrentPin;
        @NonNull private final String mNewPin;

        private ChangeSimPin(@NonNull String currentPin, @NonNull String newPin) {
            mCurrentPin = currentPin;
            mNewPin = newPin;
        }

        @Override
        public PinResult call() {
            TelephonyManager telephonyManager = mContext.getSystemService(
                    TelephonyManager.class).createForSubscriptionId(mSubId);
            return telephonyManager.changeIccLockPin(mCurrentPin, mNewPin);
        }
    }
}
