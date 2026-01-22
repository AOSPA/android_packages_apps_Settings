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

package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.flags.Flags;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.Initializer;
import com.android.settings.R;
import com.android.settings.Utils;

/**
 * This class controls the switch for setting if we should enable PCI tunneling over USB Type-C (if
 * supported).
 */
public class UsbDetailsPciTunnelingController extends UsbDetailsController
        implements Preference.OnPreferenceClickListener {
    private static final String PREFERENCE_KEY = "usb_details_pci_tunnel";
    private static final String KEY_USB_PCI_TUNNEL_CONTROL = "usb_pci_tunnel_control";

    private PreferenceCategory mPreferenceCategory;
    private TwoStatePreference mSwitchPreference;

    public UsbDetailsPciTunnelingController(
            Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    @Initializer
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mSwitchPreference = new SwitchPreferenceCompat(mPreferenceCategory.getContext());
        mSwitchPreference.setTitle(R.string.usb_pci_tunnel_control);
        mSwitchPreference.setKey(KEY_USB_PCI_TUNNEL_CONTROL);
        mSwitchPreference.setOnPreferenceClickListener(this);
        mPreferenceCategory.addPreference(mSwitchPreference);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (!Flags.enablePciTunnelControl()) {
            mFragment.getPreferenceScreen().removePreference(mPreferenceCategory);
            return;
        }

        int controlAllowed = mUsbBackend.getPciTunnelingControlAllowedStatus();
        if (controlAllowed != UsbManager.PCI_TUNNEL_CTRL_UNSUPPORTED) {
            mFragment.getPreferenceScreen().addPreference(mPreferenceCategory);
        } else {
            mFragment.getPreferenceScreen().removePreference(mPreferenceCategory);
        }

        boolean checked = mUsbBackend.isPciTunnelingEnabled();
        boolean enabled = controlAllowed == UsbManager.PCI_TUNNEL_CTRL_SUPPORTED;

        mSwitchPreference.setChecked(checked);
        mPreferenceCategory.setEnabled(enabled);

        if (!enabled) {
            if (controlAllowed == UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_FOR_NONADMIN_USER) {
                mSwitchPreference.setSummary(
                        R.string.usb_pci_tunnel_control_disallowed_nonadmin_summary);
            } else if (controlAllowed
                    == UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_BY_ENTERPRISE_POLICY) {
                mSwitchPreference.setSummary(
                        R.string.usb_pci_tunnel_control_disallowed_by_enterprise_summary);
            } else if (controlAllowed == UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_BY_APM) {
                mSwitchPreference.setSummary(
                        R.string.usb_pci_tunnel_control_disallowed_by_apm_summary);
            } else {
                mSwitchPreference.setSummary(R.string.usb_pci_tunnel_control_summary);
            }
        } else {
            mSwitchPreference.setSummary(R.string.usb_pci_tunnel_control_summary);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mUsbBackend.setPciTunnelingEnabled(mSwitchPreference.isChecked());
        return true;
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }
}
