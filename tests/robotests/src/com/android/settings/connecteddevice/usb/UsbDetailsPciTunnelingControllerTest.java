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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
@EnableFlags({
    android.hardware.usb.flags.Flags.FLAG_ENABLE_PCI_TUNNEL_CONTROL
})
public class UsbDetailsPciTunnelingControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private UsbBackend mUsbBackend;
    @Mock private FragmentActivity mActivity;

    private Context mContext;
    private PreferenceCategory mPreference;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private UsbDetailsPciTunnelingController mUnderTest;
    private UsbDetailsFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new UsbDetailsFragment());
        mContext = RuntimeEnvironment.application;
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);

        mUnderTest = new UsbDetailsPciTunnelingController(mContext, mFragment, mUsbBackend);

        mPreference = new PreferenceCategory(mContext);
        mPreference.setKey(mUnderTest.getPreferenceKey());
        mScreen.addPreference(mPreference);

        mUnderTest.displayPreference(mScreen);
    }

    private SwitchPreferenceCompat getSwitchPreference() {
        return (SwitchPreferenceCompat) mPreference.getPreference(0);
    }

    @Test
    public void displayRefresh_notSupported_shouldHidePrefCategory() {
        when(mUsbBackend.getPciTunnelingControlAllowedStatus())
                .thenReturn(UsbManager.PCI_TUNNEL_CTRL_UNSUPPORTED);
        when(mUsbBackend.isPciTunnelingEnabled()).thenReturn(false);

        mUnderTest.refresh(
                /* connected= */ false, UsbManager.FUNCTION_NONE, POWER_ROLE_NONE, DATA_ROLE_NONE);

        assertThat((PreferenceCategory) mScreen.findPreference(mUnderTest.getPreferenceKey()))
                .isNull();
        assertThat(getSwitchPreference().isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void displayRefresh_disallowed_shouldDisablePrefCategoryWithSummary() {
        ArrayMap<Integer, String> disallowedMap = new ArrayMap<Integer, String>();
        disallowedMap.put(
                UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_FOR_NONADMIN_USER,
                mContext.getString(R.string.usb_pci_tunnel_control_disallowed_nonadmin_summary));
        disallowedMap.put(
                UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_BY_ENTERPRISE_POLICY,
                mContext.getString(
                        R.string.usb_pci_tunnel_control_disallowed_by_enterprise_summary));
        disallowedMap.put(
                UsbManager.PCI_TUNNEL_CTRL_DISALLOWED_BY_APM,
                mContext.getString(R.string.usb_pci_tunnel_control_disallowed_by_apm_summary));

        when(mUsbBackend.isPciTunnelingEnabled()).thenReturn(false);

        for (int ctrlAllowed : disallowedMap.keySet()) {
            when(mUsbBackend.getPciTunnelingControlAllowedStatus()).thenReturn(ctrlAllowed);

            mUnderTest.refresh(
                    /* connected= */ false,
                    UsbManager.FUNCTION_NONE,
                    POWER_ROLE_NONE,
                    DATA_ROLE_NONE);

            assertThat(mPreference.isEnabled()).isFalse();
            assertThat(getSwitchPreference().getSummary().toString())
                    .matches(disallowedMap.get(ctrlAllowed));
        }
    }

    @Ignore("b/447428699")
    @Test
    public void displayRefresh_supported_shouldBeEnabled() {
        when(mUsbBackend.getPciTunnelingControlAllowedStatus())
                .thenReturn(UsbManager.PCI_TUNNEL_CTRL_SUPPORTED);
        when(mUsbBackend.isPciTunnelingEnabled()).thenReturn(true);

        mUnderTest.refresh(
                /* connected= */ false, UsbManager.FUNCTION_NONE, POWER_ROLE_NONE, DATA_ROLE_NONE);

        assertThat(getSwitchPreference().isChecked()).isTrue();
        // TODO(b/447428699) - Something weird is going on here and the preference is never enabled.
        // i.e. The following also fails
        // mPreference.setEnabled(true); assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onClick_callsSetPciTunnelingEnabled() {
        when(mUsbBackend.getPciTunnelingControlAllowedStatus())
                .thenReturn(UsbManager.PCI_TUNNEL_CTRL_SUPPORTED);
        when(mUsbBackend.isPciTunnelingEnabled()).thenReturn(false);

        mUnderTest.refresh(
                /* connected= */ false, UsbManager.FUNCTION_NONE, POWER_ROLE_NONE, DATA_ROLE_NONE);
        assertThat(getSwitchPreference().isChecked()).isFalse();

        getSwitchPreference().performClick();
        verify(mUsbBackend, times(1)).setPciTunnelingEnabled(true);
        assertThat(getSwitchPreference().isChecked()).isTrue();

        getSwitchPreference().performClick();
        verify(mUsbBackend, times(1)).setPciTunnelingEnabled(false);
        assertThat(getSwitchPreference().isChecked()).isFalse();
    }
}
