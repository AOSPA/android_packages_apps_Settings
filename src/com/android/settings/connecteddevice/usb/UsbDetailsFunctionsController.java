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

package com.android.settings.connecteddevice.usb;

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.net.TetheringManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedSelectorWithWidgetPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.LinkedHashMap;
import java.util.Map;

/** This class controls the radio buttons for choosing between different USB functions. */
public class UsbDetailsFunctionsController extends UsbDetailsController
        implements SelectorWithWidgetPreference.OnClickListener {

    private static final String TAG = "UsbFunctionsCtrl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final Map<Long, Integer> FUNCTIONS_MAP = new LinkedHashMap<>();

    static {
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MTP, R.string.usb_use_file_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_RNDIS, R.string.usb_use_tethering);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_MIDI, R.string.usb_use_MIDI);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_PTP, R.string.usb_use_photo_transfers);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_UVC, R.string.usb_use_uvc_webcam);
        FUNCTIONS_MAP.put(UsbManager.FUNCTION_NONE, R.string.usb_use_charging_only);
    }

    @Nullable
    private PreferenceCategory mProfilesContainer;
    private TetheringManager mTetheringManager;
    private Handler mHandler;
    @VisibleForTesting
    OnStartTetheringCallback mOnStartTetheringCallback;
    @VisibleForTesting
    long mPreviousFunction;

    private boolean mRetryingEnableTethering = false;
    private int mTetheringEnableRetryCount = 0;

    // UsbDeviceManager's SET_FUNCTIONS_TIMEOUT_MS is 3000ms. When we pass that timeout, additional
    // setCurrentFunctions() will be called to revert the function, which might complicate the retry
    // flow. To avoid that, we keep the window for retrying shorter.
    @VisibleForTesting
    static final long RETRY_TETHERING_TIMEOUT_MS = 2000;

    @VisibleForTesting
    static final int MAX_TETHERING_ENABLE_RETRY_COUNT = 3;

    private final Runnable mRetryingTetheringTimeout = () -> {
        if (mRetryingEnableTethering) {
            Log.w(TAG, "Timeout waiting for USB function to get back to None for tethering retry");
            mRetryingEnableTethering = false;
            // `refresh()` might be delayed if broadcast is delayed for some reason, so we double
            // check the current data role here.
            // The logic in UsbBackend.getDataRole() already makes sure the port is connected.
            boolean shouldEnable = mUsbBackend.getDataRole() == DATA_ROLE_DEVICE;
            requireNonNull(mProfilesContainer).setEnabled(shouldEnable);
        }
    };

    public UsbDetailsFunctionsController(
            Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context, fragment, backend);
        mTetheringManager = context.getSystemService(TetheringManager.class);
        mOnStartTetheringCallback = new OnStartTetheringCallback();
        mPreviousFunction = mUsbBackend.getCurrentFunctions();
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        refresh(
                /* connected */ false, /* functions */
                mUsbBackend.getDefaultUsbFunctions(),
                /* powerRole */ 0, /* dataRole */
                0);
    }

    /** Gets a switch preference for the particular option, creating it if needed. */
    private RestrictedSelectorWithWidgetPreference getProfilePreference(String key, int titleId) {
        RestrictedSelectorWithWidgetPreference pref =
                requireNonNull(mProfilesContainer).findPreference(key);
        if (pref == null) {
            pref =
                    new RestrictedSelectorWithWidgetPreference(
                            requireNonNull(mProfilesContainer).getContext());
            pref.setKey(key);
            pref.setTitle(titleId);
            pref.setSingleLineTitle(false);
            pref.setOnClickListener(this);
            requireNonNull(mProfilesContainer).addPreference(pref);
        }
        return pref;
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "refresh() connected : "
                            + connected
                            + ", functions : "
                            + functions
                            + ", powerRole : "
                            + powerRole
                            + ", dataRole : "
                            + dataRole);
        }

        if (mRetryingEnableTethering) {
            // Retry tethering if we are "pretty sure" that we get here due to a duplicate tethering
            // request. Any other reasons for the refresh should stop the retry flow.
            mRetryingEnableTethering = false;
            mHandler.removeCallbacks(mRetryingTetheringTimeout);
            if (functions == UsbManager.FUNCTION_NONE) {
                // USB function might have been set to None by `mTetheringManager.stopTethering()`.
                // Double check other conditions, as the function might have been set to None due to
                // a USB disconnection.
                if (connected && dataRole == DATA_ROLE_DEVICE) {
                    Log.w(TAG, "Retrying tethering due to duplicated tethering request.");
                    requireNonNull(mProfilesContainer).setEnabled(true);
                    startTethering();
                    return;
                }
            }
        }

        if (!connected || dataRole != DATA_ROLE_DEVICE) {
            requireNonNull(mProfilesContainer).setEnabled(false);
        } else {
            // Functions are only available in device mode
            requireNonNull(mProfilesContainer).setEnabled(true);
        }
        RestrictedSelectorWithWidgetPreference pref;
        for (long option : FUNCTIONS_MAP.keySet()) {
            int title = FUNCTIONS_MAP.get(option);
            pref = getProfilePreference(UsbBackend.usbFunctionsToString(option), title);
            checkUserRestrictions(option, pref);
            // Only show supported options
            if (mUsbBackend.areFunctionsSupported(option)) {
                if (isAccessoryMode(functions)) {
                    pref.setChecked(UsbManager.FUNCTION_MTP == option);
                } else if (functions == UsbManager.FUNCTION_NCM) {
                    pref.setChecked(UsbManager.FUNCTION_RNDIS == option);
                } else {
                    pref.setChecked(functions == option);
                }
            } else {
                requireNonNull(mProfilesContainer).removePreference(pref);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final long function = UsbBackend.usbFunctionsFromString(preference.getKey());
        final long previousFunction = mUsbBackend.getCurrentFunctions();
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onRadioButtonClicked() function : "
                            + function
                            + ", toString() : "
                            + UsbManager.usbFunctionsToString(function)
                            + ", previousFunction : "
                            + previousFunction
                            + ", toString() : "
                            + UsbManager.usbFunctionsToString(previousFunction));
        }

        if (function != previousFunction
                && !Utils.isMonkeyRunning()
                && !isClickEventIgnored(function, previousFunction)) {
            if (isAuthRequired(function)) {
                requireAuthAndExecute(
                        () -> handleRadioButtonClicked(preference, function, previousFunction));
            } else {
                handleRadioButtonClicked(preference, function, previousFunction);
            }
        }
    }

    private void handleRadioButtonClicked(
            SelectorWithWidgetPreference preference, long function, long previousFunction) {
        mPreviousFunction = previousFunction;

        // Update the UI in advance to make it looks smooth
        final RestrictedSelectorWithWidgetPreference prevPref =
                (RestrictedSelectorWithWidgetPreference)
                        requireNonNull(mProfilesContainer)
                                .findPreference(UsbBackend.usbFunctionsToString(mPreviousFunction));
        if (prevPref != null) {
            prevPref.setChecked(false);
            preference.setChecked(true);
        }

        if (function == UsbManager.FUNCTION_RNDIS || function == UsbManager.FUNCTION_NCM) {
            // We need to have entitlement check for usb tethering, so use API in
            // TetheringManager.
            startTethering();
        } else {
            mUsbBackend.setCurrentFunctions(function);
        }
    }

    private void startTethering() {
        mTetheringManager.startTethering(
                TetheringManager.TETHERING_USB,
                new HandlerExecutor(mHandler),
                mOnStartTetheringCallback);
    }

    private boolean isAuthRequired(long function) {
        // Since webcam and MIDI don't transfer any persistent data over USB
        // don't require authentication.
        return !(function == UsbManager.FUNCTION_UVC || function == UsbManager.FUNCTION_MIDI);
    }

    private boolean isClickEventIgnored(long function, long previousFunction) {
        return isAccessoryMode(previousFunction) && function == UsbManager.FUNCTION_MTP;
    }

    private boolean isAccessoryMode(long function) {
        return (function & UsbManager.FUNCTION_ACCESSORY) != 0;
    }

    private void checkUserRestrictions(long option, RestrictedSelectorWithWidgetPreference pref) {
        String userRestriction = UsbBackend.maybeGetUserRestriction(option);
        if (userRestriction == null) {
            return;
        }
        pref.checkRestrictionAndSetDisabled(userRestriction);
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_functions";
    }

    @VisibleForTesting
    final class OnStartTetheringCallback implements TetheringManager.StartTetheringCallback {

        @Override
        public void onTetheringStarted() {
            Log.i(TAG, "onTetheringStarted()");
            mTetheringEnableRetryCount = 0;
        }

        @Override
        public void onTetheringFailed(int error) {
            Log.w(TAG, "onTetheringFailed() error : " + error);
            long currentOption = mUsbBackend.getCurrentFunctions();

            if (error == TetheringManager.TETHER_ERROR_DUPLICATE_REQUEST
                    && mTetheringEnableRetryCount < MAX_TETHERING_ENABLE_RETRY_COUNT) {
                mTetheringEnableRetryCount++;

                // b/470201989 If we get into this state, the "USB Tethering" option will not be
                // usable anymore. Until we have an official fix, we will try to work-around by
                // stopping tethering and retrying to remove the stuck tethering request.
                mTetheringManager.stopTethering(TetheringManager.TETHERING_USB);
                if (currentOption == UsbManager.FUNCTION_NONE) {
                    // The API to stop tethering will set USB function to None, but if we get to
                    // this by switching from none -> ncm, setting USB function to none again will
                    // be a no-op, we can just start tethering again immediately.
                    startTethering();
                } else {
                    // The API to stop tethering will set USB function to None, so we will start the
                    // retry when the `refresh()` function reports that.
                    //
                    // The UI is disabled during the retry flow to avoid further complications. If
                    // user changes USB function during this flow, the set USB function called by
                    // the UI will race with the one called by `stopTethering()`. This will not
                    // likely to cause any functional issues, but the `previousFunction` might be
                    // set incorrectly, leading to temporary inconsistencies in the UI (which will
                    // eventually self-correct when the final USB state broadcast is received).
                    mRetryingEnableTethering = true;
                    requireNonNull(mProfilesContainer).setEnabled(false);
                    mHandler.postDelayed(mRetryingTetheringTimeout, RETRY_TETHERING_TIMEOUT_MS);
                }
                return;
            }

            Integer titleId = FUNCTIONS_MAP.get(currentOption);
            if (titleId != null) {
                getProfilePreference(UsbBackend.usbFunctionsToString(currentOption), titleId)
                        .setChecked(true);
            } else {
                Log.e(TAG, "onTetheringFailed() failed to find title for currentOption: "
                        + currentOption);
            }

            getProfilePreference(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS),
                    R.string.usb_use_tethering).setChecked(false);
        }
    }
}
